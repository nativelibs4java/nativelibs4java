import scala.collection.generic.CanBuildFrom
import com.nativelibs4java.opencl._

import scalacl.impl._

package scalacl {
  class ScalaCLContext(val context: CLContext, val queue: CLQueue) {
    def this(context: CLContext) = this(context, context.createDefaultOutOfOrderQueueIfPossible())
    def this() = this(JavaCL.createBestContext(CLPlatform.DeviceFeature.OutOfOrderQueueSupport, CLPlatform.DeviceFeature.MaxComputeUnits))
    
    //println("Is out of order queue : " + queue.getProperties.contains(CLDevice.QueueProperties.OutOfOrderExecModeEnable))
    
    //def newArray[T](size: Int)(implicit dataIO: CLDataIO[T]) = new CLArray[T](size)(dataIO, this)
  }
  object ScalaCLContext {
    def apply() = new ScalaCLContext()
    def apply(preferredFeatures: CLPlatform.DeviceFeature*) = {
      new ScalaCLContext(JavaCL.createBestContext(preferredFeatures:_*))
    }
  }
}
package object scalacl {
  
  var verbose =
    "1" == System.getenv("SCALACL_VERBOSE")
  
  var useScalaFunctions =
    "1" == System.getenv("SCALACL_USE_SCALA_FUNCTIONS")
  
  private[scalacl] def reuse[T](value: Any, create: => T): T =
    if (value != null && value.isInstanceOf[T])
      value.asInstanceOf[T]
    else
      create
  
  implicit def ScalaCLContext2Context(sc: ScalaCLContext) = sc.context

  class CLTransformableSeq[T](seq: Seq[T])(implicit context: ScalaCLContext, io: CLDataIO[T]) {
    def cl = CLArray.fromSeq(seq)
  }
  class CLTransformableRange(rng: Range)(implicit context: ScalaCLContext) {
    def cl = new CLIntRange(rng)
  }
  implicit def Range2CLTransformableRange(rng: Range)(implicit context: ScalaCLContext) = 
    new CLTransformableRange(rng)
    
  implicit def Seq2CLTransformableSeq[T](seq: Seq[T])(implicit context: ScalaCLContext, io: CLDataIO[T]) = 
    new CLTransformableSeq(seq)
  
  implicit def Array2CLTransformableSeq[T](arr: Array[T])(implicit context: ScalaCLContext, io: CLDataIO[T]) = 
    new CLTransformableSeq(arr)
  
  /*implicit def canBuildArrayFromIndexedSeq[A](implicit context: ScalaCLContext, io: CLDataIO[A]) =
    new CLCanBuildFrom[CLIndexedSeq[_], A, CLArray[A]] {
      override def dataIO = io
      override def apply(from: CLIndexedSeq[_]) = CLArray.newBuilder[A](context, dataIO)
      override def apply() = CLArray.newBuilder[A](context, dataIO)
    }
    */
  implicit def canBuildIndexedSeqFromIndexedSeq[A](implicit context: ScalaCLContext, io: CLDataIO[A]) =
    new CLCanBuildFrom[CLIndexedSeq[_], A, CLIndexedSeq[A]] {
      override def dataIO = io
      override def apply(from: CLIndexedSeq[_]) = CLFilteredArray.newBuilder[A]//(context, dataIO)
      override def apply() = CLFilteredArray.newBuilder[A]//(context, dataIO)
    }
  implicit def canBuildArrayFromArray[A](implicit context: ScalaCLContext, io: CLDataIO[A]) =
    new CLCanBuildFrom[CLArray[_], A, CLArray[A]] {
      override def dataIO = io
      override def apply(from: CLArray[_]) = CLArray.newBuilder[A](context, dataIO)
      override def apply() = CLArray.newBuilder[A](context, dataIO)
    }
/*
  implicit def canBuildFromFilteredArray[A](implicit context: ScalaCLContext, io: CLDataIO[A]) =
    new CLCanBuildFrom[CLFilteredArray[_], A, CLFilteredArray[A]] {
      override def dataIO = io
      override def apply(from: CLFilteredArray[_]) = CLFilteredArray.newBuilder[A](context, dataIO)
      override def apply() = CLFilteredArray.newBuilder[A](context, dataIO)
    }
    */

  implicit def canFilterFromIndexedSeq[A](implicit ctx: ScalaCLContext, io: CLDataIO[A]) =
    new CLCanFilterFrom[CLIndexedSeq[A], A, CLFilteredArray[A]] {
      override def dataIO = io
      override def context = ctx
      def rawLength(from: CLIndexedSeq[A]): Int = from match {
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

  implicit def Expression2CLFunction[K, V](fx: (K => V, Seq[String]))(implicit kIO: CLDataIO[K], vIO: CLDataIO[V]) = {
    val (function, expressions) = fx
    new CLFunction[K, V](function, Seq(), expressions, Seq())
  }

  private val functionsCache = scala.collection.mutable.HashMap[Long, CLFunction[_, _]]()
  
  def getCachedFunction[K, V](uid: Long, function: K => V, declarations: => Seq[String], expressions: => Seq[String], extraArgs: => Seq[Any])(implicit kIO: CLDataIO[K], vIO: CLDataIO[V]): CLFunction[K, V] = {
    functionsCache synchronized {
      functionsCache.getOrElseUpdate(uid, new CLFunction[K, V](function, declarations, expressions, Seq()).asInstanceOf[CLFunction[_, _]]).asInstanceOf[CLFunction[K, V]]
    }
  }
  
  implicit def CLFunSeq[K, V](declarations: Seq[String], expressions: Seq[String])(implicit kIO: CLDataIO[K], vIO: CLDataIO[V]) =
    new CLFunction[K, V](null, declarations, expressions, Seq())

  implicit def CLFullFun[K, V](function: K => V, declarations: Seq[String], expressions: Seq[String])(implicit kIO: CLDataIO[K], vIO: CLDataIO[V]) =
    new CLFunction[K, V](function, declarations, expressions, Seq())

  implicit def Range2CLIntRangeMethods(r: Range)(implicit context: ScalaCLContext, dataIO: CLDataIO[Int]) = new {
    def toCLRange = new CLIntRange(r)
    def toCLArray = toCLRange.toCLArray
    def toCL = toCLRange
  }
  implicit def RichIndexedSeqCL[T](c: IndexedSeq[T])(implicit context: ScalaCLContext, dataIO: CLDataIO[T]) = new {
    def toCLArray = CLArray.fromSeq(c)
    def toCL = toCLArray
  }
}

