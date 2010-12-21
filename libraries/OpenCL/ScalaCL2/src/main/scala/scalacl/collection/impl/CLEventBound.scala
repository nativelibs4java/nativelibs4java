package scalacl
package collection
package impl

import com.nativelibs4java.opencl._

trait CLEventBoundContainer {
  def eventBoundComponents: Seq[CLEventBound]
}
trait CLEventBound extends CLEventBoundContainer {
  override def eventBoundComponents = Seq(this)
  
    protected var lastWriteEvent: CLEvent = null
    protected var lastReadEvent: CLEvent = null
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

