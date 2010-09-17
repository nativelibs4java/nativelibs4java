/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package scalacl

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
  context: ScalaCLContext
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
  lazy val presencePrefixSum = new CLGuardedBuffer[Long](buffersSize)

  def this(buffers: Array[CLGuardedBuffer[Any]])(implicit dataIO: CLDataIO[T], context: ScalaCLContext) = this(
    buffers, null)

  //def args = Seq(buffers.buffer, presence.buffer)

  override def clone =
    new CLFilteredArray[T](buffers.map(_.clone), presence.clone)
  
  def clone(newStart: Long, newEnd: Long) =
    new CLFilteredArray[T](buffers.map(_.clone(newStart, newEnd)), presence.clone(newStart, newEnd))

  def view: CLView[T, ThisCol[T]] = notImp
  def slice(from: Long, to: Long): CLCol[T] = notImp
  
  def zipWithIndex: ThisCol[(T, Long)] = notImp
  def toCLArray: CLArray[T] = {
    val prefixSum = updatedPresencePrefixSum
    val size = this.size.get
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
  def buffersSize = buffers(0).size
  def size: CLFuture[Long] = {
    //error("Filtered array size not implemented yet, needs prefix sum implementation")
    val ps = updatedPresencePrefixSum
    ps(buffersSize - 1)
    //new CLInstantFuture(ps.toArray.last)
  }

  protected def updateFun(f: T => T): CLFilteredArray[T] =
    doMapFun(f, this)

  protected def mapFun[V](f: T => V)(implicit vIO: CLDataIO[V]): CLFilteredArray[V] =
    doMapFun(f, new CLFilteredArray[V](vIO.createBuffers(buffersSize), presence.clone))
          
  protected def doMapFun[V](f: T => V, out: CLFilteredArray[V])(implicit vIO: CLDataIO[V]): CLFilteredArray[V] = {
    println("map should not be called directly, you haven't run the compiler plugin or it failed")
    readBlock {
      val ptrs = buffers.map(_.toPointer)
      val presencePtr = presence.toPointer

      val newPtrs = if (dataIO.t.erasure.equals(vIO.t.erasure))
        ptrs
      else
        buffers.map(b => allocateArray(b.t.erasure.asInstanceOf[Class[Any]], buffersSize).order(context.order))

      var i = 0L
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
  override def update(f: CLFunction[T, T]): CLFilteredArray[T] = {
    if (f.expression == null)
      updateFun(f.function)
    else {
      this.synchronized {
        prefixSumUpToDate = false
        doMap(f, this)
      }
      this
    }
  }
  override def map[V](f: CLFunction[T, V]): CLFilteredArray[V] = {
    implicit val kIO = f.aIO
    implicit val vIO = f.bIO
    if (f.expression == null)
      mapFun(f.function)
    else {
      val out = new CLFilteredArray[V](vIO.createBuffers(buffersSize), presence.clone)
      doMap(f, out)
      out
    }
  }

  private val localSizes = Array(1)

  protected def doMap[V](f: CLFunction[T, V], out: CLFilteredArray[V]) = {
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

  protected def refineFilterFun(f: T => Boolean): CLFilteredArray[T] =
    doFilterFun(f, this)

  protected def filterFun(f: T => Boolean): CLFilteredArray[T] =
    doFilterFun(f, new CLFilteredArray[T](buffers.map(_.clone), new CLGuardedBuffer[Boolean](buffersSize)))

  protected def doFilterFun(f: T => Boolean, out: CLFilteredArray[T]): CLFilteredArray[T] = {
    println("filter should not be called directly, you haven't run the compiler plugin or it failed")
    //val out = new CLFilteredArray(values.clone, new CLGuardedBuffer[Boolean](values.size), start, end)
    
    val ptrs = buffers.map(_.toPointer)
    val presencePtr = presence.toPointer
    val newPresencePtr = allocateBooleans(buffersSize).order(context.order)

    var i = 0L
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
  
  override def filter(f: CLFunction[T, Boolean]): CLFilteredArray[T] =
    if (f.expression == null)
      filterFun(f.function)
    else
      filter(f, new CLFilteredArray[T](buffers.map(_.clone), new CLGuardedBuffer[Boolean](buffersSize)))

  override def refineFilter(f: CLFunction[T, Boolean]): CLFilteredArray[T] = {
    implicit val vIO = f.bIO
    if (f.expression == null)
      refineFilterFun(f.function)
    else
      this.synchronized {
        prefixSumUpToDate = false
        filter(f, this)
      }
  }

  protected def filter(f: CLFunction[T, Boolean], out: CLFilteredArray[T]): CLFilteredArray[T] = {
    //val out = new CLFilteredArray(buffers.map(_.clone), new CLGuardedBuffer[Boolean](longSize))

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
