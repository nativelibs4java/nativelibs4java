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
  
  private def generateSources(variables: List[AbstractVar]) : String = {

      var doc = new StringBuilder;

      //val includes = new ListBuffer[String]()
      //root accept { (x, stack) => x match { case v: Fun => includes + v.include case _ => } }

      val includes = root.find[Fun] map (_.include)
      var dims = root.find[Dim].zipWithIndex.map { case (d, i) => d.name = "dim" + (i + 1); d }
      
      variables.zipWithIndex.foreach { case (v, i) => {
          v.argIndex = i;
          v.name = "var" + (i + 1);
      } }

      doc ++ includes.map("#include <" + _ + ">").implode("\n")
      doc ++ "\n"

      var argDefs = variables.map(v =>
        "__global " +
        (if (v.mode == WriteMode || v.mode == ReadWriteMode || v.mode == AggregatedMode) "" else "const ") +
        v.typeDesc.globalCType + " " + v.name
      )

      doc ++ ("void function(" + argDefs.implode(", ") + ")\n");
      doc ++ "{\n\t"
      //doc ++ "int gid = get_global_id(0);\n\t"
      doc ++ (root.toString + ";")
      doc ++ "\n}\n"

      doc.toString
  }
  def ! = {
	  setup
  }
  def setup = {
    if (source == null) {
      val variables = root.find[AbstractVar]

      root accept { (x, stack) => {
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
        }
      }

      variables filter { _.mode == AggregatedMode } foreach {x =>
          throw new UnsupportedOperationException("Reductions not implemented yet !\n" + x.toString())
      }

      source = generateSources(variables)
      println(source)
      
      var kernel = context.clContext.createProgram(source).build().createKernel("function");

      variables foreach { v => {
          v.kernel = kernel
          v.setup
      } }
    }
  }
}

