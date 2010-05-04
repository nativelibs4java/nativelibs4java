/*
 * Program.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package scalacl

import com.nativelibs4java.opencl.JavaCL._
import com.nativelibs4java.opencl._
import SyntaxUtils._

class Context(var context: CLContext, var queue: CLQueue)
object Context {
  def newContext(ctx: CLContext) = new Context(ctx, ctx.createDefaultQueue())
  /*def newContext(devices: Array[CLDevice]) : Option[Context] = {
    var ctx = CLContext.createContext(null, devices: _*);
    Some(new Context(ctx, ctx.createDefaultQueue))
  }
  private var gpu: Option[Context] = None
  private var cpu: Option[Context] = None
  private var best: Option[Context] = None
  */
  //def platform = JavaCL.listPlatforms()(0)
  //def GPU : Context = gpu.getOrElse { gpu = newContext(platform, platform.listGPUDevices(true)); gpu.get }
  //def CPU : Context = cpu.getOrElse { cpu = newContext(platform, platform.listCPUDevices(true)); cpu.get }
  def BEST = newContext(JavaCL.createBestContext());//best.getOrElse { best = Some(try { GPU } catch { case _ => CPU }); best.get }
}

class UndefinedContent extends Stat {
  def die = throw new UnsupportedOperationException("Content of the program was not defined.")
  def accept(info: VisitInfo) = die
  
}
abstract class Program(context: Context, var dimensions: Dim*)
{
  def this(dimensions: Dim*) = this(Context.BEST, dimensions: _*)

  private var contentStat: Stat = new UndefinedContent
  def content = contentStat
  def content_=(stat: Stat): Stat = {
    this.contentStat = stat
    //build
    //println("using content_=")
    alloc
    stat
  }
  private var source: String = null;

  import scala.collection.mutable.ListBuffer

  private def getVariables = unique[AbstractVar](content.find[AbstractVar])

  private var filteredVariables = List[AbstractVar]()
  private def generateSources : String = {
    var doc = new StringBuilder;

    //var dims = unique(content.find[Dim] ++ dimensions).zipWithIndex.map { case (d, i) =>
	var dims = dimensions.toList.zipWithIndex.map { case (d, i) =>
        d.name = "dim" + (i + 1);
        d.dimIndex = i;
        d
    }

    filteredVariables = getVariables
    markVarUsage;

    // Sort inputs first
    //TODO var (write, notWrite) = filteredVariables.filter(v => v.mode.read || v.mode.write).partition(_.mode.write)
    //TODO filteredVariables = notWrite ++ write
    
    import scala.collection.mutable.HashMap
    var namesPerHint = new HashMap[String, Int]
    
    filteredVariables.zipWithIndex.foreach { case (v, i) =>
      v.argIndex = i;

      namesPerHint.put(v.hintName, namesPerHint.getOrElse(v.hintName, 0) + 1)
      val d = dimensions.size
      v match {
        case a: ArrayVar[_, _] => if (d == 1) a.implicitDim = Some(dimensions(0))
        case im: ImageVar[_] => if (d == 2) {
            im.implicitDimX = Some(dimensions(0))
            im.implicitDimY = Some(dimensions(1))
        }
        case _ =>
      }
    }
    
    filteredVariables.reverse.foreach { v =>
      val hint = v.hintName
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

    var argDefs = filteredVariables.filter(_.scope != LocalScope).map(v =>
      (v match {
		  case iv: ImageVar[_] =>
			  if (v.mode.read && v.mode.write)
				throw new IllegalArgumentException("A same ImageVar cannot be read and written in the same program. Must either be used as an output-only or as an input-only");
			  if (v.mode.read)
				"read_only "
			  else
				"write_only "
		  case _ => "__global " + (if (v.mode.write) "" else "const ")
	  }) +
      v.typeDesc.cType + " " + v.name
    )

	if (context.context.isHalfSupported())
		doc.append("#pragma OPENCL EXTENSION cl_khr_fp16 : require\n");

	if (context.context.isDoubleSupported())
		doc.append("#pragma OPENCL EXTENSION cl_khr_fp64 : require\n");

	if (context.context.isByteAddressableStoreSupported())
		doc.append("#pragma OPENCL EXTENSION cl_khr_byte_addressable_store : require\n");
	
    //doc.append(variables.map(v => "\t//"+ v.name + ": " + v.mode + "\n").implode(""))
    doc.append("__kernel void function(" + argDefs.implode(", ") + ") {\n");
    doc.append(dims.map(dim => "\tint " + dim.name + " = get_global_id(" + dim.dimIndex + ");\n").implode(""))
	filteredVariables.filter(_.scope == LocalScope).foreach { v =>
		doc.append("\t" + v.typeDesc.cType + " " + v.name + ";\n")
	}
    doc.append("\t" + content.toString + "\n")
    doc.append("}\n");

    doc.toString
  }
  def markVarUsage = content accept { (x, stack) =>
  	//val stackList = stack.toList // scala 2.8.0
	val stackList = stack.toList.reverse // scala 2.7.6
	//if (stackList != Nil)
	//println(stackList(stackList.size - 1) == x)
    x match {
      case v: AbstractVar => stackList match {
          case (_: Assignment) :: xs => 
          case (_: ArrayElement[_, _]) :: (_: Assignment) :: xs => 
          case _ => v.mode.read = true
      }
      case Assignment(_, t, _) => t match {
          case v: AbstractVar => v.mode.write = true
          case e: ArrayElement[_, _] => e.array.mode.write = true
          case _ => 
      }
      case e: ArrayElement[_,_] => e.array.indexUsages + e.index
      case _ => 
    }
  }

  def ! = exec
  
  private var kernel : CLKernel = null;
  
  def build: Program = {
    if (kernel == null) {
      source = generateSources
      println(source)
      
      kernel = context.context.createProgram(source).build().createKernel("function");

      filteredVariables foreach { v =>
          v.kernel = kernel
          v.queue = context.queue
          //v.alloc
      }
    }
    this
  }
  def alloc: Program = alloc()
  def alloc(dims: Int*): Program = {
    build
    var changed = false
    if (dims.length != 0) {
      if (dims.length != dimensions.length)
        throw new IllegalArgumentException("Mismatching number of dimensions : program expects " + dimensions.length +", exec method received " + dims.length)
      for (i <- 0 until dims.length) {
        if (dimensions(i).size != dims(i)) {
          dimensions(i).size = dims(i)
          changed = true
        }
      }
    }
    filteredVariables foreach (v => if (changed) v.realloc else v.alloc)
    this
  }
  def exec: Program = {
    //build
    alloc()
    filteredVariables.foreach(_.bind)

    val globs = dimensions.map(_.size);
    kernel.enqueueNDRange(context.queue, globs.toArray, Array(1));

    filteredVariables.filter(_.mode.write).foreach(_.stale = true)

    this
  }
}

