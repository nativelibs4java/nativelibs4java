package scalacl

import com.nativelibs4java.opencl.CLContext
import com.nativelibs4java.opencl.CLQueue
import com.nativelibs4java.opencl.JavaCL
import com.nativelibs4java.opencl.CLDevice
import com.nativelibs4java.opencl.CLPlatform

class Context(val context: CLContext, val queue: CLQueue)
object Context {
  def best = {
    val context = JavaCL.createBestContext(CLPlatform.DeviceFeature.CPU)
    val queue = context.createDefaultOutOfOrderQueueIfPossible
    println("queue: " + queue + " (" + queue.getProperties + ")")
    new Context(context, queue)
  }
}