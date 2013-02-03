package com.nativelibs4java.opencl

// TODO: use ScalaMock
class MockEvent(value: Long) extends CLEvent(null, value) {
  var cleared = false
  var completionCallback: CLEvent.EventCallback = null
  override def clear {
    assert(!cleared)
    cleared = true
  }
  override def getEntity = super.getEntity
  override def setCompletionCallback(cb: CLEvent.EventCallback) {
    assert(completionCallback == null)
    completionCallback = cb
  }
  
  override def toString = "MockEvent(" + value.toString + ")"
}
