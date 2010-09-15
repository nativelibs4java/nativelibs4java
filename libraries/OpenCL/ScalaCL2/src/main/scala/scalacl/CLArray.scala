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
  val buffer: CLGuardedBuffer[T]
)(
  implicit t: ClassManifest[T],
  context: ScalaCLContext
)
extends CLCol[T]
   with MappableInPlace[T]
{
  type ThisCol[T] = CLArray[T]

  def this(fixedSize: Long)(implicit t: ClassManifest[T], context: ScalaCLContext) =
    this(new CLGuardedBuffer(context.context.createBuffer[T](CLMem.Usage.InputOutput, t.erasure.asInstanceOf[Class[T]], fixedSize)))

  def args = Seq(buffer.buffer)

  val elementClass = t.erasure.asInstanceOf[Class[T]]
  
  def apply(index: Long): CLFuture[T] = buffer(index)
  override def toArray = buffer.toArray

  override def size = CLInstantFuture(buffer.size)
  override def toCLArray = this
  
  private val localSizes = Array(1)

  override def map[V](f: T => V)(implicit v: ClassManifest[V]): CLArray[V] = {
    println("map should not be called directly, you haven't run the compiler plugin or it failed")
    val out = new CLArray[V](buffer.size)
    readBlock {
      val ptr = buffer.toPointer
      val newPtr = if (v.erasure.equals(t.erasure))
        ptr.asInstanceOf[Pointer[V]]
      else
        allocateArray(v.erasure.asInstanceOf[Class[V]], buffer.size).order(context.order)

      var i = 0L
      while (i < buffer.size) {
        val x = ptr.get(i)
        val y = f(x)
        newPtr.set(i, y)
        i += 1
      }
      out.buffer.write(evts => {
        assert(evts.forall(_ == null))
        out.buffer.buffer.write(context.queue, newPtr, false)
      })
    }
    out
  }
  override def mapInPlace(f: CLFunction[T, T]): CLArray[T] = {
    doMap(f, this)
    this
  }
  override def map[V](f: CLFunction[T, V])(implicit v: ClassManifest[V]): CLArray[V] = {
    val out = new CLArray[V](buffer.size)
    doMap(f, out)
    out
  }

  protected def doMap[V](f: CLFunction[T, V], out: CLArray[V]) = {
    val kernel = f.getKernel(context, this, out)
    assert(buffer.size <= Int.MaxValue)
    val globalSizes = Array(buffer.size.asInstanceOf[Int])
    kernel.synchronized {
      kernel.setArgs(new SizeT(buffer.size), buffer.buffer, out.buffer.buffer)
      // TODO cut size bigger than int into global and local sizes
      if (this == out)
        buffer.write(evts => {
          kernel.enqueueNDRange(context.queue, globalSizes, localSizes, evts:_*)
        })
      else
        buffer.read(readEvts => {
          out.buffer.write(writeEvts => {
              kernel.enqueueNDRange(context.queue, globalSizes, localSizes, (readEvts ++ writeEvts):_*)
          })
        })
    }
    out
  }


  override def filter(f: T => Boolean): CLCol[T] = {
    println("filter should not be called directly, you haven't run the compiler plugin or it failed")
    val out = new CLFilteredArray[T](buffer)
    readBlock {
      val ptr = buffer.toPointer
      val newPtr = allocateBooleans(buffer.size).order(context.order)

      var i = 0L
      while (i < buffer.size) {
        val x = ptr.get(i)
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
    val out = new CLFilteredArray(buffer)
    val kernel = f.getKernel(context, this, out)
    assert(buffer.size <= Int.MaxValue)
    val globalSizes = Array(buffer.size.asInstanceOf[Int])
    kernel.synchronized {
      kernel.setArgs(new SizeT(buffer.size), buffer.buffer, out.presence.buffer)
      // TODO cut size bigger than int into global and local sizes
      buffer.read(readEvts => {
          out.presence.write(writeEvts => {
            kernel.enqueueNDRange(context.queue, globalSizes, localSizes, (readEvts ++ writeEvts):_*)
        })
      })
    }
    out
  }

  override def slice(from: Long, to: Long) = new CLArray(buffer.clone(from, to))
  override def take(n: Long) = new CLArray(buffer.clone(0, n))
  override def drop(n: Long) = new CLArray(buffer.clone(n, buffer.size))

  def view: CLView[T, ThisCol[T]] = error("Not implemented")
  def zipWithIndex: ThisCol[(T, Long)] = error("Not implemented")
}
