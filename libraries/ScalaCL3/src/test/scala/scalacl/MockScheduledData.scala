package scalacl.impl
import com.nativelibs4java.opencl.CLEvent
import scala.collection.mutable.ArrayBuffer

// TODO: use ScalaMock
class MockScheduledData extends ScheduledData {
  private var _calls = new ArrayBuffer[(Symbol, List[Any])]
  def calls: List[(Symbol, List[Any])] = {
    val calls = _calls.toList
    _calls.clear()
    calls
  }
  
  override def finish { 
    _calls += ('finish -> Nil)
  }
  override def eventCompleted(event: CLEvent) { 
    _calls += ('eventCompleted -> List(event)) 
  }
  override def startRead(out: ArrayBuffer[CLEvent]) { 
    _calls += ('startRead -> List(out.toList))
  }
  override def startWrite(out: ArrayBuffer[CLEvent]) { 
    _calls += ('startWrite -> List(out.toList)) 
  }
  override def endRead(event: CLEvent) { 
    _calls += ('endRead -> List(event))
  }
  override def endWrite(event: CLEvent) { 
    _calls += ('endWrite -> List(event)) 
  }
}
