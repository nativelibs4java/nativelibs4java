package scalacl
package collection
import impl._

import scala.collection.generic.CanBuildFrom
import com.nativelibs4java.opencl._
import scala.collection._
import scala.collection.mutable.Builder

trait WithScalaCLContext {
  def context: ScalaCLContext

}

trait CLCollection[A]
  extends /*Traversable[A] 
  with*/ CLCollectionLike[A, CLCollection[A]]
{
  //protected override def newBuilder: Builder[A, CLCollection[A]]
}
  
trait CLCollectionLike[A, +Repr]
  extends WithScalaCLContext
  with CLEventBoundContainer
  //with Traversable[A]
  with TraversableLike[A, Repr]
{
  this: Repr =>
  //self =>
  
  def toArray: Array[A] = toCLArray.toArray
  override def copyToArray[B >: A](a: Array[B], start: Int, len: Int): Unit = toCLArray.copyToArray(a, start, len)
    override def toArray[B >: A](implicit b: ClassManifest[B]) = {
    val out = new Array[B](size)
    copyToArray(out, 0, size)
    out
  }

  def toCLArray: CLArray[A]
  
  def map[B, That](f: A => B, out: That)(implicit bf: CanBuildFrom[Repr, B, That]): That

  override def map[B, That](f: A => B)(implicit bf: CanBuildFrom[Repr, B, That]): That =
    map(f, null.asInstanceOf[That])

  def filterFallback[That <: CLCollection[A]](p: A => Boolean, out: That)(implicit ff: CLCanFilterFrom[Repr, A, That]): Unit
  //def filter[That](p: A => Boolean, out: That)(implicit ff: CLCanFilterFrom[Repr, A, That]): That = {
  def filter[That <: CLCollection[A]](p: A => Boolean, out: That)(implicit ff: CLCanFilterFrom[Repr, A, That]): That = {
      val result = reuse(out, ff.newFilterResult(this))//new CLFilteredArray[A](length))

      p match {
        case clf: CLRunnable =>
          val filteredOut = result.asInstanceOf[CLFilteredArray[A]]
          val valuesOut = filteredOut.array
          val presenceOut = filteredOut.presence
          val valuesIn = (this: Any) match {
            case a: CLArray[A] =>
              a
            case fa: CLFilteredArray[A] =>
              fa.array
          }
          valuesIn.copyTo(valuesOut)
          clf.run(
            dims = Array(ff.rawLength(this)),
            args = Array(this, presenceOut),
            reads = Array(this),
            writes = Array(result)
          )(ff.context)
        case _ =>
          filterFallback(p, result)
      }
      result
    }
}

trait CLIndexedSeq[A] 
extends /*IndexedSeq[A] 
   with*/ CLCollection[A] 
   with CLIndexedSeqLike[A, CLIndexedSeq[A]] 
{
  //protected override def newBuilder: Builder[A, CLIndexedSeq[A]]
}

trait CLIndexedSeqLike[A, +Repr] 
extends IndexedSeqLike[A, Repr] 
   with CLCollectionLike[A, Repr]
{
  //self =>
  this: Repr =>
  //this: Repr with CLIndexedSeq[A] =>//Repr =>
  
  //def apply(index: Int): A
  //def update(index: Int, value: A): Unit
}
