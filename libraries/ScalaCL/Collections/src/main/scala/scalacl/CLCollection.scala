package scalacl

import impl._

import scala.collection.generic.CanBuildFrom
import com.nativelibs4java.opencl._
import scala.collection._
import scala.collection.mutable.Builder
import scala.collection.generic._

trait WithContext {
  def context: Context

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
  extends WithContext
  with CLEventBoundContainer
  //with Traversable[A]
  with IterableLike[A, Repr]
{
  //this: Repr =>
  self =>
  
  def release: Unit
  
  def toArray: Array[A] = toCLArray.toArray
  override def toSeq = toArray.toSeq
  override def toSet[B >: A] = toArray.toSet[B]
  override def toIndexedSeq[B >: A] = toArray.toIndexedSeq[B]
  
  override def copyToArray[B >: A](a: Array[B], start: Int, len: Int): Unit = toCLArray.copyToArray(a, start, len)
    override def toArray[B >: A](implicit b: ClassManifest[B]) = {
    val out = new Array[B](size)
    copyToArray(out, 0, size)
    out
  }

  def toCLArray: CLArray[A]
  
  override def toString = getClass.getSimpleName + "(" + toArray.mkString(", ") + ")"
  
  def zip$into[A1 >: A, B, That](that: Iterable[B], out: That)(implicit bf: CanBuildFrom[Repr, (A1, B), That]): That = 
    toCLArray.zip$into[A1, B, That](that, out)(bf.asInstanceOf[CanBuildFrom[CLIndexedSeq[A],(A1, B),That]])
  
  def zipWithIndex$into[A1 >: A, That](out: That)(implicit bf: CanBuildFrom[Repr, (A1, Int), That]): That = 
    toCLArray.zipWithIndex$into[A1, That](out)(bf.asInstanceOf[CanBuildFrom[CLIndexedSeq[A],(A1, Int),That]])
    
  override def zip[A1 >: A, B, That](that: Iterable[B])(implicit bf: CanBuildFrom[Repr, (A1, B), That]): That =  
    zip$into[A1, B, That](that, null.asInstanceOf[That])(bf)
  
  override def zipWithIndex[A1 >: A, That](implicit bf: CanBuildFrom[Repr, (A1, Int), That]): That =
    zipWithIndex$into[A1, That](null.asInstanceOf[That])(bf)
    
  def zip$shareBuffers[A1 >: A, B, That](that: Iterable[B])(implicit bf: CanBuildFrom[Repr, (A1, B), That]): That = 
    toCLArray.zip$shareBuffers[A1, B, That](that)(bf.asInstanceOf[CanBuildFrom[CLIndexedSeq[A],(A1, B),That]])
  
  def zipWithIndex$shareBuffers[A1 >: A, That](implicit bf: CanBuildFrom[Repr, (A1, Int), That]): That =
    toCLArray.zipWithIndex$shareBuffers[A1, That](bf.asInstanceOf[CanBuildFrom[CLIndexedSeq[A],(A1, Int),That]])
  
  def map[B, That](f: A => B, out: That)(implicit bf: CanBuildFrom[Repr, B, That]): That

  override def map[B, That](f: A => B)(implicit bf: CanBuildFrom[Repr, B, That]): That =
    map(f, null.asInstanceOf[That])

  protected def filterFallback[That <: CLCollection[A]](p: A => Boolean, out: That)(implicit ff: CLCanFilterFrom[Repr, A, That]): Unit
  
  //def filter[That](p: A => Boolean, out: That)(implicit ff: CLCanFilterFrom[Repr, A, That]): That = {
  def filter[That <: CLCollection[A]](p: A => Boolean, out: That)(implicit ff: CLCanFilterFrom[Repr, A, That]): That = {
      val result = reuse(out, ff.newFilterResult(this.asInstanceOf[Repr]))//new CLFilteredArray[A](length))

      p match {
        case clf: CLRunnable if !clf.isOnlyInScalaSpace && !useScalaFunctions =>
          val filteredOut = result.asInstanceOf[CLFilteredArray[A]]
          val valuesOut = filteredOut.array
          val presenceOut = filteredOut.presence
          val valuesIn = (this: Any) match {
            case r: CLRange =>
              r.asInstanceOf[CopiableToCLArray[A]]
            case a: CLArray[A] =>
              a
            case fa: CLFilteredArray[A] =>
              fa.array
          }
          valuesIn.copyTo(valuesOut)
          clf.run(
            args = Array(this, presenceOut),
            reads = Array(this),
            writes = Array(result, presenceOut)
          )(
            dims = Array(ff.rawLength(this.asInstanceOf[Repr]))
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
