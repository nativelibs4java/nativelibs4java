package scalacl

package impl

import _root_.scala.collection._

import com.nativelibs4java.opencl._

trait CLCode {
  val sources: Seq[String]
  val macros: Map[String, String]
  val compilerArguments: Seq[String]

  private val flatten: ((String, String)) => String = { case ((a: String, b: String)) => a + b }
  lazy val strs = sources ++ macros.map(flatten)// ++ compilerArguments ++ templateParameters.toSeq.map(flatten)
  private lazy val hc = strs.map(_.hashCode).reduceLeft(_ ^ _)

  val map = new mutable.HashMap[CLContext, Map[String, CLKernel]]
  
  def getKernel(context: ScalaCLContext, name: String = null) = map.synchronized {
    val kernels = map.getOrElseUpdate(
      context.context,
      {
        val program = context.context.createProgram(sources.map(s => """
            #pragma OPENCL EXTENSION cl_khr_byte_addressable_store : enable
        """ + s):_*)
        for ((key, value) <- macros)
          program.defineMacro(key, value)

        compilerArguments.foreach(program.addBuildOption(_))
        program.createKernels.map(k => (k.getFunctionName, k)).toMap
      }
    )
    if (name == null)
      kernels.values.head
    else
      kernels(name)
  }

  override def hashCode = hc
  override def equals(o: Any) = o.isInstanceOf[CLCode] && strs.equals(o.asInstanceOf[CLCode].strs)
}
