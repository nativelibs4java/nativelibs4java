package scalacl
package collection
package impl

import com.nativelibs4java.opencl._
import scala.collection.mutable.{ArrayBuilder, ListBuffer, ArrayBuffer}

trait CLEventBoundContainer {
  def eventBoundComponents: Seq[CLEventBound]
}
trait CLEventBound extends CLEventBoundContainer {
  override def eventBoundComponents = Seq(this)
  
    protected var lastWriteEvent: CLEvent = null
    protected var lastReadEvent: CLEvent = null
    protected val readEvents = new ArrayBuffer[CLEvent]

    def write(action: Array[CLEvent] => CLEvent): CLEvent = this.synchronized {
        lastWriteEvent = action(Array(lastWriteEvent, lastReadEvent))
        lastReadEvent = null
        lastWriteEvent
    }
    def read(action: Array[CLEvent] => CLEvent): CLEvent = this.synchronized {
        lastReadEvent = action(Array(lastWriteEvent))
        lastReadEvent
    }
    protected def readValue[V](f: Array[CLEvent] => V): V = this.synchronized {
        val v = f(Array(lastWriteEvent))
        lastWriteEvent = null
        v
    }

    protected def readBlock[V](block: => V) = this.synchronized {
        CLEvent.waitFor(lastWriteEvent)
        lastWriteEvent = null
        block
    }
    def waitFor = this.synchronized {
        CLEvent.waitFor(Array(lastWriteEvent, lastReadEvent):_*)
        lastWriteEvent = null
        lastReadEvent = null
    }
}

object CLEventBound {
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
    recursiveSync(lb.result, Array.newBuilder[CLEvent])
  }
}