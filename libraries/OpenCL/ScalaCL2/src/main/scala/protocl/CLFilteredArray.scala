/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package protocl

import com.nativelibs4java.opencl._
import org.bridj.Pointer
import org.bridj.Pointer._
import org.bridj.SizeT
import scala.collection.JavaConversions._

class CLFilteredArray[T](
  val buffers: Array[CLGuardedBuffer[Any]],
  initialPresence: CLGuardedBuffer[Boolean]
)(
  implicit dataIO: CLDataIO[T],
  val context: ScalaCLContext
) 
extends CLCol[T] 
   with CLUpdatableFilteredCol[T]
   with CLUpdatableCol[T]
{
  type ThisCol[T] = CLFilteredArray[T]
  
  val presence = if (initialPresence == null)
    new CLGuardedBuffer[Boolean](buffersSize)
  else
    initialPresence

  lazy val buffersList = buffers.toList
  lazy val presencePrefixSum = new CLGuardedBuffer[Int](buffersSize)

  def this(buffers: Array[CLGuardedBuffer[Any]])(implicit dataIO: CLDataIO[T], context: ScalaCLContext) = this(
    buffers, null)

  //def args = Seq(buffers.buffer, presence.buffer)

  override def clone =
    new CLFilteredArray[T](buffers.map(_.clone), presence.clone)
  
  def clone(newStart: Int, newEnd: Int) =
    new CLFilteredArray[T](buffers.map(_.clone(newStart, newEnd)), presence.clone(newStart, newEnd))

  override def slice(from: Int, to: Int): CLCol[T] = notImp
  
  def toCLArray: CLArray[T] = {
    val prefixSum = updatedPresencePrefixSum
    val size = this.size
    new CLArray(buffers.map(b => {
      val out = new CLGuardedBuffer[Any](size)(b.dataIO, context)
      ScalaCLUtils.copyPrefixed(size, prefixSum, b, out)(b.t, context)
      out
    }))
  }

  var prefixSumUpToDate = false
  def updatedPresencePrefixSum = this.synchronized {
    if (!prefixSumUpToDate) {
      ScalaCLUtils.prefixSum(presence, presencePrefixSum)
      prefixSumUpToDate = true
    }
    presencePrefixSum
  }
  def buffersSize: Int = buffers(0).size.toInt
  def sizeFuture: CLFuture[Int] = {
    //error("Filtered array size not implemented yet, needs prefix sum implementation")
    val ps = updatedPresencePrefixSum
    ps(buffersSize - 1)
    //new CLInstantFuture(ps.toArray.last)
  }

  override def update(f: T => T)(implicit dataIO: CLDataIO[T]): CLFilteredArray[T] =
    doMap(f, this)

  override def map[V](f: T => V)(implicit dataIO: CLDataIO[T], vIO: CLDataIO[V]): CLFilteredArray[V] =
    doMap(f, new CLFilteredArray[V](vIO.createBuffers(buffersSize), presence.clone))
          
  protected def doMap[V](f: T => V, out: CLFilteredArray[V])(implicit dataIO: CLDataIO[T], vIO: CLDataIO[V]): CLFilteredArray[V] = {
    //println("map should not be called directly, you haven't run the compiler plugin or it failed")
    readBlock {
      val ptrs = buffers.map(_.toPointer)
      val presencePtr = presence.toPointer

      val newPtrs = if (dataIO.t.erasure.equals(vIO.t.erasure))
        ptrs
      else
        buffers.map(b => allocateArray(b.t.erasure.asInstanceOf[Class[Any]], buffersSize).order(context.order))

      var i = 0
      while (i < buffersSize) {
        if (presencePtr.get(i).booleanValue) {
          val x = dataIO.extract(ptrs, i)
          val y = f(x)
          vIO.store(y, newPtrs, i)
        }
        i += 1
      }
      syncAll(Nil)(out.buffersList)(evts => {
        assert(evts.forall(_ == null))
        var iBuffer = 0
        var evt: CLEvent = null
        while (iBuffer < buffers.length)
          evt = out.buffers(iBuffer).buffer.write(context.queue, newPtrs(iBuffer), false, (Seq(evt) ++ evts):_*)
        evt
      })
    }
    out
  }
  override def updateFun(f: CLFunction[T, T])(implicit dataIO: CLDataIO[T]): CLFilteredArray[T] = {
    if (f.isOnlyInScalaSpace)
      update(f.function)
    else {
      this.synchronized {
        prefixSumUpToDate = false
        doMap(f, this)
      }
      this
    }
  }
  override def mapFun[V](f: CLFunction[T, V])(implicit dataIO: CLDataIO[T], vIO: CLDataIO[V]): CLFilteredArray[V] = {
    if (f.isOnlyInScalaSpace)
      map(f.function)
    else {
      val out = new CLFilteredArray[V](vIO.createBuffers(buffersSize), presence.clone)
      doMap(f, out)
      out
    }
  }

  private val localSizes = Array(1)

  protected def doMap[V](f: CLFunction[T, V], out: CLFilteredArray[V])(implicit dataIO: CLDataIO[T], vIO: CLDataIO[V]) = {
    val kernel = f.getKernel(context, this, out, "filtered")
    assert(buffersSize <= Int.MaxValue)
    val globalSizes = Array(buffersSize.toInt)
    kernel.synchronized {
      kernel.setArgs((Seq(new SizeT(buffersSize)) ++ buffers.map(_.buffer) ++ Seq(presence.buffer) ++ out.buffers.map(_.buffer)):_*)
      // TODO cut size bigger than int into global and local sizes
      syncAll(presence :: buffersList)(out.buffersList)(evts => {
        kernel.enqueueNDRange(context.queue, globalSizes, localSizes, evts:_*)
      })
    }
    out
  }

  override def refineFilter(f: T => Boolean)(implicit dataIO: CLDataIO[T]): CLFilteredArray[T] =
    doFilter(f, this)

  override def filter(f: T => Boolean)(implicit dataIO: CLDataIO[T]): CLFilteredArray[T] =
    doFilter(f, new CLFilteredArray[T](buffers.map(_.clone), new CLGuardedBuffer[Boolean](buffersSize)))

  protected def doFilter(f: T => Boolean, out: CLFilteredArray[T])(implicit dataIO: CLDataIO[T]): CLFilteredArray[T] = {
    //println("filter should not be called directly, you haven't run the compiler plugin or it failed")
    //val out = new CLFilteredArray(values.clone, new CLGuardedBuffer[Boolean](values.size), start, end)
    
    val ptrs = buffers.map(_.toPointer)
    val presencePtr = presence.toPointer
    val newPresencePtr = allocateBooleans(buffersSize).order(context.order)

    var i = 0
    while (i < buffersSize) {
      val v = dataIO.extract(ptrs, i)
      newPresencePtr.set(i, presencePtr.get(i).booleanValue && f(v))
      i += 1
    }
    out.presence.write(evts => {
      assert(evts.forall(_ == null))
      out.presence.buffer.write(context.queue, newPresencePtr.asInstanceOf[Pointer[Boolean]], false)
    })
    out
  }
  
  override def filterFun(f: CLFunction[T, Boolean])(implicit dataIO: CLDataIO[T]): CLFilteredArray[T] =
    if (f.isOnlyInScalaSpace)
      filter(f.function)
    else
      filterFun(f, new CLFilteredArray[T](buffers.map(_.clone), new CLGuardedBuffer[Boolean](buffersSize)))

  override def refineFilterFun(f: CLFunction[T, Boolean])(implicit dataIO: CLDataIO[T]): CLFilteredArray[T] = {
    implicit val vIO = f.bIO
    if (f.isOnlyInScalaSpace)
      refineFilter(f.function)
    else
      this.synchronized {
        prefixSumUpToDate = false
        filterFun(f, this)
      }
  }

  protected def filterFun(f: CLFunction[T, Boolean], out: CLFilteredArray[T])(implicit dataIO: CLDataIO[T]): CLFilteredArray[T] = {
    //val out = new CLFilteredArray(buffers.map(_.clone), new CLGuardedBuffer[Boolean](size))

    val kernel = f.getKernel(context, this, out, "filtered")
    assert(buffersSize <= Int.MaxValue)
    val globalSizes = Array(buffersSize.toInt)
    kernel.synchronized {
      kernel.setArgs((Seq(new SizeT(buffersSize)) ++ buffers.map(_.buffer) ++ Seq(presence.buffer, out.presence.buffer)):_*)
      // TODO cut size bigger than int into global and local sizes
      syncAll(presence :: buffersList)(out.presence :: Nil)(evts => {
        kernel.enqueueNDRange(context.queue, globalSizes, localSizes, evts:_*)
      })
    }
    out
  }
}
