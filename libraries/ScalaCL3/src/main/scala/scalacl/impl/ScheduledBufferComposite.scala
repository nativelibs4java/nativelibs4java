package scalacl.impl

import scala.collection.mutable.ArrayBuffer
import com.nativelibs4java.opencl.CLEvent

private[scalacl] trait ScheduledBufferComposite extends ScheduledData {
  
  private[scalacl] def foreachBuffer(f: ScheduledBuffer[_] => Unit): Unit

  override def finish = 
    foreachBuffer(_.finish)
    
  def release = 
    foreachBuffer(_.release)
    
  override def eventCompleted(event: CLEvent) {
    foreachBuffer(_.eventCompleted(event))
  }

  override def startRead(out: ArrayBuffer[CLEvent]) {
    foreachBuffer(_.startRead(out))
  }

  override def startWrite(out: ArrayBuffer[CLEvent]) {
    foreachBuffer(_.startWrite(out))
  }

  override def endRead(event: CLEvent) {
    foreachBuffer(_.endRead(event))
  }

  override def endWrite(event: CLEvent) {
    foreachBuffer(_.endWrite(event))
  }
}