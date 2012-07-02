package scalacl
package impl

import com.nativelibs4java.opencl.{ CLMem, CLEvent }
import org.bridj.{ Pointer, PointerIO }
import scala.collection.mutable.ArrayBuffer

trait DataIO[T] {
  def typeString: String
  def bufferCount: Int
  def foreachScalar(f: ScalarDataIO[_] => Unit): Unit
  def allocateBuffers(length: Long)(implicit context: Context): Array[ScheduledBuffer[_]] = {
    val buffers = new ArrayBuffer[ScheduledBuffer[_]]
    allocateBuffers(length, buffers)
    buffers.toArray
  }
  
  def toArray(length: Int, buffers: Array[ScheduledBuffer[_]]): Array[T]
  
  def allocateBuffers(length: Long, values: Array[T])(implicit context: Context, m: ClassManifest[T]): Array[ScheduledBuffer[_]] = {
    val pointersBuf = new ArrayBuffer[Pointer[_]]
    foreachScalar(io => pointersBuf += Pointer.allocateArray(io.pointerIO, length))
    
    val pointers = pointersBuf.toArray
    for (i <- 0 until length.toInt) {
      set(i, pointers, 0, values(i))
    }
    
    pointers.map(pointer => new ScheduledBuffer(context.context.createBuffer(CLMem.Usage.InputOutput, pointer)))
  }
  def allocateBuffers(length: Long, out: ArrayBuffer[ScheduledBuffer[_]])(implicit context: Context): Unit
  def get(index: Long, buffers: Array[Pointer[_]], bufferOffset: Int): T
  def set(index: Long, buffers: Array[Pointer[_]], bufferOffset: Int, value: T): Unit
}
