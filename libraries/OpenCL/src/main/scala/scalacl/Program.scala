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

abstract class Program(context: Context, var dimensions: Dim*) {
  var statements: Stat

  var source: String = null;

  import scala.collection.mutable.ListBuffer
  
  private def generateSources : String = {
    var doc = new StringBuilder;

    var dims = statements.findUnique[Dim].zipWithIndex.map { case (d, i) =>
        d.name = "dim" + (i + 1);
        d.dimIndex = i;
        d
    }

    var variables = statements.findUnique[AbstractVar]
    /*variables = (_ ++ _)(variables.partition (_ match {
        case ReadMode => true
        case ReadWrite
    }
    }))*/
    markVarUsage
    variables.zipWithIndex.foreach { case (v, i) => {
          v.argIndex = i;
          v.name = v.mode.hintName + (i + 1);
          val d = dimensions.size
          v match {
            case a: ArrayVar[_] => if (d == 1) a.implicitDim = Some(dimensions(0))
            case im: ImageVar[_] => if (d == 2) {
                im.implicitDimX = Some(dimensions(0))
                im.implicitDimY = Some(dimensions(1))
            }
            case _ =>
          }
    } }

    

    
    variables filter { _.mode == AggregatedMode } foreach {x =>
      throw new UnsupportedOperationException("Reductions not implemented yet !\n" + x.toString())
    }

    val includes = statements.findUnique[Fun] map (_.include)
    doc ++ includes.map("#include <" + _ + ">\n").implode("")

    var argDefs = variables.map(v =>
      "__global " +
      (if (v.mode == WriteMode || v.mode == ReadWriteMode || v.mode == AggregatedMode) "" else "const ") +
      v.typeDesc.globalCType + " " + v.name
    )

    //doc ++ variables.map(v => "\t//"+ v.name + ": " + v.mode + "\n").implode("")
    doc ++ ("void function(" + argDefs.implode(", ") + ") {\n");
    doc ++ dims.map(dim => "\tint " + dim.name + " = get_global_id(" + dim.dimIndex + ");\n").implode("")
    doc ++ ("\t" + statements.toString + "\n")
    doc ++ "}\n"

    doc.toString
  }
  def markVarUsage = statements accept { (x, stack) =>
    x match {
      case v: AbstractVar => if (stack.size <= 1 || !stack(stack.size - 2).isInstanceOf[Assignment])
        v.mode = v.mode union ReadMode
      case Assignment(_, t, _) => t match {
          case v: AbstractVar => v.mode = v.mode union WriteMode
          case e: ArrayElement[_] => e.array.mode = e.array.mode union WriteMode
          case _ => 
      }
      case _ =>
    }
  }

  def ! = exec
  
  def setup = {
    if (source == null) {
      source = generateSources
      println(source)
      
      var kernel = context.clContext.createProgram(source).build().createKernel("function");

      statements.find[AbstractVar] foreach { v => {
          v.kernel = kernel
          v.setup
        } }
    }
  }
  def exec = {
    setup

    //dims
  }
}

