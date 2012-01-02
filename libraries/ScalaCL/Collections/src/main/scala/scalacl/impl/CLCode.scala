package scalacl

package impl

import _root_.scala.collection._

import com.nativelibs4java.opencl._

trait CLCode {
  protected val sources: Array[String]
  protected val macros: Map[String, String]
  protected val compilerArguments: Array[String]

  private val flatten: ((String, String)) => String = { case ((a: String, b: String)) => a + b }
  private lazy val strs = sources ++ macros.map(flatten)// ++ compilerArguments ++ templateParameters.toSeq.map(flatten)
  private lazy val hc = strs.map(_.hashCode).reduceLeft(_ ^ _)

  private val map = new mutable.HashMap[CLContext, (CLProgram, Map[String, CLKernel])]
  
  def compile(context: Context): Unit = getProgramAndKernels(context)
  
  private[impl] def getProgramAndKernels(context: Context): (CLProgram, Map[String, CLKernel]) = map.synchronized {
    map.getOrElseUpdate(
      context.context,
      {
        val srcs = 
          if (context.context.isByteAddressableStoreSupported)
            sources.map("#pragma OPENCL EXTENSION cl_khr_byte_addressable_store : enable\n" + _)
          else
            sources
            
        val program = context.context.createProgram(srcs:_*)
        if (useFastRelaxedMath)
          program.setFastRelaxedMath
        for ((key, value) <- macros)
          program.defineMacro(key, value)

        /*
        import java.io._
        val out = new PrintStream(new FileOutputStream("out.cl"))
        out.println("// " + compilerArguments.mkString(" "))
        out.println(srcs.mkString("\n"))
        out.close
        */
        
        compilerArguments.foreach(program.addBuildOption(_))
        (program, program.createKernels.map(k => (k.getFunctionName, k)).toMap)
      }
    )
  }

  def release(context: Context): Unit = map.synchronized {
    for ((program, kernels) <- map.get(context.context)) {
      kernels.values.foreach(_.release)
      program.release
      map.remove(context.context)
    }
  }
  def getKernel(context: Context, name: String = null) = {
    val kernels = getProgramAndKernels(context)._2
    if (name == null)
      kernels.values.head
    else
      kernels(name)
  }

  override def hashCode = hc
  override def equals(o: Any) = o.isInstanceOf[CLCode] && strs.equals(o.asInstanceOf[CLCode].strs)
  
  /**
   * Execute the code using the provided arguments (may be CLArray instances, CLGuardedBuffer instances, CLBuffer instances or primitive values), using the provided global sizes (and optional local group sizes) and respecting the execution order of arguments declared as "read from" and "written to".<br>
   * Example :
   * <code>
     import scalacl._
     implicit val context = Context.best(CPU)
     val n = 100
     val f = 0.5f
     val sinCosOutputs: CLArray[Float] = new CLArray[Float](2 * n)
     val sinCosCode = customCode("""
       __kernel void sinCos(__global float2* outputs, float f) {
        int i = get_global_id(0);
        float c, s = sincos(i * f, &c);
        outputs[i] = (float2)(s, c);
       }
     """)
     sinCosCode.execute(
       args = Array(sinCosOutputs, f),
       writes = Array(sinCosOutputs),
       globalSizes = Array(n)
     )
     val resCL = sinCosOutputs.toArray
   * </code>
   */
  def execute(
    args: Array[Any],
    globalSizes: Array[Int],
    localSizes: Array[Int] = null,
    reads: Array[CLEventBoundContainer] = Array(),
    writes: Array[CLEventBoundContainer] = Array(),
    kernelName: String = null
  )(implicit context: Context): Unit = {
    val flatArgs: Array[Object] = args.flatMap(_ match {
      case b: CLArray[_] =>
        b.buffers.map(_.buffer.asInstanceOf[Object])
      case b: CLGuardedBuffer[_] =>
        Array[Object](b.buffer)
      case a =>
        Array[Object](a.asInstanceOf[Object])
    })
    val kernel = getKernel(context, kernelName)
    kernel.synchronized {
      //println("flatArgs = " + flatArgs.map(a => a + ": " + a.getClass.getSimpleName).mkString(", "))
      kernel.setArgs(flatArgs:_*)
      CLEventBound.syncBlock(CLEventBound.flatten(reads), CLEventBound.flatten(writes), evts => {
        kernel.enqueueNDRange(context.queue, globalSizes, localSizes, evts:_*)
      })
    }
  }
}
