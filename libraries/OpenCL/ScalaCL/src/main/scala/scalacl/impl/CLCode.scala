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
        val srcs = sources.map("#pragma OPENCL EXTENSION cl_khr_byte_addressable_store : enable\n" + _)
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
}
