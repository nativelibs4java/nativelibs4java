package scalacl

package impl

import scala.collection._

import scala.collection.generic.CanBuildFrom
import com.nativelibs4java.opencl._


trait CLRunnable {
  def isOnlyInScalaSpace: Boolean
  
  protected def run(dims: Array[Int], args: Array[Any], eventsToWaitFor: Array[CLEvent])(implicit context: Context): CLEvent

  def run(args: Array[Any], reads: Array[CLEventBoundContainer] = null, writes: Array[CLEventBoundContainer] = null)(dims: Array[Int], groupSizes: Array[Int] = null)(implicit context: Context): Unit = {
    if (dims.sum > 0) {
      lazy val defaultContainers = args collect { case c: CLEventBoundContainer => c }
      CLEventBound.syncBlock(
        CLEventBound.flatten(Option(reads).getOrElse(defaultContainers)), 
        CLEventBound.flatten(Option(writes).getOrElse(defaultContainers)), 
        evts => {
          run(dims = dims, args = args, eventsToWaitFor = evts)
        }
      )
    }
  }
}

import CLFunctionCode._

class CLFunction[A, B](
  val function: A => B,
  val code: CLFunctionCode[A, B]
)(
  implicit
  val aIO: CLDataIO[A],
  val bIO: CLDataIO[B]
)
extends (A => B)
   with CLRunnable
{
  def this(
    function: A => B,
    outerDeclarations: Array[String],
    declarations: Array[String],
    expressions: Array[String],
    includedSources: Array[String],
    extraArgsIOs: CapturedIOs = CapturedIOs()
  )(implicit aIO: CLDataIO[A], bIO: CLDataIO[B]) = {
    this(
      function,
      if (expressions.isEmpty) null
      else
        new CLFunctionCode[A, B](
          buildSourceData[A, B](
            outerDeclarations,
            declarations,
            expressions,
            includedSources,
            extraArgsIOs
          )
        )
    )
  }
  override def isOnlyInScalaSpace = code == null
  
  import code._
  
  def apply(arg: A): B =
    if (function == null)
      error("Function is not defined in Scala land !")
    else
      function(arg)

  def compose[C](f: CLFunction[C, A])(implicit cIO: CLDataIO[C]): CLFunction[C, B] = {
    new CLFunction[C, B](
      function.compose(f.function),
      code.compose(f.code)
    )
  }

  def and(f: CLFunction[A, B])(implicit el: B =:= Boolean): CLFunction[A, B] = {
    new CLFunction[A, B](
      a => (function(a) && f.function(a)).asInstanceOf[B],
      code.and(f.code)
    )
  }

  /**
   * Args must be : Array(in, out, extraInputBuffers..., extraOutputBuffers..., extraScalars...) 
   */
  override def run(dims: Array[Int], args: Array[Any], eventsToWaitFor: Array[CLEvent])(implicit context: Context): CLEvent = 
  {
    val in = args(0)//.asInstanceOf[CLCollection[A]]
    val out = args(1)//.asInstanceOf[CLCollection[B]]
    val extraArgs = args.drop(2)
    
    val nExtraInputBufferArgs = extraArgsIOs.inputBuffers.size
    val nExtraOutputBufferArgs = extraArgsIOs.outputBuffers.size
    val nExtraBufferArgs = nExtraInputBufferArgs + nExtraOutputBufferArgs
    val nExtraScalarArgs = extraArgsIOs.scalars.size
    
    val extraInputBufferArgs: Array[CLArray[Any]] = extraArgs.take(nExtraInputBufferArgs).map(_.asInstanceOf[CLArray[Any]])
    val extraOutputBufferArgs: Array[CLArray[Any]] = extraArgs.slice(nExtraInputBufferArgs, nExtraBufferArgs).map(_.asInstanceOf[CLArray[Any]])
    val extraScalarArgs: Array[Any] = extraArgs.drop(nExtraBufferArgs)
    
    run(dims, in, out, extraInputBufferArgs, extraOutputBufferArgs, extraScalarArgs, eventsToWaitFor)
  }
  
  def withCapture(
    extraInputBufferArgs: Array[CLArray[Any]],
    extraOutputBufferArgs: Array[CLArray[Any]],
    extraScalarArgs: Array[Any]
  ) = {
    new CLFunction[A, B](function, code) {
      override def run(dims: Array[Int], args: Array[Any], eventsToWaitFor: Array[CLEvent])(implicit context: Context): CLEvent = 
      {
        val Array(in, out) = args
        run(dims, in, out, extraInputBufferArgs, extraOutputBufferArgs, extraScalarArgs, eventsToWaitFor)
      }
    }
  }

  protected def getActualArgs(arg: Any, skipPresenceInFilteredArray: Boolean = false): (Array[AnyRef], Array[CLGuardedBuffer[Any]]) = arg match {
    case g: CLGuardedBuffer[_] =>
      (Array(g.buffer), Array(g.asInstanceOf[CLGuardedBuffer[Any]]))
    case a: CLArray[Any] =>
      val bufs = a.buffers
      (bufs.map(_.buffer), bufs)
    case r: CLRange =>
      val bufs = Array(r.buffer.asInstanceOf[CLGuardedBuffer[Any]])
      (bufs.map(_.buffer), bufs)
    case f: CLFilteredArray[_] =>
      var bufs = f.array.buffers
      // prepend presence :
      if (!skipPresenceInFilteredArray)
        bufs = f.presence.asInstanceOf[CLGuardedBuffer[Any]] +: bufs

      (bufs.map(_.buffer), bufs)
  }

  protected def getFunctionKernelNameAndSizeFromInAndOut(in: Any, out: Any) = (in, out) match {
    case (in: CLArray[Any], out: CLGuardedBuffer[Any]) => // case of CLArray.filter (output to the presence array of a CLFilteredArray
      ("array_array", in.length)
    case (in: CLArray[Any], out: CLArray[Any]) => // CLArray.map
      ("array_array", in.length)
    case (in: CLRange, out: CLArray[Any]) => // CLRange.map
      ("range_array", out.length)
    case (in: CLRange, out: CLGuardedBuffer[_]) => // CLRange.map
      ("range_array", in.length)
    case (in: CLFilteredArray[_], out: CLFilteredArray[Any]) => // CLFilteredArray.map
      ("filteredArray_filteredArray", in.array.length)
    case _ => 
      error("ERROR, in = " + in + ", out = " + out)
  }
      
  def run(
    dims: Array[Int], 
    in: Any,//CLCollection[A],
    out: Any,//CLCollection[B],
    extraInputBufferArgs: Array[CLArray[Any]],
    extraOutputBufferArgs: Array[CLArray[Any]],
    extraScalarArgs: Array[Any],
    eventsToWaitFor: Array[CLEvent]
  )(implicit context: Context): CLEvent = {
    
    val (inArgs, readBufs) = getActualArgs(in)
    val (outArgs, writeBufs) = getActualArgs(out, skipPresenceInFilteredArray = true)
    
    val extraInBufs = extraInputBufferArgs.flatMap(_.buffers)
    val extraOutBufs = extraOutputBufferArgs.flatMap(_.buffers)
    
    val (kernelName, size: Int) = getFunctionKernelNameAndSizeFromInAndOut(in, out)
    
    val kernel = code.getKernel(context, kernelName)
    assert(kernel.getFunctionName() == kernelName, "not getting the expected kernel !")
    
    val kernelArgs: Array[AnyRef] = 
      Array(size.asInstanceOf[AnyRef]) ++ 
      inArgs ++ 
      outArgs ++ 
      extraInBufs.map(_.buffer) ++ 
      extraOutBufs.map(_.buffer) ++ 
      extraScalarArgs.map(_.asInstanceOf[AnyRef])
      
    val readBuffers: Array[CLEventBound] = readBufs ++ extraInBufs
    val writeBuffers: Array[CLEventBound] = writeBufs ++ extraOutBufs
    
    CLEventBound.syncBlock(
      readBuffers, 
      writeBuffers, 
      evts => {
        kernel.synchronized {
          try {
            //println("kernelArgs = \n\t" + kernelArgs.map(a => a + ": " + a.getClass.getName).mkString("\n\t"))
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