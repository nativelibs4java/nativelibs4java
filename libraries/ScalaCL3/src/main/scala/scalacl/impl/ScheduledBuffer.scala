package scalacl
package impl

import com.nativelibs4java.opencl.CLBuffer
import scala.collection.mutable.ArrayBuffer
import com.nativelibs4java.opencl.CLEvent
import org.bridj.Pointer
import com.nativelibs4java.opencl.CLMem

/*

ScheduledBuffer:
- shallowClones: ArrayBuffer[ScheduledBuffer]
- cloneModel: ScheduledBuffer

 */

private[scalacl] class ScheduledBuffer[T](initialBuffer: CLBuffer[T])(implicit context: Context) extends DefaultScheduledData {

  private var buffer_ = initialBuffer
  private var lazyClones = new ArrayBuffer[ScheduledBuffer[T]]
  private var lazyCloneModel: ScheduledBuffer[T] = _

  def buffer = buffer_
  
  def release() = this synchronized {
    if (lazyCloneModel == null)
      buffer.release
  }
  override def clone: ScheduledBuffer[T] = this synchronized {
    if (lazyCloneModel != null) {
      lazyCloneModel.clone
    } else {
      val c = new ScheduledBuffer(buffer)
      c.lazyCloneModel = this
      lazyClones += c
      c
    }
  }

  private def performClone() = this synchronized {
    if (lazyCloneModel != null) {
      lazyCloneModel synchronized {
        buffer_ = context.context.createBuffer(CLMem.Usage.InputOutput, initialBuffer.getIO, initialBuffer.getElementCount)
        lazyCloneModel.lazyClones -= this
        lazyCloneModel = null
        ScheduledData.schedule(
          Array(lazyCloneModel),
          Array(this),
          eventsToWaitFor => initialBuffer.copyTo(context.queue, buffer, eventsToWaitFor: _*))
      }
    }
  }
  
  override def startWrite(out: ArrayBuffer[CLEvent]) = this synchronized {
    performClone
    lazyClones.toArray.foreach(_.performClone)
    super.startWrite(out)
  }

  def write(in: Pointer[T]) {
    ScheduledData.schedule(
        Array(),
        Array(this),
        eventsToWaitFor => buffer.write(context.queue, in, false, eventsToWaitFor: _*)
  	)
  }
  def read(): Pointer[T] = {
    val queue = context.queue
    val p = buffer.allocateCompatibleMemory(queue.getDevice)
    
    val event = ScheduledData.schedule(
      Array(this),
      Array[ScheduledData](),
      eventsToWaitFor => buffer.read(queue, p, true, eventsToWaitFor: _*))
    p
  }
  def read(out: ArrayBuffer[CLEvent]): Pointer[T] = {
    val queue = context.queue
    val p = buffer.allocateCompatibleMemory(queue.getDevice)
    
    val event = ScheduledData.schedule(
      Array(this),
      Array[ScheduledData](),
      eventsToWaitFor => buffer.read(queue, p, false, eventsToWaitFor: _*))
    if (out != null)
      out += event
    p
  }
  
  def read(p: Pointer[T]) {
    val event = ScheduledData.schedule(
      Array(this),
      Array(),
      eventsToWaitFor => buffer.read(context.queue, p, false, eventsToWaitFor: _*))
    event.waitFor
  }
}
