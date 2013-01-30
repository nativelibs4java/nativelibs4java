package scalacl.impl
import com.nativelibs4java.opencl._
import java.util.concurrent.locks._
import collection.mutable.ArrayBuffer

private[scalacl] trait DefaultScheduledData extends ScheduledData {
  private val scheduleLock = new ReentrantLock

  private def locked[V](block: => V): V = {
	scheduleLock.lock
    try {
      block
    } finally {
      scheduleLock.unlock
    }
  }
  override def finish = {
    val events = new ArrayBuffer[CLEvent]
    locked {
      if (dataWrite != null)
        events += dataWrite
      events ++= dataReads
    }
    CLEvent.waitFor(events.toArray: _*)
    locked {
      events.foreach(doEventCompleted(_))
    }
  }
  private def doEventCompleted(event: CLEvent) {
    if (event.equals(dataWrite)) {
	    dataWrite = null
	    dataReads.clear
	  } else {
	    dataReads -= event
	  }
  } 
  override def eventCompleted(event: CLEvent) {
    locked {
      doEventCompleted(event)
    }
  }
  override def startRead(out: ArrayBuffer[CLEvent]) = {
    scheduleLock.lock
    if (dataWrite != null)
      out += dataWrite
  }

  override def startWrite(out: ArrayBuffer[CLEvent]) = {
    scheduleLock.lock
    out ++= dataReads
    if (dataWrite != null)
      out += dataWrite
  }

  override def endRead(event: CLEvent) {
    if (event != null)
      dataReads += event
    scheduleLock.unlock
  }

  override def endWrite(event: CLEvent) {
    if (event != null)
      dataWrite = event
    scheduleLock.unlock
  }

  private var dataWrite: CLEvent = _
  private val dataReads = new ArrayBuffer[CLEvent]
}
