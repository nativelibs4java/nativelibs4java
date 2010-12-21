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
  with CLEventBound
{
  def map[B, That](f: A => B, out: That)(implicit bf: CanBuildFrom[Repr, B, That]): That

  def map[B, That](f: A => B, queue: CLQueue)(implicit bf: CanBuildFrom[Repr, B, That]): That =
    map(f, null.asInstanceOf[That])

  def toCLArray: CLArray[A]
}
trait CLIndexedSeq[A, Repr] extends CLCollection[A, Repr]
{
  def apply(index: Int): A
  def update(index: Int, value: A): Unit
}
