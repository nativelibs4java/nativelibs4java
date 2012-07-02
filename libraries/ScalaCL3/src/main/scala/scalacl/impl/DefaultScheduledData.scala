package scalacl.impl
import com.nativelibs4java.opencl._
import java.util.concurrent.locks._
import collection.mutable.ArrayBuffer

trait DefaultScheduledData extends ScheduledData {
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
  def eventCompleted(event: CLEvent): Unit = {
    locked {
      doEventCompleted(event)
    }
  }
  def startRead(out: ArrayBuffer[CLEvent]) = {
    scheduleLock.lock
    if (dataWrite != null)
      out += dataWrite
    this
  }

  def startWrite(out: ArrayBuffer[CLEvent]) = {
    scheduleLock.lock
    out ++= dataReads
    if (dataWrite != null)
      out += dataWrite
    this
  }

  def endRead(event: CLEvent): Unit = {
    if (event != null)
      dataReads += event
    scheduleLock.unlock
  }

  def endWrite(event: CLEvent): Unit = {
    if (event != null)
      dataWrite = event
    scheduleLock.unlock
  }

  private var dataWrite: CLEvent = _
  private val dataReads = new ArrayBuffer[CLEvent]
}
