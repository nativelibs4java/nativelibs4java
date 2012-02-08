package scalacl

package impl

import com.nativelibs4java.opencl._
import scala.collection.mutable.{ArrayBuilder, ListBuffer, ArrayBuffer}

trait CLEventBoundContainer {
  def eventBoundComponents: Seq[CLEventBound]
  def waitFor: Unit = eventBoundComponents.foreach(_.waitFor)
}
trait CLEventBound extends CLEventBoundContainer {
  override def eventBoundComponents = Seq(this)
  
  protected var lastWriteEvent: CLEvent = null
  protected val readEvents = new ArrayBuffer[CLEvent]

  protected def releaseEvents = this.synchronized {
    if (lastWriteEvent == null) {
      lastWriteEvent.release
      lastWriteEvent = null
    }
    readEvents.map(_.release)
    readEvents.clear
  }
    
  protected def allEvents = this.synchronized {
    val rea = readEvents.toArray
    if (lastWriteEvent == null)
      rea
    else
      Array(lastWriteEvent) ++ rea
  }

  def write(action: Array[CLEvent] => CLEvent): CLEvent = this.synchronized {
    val evt = action(allEvents)
    if (evt != null) {
      lastWriteEvent = evt
      readEvents.clear
    }
    lastWriteEvent
  }
  def read(action: Array[CLEvent] => CLEvent): CLEvent = this.synchronized {
    val evt = action(if (lastWriteEvent == null) Array() else Array(lastWriteEvent))
    if (evt != null)
      readEvents += evt

    evt
  }

  protected def readValue[V](f: Array[CLEvent] => V): V = this.synchronized {
    f(if (lastWriteEvent == null) Array() else Array(lastWriteEvent))
  }

  def readBlock[V](block: => V) = this.synchronized {
      waitFor
      block
  }

  override def waitFor = this.synchronized {
    if (lastWriteEvent != null)
      readEvents += lastWriteEvent

    CLEvent.waitFor(readEvents.result:_*)
    lastWriteEvent = null
    readEvents.clear
    //lastReadEvent = null
  }
}

object CLEventBound {
  def flatten(containers: Array[CLEventBoundContainer]) =
    containers.flatMap(_.eventBoundComponents)
    
  def syncBlock(reads: Array[CLEventBound], writes: Array[CLEventBound], action: Array[CLEvent] => CLEvent): CLEvent = {

    def recursiveSync(ebs: List[(CLEventBound, Boolean)], evts: ArrayBuilder[CLEvent]): CLEvent = {
      val (eb, write) :: tail = ebs
      eb synchronized {
        // Whether we're reading of writing to eb, we wait for the last write to finish :
        if (eb.lastWriteEvent != null)
          evts += eb.lastWriteEvent

        if (write) // If writing to eb, we wait for all those reading from eb :
          evts ++= eb.readEvents

        val evt = if (tail.isEmpty)
          action(evts.result())
        else
          recursiveSync(tail, evts)

        if (evt != null) {
          if (write) {
            // Flush read events, as we now have a much more blocking write event :
            eb.lastWriteEvent = evt
            eb.readEvents.clear
          } else {
            eb.readEvents += evt
          }
        }
        evt
      }
    }
    val lb = new ListBuffer[(CLEventBound, Boolean)]
    for (eb <- reads)
      lb += ((eb, false))
    for (eb <- writes)
      lb += ((eb, true))
      
    if (lb.isEmpty)
      action(Array())
    else
      recursiveSync(lb.result, Array.newBuilder[CLEvent])
  }
}