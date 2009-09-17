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

  import scala.collection.mutable.ListBuffer
  
  private def generateSources : String = {
      markVarUsage
      
      var doc = new StringBuilder;

      var dims = root.findUnique[Dim].zipWithIndex.map { case (d, i) =>
          d.name = "dim" + (i + 1);
          d.dimIndex = i;
          d
      }
      val variables = root.findUnique[AbstractVar]
      variables.zipWithIndex.foreach { case (v, i) => {
          v.argIndex = i;
          v.name = "var" + (i + 1);
      } }
      variables filter { _.mode == AggregatedMode } foreach {x =>
          throw new UnsupportedOperationException("Reductions not implemented yet !\n" + x.toString())
      }

      val includes = root.findUnique[Fun] map (_.include)
      doc ++ includes.map("#include <" + _ + ">\n").implode("")

      var argDefs = variables.map(v =>
        "__global " +
        (if (v.mode == WriteMode || v.mode == ReadWriteMode || v.mode == AggregatedMode) "" else "const ") +
        v.typeDesc.globalCType + " " + v.name
      )

      doc ++ ("void function(" + argDefs.implode(", ") + ") {\n");
      doc ++ dims.map(dim => "\tint " + dim.name + " = get_global_id(" + dim.dimIndex + ");\n").implode("")
      doc ++ ("\t" + root.toString + ";\n")
      doc ++ "}\n"

      doc.toString
  }
  def markVarUsage = root accept { (x, stack) => {
    x match {
      case v: AbstractVar => {
//                if (stack.size > 1) {
//                  stack(stack.size - 2).match {
//                    case BinOp("=", v: AbstractVar, _)
//                  }
//                }
          v.mode = v.mode union ReadMode
      }
      // case BinOp(""".*~""", v: Var[_], _) => v.isWritten = true
      case BinOp("=", v: AbstractVar, _) => v.mode = v.mode union WriteMode
      case _ =>
    }
  } }

  def ! = {
	  setup
  }
  def setup = {
    if (source == null) {
      source = generateSources
      println(source)
      
      var kernel = context.clContext.createProgram(source).build().createKernel("function");

      root.find[AbstractVar] foreach { v => {
          v.kernel = kernel
          v.setup
      } }
    }
  }
}

