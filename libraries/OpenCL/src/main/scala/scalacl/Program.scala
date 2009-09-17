/*
 * Program.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package scalacl

import com.nativelibs4java.opencl.OpenCL4Java._
import SyntaxUtils._

class Context(var clContext: CLContext)
object Context {
  def GPU = new Context(CLContext.createContext(CLDevice.listGPUDevices()));
  def CPU = new Context(CLContext.createContext(CLDevice.listCPUDevices()));
  def BEST =
  try {
    GPU
  } catch {
    case _ => CPU
  }
}

abstract class Program(context: Context) {
  var root: Expr

  var source: String = null;

  private def generateSources(variables: List[AbstractVar]) : String = {

      var doc = new StringBuilder;

      doc ++ implode(root.includes.map { "#include <" + _ + ">" }, "\n")
      doc ++ "\n"
      var argDefs = variables.map { v =>
        "__global " +
        (if (v.mode == WriteMode || v.mode == AggregatedMode) "" else "const ") +
        v.typeDesc.globalCType + " " + v.name
      }

      doc ++ ("void function(" + implode(argDefs, ", ") + ")\n");
      doc ++ "{\n\t"
      //doc ++ "int gid = get_global_id(0);\n\t"
      doc ++ root.toString
      doc ++ "\n}\n"

      doc.toString
  }
  def ! = {
	  setup
  }
  def setup = {
    if (source == null) {
      val variables = root.variables;

      (variables zipWithIndex) foreach { case (v, i) => {
          v.argIndex = i;
          v.name = "var" + i;
          //if (v.variable.typeDesc.valueType == Parallel)
          //  v.parallelIndexName = "gid"
      } }

      root accept { (x, stack) => x match {
          case v: AbstractVar => v.mode = v.mode union ReadMode
            //			  case BinOp(""".*~""", v: Var[_], _) => v.isWritten = true
          case BinOp("=", v: AbstractVar, _) => v.mode = v.mode union WriteMode
          case _ =>
        } }

      variables filter { _.mode == AggregatedMode } foreach {
          throw new UnsupportedOperationException("Reductions not implemented yet !")
      }

      source = generateSources(variables)

      var kernel = context.clContext.createProgram(source).build().createKernel("function");

      variables foreach { v => {
          v.kernel = kernel
          v.setup
      } }
    }
    println(source)
  }
}

