package scalacl
package impl

import com.nativelibs4java.opencl._

class CLSimpleCode(
  override val sources: Array[String],
  override val compilerArguments: Array[String],
  override val macros: Map[String, String],
  val kernelName: Option[String] = None
) extends CLCode with CLRunnable {
  def this(source: String) = this(Array(source), Array(), Map())
  
  override def isOnlyInScalaSpace = false
  
  protected def flattenArgs(arg: Any): Array[AnyRef] = arg match {
    case g: CLGuardedBuffer[_] =>
      Array(g.buffer)
    case a: CLArray[Any] =>
      a.buffers.map(_.buffer)
    case r: CLRange =>
      Array(r.buffer.buffer)
    case f: CLFilteredArray[_] =>
      f.presence.buffer +: f.array.buffers
    case Int | Short | Long | Byte | Char | Boolean | Double | Float =>
      Array(arg.asInstanceOf[AnyRef])
  }
  
  protected override def run(dims: Array[Int], args: Array[Any], eventsToWaitFor: Array[CLEvent])(implicit context: Context): CLEvent = {
    val kernel = kernelName.map(getKernel(context, _)).getOrElse {
      val (program, kernels) = getProgramAndKernels(context)
      if (kernels.size != 1)
        throw new RuntimeException("Expected a unique kernel in the program, found " + kernels.size + " : " + kernels.keys.mkString(", "))
        
      kernels.first._2
    }
    
    val flatArgs = args.flatMap(flattenArgs _)
        
    kernel.synchronized {
      try {
        kernel.setArgs(flatArgs:_*)
        if (verbose)
          println("[ScalaCL] Enqueuing kernel " + kernel.getFunctionName + " with dims " + dims.mkString(", "))
        kernel.enqueueNDRange(context.queue, dims, eventsToWaitFor:_*)
      } catch { case ex =>
        ex.printStackTrace(System.out)
        throw ex
      }
    }
  }
}

