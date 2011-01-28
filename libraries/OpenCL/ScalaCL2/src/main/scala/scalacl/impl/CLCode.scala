package scalacl

package impl

import _root_.scala.collection._

import com.nativelibs4java.opencl._

trait CLCode {
  protected val sources: Seq[String]
  protected val macros: Map[String, String]
  protected val compilerArguments: Seq[String]

  private val flatten: ((String, String)) => String = { case ((a: String, b: String)) => a + b }
  private lazy val strs = sources ++ macros.map(flatten)// ++ compilerArguments ++ templateParameters.toSeq.map(flatten)
  private lazy val hc = strs.map(_.hashCode).reduceLeft(_ ^ _)

  private val map = new mutable.HashMap[CLContext, (CLProgram, Map[String, CLKernel])]
  
  def compile(context: ScalaCLContext): Unit = getProgramAndKernels(context)
  
  private[impl] def getProgramAndKernels(context: ScalaCLContext) = map.synchronized {
    map.getOrElseUpdate(
      context.context,
      {
        val program = context.context.createProgram(sources.map(s => """
            #pragma OPENCL EXTENSION cl_khr_byte_addressable_store : enable
        """ + s):_*)
        for ((key, value) <- macros)
          program.defineMacro(key, value)

        compilerArguments.foreach(program.addBuildOption(_))
        (program, program.createKernels.map(k => (k.getFunctionName, k)).toMap)
      }
    )
  }

  def release(context: ScalaCLContext): Unit = map.synchronized {
    for ((program, kernels) <- map.get(context.context)) {
      kernels.values.foreach(_.release)
      program.release
      map.remove(context.context)
    }
  }
  def getKernel(context: ScalaCLContext, name: String = null) = {
    val kernels = getProgramAndKernels(context)._2
    if (name == null)
      kernels.values.head
    else
      kernels(name)
  }

  override def hashCode = hc
  override def equals(o: Any) = o.isInstanceOf[CLCode] && strs.equals(o.asInstanceOf[CLCode].strs)
}
