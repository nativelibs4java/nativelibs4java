/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package scalacl


import com.nativelibs4java.opencl.CLContext
import com.nativelibs4java.opencl.CLKernel
import scala.collection.mutable.HashMap

class CLCode(
  name: String,
  sources: Seq[String],
  macros: Map[String, String],
  compilerArguments: Seq[String],
  templateParameters: Map[String, String]
) {

  def this(source: String) =
    this(null, Seq(source), Map(), Seq(), Map())

  private val flatten: ((String, String)) => String = { case ((a: String, b: String)) => a + b }
  val strs = sources ++ macros.map(flatten)// ++ compilerArguments ++ templateParameters.toSeq.map(flatten)
  private val hc = strs.map(_.hashCode).reduceLeft(_ ^ _)

  val map = new HashMap[CLContext, CLKernel]
  def getKernel(context: ScalaCLContext, in: CLArgsProvider, out: CLArgsProvider) = map.synchronized {
    // TODO differentiate between collection types !!!
    map.getOrElseUpdate(
      context.context,
      {
        val program = context.context.createProgram(sources:_*)
        for ((key, value) <- macros)
          program.defineMacro(key, value)

        program.addArgs(compilerArguments:_*)
        if (name != null)
          program.createKernel(name)
        else
          program.createKernels.last
      }
    )
  }

  override def hashCode = hc
  override def equals(o: Any) =
    o.isInstanceOf[CLCode] &&
    strs.equals(o.asInstanceOf[CLCode].strs)
}

class CLFunction[K, V](
  name: String,
  sources: Seq[String],
  macros: Map[String, String],
  compilerArguments: Seq[String],
  templateParameters: Map[String, String]
) extends CLCode(name, sources, macros, compilerArguments, templateParameters)
{
  def this(source: String) =
    this(null, Seq(source), Map(), Seq(), Map())
}
