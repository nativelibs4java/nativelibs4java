/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package protocl
import com.nativelibs4java.opencl._
import org.bridj.Pointer
import org.bridj.Pointer._

class CLGuardedBuffer[T](val buffer: CLBuffer[T])(implicit val dataIO: CLDataIO[T], context: ScalaCLContext) extends CLEventBound {
  implicit val t = dataIO.t
  lazy val elementClass = t.erasure.asInstanceOf[Class[T]]
  
  def this(size: Long)(implicit dataIO: CLDataIO[T], context: ScalaCLContext) =
    this(context.context.createBuffer(CLMem.Usage.InputOutput, dataIO.pointerIO, size))
    
  def this(values: Array[T])(implicit dataIO: CLDataIO[T], context: ScalaCLContext) = {
    this(context.context.createBuffer(CLMem.Usage.InputOutput, {
        val ptr = allocateArray(dataIO.pointerIO, values.length)
        ptr.setArray(values)
        ptr
      }, 
      true
    ))
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
    val out = new CLGuardedBuffer(context.context.createBuffer(CLMem.Usage.InputOutput, elementClass, size))
    read(readEvents => {
        out.write(writeEvents => {
            assert(writeEvents.isEmpty)
            buffer.copyTo(context.queue, 0, size, out.buffer, 0, readEvents:_*)
        })
    })
    out
  }

  def clone(start: Long, end: Long): CLGuardedBuffer[T] = {
    val newSize = end - start
    assert(newSize > 0)
    assert(start > 0)
    assert(end <= size)
    val out = new CLGuardedBuffer(context.context.createBuffer(CLMem.Usage.InputOutput, elementClass, newSize))
    read(readEvents => {
      out.write(writeEvents => {
        assert(writeEvents.isEmpty)
        buffer.copyTo(context.queue, start, newSize, out.buffer, 0, readEvents:_*)
      })
    })
    out
  }
}