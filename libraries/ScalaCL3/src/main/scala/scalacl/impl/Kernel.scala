package scalacl
package impl

import com.nativelibs4java.opencl._
import java.util.concurrent.locks._
import collection.mutable.ArrayBuffer

case class KernelExecutionParameters(
  globalSizes: Array[Long],
  localSizes: Array[Long] = null,
  globalOffsets: Array[Long] = null) {
  def this(uniqueSize: Long) = this(Array(uniqueSize))
}

class Kernel(sources: String*) {
  def getKernel(context: CLContext): CLKernel = {
    var kernel = context.getClientProperty(this).asInstanceOf[CLKernel]
    if (kernel == null) {
      val Array(k) = context.createProgram(sources: _*).createKernels
      kernel = k
    }
    context.putClientProperty(this, kernel)
    kernel
  }
  def enqueue(queue: CLQueue, params: KernelExecutionParameters, args: Array[AnyRef], eventsToWaitFor: Array[CLEvent]): CLEvent = {
    var kernel = getKernel(queue.getContext)
    kernel synchronized {
      kernel.setArgs(args: _*)
      kernel.enqueueNDRange(queue, params.globalOffsets, params.globalSizes, params.localSizes, eventsToWaitFor: _*)
    }
  }
}