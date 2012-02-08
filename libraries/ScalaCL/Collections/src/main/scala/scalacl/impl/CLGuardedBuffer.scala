package scalacl

package impl

import scala.collection.generic.CanBuildFrom
import com.nativelibs4java.opencl._

import com.nativelibs4java.opencl._
import org.bridj.Pointer
import org.bridj.Pointer._

object CLGuardedBuffer {
  val debugFakeClone = System.getenv("SCALACL_DEBUG_FAKE_CLONE") == "1"
}

trait CopiableToCLArray[A] {
  def copyTo(other: CLArray[A]): Unit 
}
class CLGuardedBuffer[T](val buffer: CLBuffer[T])(implicit val context: Context, val dataIO: CLDataIO[T]) 
extends CLEventBound
{
  implicit val t = dataIO.t
  lazy val elementClass = t.erasure.asInstanceOf[Class[T]]
  
  def this(size: Long)(implicit context: Context, dataIO: CLDataIO[T]) = {
    this(context.context.createBuffer(CLMem.Usage.InputOutput, dataIO.pointerIO, size))
    //clear
  }
    
  def this(values: Array[T])(implicit context: Context, dataIO: CLDataIO[T]) = {
    this(context.context.createBuffer(CLMem.Usage.InputOutput, {
        val ptr = allocateArray(dataIO.pointerIO, values.length)
        ptr.setArray(values)
        ptr
      }, 
      true
    ))
  }

  def clear = {
    write(evts => dataIO.clear(buffer, evts:_*))
  }
  def release =  {
    releaseEvents
    buffer.release
  }
  
  def args = Seq(buffer)

  def apply(index: Long): CLFuture[T] = {
    val b: Pointer[T] = allocate(elementClass)
    new CLPointerFuture[T](b, read(evts => buffer.read(context.queue, index, 1, b, false, evts:_*)))
  }

  def update(values: Array[T]): CLGuardedBuffer[T] = {
    val b: Pointer[T] = pointerToArray(values)
    write(evts => buffer.write(context.queue, 0, b.getValidElements, b, false, evts:_*))
    this
  }

  def withReadablePointer[V](f: Pointer[T] => V): V = withPointer(CLMem.MapFlags.Read, f)
  def withWritablePointer[V](f: Pointer[T] => V): V = withPointer(CLMem.MapFlags.Write, f)
  def withReadableWritablePointer[V](f: Pointer[T] => V): V = withPointer(CLMem.MapFlags.ReadWrite, f)
  
  def withReadableMappedPointer[V](f: Pointer[T] => V): Option[V] = withMappedPointer(CLMem.MapFlags.Read, f)
  def withWritableMappedPointer[V](f: Pointer[T] => V): Option[V] = withMappedPointer(CLMem.MapFlags.Write, f)
  def withReadableWritableMappedPointer[V](f: Pointer[T] => V): Option[V] = withMappedPointer(CLMem.MapFlags.ReadWrite, f)
  
  protected def withPointer[V](usage: CLMem.MapFlags, f: Pointer[T] => V): V = this synchronized {
    withMappedPointer(usage, f).getOrElse {
      var copied = toPointer
      try {
        f(copied)
      } finally {
        copied.release
      }
    }
  }
  
  protected def withMappedPointer[V](usage: CLMem.MapFlags, f: Pointer[T] => V): Option[V] = this synchronized {
    var mapped: Pointer[T] = null
    try {
      //println("Map...")
      CLEventBound.syncBlock(Array(this), Array(), evts => {
        mapped = buffer.map(context.queue, usage, evts:_*)
        null
      })
      //println("Map succeeded !")
      Some(f(mapped))
    } catch {
      case ex: CLException.MapFailure =>
        //println("Map failed !")
        None
    } finally {
      if (mapped != null)
        buffer.unmap(context.queue, mapped)
    }
  }
  
  def update(index: Long, value: T): Unit = {
    val b: Pointer[T] = allocate(elementClass)
    b.set(value)
    write(evts => buffer.write(context.queue, index, 1, b, false, evts:_*))
  }

  def toPointer: Pointer[T] =
    readValue(evts => buffer.read(context.queue, evts:_*))

  def toArray: Array[T] = toPointer.getArray.asInstanceOf[Array[T]]
  
  val size = buffer.getElementCount

  
  override def clone: CLGuardedBuffer[T] = {
    // TODO allocate and copy on write, with value groups !
    if (CLGuardedBuffer.debugFakeClone)
      this
    else {
      val out = new CLGuardedBuffer(context.context.createBuffer(CLMem.Usage.InputOutput, elementClass, size))
      copyTo(out)
      out
    }
  }

  def copyTo(out: CLGuardedBuffer[T]): Unit = if (this ne out) {
    assert(buffer.getByteCount == out.buffer.getByteCount)
    CLEventBound.syncBlock(Array(this), Array(out), evts => {
      buffer.copyTo(context.queue, 0, size /* * buffer.getElementSize*/, out.buffer, 0, evts:_*)
    })
  }
  def clone(start: Long, end: Long): CLGuardedBuffer[T] = {
    val newSize = end - start
    assert(newSize > 0)
    assert(start > 0)
    assert(end <= size)
    val out = new CLGuardedBuffer(context.context.createBuffer(CLMem.Usage.InputOutput, elementClass, newSize))
    CLEventBound.syncBlock(Array(this), Array(out), evts => {
      buffer.copyTo(context.queue, start, newSize, out.buffer, 0, readEvents:_*)
    })
    out
  }
}
