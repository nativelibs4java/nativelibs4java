package scalacl
package impl

import com.nativelibs4java.opencl.CLMem
import org.bridj.{ Pointer, PointerIO }
import scala.collection.mutable.ArrayBuffer

abstract class ScalarDataIO[T : Manifest](io: PointerIO[_]) extends DataIO[T] {
  override val typeString = implicitly[ClassManifest[T]].erasure.getSimpleName
  override def bufferCount = 1
  
  private[scalacl] val pointerIO: PointerIO[T] = io.asInstanceOf[PointerIO[T]]
  
  private[scalacl] def foreachScalar(f: ScalarDataIO[_] => Unit): Unit =
    f(this)
    
  override def toArray(length: Int, buffers: Array[ScheduledBuffer[_]]): Array[T] = {
    val Array(buffer: ScheduledBuffer[T]) = buffers
    buffer.read().getArray.asInstanceOf[Array[T]]
  }
  
  override def allocateBuffers(length: Long, values: Array[T])(implicit context: Context, m: ClassManifest[T]): Array[ScheduledBuffer[_]] = {
    val pointer = Pointer.pointerToArray[T](values)
    Array(new ScheduledBuffer(context.context.createBuffer(CLMem.Usage.InputOutput, pointer)))
  }
  private[scalacl] def allocateBuffer(length: Long)(implicit context: Context) =
    context.context.createBuffer(CLMem.Usage.InputOutput, pointerIO, length)
    
  override def allocateBuffers(length: Long, out: ArrayBuffer[ScheduledBuffer[_]])(implicit context: Context) = {
    out += new ScheduledBuffer(allocateBuffer(length))
  }
}

object IntDataIO extends ScalarDataIO[Int](PointerIO.getIntInstance) {
  override def get(index: Long, buffers: Array[Pointer[_]], bufferOffset: Int) =
    buffers(bufferOffset).getIntAtOffset(index * 4)
  override def set(index: Long, buffers: Array[Pointer[_]], bufferOffset: Int, value: Int) =
    buffers(bufferOffset).setIntAtOffset(index * 4, value)
}

object BooleanDataIO extends ScalarDataIO[Boolean](PointerIO.getBooleanInstance) {
  override def get(index: Long, buffers: Array[Pointer[_]], bufferOffset: Int) =
    buffers(bufferOffset).getByteAtOffset(index * 1) != 0
  override def set(index: Long, buffers: Array[Pointer[_]], bufferOffset: Int, value: Boolean) =
    buffers(bufferOffset).setByteAtOffset(index * 1, (if (value) 1 else 0).asInstanceOf[Byte])
}