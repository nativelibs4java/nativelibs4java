package scalacl.impl
import com.nativelibs4java.opencl._
import java.util.concurrent.locks._
import collection.mutable.ArrayBuffer

private[scalacl] object ScheduledData {
  def schedule[S1 <: ScheduledData, S2 <: ScheduledData](
    inputs: Array[S1],
    outputs: Array[S2],
    operation: Array[CLEvent] => CLEvent): CLEvent = {

    val nData = inputs.length + outputs.length
    val eventsToWaitFor = new ArrayBuffer[CLEvent](nData)

    inputs.foreach(_.startRead(eventsToWaitFor))
    outputs.foreach(_.startWrite(eventsToWaitFor))

    var event: CLEvent = null
    try {
      event = operation(eventsToWaitFor.toArray)
      if (event != null)
	      event.setCompletionCallback(new CLEvent.EventCallback {
	        override def callback(status: Int) = {
	//          println("completed")
	          inputs.foreach(_.eventCompleted(event))
	          outputs.foreach(_.eventCompleted(event))
	        }
	      })
      event
    } finally {
      inputs.foreach(_.endRead(event))
      outputs.foreach(_.endWrite(event))
    }
  }
}

private[scalacl] trait ScheduledData {
  def finish: Unit
  def eventCompleted(event: CLEvent): Unit
  def startRead(out: ArrayBuffer[CLEvent]): Unit
  def startWrite(out: ArrayBuffer[CLEvent]): Unit
  def endRead(event: CLEvent): Unit
  def endWrite(event: CLEvent): Unit
}
