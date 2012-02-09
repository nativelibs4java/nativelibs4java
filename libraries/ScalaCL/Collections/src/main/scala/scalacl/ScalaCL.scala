import scala.collection.generic.CanBuildFrom
import com.nativelibs4java.opencl._
import scala.collection.JavaConversions._
import scalacl.impl._

package scalacl {
  trait AbstractProduct extends Product {
    def productValues: Array[Any]
    override def productArity = productValues.length
    override def productElement(n: Int) = productValues(n)
    override def productIterator = productValues.toIterator
    override def canEqual(that: Any) = getClass.isInstance(that.asInstanceOf[AnyRef])
  }
  class Context(val context: CLContext, val queue: CLQueue) extends AbstractProduct {
    def this(context: CLContext) = this(context, context.createDefaultQueue())//createDefaultOutOfOrderQueueIfPossible())
    def this() = this(JavaCL.createBestContext(CLPlatform.DeviceFeature.OutOfOrderQueueSupport, CLPlatform.DeviceFeature.MaxComputeUnits))

    def release = {
      queue.release
      context.release
    }
    override def toString = "Context(platform = " + context.getPlatform.getName + ", devices = " + context.getDevices.mkString(", ") + ")"
    
    override def productValues = Array(context, queue)
    //println("Is out of order queue : " + queue.getProperties.contains(CLDevice.QueueProperties.OutOfOrderExecModeEnable))
  }
  object Context {
    def best = new Context()
    def best(preferredFeatures: CLPlatform.DeviceFeature*) = {
      new Context(JavaCL.createBestContext(preferredFeatures:_*))
    } 
    
    def apply = best
    def apply(devices: CLDevice*) = new Context(JavaCL.createContext(null, devices:_*))
    
    def platforms = JavaCL.listPlatforms.map(Platform)
  }
  case class Platform(platform: CLPlatform) {
    def name = platform.getName
    def vendor = platform.getVendor
    def devices = platform.listAllDevices(true)
    def bestDevice = platform.getBestDevice
    def bestDevice(preferredFeatures: CLPlatform.DeviceFeature*) = 
      CLPlatform.getBestDevice(
        java.util.Arrays.asList(preferredFeatures:_*), 
        java.util.Arrays.asList(devices:_*)
      )
  }
}
package object scalacl {
  
  var verbose =
    "1" == System.getenv("SCALACL_VERBOSE")
  
  var useFastRelaxedMath =
    "0" != System.getenv("SCALACL_STRICT_MATH")
  
  var useScalaFunctions =
    "1" == System.getenv("SCALACL_USE_SCALA_FUNCTIONS")
  
  var enforceUsingOpenCL =
    "1" == System.getenv("SCALACL_ENFORCE_OPENCL")
    
  type Device = CLDevice
  
  // backward compatibility
  @deprecated
  type ScalaCLContext = Context
  
  
  val GPU = CLPlatform.DeviceFeature.GPU
  val CPU = CLPlatform.DeviceFeature.CPU
  val DoubleSupport = CLPlatform.DeviceFeature.DoubleSupport
  val MaxComputeUnits = CLPlatform.DeviceFeature.MaxComputeUnits
  val NativeEndianness = CLPlatform.DeviceFeature.NativeEndianness
  val ImageSupport = CLPlatform.DeviceFeature.ImageSupport
  val OutOfOrderQueueSupport = CLPlatform.DeviceFeature.OutOfOrderQueueSupport
  val MostImageFormats = CLPlatform.DeviceFeature.MostImageFormats
  
  def customCode(
    source: String,
    compilerArguments: Array[String] = Array(),
    macros: Map[String, String] = Map()
  ): CLCode = {
    new CLSimpleCode(
      Array(source),
      compilerArguments,
      macros
    )
  }
  
  private[scalacl] def executingScalaFallbackOperation(opName: => String) = {
    if (enforceUsingOpenCL)
      throw new RuntimeException("Not using OpenCL !")
    if (verbose)
      println("Running " + opName + " instead of OpenCL version")
  }
  
  private[scalacl] def reuse[T](value: Any, create: => T): T =
    if (value != null && value.isInstanceOf[T])
      value.asInstanceOf[T]
    else
      create
  
  implicit def Context2Context(sc: Context) = sc.context

  /*implicit def canBuildArrayFromIndexedSeq[A](implicit context: Context, io: CLDataIO[A]) =
    new CLCanBuildFrom[CLIndexedSeq[_], A, CLArray[A]] {
      override def dataIO = io
      override def apply(from: CLIndexedSeq[_]) = CLArray.newBuilder[A](context, dataIO)
      override def apply() = CLArray.newBuilder[A](context, dataIO)
    }
    */
  implicit def canBuildIndexedSeqFromIndexedSeq[A](implicit context: Context, io: CLDataIO[A]) =
    new CLCanBuildFrom[CLIndexedSeq[_], A, CLIndexedSeq[A]] {
      override def dataIO = io
      override def apply(from: CLIndexedSeq[_]) = CLFilteredArray.newBuilder[A]//(context, dataIO)
      override def apply() = CLFilteredArray.newBuilder[A]//(context, dataIO)
    }
  implicit def canBuildArrayFromArray[A](implicit context: Context, io: CLDataIO[A]) =
    new CLCanBuildFrom[CLArray[_], A, CLArray[A]] {
      override def dataIO = io
      override def apply(from: CLArray[_]) = CLArray.newBuilder[A](context, dataIO)
      override def apply() = CLArray.newBuilder[A](context, dataIO)
    }
/*
  implicit def canBuildFromFilteredArray[A](implicit context: Context, io: CLDataIO[A]) =
    new CLCanBuildFrom[CLFilteredArray[_], A, CLFilteredArray[A]] {
      override def dataIO = io
      override def apply(from: CLFilteredArray[_]) = CLFilteredArray.newBuilder[A](context, dataIO)
      override def apply() = CLFilteredArray.newBuilder[A](context, dataIO)
    }
    */

  implicit def canFilterFromIndexedSeq[A](implicit ctx: Context, io: CLDataIO[A]) =
    new CLCanFilterFrom[CLIndexedSeq[A], A, CLFilteredArray[A]] {
      override def dataIO = io
      override def context = ctx
      def rawLength(from: CLIndexedSeq[A]): Int = from match {
        case r: CLRange =>
          r.length
        case a: CLArray[A] =>
          a.length
        case fa: CLFilteredArray[A] =>
          fa.array.length
      }
      def newFilterResult(from: CLIndexedSeq[A]) = new CLFilteredArray[A](rawLength(from))
    }

  implicit def CastCanBuildFrom2CL[Repr, B, That](bf: CanBuildFrom[Repr, B, That]): CLCanBuildFrom[Repr, B, That] = bf match {
      case cbf: CLCanBuildFrom[Repr, B, That] =>
        cbf
      case _ =>
        error("Not a " + classOf[CLCanBuildFrom[Repr, B, That]].getName + ": " + bf)
    }
  //implicit def CanBuildFrom2CLDataIO[Repr, B, That](bf: CanBuildFrom[Repr, B, That]): CLDataIO[B] =
  //  bf.dataIO

  implicit def Tuple2ToCLDataIO[T1, T2](
    implicit
      io1: CLDataIO[T1],
      io2: CLDataIO[T2]) =
    new CLTupleDataIO[(T1, T2)](
      Array(io1, io2).map(_.asInstanceOf[CLDataIO[Any]]),
      t => Array(t._1, t._2),
      a => (
        a(0).asInstanceOf[T1],
        a(1).asInstanceOf[T2]
      )
    )


  implicit def Tuple3ToCLDataIO[T1, T2, T3](
    implicit
      io1: CLDataIO[T1],
      io2: CLDataIO[T2],
      io3: CLDataIO[T3]) =
    new CLTupleDataIO[(T1, T2, T3)](
      Array(io1, io2, io3).map(_.asInstanceOf[CLDataIO[Any]]),
      t => Array(t._1, t._2, t._3),
      a => (
        a(0).asInstanceOf[T1],
        a(1).asInstanceOf[T2],
        a(2).asInstanceOf[T3]
      )
    )

  implicit def Tuple4ToCLDataIO[T1, T2, T3, T4](
    implicit
      io1: CLDataIO[T1],
      io2: CLDataIO[T2],
      io3: CLDataIO[T3],
      io4: CLDataIO[T4]) =
    new CLTupleDataIO[(T1, T2, T3, T4)](
      Array(io1, io2, io3, io4).map(_.asInstanceOf[CLDataIO[Any]]),
      t => Array(t._1, t._2, t._3, t._4),
      a => (
        a(0).asInstanceOf[T1],
        a(1).asInstanceOf[T2],
        a(2).asInstanceOf[T3],
        a(3).asInstanceOf[T4]
      )
    )

  implicit def Tuple5ToCLDataIO[T1, T2, T3, T4, T5](
    implicit
      io1: CLDataIO[T1],
      io2: CLDataIO[T2],
      io3: CLDataIO[T3],
      io4: CLDataIO[T4],
      io5: CLDataIO[T5]) =
    new CLTupleDataIO[(T1, T2, T3, T4, T5)](
      Array(io1, io2, io3, io4, io5).map(_.asInstanceOf[CLDataIO[Any]]),
      t => Array(t._1, t._2, t._3, t._4, t._5),
      a => (
        a(0).asInstanceOf[T1],
        a(1).asInstanceOf[T2],
        a(2).asInstanceOf[T3],
        a(3).asInstanceOf[T4],
        a(4).asInstanceOf[T5]
      )
    )

  implicit def Tuple6ToCLDataIO[T1, T2, T3, T4, T5, T6](
    implicit
      io1: CLDataIO[T1],
      io2: CLDataIO[T2],
      io3: CLDataIO[T3],
      io4: CLDataIO[T4],
      io5: CLDataIO[T5],
      io6: CLDataIO[T6]) =
    new CLTupleDataIO[(T1, T2, T3, T4, T5, T6)](
      Array(io1, io2, io3, io4, io5, io6).map(_.asInstanceOf[CLDataIO[Any]]),
      t => Array(t._1, t._2, t._3, t._4, t._5, t._6),
      a => (
        a(0).asInstanceOf[T1],
        a(1).asInstanceOf[T2],
        a(2).asInstanceOf[T3],
        a(3).asInstanceOf[T4],
        a(4).asInstanceOf[T5],
        a(5).asInstanceOf[T6]
      )
    )

  implicit def Tuple7ToCLDataIO[T1, T2, T3, T4, T5, T6, T7](
    implicit
      io1: CLDataIO[T1],
      io2: CLDataIO[T2],
      io3: CLDataIO[T3],
      io4: CLDataIO[T4],
      io5: CLDataIO[T5],
      io6: CLDataIO[T6],
      io7: CLDataIO[T7]) =
    new CLTupleDataIO[(T1, T2, T3, T4, T5, T6, T7)](
      Array(io1, io2, io3, io4, io5, io6, io7).map(_.asInstanceOf[CLDataIO[Any]]),
      t => Array(t._1, t._2, t._3, t._4, t._5, t._6, t._7),
      a => (
        a(0).asInstanceOf[T1],
        a(1).asInstanceOf[T2],
        a(2).asInstanceOf[T3],
        a(3).asInstanceOf[T4],
        a(4).asInstanceOf[T5],
        a(5).asInstanceOf[T6],
        a(6).asInstanceOf[T7]
      )
    )

  implicit def Tuple8ToCLDataIO[T1, T2, T3, T4, T5, T6, T7, T8](
    implicit
      io1: CLDataIO[T1],
      io2: CLDataIO[T2],
      io3: CLDataIO[T3],
      io4: CLDataIO[T4],
      io5: CLDataIO[T5],
      io6: CLDataIO[T6],
      io7: CLDataIO[T7],
      io8: CLDataIO[T8]) =
    new CLTupleDataIO[(T1, T2, T3, T4, T5, T6, T7, T8)](
      Array(io1, io2, io3, io4, io5, io6, io7, io8).map(_.asInstanceOf[CLDataIO[Any]]),
      t => Array(t._1, t._2, t._3, t._4, t._5, t._6, t._7, t._8),
      a => (
        a(0).asInstanceOf[T1],
        a(1).asInstanceOf[T2],
        a(2).asInstanceOf[T3],
        a(3).asInstanceOf[T4],
        a(4).asInstanceOf[T5],
        a(5).asInstanceOf[T6],
        a(6).asInstanceOf[T7],
        a(7).asInstanceOf[T8]
      )
    )

  implicit val IntCLDataIO = CLIntDataIO
  implicit val ShortCLDataIO = CLShortDataIO
  implicit val ByteCLDataIO = CLByteDataIO
  implicit val CharCLDataIO = CLCharDataIO
  implicit val LongCLDataIO = CLLongDataIO
  implicit val BooleanCLDataIO = CLBooleanDataIO
  implicit val FloatCLDataIO = CLFloatDataIO
  implicit val DoubleCLDataIO = CLDoubleDataIO
  //implicit def AnyValCLDataIO[T <: AnyVal](implicit t: ClassManifest[T]) = new CLValDataIO[T]

  implicit def Expression2CLFunction[K, V](fx: (K => V, Array[String]))(implicit kIO: CLDataIO[K], vIO: CLDataIO[V]): CLFunction[K, V] = {
    val (function, expressions) = fx
    new CLFunction[K, V](function, Array(), Array(), expressions, Array())
  }
  implicit def ExpressionWithCaptures2CLFunction[K, V](fx: (K => V, Array[String], impl.CapturedIOs))(implicit kIO: CLDataIO[K], vIO: CLDataIO[V]): CLFunction[K, V] = {
    val (function, expressions, captures) = fx
    new CLFunction[K, V](function, Array(), Array(), expressions, Array(), captures)
  }

  /**
   * This MUST be transformed by the ScalaCL compiler plugin to be usable in an OpenCL context (otherwise operations will happen in Scala land
   */
  implicit def Function2CLFunction[K, V](f: K => V)(implicit kIO: CLDataIO[K], vIO: CLDataIO[V]): CLFunction[K, V] = {
    new CLFunction[K, V](f, Array(), Array(), Array(), Array())
  }

  private val functionsCache = scala.collection.mutable.HashMap[Long, CLFunction[_, _]]()
  
  def getCachedFunction[K, V](
    uid: Long, 
    function: K => V, 
    outerDeclarations: => Array[String], 
    declarations: => Array[String], 
    expressions: => Array[String], 
    extraInputBufferArgsIOs: => Array[CLDataIO[Any]],
    extraOutputBufferArgsIOs: => Array[CLDataIO[Any]],
    extraScalarArgsIOs: => Array[CLDataIO[Any]]
  )(implicit kIO: CLDataIO[K], vIO: CLDataIO[V]): CLFunction[K, V] = {
    functionsCache synchronized {
      functionsCache.getOrElseUpdate(
        uid, 
        new CLFunction[K, V](
          function, 
          outerDeclarations, 
          declarations, 
          expressions, 
          Array(),
          CapturedIOs(
            extraInputBufferArgsIOs,
            extraOutputBufferArgsIOs,
            extraScalarArgsIOs
          )
        ).asInstanceOf[CLFunction[_, _]]
      ).asInstanceOf[CLFunction[K, V]]
    }
  }
  
  class CLTransformableRange(r: Range)(implicit context: Context) {
    def toCLRange = new CLRange(r)
    def toCLArray = toCLRange.toCLArray
    def toCL = toCLRange
    def cl = toCLRange
  }
  implicit def Range2CLRangeMethods(r: Range)(implicit context: Context) =
    new CLTransformableRange(r)
    
  /*implicit def RichIndexedSeqCL[T](c: IndexedSeq[T])(implicit context: Context, dataIO: CLDataIO[T]) = new {
    def toCLArray = CLArray.fromSeq(c)
    def toCL = toCLArray
    def cl = toCLArray
  }*/
  class CLTransformableSeq[T](seq: Seq[T])(implicit context: Context, io: CLDataIO[T]) {
    def toCLArray = CLArray.fromSeq(seq)
    def toCL = toCLArray
    def cl = toCLArray
  }
  
  implicit def Seq2CLTransformableSeq[T](seq: Seq[T])(implicit context: Context, io: CLDataIO[T]) = 
    new CLTransformableSeq(seq)
  
  implicit def Array2CLTransformableSeq[T](arr: Array[T])(implicit context: Context, io: CLDataIO[T]) = 
    new CLTransformableSeq(arr)
  
  

  class RichCLKernel(k: CLKernel) {
    def setArgs(args: Any*) = k.setArgs(args.map(_.asInstanceOf[Object]):_*)

    def enqueueNDRange(global: Array[Int], local: Array[Int] = null)(args: Any*)(implicit context: Context) = k synchronized {
      setArgs(args: _*)
      k.enqueueNDRange(context.queue, global, local)
    }
  }
  implicit def CLKernel2RichCLKernel(k: CLKernel) = new RichCLKernel(k)
  
}

