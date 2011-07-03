package scalacl

package impl

import scala.collection._

import scala.collection.generic.CanBuildFrom
import com.nativelibs4java.opencl._


trait CLRunnable {
  def isOnlyInScalaSpace: Boolean
  
  def run(dims: Array[Int], args: Array[Any], eventsToWaitFor: Array[CLEvent])(implicit context: Context): CLEvent

  /*
  def run(dims: Array[Int], args: Array[Any], reads: Array[CLEventBoundContainer], writes: Array[CLEventBoundContainer])(implicit context: Context): Unit = {
    if (dims.sum > 0)
      CLEventBound.syncBlock(reads.flatMap(_.eventBoundComponents), writes.flatMap(_.eventBoundComponents), evts => {
        run(dims = dims, args = args, eventsToWaitFor = evts)
      })
  }*/
  def run(args: Array[Any], reads: Array[CLEventBoundContainer] = null, writes: Array[CLEventBoundContainer] = null)(dims: Array[Int], groupSizes: Array[Int] = null)(implicit context: Context): Unit = {
    if (dims.sum > 0) {
      lazy val defaultContainers = args collect { case c: CLEventBoundContainer => c }
      CLEventBound.syncBlock(
        Option(reads).getOrElse(defaultContainers).flatMap(_.eventBoundComponents), 
        Option(writes).getOrElse(defaultContainers).flatMap(_.eventBoundComponents), 
        evts => {
          run(dims = dims, args = args, eventsToWaitFor = evts)
        }
      )
    }
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
  def clType[T](implicit dataIO: CLDataIO[T]) = dataIO.clType
}

class CLFunction[A, B](
  val function: A => B,
  val outerDeclarations: Seq[String],
  declarations: Seq[String],
  val expressions: Seq[String],
  includedSources: Seq[String],
  extraInputBufferArgsIOs: Seq[CLDataIO[_]] = Seq(),
  extraOutputBufferArgsIOs: Seq[CLDataIO[_]] = Seq(),
  extraScalarArgsIOs: Seq[CLDataIO[_]] = Seq(),
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
  import CLFunction._
  
  def apply(arg: A): B =
    if (function == null)
      error("Function is not defined in Scala land !")
    else
      function(arg)

  case class InternalData(
    functionSource: String,
    kernelsSource: String,
    includedSources: Seq[String]
  )
  lazy val uid = newuid
  private lazy val functionName = "f" + uid
  
  private val internalData = {
    
    def toRxb(s: String) = {
      val rx = //"(^|\\b)" +
        java.util.regex.Matcher.quoteReplacement(s)// + "($|\\b)"
      rx
    }
    val ta = clType[A]
    val tb = clType[B]
  
    val inParams: Seq[String] = aIO.openCLKernelArgDeclarations(CLDataIO.InputPointer, 0)
    val inValueParams: Seq[String] = aIO.openCLKernelArgDeclarations(CLDataIO.Value, 0)
    val extraParams: Seq[String] =
      extraInputBufferArgsIOs.flatMap(_.openCLKernelArgDeclarations(CLDataIO.InputPointer, 0)) ++
      extraOutputBufferArgsIOs.flatMap(_.openCLKernelArgDeclarations(CLDataIO.OutputPointer, 0)) ++
      extraScalarArgsIOs.flatMap(_.openCLKernelArgDeclarations(CLDataIO.Value, 0))
    val outParams: Seq[String] = bIO.openCLKernelArgDeclarations(CLDataIO.OutputPointer, 0)

    case class DataIOInfo(io: CLDataIO[_], placeholder: String, isBuffer: Boolean, isExtraArg: Boolean)

    val iosAndPlaceholders: Seq[DataIOInfo] =
      (
        DataIOInfo(aIO, "_", isExtraArg = false, isBuffer = true) ::
        (extraInputBufferArgsIOs.map((_, true)) ++ extraOutputBufferArgsIOs.map((_, true)) ++ extraScalarArgsIOs.map((_, false)): Seq[(CLDataIO[_], Boolean)]).toList.zipWithIndex.map {
          case ((io, isBuffer), i) =>
            DataIOInfo(io, "_" + (i + 1), isExtraArg = false, isBuffer = isBuffer)
        }
      ).toSeq
      
    val varExprs = 
      (
        iosAndPlaceholders.flatMap(info => {
          info.io.openCLIntermediateKernelTupleElementsExprs(info.placeholder).map {
            case (expr, tupleIndexes) =>
              (expr, tupleIndexes, info)
          }
        })
      ).sortBy(-_._1.length).toArray // sort by decreasing expression length (enough to avoid conflicts) TODO unhack this !
       
    //println("varExprs = " + varExprs.toSeq)
    
    val indexVarName = "__cl_i"
    
    def replaceForFunction(argType: CLDataIO.ArgType, s: String,  i: String, isRange: Boolean) = if (s == null) null else {
      var r = s.replaceAll(toRxb(indexVar), indexVarName)
      r = r.replaceAll(toRxb(sizeVar), "size")
  
      var iExpr = 0
      val nExprs = varExprs.length
      while (iExpr < nExprs) {
        val (expr, tupleIndexes, DataIOInfo(io, placeholder, isBuffer, isExtraArg)) = varExprs(iExpr)
        def rawith(i: String) = io.openCLIthTupleElementNthItemExpr(argType, 0, tupleIndexes, i) 
        val x = if (isRange && isBuffer)
          "(" + rawith("0") + " + (" + i + ") * " + rawith("2") + ")" // buffer contains (from, to, by, inclusive). Value is (from + i * by)  
        else
          rawith(i)
        r = r.replaceAll(toRxb(expr), x)
        iExpr += 1
      }
      //r = r.replaceAll(toRxb(inVar), "(*in)")
      r
    }
  
    val indexHeader = """
        int """ + indexVarName + """ = get_global_id(0);
    """
    val sizeHeader =
        indexHeader + """
        if (""" + indexVarName + """ >= size)
            return;
    """
  
    def assignts(argType: CLDataIO.ArgType, i: String, isRange: Boolean) =
      expressions.zip(bIO.openCLKernelNthItemExprs(CLDataIO.OutputPointer, 0, i)).map {
        case (expression, (xOut, indexes)) => xOut + " = " + replaceForFunction(argType, expression, i, isRange)
      }.mkString(";\n\t")
  
    val funDecls = declarations.map(replaceForFunction(CLDataIO.Value, _, "0", false)).mkString("\n")
    lazy val funDeclsRange = declarations.map(replaceForFunction(CLDataIO.Value, _, "0", true)).mkString("\n")
    val functionSource = if (expressions.isEmpty) null else """
        inline void """ + functionName + """(
            """ + (inValueParams ++ extraParams ++ outParams).mkString(", ") + """
        ) {
            """ + indexHeader + """
            """ + funDecls + """
            """ + assignts(CLDataIO.Value, "0", false) + """;
        }
    """
    //println("expressions = " + expressions)
    //println("functionSource = " + functionSource)
  
    //def replaceForKernel(s: String) = if (s == null) null else replaceAllButIn(s).replaceAll(toRxb(inVar), "in[i]")
    val presenceName = "__cl_presence"
    val presenceParams = Seq("__global const " + CLFilteredArray.presenceCLType + "* " + presenceName)
    val kernDecls = declarations.map(replaceForFunction(CLDataIO.InputPointer, _, indexVarName, false)).reduceLeftOption(_ + "\n" + _).getOrElse("")
    lazy val kernDeclsRange = declarations.map(replaceForFunction(CLDataIO.InputPointer, _, indexVarName, true)).reduceLeftOption(_ + "\n" + _).getOrElse("")
    val assignt = assignts(CLDataIO.InputPointer, indexVarName, false)
    lazy val assigntRange = assignts(CLDataIO.InputPointer, indexVarName, true)
    var kernelsSource = outerDeclarations.mkString("\n")

    if (!expressions.isEmpty) 
      kernelsSource += """
        __kernel void array_array(
            int size,
            """ + (inParams ++ extraParams ++ outParams).mkString(", ") + """
        ) {
            """ + sizeHeader + """
            """ + kernDecls + """
            """ + assignt + """;
        }
        __kernel void filteredArray_filteredArray(
            int size,
            """ + (inParams ++ presenceParams ++ extraParams ++ outParams).mkString(", ") + """
        ) {
            """ + sizeHeader + """
            if (!""" + presenceName + "[" + indexVarName + """])
                return;
            """ + kernDecls + """
            """ + assignt + """;
        }
    """
    
    if (!expressions.isEmpty && aIO.t.erasure == classOf[Int]) {// || aIO.t.erasure == classOf[Integer])) {
      kernelsSource += """
        __kernel void range_array(
            int size,
            """ + (inParams ++ extraParams ++ outParams).mkString(", ") + """
        ) {
            """ + sizeHeader + """
            """ + kernDeclsRange + """
            """ + assigntRange + """;
        }
      """
    }
    if (verbose)
      println("[ScalaCL] Creating kernel with source <<<\n\t" + kernelsSource.replaceAll("\n", "\n\t") + "\n>>>")
      
    InternalData(
      functionSource = functionSource,
      kernelsSource = kernelsSource,
      includedSources = includedSources
    )
  }
  import internalData._

  val sourcesToInclude = if (expressions.isEmpty) null else includedSources ++ Seq(functionSource)
    
  override val sources = if (expressions.isEmpty) null else sourcesToInclude ++ Seq(kernelsSource)
  override val macros = Map[String, String]()
  override val compilerArguments = Seq[String]()

  override def isOnlyInScalaSpace = expressions.isEmpty

  def compose[C](f: CLFunction[C, A])(implicit cIO: CLDataIO[C]): CLFunction[C, B] = {
    compositions.synchronized {
      compositions.getOrElseUpdate((uid, f.uid), {
        // TODO FIXME !
        new CLFunction[C, B](
          function.compose(f.function), 
          outerDeclarations ++ f.outerDeclarations, 
          Seq(), 
          Seq(functionName + "(" + f.functionName + "(_))"), 
          sourcesToInclude ++ f.sourcesToInclude
        ).asInstanceOf[CLFunction[_, _]]
      }).asInstanceOf[CLFunction[C, B]]
    }
  }

  def and(f: CLFunction[A, B])(implicit el: B =:= Boolean): CLFunction[A, B] = {
    ands.synchronized {
      ands.getOrElseUpdate((uid, f.uid), {
        // TODO FIXME !
        new CLFunction[A, B](
          a => (function(a) && f.function(a)).asInstanceOf[B],
          Seq(),  
          Seq(), 
          Seq("(" + functionName + "(_) && " + f.functionName + "(_))"), 
          sourcesToInclude ++ f.sourcesToInclude
        ).asInstanceOf[CLFunction[_, _]]
      }).asInstanceOf[CLFunction[A, B]]
    }
  }

  /**
   * Args must be : Array(in, out, extraInputBuffers..., extraOutputBuffers..., extraScalars...) 
   */
  override def run(dims: Array[Int], args: Array[Any], eventsToWaitFor: Array[CLEvent])(implicit context: Context): CLEvent = 
  {
    val in = args(0)//.asInstanceOf[CLCollection[A]]
    val out = args(1)//.asInstanceOf[CLCollection[B]]
    val extraArgs = args.drop(2)
    
    val nExtraInputBufferArgs = extraInputBufferArgsIOs.size
    val nExtraOutputBufferArgs = extraOutputBufferArgsIOs.size
    val nExtraBufferArgs = nExtraInputBufferArgs + nExtraOutputBufferArgs
    val nExtraScalarArgs = extraScalarArgsIOs.size
    
    val extraInputBufferArgs: Array[CLArray[_]] = extraArgs.take(nExtraInputBufferArgs).map(_.asInstanceOf[CLArray[_]])
    val extraOutputBufferArgs: Array[CLArray[_]] = extraArgs.slice(nExtraInputBufferArgs, nExtraBufferArgs).map(_.asInstanceOf[CLArray[_]])
    val extraScalarArgs: Array[Any] = extraArgs.drop(nExtraBufferArgs)
    
    run(dims, in, out, extraInputBufferArgs, extraOutputBufferArgs, extraScalarArgs, eventsToWaitFor)
  }
  /*override def withExtraArgs(
    extraInputBufferArgs: Array[CLArray[_]],
    extraOutputBufferArgs: Array[CLArray[_]],
    extraScalarArgs: Array[Any]
) = {
    new CLFunction[A, B]() {
    }
  }*/

  protected def getActualArgs(arg: Any, skipPresenceInFilteredArray: Boolean = false): (Array[AnyRef], Array[CLGuardedBuffer[Any]]) = arg match {
    case g: CLGuardedBuffer[_] =>
      (Array(g.buffer), Array(g.asInstanceOf[CLGuardedBuffer[Any]]))
    case a: CLArray[_] =>
      val bufs = a.buffers
      (bufs.map(_.buffer), bufs)
    case r: CLRange =>
      val bufs = Array(r.buffer.asInstanceOf[CLGuardedBuffer[Any]])
      (bufs.map(_.buffer), bufs)
    case f: CLFilteredArray[_] =>
      var bufs = f.array.buffers
      if (!skipPresenceInFilteredArray)
        bufs = bufs :+ f.presence.asInstanceOf[CLGuardedBuffer[Any]]

      (bufs.map(_.buffer), bufs)
  }

      
  def run(
    dims: Array[Int], 
    in: Any,//CLCollection[A],
    out: Any,//CLCollection[B],
    extraInputBufferArgs: Array[CLArray[_]],
    extraOutputBufferArgs: Array[CLArray[_]],
    extraScalarArgs: Array[Any],
    eventsToWaitFor: Array[CLEvent]
  )(implicit context: Context): CLEvent = {
    
    val (inArgs, readBufs) = getActualArgs(in)
    val (outArgs, writeBufs) = getActualArgs(out, skipPresenceInFilteredArray = true)
    
    val extraInBufs = extraInputBufferArgs.flatMap(_.buffers)
    val extraOutBufs = extraOutputBufferArgs.flatMap(_.buffers)
    
    val (kernelName, size: Int) = (in, out) match {
      case (in: CLArray[_], out: CLGuardedBuffer[Any]) => // case of CLArray.filter (output to the presence array of a CLFilteredArray
        ("array_array", in.length)
      case (in: CLArray[_], out: CLArray[_]) => // CLArray.map
        ("array_array", in.length)
      case (in: CLRange, out: CLArray[_]) => // CLRange.map
        ("range_array", out.length)
      case (in: CLRange, out: CLGuardedBuffer[_]) => // CLRange.map
        ("range_array", in.length)
      case (in: CLFilteredArray[_], out: CLFilteredArray[Any]) => // CLFilteredArray.map
        ("filteredArray_filteredArray", in.array.length)
      case _ => 
        error("ERROR, in = " + in + ", out = " + out)
    }
    val kernel = getKernel(context, kernelName)
    assert(kernel.getFunctionName() == kernelName, "not getting the expected kernel !")
    
    val kernelArgs: Array[AnyRef] = Array(size.asInstanceOf[AnyRef]) ++ inArgs ++ outArgs ++ extraInBufs.map(_.buffer) ++ extraOutBufs.map(_.buffer) ++ extraScalarArgs.map(_.asInstanceOf[AnyRef])
    val readBuffers: Array[CLEventBound] = readBufs ++ extraInBufs
    val writeBuffers: Array[CLEventBound] = writeBufs ++ extraOutBufs
    
    CLEventBound.syncBlock(
      readBuffers, 
      writeBuffers, 
      evts => {
        kernel.synchronized {
          try {
            //println("kernelArgs = " + kernelArgs.map(a => a + ": " + a.getClass.getName).mkString(", "))
            kernel.setArgs(kernelArgs:_*)
            if (verbose)
              println("[ScalaCL] Enqueuing kernel " + kernelName + " with dims " + dims.mkString(", "))
            kernel.enqueueNDRange(context.queue, dims, evts ++ eventsToWaitFor:_*)
          } catch { case ex =>
            ex.printStackTrace(System.out)
            throw ex
          }
        }
      }
    )
    
  }
}