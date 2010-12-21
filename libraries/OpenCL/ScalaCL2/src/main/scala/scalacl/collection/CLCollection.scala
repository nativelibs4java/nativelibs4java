package scalacl
package collection
import impl._

import scala.collection.generic.CanBuildFrom
import com.nativelibs4java.opencl._
import scala.collection.TraversableLike

trait WithScalaCLContext {
  def context: ScalaCLContext

}

trait CLCollection[A, Repr]
  extends WithScalaCLContext
  with TraversableLike[A, Repr]
  with CLEventBoundContainer
{
  this: Repr =>
  
  def map[B, That](f: A => B, out: That)(implicit bf: CanBuildFrom[Repr, B, That]): That

  override def map[B, That](f: A => B)(implicit bf: CanBuildFrom[Repr, B, That]): That =
    map(f, null.asInstanceOf[That])

  def filterFallback[That <: CLCollection[A, _]](p: A => Boolean, out: That)(implicit ff: CLCanFilterFrom[Repr, A, That]): Unit
  //def filter[That](p: A => Boolean, out: That)(implicit ff: CLCanFilterFrom[Repr, A, That]): That = {
  def filter[That <: CLCollection[A, _]](p: A => Boolean, out: That)(implicit ff: CLCanFilterFrom[Repr, A, That]): That = {
      val result = reuse(out, ff.newFilterResult(this))//new CLFilteredArray[A](length))

      p match {
        case clf: CLFunction =>
          clf.apply(
            dims = Array(ff.rawLength(this)),
            args = Array(result, this),
            reads = Array(this),
            writes = Array(result)
          )
        case _ =>
          filterFallback(p, result)
      }
      result
    }


  def toArray: Array[A] = toCLArray.toArray
  override def copyToArray[B >: A](a: Array[B], start: Int, len: Int): Unit = toCLArray.copyToArray(a, start, len)
    override def toArray[B >: A](implicit b: ClassManifest[B]) = {
    val out = new Array[B](size)
    copyToArray(out, 0, size)
    out
  }

  def toCLArray: CLArray[A]
}

trait CLIndexedSeq[A, Repr] extends CLCollection[A, Repr]
{
  this: Repr =>
  def apply(index: Int): A
  //def update(index: Int, value: A): Unit
}
