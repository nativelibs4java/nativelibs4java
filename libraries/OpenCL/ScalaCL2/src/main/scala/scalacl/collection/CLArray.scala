package scalacl
package collection
import impl._

import scala.collection.generic.CanBuildFrom
import com.nativelibs4java.opencl._
import scala.collection.IndexedSeqLike
import scala.collection.mutable.{ArrayBuffer, IndexedSeqOptimized, Builder}

object CLArray {
  def apply[A](values: A*)(implicit context: ScalaCLContext, dataIO: CLDataIO[A]) =
    fromSeq(values)

  def fromSeq[A](values: Seq[A])(implicit context: ScalaCLContext, dataIO: CLDataIO[A]) = {
    implicit val t = dataIO.t
    val valuesArray = values.toArray
    val length = valuesArray.length
    if (t.erasure.isPrimitive) {
      new CLArray[A](length, Array(new CLGuardedBuffer[A](length).update(valuesArray).asInstanceOf[CLGuardedBuffer[Any]]))
    } else {
      val a = new CLArray[A](length)
      // TODO find a faster way to handle initial copy of complex types !!!
      for (i <- 0 until length)
        dataIO.store(values(i), a.buffers, 0)
      
      a
    }
  }

  def newBuilder[A](implicit context: ScalaCLContext, dataIO: CLDataIO[A]): Builder[A, CLArray[A]] =
    new ArrayBuffer[A].mapResult(b => fromSeq(b))
}

trait MappableToCLArray[A, Repr] {
  this: CLCollection[A, Repr] =>
  
  def length: Int
  protected def mapFallback[B](f: A => B, out: CLArray[B]): Unit
  def map[B, That](f: A => B, out: That)(implicit bf: CanBuildFrom[Repr, B, That]): That = {
    val result = reuse(out, new CLArray[B](length)(context, bf.dataIO))

    f match {
      case clf: CLFunction =>
        clf.apply(
          dims = Array(length),
          args = Array(result, this),
          reads = Array(this),
          writes = Array(result)
        )
      case _ =>
        mapFallback(f, result)
    }
    result.asInstanceOf[That]
  }
}
class CLArray[A](
  val length: Int, 
  val buffers: Array[CLGuardedBuffer[Any]]
)(
  implicit val context: ScalaCLContext,
  val dataIO: CLDataIO[A]
)
  extends IndexedSeqLike[A, CLArray[A]]
  with CLIndexedSeq[A, CLArray[A]]
  with IndexedSeqOptimized[A, CLArray[A]]
  with MappableToCLArray[A, CLArray[A]]
{
  def this(length: Int)(implicit context: ScalaCLContext, dataIO: CLDataIO[A]) =
    this(length, dataIO.createBuffers(length))

  assert(buffers.forall(_.buffer.getElementCount == length))
  
  override def newBuilder: Builder[A, CLArray[A]] = CLArray.newBuilder[A]

  override def toCLArray = this

  override def apply(index: Int): A = dataIO.extract(buffers, index).get
  override def update(index: Int, value: A): Unit = dataIO.store(value, buffers, index)

  def update(f: A => A): CLArray[A] =
    map(f, this)(new CLCanBuildFrom[CLArray[A], A, CLArray[A]] {
      override def dataIO = CLArray.this.dataIO
      override def apply() = newBuilder
      override def apply(from: CLArray[A]) = newBuilder
    })

  override def clone: CLArray[A] = error("Not implemented")

  protected override def mapFallback[B](f: A => B, result: CLArray[B]) = {
    for (i <- 0 until size)
      result(i) = f(this(i))
  }
  
  /*
  protected def filterFallback(p: A => Boolean, out: CLFilteredArray[A]): Unit = {
    for (i <- 0 until length) {
      val value = this(i)
      val b = p(value)
      out.presence(i) = b
      if (b && out.values != this)
        out.values(i) = value
    }
  }
  */

  /*
  protected def newFiltered(inplace: Boolean): CLFilteredArray[A] = {
    new CLFilteredArray[A](if (inplace) this else this.clone)
  }*/

  override def size: Int = length
}

