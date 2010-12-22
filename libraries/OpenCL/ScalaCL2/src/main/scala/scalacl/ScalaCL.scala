import scala.collection.generic.CanBuildFrom
import com.nativelibs4java.opencl._

import scalacl.collection._
import scalacl.collection.impl._

package object scalacl {
  class ScalaCLContext(val context: CLContext, val queue: CLQueue) {
    def this(context: CLContext) = this(context, context.createDefaultQueue())
    def this() = this(JavaCL.createBestContext())
    //def newArray[T](size: Int)(implicit dataIO: CLDataIO[T]) = new CLArray[T](size)(dataIO, this)
  }

  def clType[T](implicit dataIO: CLDataIO[T]) = dataIO.clType

  //protected
  def reuse[T](value: Any, create: => T): T =
    if (value != null && value.isInstanceOf[T])
      value.asInstanceOf[T]
    else
      create
  
  implicit def ScalaCLContext2Context(sc: ScalaCLContext) = sc.context

  implicit def canBuildFromIndexedSeq[A](implicit context: ScalaCLContext, io: CLDataIO[A]) =
    new CLCanBuildFrom[CLIndexedSeq[_, _], A, CLArray[A]] {
      override def dataIO = io
      override def apply(from: CLIndexedSeq[_, _]) = CLArray.newBuilder[A](context, dataIO)
      override def apply() = CLArray.newBuilder[A](context, dataIO)
    }

  implicit def canBuildFromFilteredArray[A](implicit context: ScalaCLContext, io: CLDataIO[A]) =
    new CLCanBuildFrom[CLFilteredArray[_], A, CLFilteredArray[A]] {
      override def dataIO = io
      override def apply(from: CLFilteredArray[_]) = CLFilteredArray.newBuilder[A](context, dataIO)
      override def apply() = CLFilteredArray.newBuilder[A](context, dataIO)
    }

  implicit def canFilterFromIndexedSeq[A](implicit ctx: ScalaCLContext, io: CLDataIO[A]) =
    new CLCanFilterFrom[CLIndexedSeq[A, _], A, CLFilteredArray[A]] {
      override def dataIO = io
      override def context = ctx
      def rawLength(from: CLIndexedSeq[A, _]): Int = from match {
        case a: CLArray[A] =>
          a.length
        case fa: CLFilteredArray[A] =>
          fa.array.length
      }
      def newFilterResult(from: CLIndexedSeq[A, _]) = new CLFilteredArray[A](rawLength(from))
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

  implicit def AnyValCLDataIO[T <: AnyVal](implicit t: ClassManifest[T]) = new CLValDataIO[T]

  implicit def Expression2CLFunction[K, V](fx: (K => V, Seq[String]))(implicit kIO: CLDataIO[K], vIO: CLDataIO[V]) = {
    val (function, expressions) = fx
    new CLFunction[K, V](null, function, Seq(), expressions, Seq())
  }

  implicit def CLFunSeq[K, V](declarations: Seq[String], expressions: Seq[String])(implicit kIO: CLDataIO[K], vIO: CLDataIO[V]) =
    new CLFunction[K, V](null, null, declarations, expressions, Seq())

  implicit def CLFullFun[K, V](uniqueSignature: String, function: K => V, declarations: Seq[String], expressions: Seq[String])(implicit kIO: CLDataIO[K], vIO: CLDataIO[V]) =
    new CLFunction[K, V](uniqueSignature, function, declarations, expressions, Seq())

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

