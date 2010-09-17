/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package object scalacl {
  import com.nativelibs4java.opencl._
  
  def writeAll[E <: CLEventBound](f: Array[CLEvent] => CLEvent, buffs: List[E]): CLEvent = buffs match {
    case h :: s :: t =>
      writeAll(es => h.write(evts => f(evts ++ es)), s :: t)
    case h :: Nil =>
      h.write(evts => f(evts))
    case Nil =>
      f(Array())
  }
  def readAll[E <: CLEventBound](f: Array[CLEvent] => CLEvent, buffs: List[E]): CLEvent = buffs match {
    case h :: s :: t =>
      readAll(es => h.read(evts => f(evts ++ es)), s :: t)
    case h :: Nil =>
      h.read(evts => f(evts))
    case Nil =>
      f(Array())
  }
  def syncAll[E <: CLEventBound](reads: List[E])(writes: List[E])(f: Array[CLEvent] => CLEvent): CLEvent = {
    reads match {
      case h :: r =>
        syncAll(r)(writes)(es => h.read(evts => f(evts ++ es)))
      case Nil =>
        writes match {
          case h :: (r @ (s :: t)) =>
            syncAll(Nil)(r)(es => h.write(evts => f(evts ++ es)))
          case h :: Nil =>
            h.write(evts => f(evts))
          case Nil =>
            f(Array())
        }
    }
  }

  implicit def Tuple2ToCLDataIO[T1, T2](implicit io1: CLDataIO[T1], io2: CLDataIO[T2]) =
    new CLTupleDataIO[(T1, T2)](
      Array(io1, io2).map(_.asInstanceOf[CLDataIO[Any]]),
      t => Array(t._1, t._2),
      a => (
        a(0).asInstanceOf[T1],
        a(1).asInstanceOf[T2]
      )
    )


  implicit def Tuple3ToCLDataIO[T1, T2, T3](implicit io1: CLDataIO[T1], io2: CLDataIO[T2], io3: CLDataIO[T3]) =
    new CLTupleDataIO[(T1, T2, T3)](
      Array(io1, io2, io3).map(_.asInstanceOf[CLDataIO[Any]]),
      t => Array(t._1, t._2, t._3),
      a => (
        a(0).asInstanceOf[T1],
        a(1).asInstanceOf[T2],
        a(2).asInstanceOf[T3]
      )
    )

  implicit def Tuple4ToCLDataIO[T1, T2, T3, T4](implicit io1: CLDataIO[T1], io2: CLDataIO[T2], io3: CLDataIO[T3], io4: CLDataIO[T4]) =
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

  implicit def AnyValCLDataIO[T <: AnyVal](implicit t: ClassManifest[T]) = new CLValDataIO[T]

  def clType[T](implicit dataIO: CLDataIO[T]) = dataIO.clType

  def clArray[T](fixedSize: Long)(implicit dataIO: CLDataIO[T], context: ScalaCLContext) = {
    implicit val t = dataIO.t
    new CLArray[T](dataIO.createBuffers(fixedSize))
  }

  implicit def expression2CLFunction[K, V](expression: String)(implicit kIO: CLDataIO[K], vIO: CLDataIO[V]) =
    new CLFunction[K, V](Seq(), expression, Seq())

  implicit def expression2CLFunction[K, V](declarationsAndExpression: (Seq[String], String))(implicit kIO: CLDataIO[K], vIO: CLDataIO[V]) =
    new CLFunction[K, V](declarationsAndExpression._1, declarationsAndExpression._2, Seq())
}