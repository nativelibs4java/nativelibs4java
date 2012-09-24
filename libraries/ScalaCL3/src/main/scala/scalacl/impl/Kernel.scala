package scalacl
package impl

import com.nativelibs4java.opencl._
import java.util.concurrent.locks._
import collection.mutable.ArrayBuffer

private[scalacl] case class KernelExecutionParameters(
  globalSizes: Array[Long],
  localSizes: Array[Long] = null,
  globalOffsets: Array[Long] = null) {
  def this(uniqueSize: Long) = this(Array(uniqueSize))
}

/**
 * Thin wrapper for OpenCL kernel sources, which can act as a fast cache key for the corresponding CLKernel
 */
class Kernel(protected val id: Long, protected val sources: String) {
  def getKernel(context: Context): CLKernel = {
    context.kernels(this, _.release) {
      val Array(k) = context.context.createProgram(sources).createKernels
      k
    }
  }
  def enqueue(context: Context, params: KernelExecutionParameters, args: Array[AnyRef], eventsToWaitFor: Array[CLEvent]): CLEvent = {
    var kernel = getKernel(context)
    kernel synchronized {
      kernel.setArgs(args: _*)
      kernel.enqueueNDRange(context.queue, params.globalOffsets, params.globalSizes, params.localSizes, eventsToWaitFor: _*)
    }
  }
  
  override def equals(o: Any) = o.isInstanceOf[Kernel] && {
    val k = o.asInstanceOf[Kernel]
    id == k.id && (sources eq k.sources) // identity test: assume interned strings coming from class resources!
  }
  
  override def hashCode = id.hashCode
  
  override def toString = "Kernel(" + sources + ")"
}