package scalacl

import com.nativelibs4java.opencl.CLContext
import com.nativelibs4java.opencl.CLQueue
import com.nativelibs4java.opencl.JavaCL
import com.nativelibs4java.opencl.CLDevice
import com.nativelibs4java.opencl.CLPlatform
import scalacl.impl.Kernel
import com.nativelibs4java.opencl.CLKernel
import scalacl.impl.ConcurrentCache

/**
 * ScalaCL context, which gathers an OpenCL context and a command queue.
 */
class Context(val context: CLContext, val queue: CLQueue) {
  private[scalacl] val kernels = new ConcurrentCache[Kernel, CLKernel]
}

object Context {
  def best = {
    val context = JavaCL.createBestContext(CLPlatform.DeviceFeature.GPU)
    val queue = context.createDefaultOutOfOrderQueueIfPossible
    println("queue: " + queue + " (" + queue.getProperties + ")")
    new Context(context, queue)
  }
}