package scalacl
package impl

import com.nativelibs4java.opencl.{ CLMem, CLEvent }
import org.bridj.{ Pointer, PointerIO }
import scala.collection.mutable.ArrayBuffer

private[scalacl] trait DataIO[T] {
  private[scalacl] def typeString: String
  private[scalacl] def bufferCount: Int
  private[scalacl] def foreachScalar(f: ScalarDataIO[_] => Unit): Unit
  private[scalacl] def allocateBuffers(length: Long)(implicit context: Context): Array[ScheduledBuffer[_]] = {
    val buffers = new ArrayBuffer[ScheduledBuffer[_]]
    allocateBuffers(length, buffers)
    buffers.toArray
  }
  
  private[scalacl] def toArray(length: Int, buffers: Array[ScheduledBuffer[_]]): Array[T]
  
  private[scalacl] def allocateBuffers(length: Long, values: Array[T])(implicit context: Context, m: ClassManifest[T]): Array[ScheduledBuffer[_]] = {
    val pointersBuf = new ArrayBuffer[Pointer[_]]
    foreachScalar(io => pointersBuf += Pointer.allocateArray(io.pointerIO, length))
    
    val pointers = pointersBuf.toArray
    for (i <- 0 until length.toInt) {
      set(i, pointers, 0, values(i))
    }
    
    pointers.map(pointer => new ScheduledBuffer(context.context.createBuffer(CLMem.Usage.InputOutput, pointer)))
  }
  private[scalacl] def allocateBuffers(length: Long, out: ArrayBuffer[ScheduledBuffer[_]])(implicit context: Context): Unit
  private[scalacl] def get(index: Long, buffers: Array[Pointer[_]], bufferOffset: Int): T
  private[scalacl] def set(index: Long, buffers: Array[Pointer[_]], bufferOffset: Int, value: T): Unit
}
