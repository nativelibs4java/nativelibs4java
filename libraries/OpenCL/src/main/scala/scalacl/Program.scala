/*
 * Program.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package scalacl

import com.nativelibs4java.opencl.OpenCL4Java._
import SyntaxUtils._

class Context(var context: CLContext, var queue: CLQueue)
object Context {
  def newContext(devices: Array[CLDevice]) : Option[Context] = {
    var ctx = CLContext.createContext(devices);
    Some(new Context(ctx, ctx.createDefaultQueue))
  }
  private var gpu: Option[Context] = None
  private var cpu: Option[Context] = None
  private var best: Option[Context] = None
  
  def GPU : Context = gpu.getOrElse { gpu = newContext(CLDevice.listGPUDevices); gpu.get }
  def CPU : Context = cpu.getOrElse { cpu = newContext(CLDevice.listCPUDevices); cpu.get }
  def BEST = best.getOrElse { best = Some(try { GPU } catch { case _ => CPU }); best.get }
}

abstract class Program(context: Context, var dimensions: Dim*) {

  //def this(dimensions: Dim*) = this(Context.BEST, dimensions)

  var content: Stat
  private var source: String = null;

  import scala.collection.mutable.ListBuffer

  private def getVariables = unique[AbstractVar](content.find[AbstractVar])

  private var filteredVariables = List[AbstractVar]()
  private def generateSources : String = {
    var doc = new StringBuilder;

    var dims = unique[Dim](content.find[Dim] ++ dimensions).zipWithIndex.map { case (d, i) =>
        d.name = "dim" + (i + 1);
        d.dimIndex = i;
        d
    }

    filteredVariables = getVariables
    markVarUsage;

    // Sort inputs first
    var (write, notWrite) = filteredVariables.filter(v => v.mode.read || v.mode.write).partition(_.mode.write)
    filteredVariables = notWrite ++ write
    
    import scala.collection.mutable.HashMap
    var namesPerHint = new HashMap[String, Int]
    
    filteredVariables.zipWithIndex.foreach { case (v, i) => {
      v.argIndex = i;

      namesPerHint.put(v.mode.hintName, namesPerHint.getOrElse(v.mode.hintName, 0) + 1)
      val d = dimensions.size
      v match {
        case a: ArrayVar[_, _] => if (d == 1) a.implicitDim = Some(dimensions(0))
        case im: ImageVar[_] => if (d == 2) {
            im.implicitDimX = Some(dimensions(0))
            im.implicitDimY = Some(dimensions(1))
        }
        case _ =>
      }
    } }
    
    filteredVariables.reverse.foreach { v =>
      val hint = v.mode.hintName
      var count = namesPerHint(hint)
      if (count == 1)
        v.name = hint
      else {
        if (count > 0)
          count = -count;
        v.name = hint + (-count);
        namesPerHint.put(hint, count + 1)
      }
    }
    

    
    filteredVariables filter { _.mode.reduction } foreach { x =>
      throw new UnsupportedOperationException("Reductions not implemented yet !\n" + x.toString())
    }

    //doc ++ unique[Fun](content.find[Fun]) map (_.include).map("#include <" + _ + ">\n").implode("")

    var argDefs = filteredVariables.map(v =>
      "__global " +
      (if (v.mode.write) "" else "const ") +
      v.typeDesc.globalCType + " " + v.name
    )

    //doc ++ variables.map(v => "\t//"+ v.name + ": " + v.mode + "\n").implode("")
    doc ++ ("__kernel function(" + argDefs.implode(", ") + ") {\n");
    doc ++ dims.map(dim => "\tint " + dim.name + " = get_global_id(" + dim.dimIndex + ");\n").implode("")
    doc ++ ("\t" + content.toString + "\n")
    doc ++ "}\n"

    doc.toString
  }
  def markVarUsage = content accept { (x, stack) =>
    x match {
      case v: AbstractVar => stack.toList.reverse match {
          case (_: Assignment) :: xs =>
          case (_: ArrayElement[_, _]) :: (_: Assignment) :: xs =>
          case _ => v.mode.read = true
      }
      case Assignment(_, t, _) => t match {
          case v: AbstractVar => v.mode.write = true
          case e: ArrayElement[_, _] => e.array.mode.write = true
          case _ => 
      }
      case _ => 
    }
  }

  def ! = exec()
  
  private var kernel : CLKernel = null;
  
  def setup = {
    if (kernel == null) {
      source = generateSources
      println(source)
      
      kernel = context.context.createProgram(source).build().createKernel("function");

      content.find[AbstractVar] foreach { v => {
          v.kernel = kernel;
          v.setup;
          v.queue = context.queue
        } }
    }
  }
  def exec(dims: Int*) = {
    setup
    val globs = if (dims.length == 0) dimensions.map(_.size) else dims;
    kernel.enqueueNDRange(context.queue, globs.toArray, Array(1));
    
    getVariables.filter(_.mode.write).foreach(_.stale = true)
  }
}

