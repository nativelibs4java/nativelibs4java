package scalacl
package collection
import impl._

import scala.collection.generic.CanBuildFrom
import com.nativelibs4java.opencl._
import scala.collection._
import scala.collection.mutable.Builder
import scala.collection.generic._

trait WithScalaCLContext {
  def context: ScalaCLContext

}

object CLCollection extends SeqFactory[CLCollection] {
  implicit def canBuildFrom[A]: CanBuildFrom[Coll, A, CLCollection[A]] = new GenericCanBuildFrom[A]
  def newBuilder[A]: Builder[A, CLCollection[A]] = error("Not implemented") // TODO
}

trait CLCollection[A]
extends Seq[A] 
   with GenericTraversableTemplate[A, CLCollection]
   with CLCollectionLike[A, CLCollection[A]]
{
  override def companion: GenericCompanion[CLCollection] = CLCollection
}
  
trait CLCollectionLike[A, +Repr]
  extends WithScalaCLContext
  with CLEventBoundContainer
  //with Traversable[A]
  with IterableLike[A, Repr]
{
  //this: Repr =>
  self =>
  
  def release: Unit
  
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
      val result = reuse(out, ff.newFilterResult(this.asInstanceOf[Repr]))//new CLFilteredArray[A](length))

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
            dims = Array(ff.rawLength(this.asInstanceOf[Repr])),
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

object CLIndexedSeq extends SeqFactory[CLIndexedSeq] {
  implicit def canBuildFrom[A]: CanBuildFrom[Coll, A, CLIndexedSeq[A]] = new GenericCanBuildFrom[A]
  def newBuilder[A]: Builder[A, CLIndexedSeq[A]] = error("Not implemented") // TODO
}


trait CLIndexedSeq[A] 
extends CLCollection[A]
   with IndexedSeq[A] 
   with GenericTraversableTemplate[A, CLIndexedSeq]
   with CLIndexedSeqLike[A, CLIndexedSeq[A]] 
{
  //protected override def newBuilder: Builder[A, CLIndexedSeq[A]]
  override def companion: GenericCompanion[CLIndexedSeq] = CLIndexedSeq
}

trait CLIndexedSeqLike[A, +Repr] 
extends CLCollectionLike[A, Repr]
   with IndexedSeqLike[A, Repr]
{ self =>
  //self =>
  //this: Repr =>
  //this: Repr with CLIndexedSeq[A] =>//Repr =>
  
  //def apply(index: Int): A
  //def update(index: Int, value: A): Unit
}
