package scalacl.impl

import scala.collection.mutable.ArrayBuffer
import com.nativelibs4java.opencl.CLEvent

private[scalacl] trait ScheduledBufferComposite extends ScheduledData {
  
  def foreachBuffer(f: ScheduledBuffer[_] => Unit): Unit

  def finish = 
    foreachBuffer(_.finish)
    
  def release = 
    foreachBuffer(_.release)
    
  def eventCompleted(event: CLEvent): Unit =
    foreachBuffer(_.eventCompleted(event))

  def startRead(out: ArrayBuffer[CLEvent]): ScheduledData = {
    foreachBuffer(_.startRead(out))
    this
  }

  def startWrite(out: ArrayBuffer[CLEvent]): ScheduledData = {
    foreachBuffer(_.startWrite(out))
    this
  }

  def endRead(event: CLEvent): Unit =
    foreachBuffer(_.endRead(event))

  def endWrite(event: CLEvent): Unit =
    foreachBuffer(_.endWrite(event))
}