/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package scalacl
import com.nativelibs4java.opencl._

import org.bridj.Pointer
import org.bridj.Pointer._
import org.bridj.SizeT

class CLArray[T](
  val buffers: Array[CLGuardedBuffer[Any]]
)(
  implicit val context: ScalaCLContext,
  dataIO: CLDataIO[T]
)
extends CLCol[T]
   with CLUpdatableCol[T]
{
  type ThisCol[T] = CLArray[T]
  lazy val buffersList = buffers.toList

  implicit val t = dataIO.t
  //def args = Seq(buffer.buffer)

  val elementClasses = buffers.map(_.elementClass)//t.erasure.asInstanceOf[Class[T]]
  
  def apply(index: Long): CLFuture[T] = dataIO.extract(buffers, index)
  override def toArray = dataIO.toArray(buffers)

  override def size = new CLInstantFuture(buffers(0).size)
  override def toCLArray = this
  
  private val localSizes = Array(1)

  override def update(f: T => T): CLArray[T] =
    doMap(f, this)

  override def map[V](f: T => V)(implicit vIO: CLDataIO[V]): CLArray[V] = {
    val out = clArray[V](longSize)
    implicit val t = out.t
    doMap(f, out)
  }
          
  protected def doMap[V](f: T => V, out: CLArray[V])(implicit vIO: CLDataIO[V]): CLArray[V] = {

    println("map should not be called directly, you haven't run the compiler plugin or it failed")
    readBlock {
      val ptrs = buffers.map(_.toPointer)
      val newPtrs = if (dataIO.t.erasure.equals(vIO.t.erasure))
        ptrs
      else
        buffers.map(b => allocateArray(b.t.erasure.asInstanceOf[Class[Any]], longSize).order(context.order))
      
      var i = 0L
      while (i < longSize) {
        val x = dataIO.extract(ptrs, i)
        val y = f(x)
        vIO.store(y, newPtrs, i)
        i += 1
      }
      out.buffers.zip(newPtrs).foreach { case (buffer, newPtr) =>
        buffer.write(evts => {
          assert(evts.forall(_ == null))
          buffer.buffer.asInstanceOf[CLBuffer[Any]].write(context.queue, newPtr.asInstanceOf[Pointer[Any]], false)
        })
      }
    }
    out
  }
  override def update(f: CLFunction[T, T]): CLArray[T] = {
    doMap(f, this)
    this
  }
  override def map[V](f: CLFunction[T, V])(implicit vIO: CLDataIO[V]): CLArray[V] = {
    val out = clArray[V](longSize)
    doMap(f, out)
    out
  }

  protected def doMap[V](f: CLFunction[T, V], out: CLArray[V]) = {
    val kernel = f.getKernel(context, this, out, "array")
    assert(longSize <= Int.MaxValue)
    val globalSizes = Array(longSize.asInstanceOf[Int])
    kernel.synchronized {
      kernel.setArgs((Seq(new SizeT(longSize)) ++ buffers.map(_.buffer) ++ out.buffers.map(_.buffer)):_*)
      // TODO cut size bigger than int into global and local sizes
      syncAll(buffersList)(out.buffersList)(evts => {
        kernel.enqueueNDRange(context.queue, globalSizes, localSizes, evts:_*)
      })
    }
    out
  }


  override def filter(f: T => Boolean): CLCol[T] = {
    println("filter should not be called directly, you haven't run the compiler plugin or it failed")
    val out = new CLFilteredArray[T](buffers)
    readBlock {
      val ptrs = buffers.map(_.toPointer)
      val newPtr = allocateBooleans(longSize).order(context.order)

      var i = 0L
      while (i < longSize) {
        val x = dataIO.extract(ptrs, i)
        val b = f(x)
        newPtr.set(i, b)
        i += 1
      }
      out.presence.write(evts => {
        assert(evts.forall(_ == null))
        out.presence.buffer.write(context.queue, newPtr.asInstanceOf[Pointer[Boolean]], false)
      })
    }
    out
  }
  override def filter(f: CLFunction[T, Boolean]): CLFilteredArray[T] = {
    val out = new CLFilteredArray[T](buffers)
    val kernel = f.getKernel(context, this, out, "array")
    assert(longSize <= Int.MaxValue)
    val globalSizes = Array(longSize.asInstanceOf[Int])
    kernel.synchronized {
      
      kernel.setArgs((Seq(new SizeT(longSize)) ++ buffers.map(_.buffer) ++ Seq(out.presence.buffer)):_*)
      // TODO cut size bigger than int into global and local sizes
      syncAll(buffersList)(List(out.presence.asInstanceOf[CLGuardedBuffer[Any]]))(evts => {
        kernel.enqueueNDRange(context.queue, globalSizes, localSizes, evts:_*)
      })
    }
    out
  }

  def longSize = buffers(0).size
  override def slice(from: Long, to: Long) = new CLArray(buffers.map(_.clone(from, to)))
  override def take(n: Long) = new CLArray(buffers.map(_.clone(0, n)))
  override def drop(n: Long) = new CLArray(buffers.map(_.clone(n, longSize)))

  def view: CLView[T, ThisCol[T]] = new CLArrayView[T, T, CLArray[T]](this, 0, longSize, null, null, null, null)
  def zipWithIndex: ThisCol[(T, Long)] = notImp
}
