package scalacl

package impl

import scala.collection._

import scala.collection.generic.CanBuildFrom
import com.nativelibs4java.opencl._


trait CLRunnable {
  def run(dims: Array[Int], args: Array[Any], eventsToWaitFor: Array[CLEvent])(implicit context: ScalaCLContext): CLEvent

  def run(dims: Array[Int], args: Array[Any], reads: Array[CLEventBoundContainer], writes: Array[CLEventBoundContainer])(implicit context: ScalaCLContext): Unit = {
    if (dims.sum > 0)
      CLEventBound.syncBlock(reads.flatMap(_.eventBoundComponents), writes.flatMap(_.eventBoundComponents), evts => {
        run(dims, args, evts)
      })
  }
}

object CLFunction {
  val compositions = new mutable.HashMap[(Long, Long), CLFunction[_, _]]
  val ands = new mutable.HashMap[(Long, Long), CLFunction[_, _]]
  private var nextuid = 1
  protected def newuid = this.synchronized {
    val uid = nextuid
    nextuid += 1
    uid
  }
}

class CLFunction[A, B](
  val function: A => B,
  declarations: Seq[String],
  val expressions: Seq[String],
  includedSources: Seq[String],
  inVar: String = "_",
  indexVar: String = "$i",
  sizeVar: String = "$size"
)(
  implicit
  val aIO: CLDataIO[A],
  val bIO: CLDataIO[B]
)
extends (A => B)
   with CLCode
   with CLRunnable
{
  lazy val uid = CLFunction.newuid
  lazy val functionName = "f" + uid

  def apply(arg: A): B =
    if (function == null)
      error("Function is not defined in Scala land !")
    else
      function(arg)

  private def toRxb(s: String) = {
    val rx = //"(^|\\b)" +
      java.util.regex.Matcher.quoteReplacement(s)// + "($|\\b)"
    rx
  }
  val ta = clType[A]
  val tb = clType[B]

  val inParams = aIO.openCLKernelArgDeclarations(true, 0).mkString(", ")
  val outParams = bIO.openCLKernelArgDeclarations(false, 0).mkString(", ")
  val varExprs = aIO.openCLIntermediateKernelTupleElementsExprs(inVar).sortBy(-_._1.length).toArray
  //println("varExprs = " + varExprs.toSeq)
  def replaceForFunction(s: String,  i: String) = if (s == null) null else {
    var r = s.replaceAll(toRxb(indexVar), "i")
    r = r.replaceAll(toRxb(sizeVar), "size")

    var iExpr = 0
    val nExprs = varExprs.length
    while (iExpr < nExprs) {
      val (expr, tupleIndexes) = varExprs(iExpr)
      val x = aIO.openCLIthTupleElementNthItemExpr(true, 0, tupleIndexes, i)
      r = r.replaceAll(toRxb(expr), x)
      iExpr += 1
    }
    //r = r.replaceAll(toRxb(inVar), "(*in)")
    r
  }

  val indexHeader = """
      int i = get_global_id(0);
  """
  val sizeHeader =
      indexHeader + """
      if (i >= size)
          return;
  """

  def assignts(i: String) =
    expressions.zip(bIO.openCLKernelNthItemExprs(false, 0, i)).map {
      case (expression, (xOut, indexes)) => xOut + " = " + replaceForFunction(expression, i)
    }.mkString(";\n\t")

  val funDecls = declarations.map(replaceForFunction(_, "0")).mkString("\n")
  val functionSource = if (expressions.isEmpty) null else """
      inline void """ + functionName + """(
          """ + inParams + """,
          """ + outParams + """
      ) {
          """ + indexHeader + """
          """ + funDecls + """
          """ + assignts("0") + """;
      }
  """
  //println("expressions = " + expressions)
  //println("functionSource = " + functionSource)

  //def replaceForKernel(s: String) = if (s == null) null else replaceAllButIn(s).replaceAll(toRxb(inVar), "in[i]")
  val presenceParam = "__global const char* presence"
  val kernDecls = declarations.map(replaceForFunction(_, "i")).reduceLeftOption(_ + "\n" + _).getOrElse("")
  val assignt = assignts("i")
  val kernelsSource = if (expressions.isEmpty) null else """
      __kernel void array_array""" + uid + """(
          int size,
          """ + inParams + """,
          """ + outParams + """
      ) {
          """ + sizeHeader + """
          """ + kernDecls + """
          """ + assignt + """;
      }
      __kernel void filteredArray_filteredArray""" + uid + """(
          int size,
          """ + inParams + """,
          """ + presenceParam + """,
          """ + outParams + """
      ) {
          """ + sizeHeader + """
          if (!presence[i])
              return;
          """ + kernDecls + """
          """ + assignt + """;
      }
  """
  if (verbose)
    println("[ScalaCL] Creating kernel with source <<<\n\t" + kernelsSource.replaceAll("\n", "\n\t") + "\n>>>")

  val sourcesToInclude = if (expressions.isEmpty) null else includedSources ++ Seq(functionSource)
  override val sources = if (expressions.isEmpty) null else sourcesToInclude ++ Seq(kernelsSource)
  override val macros = Map[String, String]()
  override val compilerArguments = Seq[String]()

  def isOnlyInScalaSpace = expressions.isEmpty

  import CLFunction._
  def compose[C](f: CLFunction[C, A])(implicit cIO: CLDataIO[C]): CLFunction[C, B] = {
    compositions.synchronized {
      compositions.getOrElseUpdate((uid, f.uid), {
        // TODO FIXME !
        new CLFunction[C, B](function.compose(f.function), Seq(), Seq(functionName + "(" + f.functionName + "(_))"), sourcesToInclude ++ f.sourcesToInclude).asInstanceOf[CLFunction[_, _]]
      }).asInstanceOf[CLFunction[C, B]]
    }
  }

  def and(f: CLFunction[A, B])(implicit el: B =:= Boolean): CLFunction[A, B] = {
    ands.synchronized {
      ands.getOrElseUpdate((uid, f.uid), {
        // TODO FIXME !
        new CLFunction[A, B](a => (function(a) && f.function(a)).asInstanceOf[B], Seq(), Seq("(" + functionName + "(_) && " + f.functionName + "(_))"), sourcesToInclude ++ f.sourcesToInclude).asInstanceOf[CLFunction[_, _]]
      }).asInstanceOf[CLFunction[A, B]]
    }
  }

  override def run(dims: Array[Int], args: Array[Any], eventsToWaitFor: Array[CLEvent])(implicit context: ScalaCLContext): CLEvent = {
    val (kernelName, size: Int, buffers: Array[CLGuardedBuffer[Any]]) = args match {
      case Array(in: CLArray[_], out: CLGuardedBuffer[Any]) =>
        // case of CLArray.filter (output to the presence array of a CLFilteredArray
        ("array_array" + uid, in.length, in.buffers ++ Array(out): Array[CLGuardedBuffer[Any]])
      case Array(in: CLArray[_], out: CLArray[_]) =>
        // CLArray.map
        ("array_array" + uid, in.length, in.buffers ++ out.buffers: Array[CLGuardedBuffer[Any]])
      case Array(in: CLFilteredArray[_], out: CLFilteredArray[Any]) =>
        // CLFilteredArray.map
        ("filteredArray_filteredArray" + uid, in.array.length, in.array.buffers ++ Array(in.presence.asInstanceOf[CLGuardedBuffer[Any]]) ++ out.array.buffers: Array[CLGuardedBuffer[Any]])
      case _ =>
        error("ERROR, args = " + args.mkString(", "))
    }
    val kernel = getKernel(context, kernelName)
    assert(kernel.getFunctionName() == kernelName, "not getting the expected kernel !")
    
    kernel.synchronized {
      val args = Array(size.asInstanceOf[Object]) ++ buffers.map(_.buffer: Object)
      //println("kernelName = " + kernelName + ", args = " + args.toSeq + ", kernelsSource = " + kernelsSource)
      try {
        kernel.setArgs(args:_*)
        //println("dims = " + dims.toSeq)
        if (verbose)
          println("[ScalaCL] Enqueuing kernel " + kernelName + " with dims " + dims.mkString(", "))
        kernel.enqueueNDRange(context.queue, dims, eventsToWaitFor:_*)
      } catch { case ex =>
        ex.printStackTrace(System.out)
        throw ex
      }
    }
  }
}