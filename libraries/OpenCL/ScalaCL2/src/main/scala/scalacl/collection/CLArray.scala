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
      case clf: CLRunnable =>
        clf.run(
          dims = Array(length),
          args = Array(this, result),
          reads = Array(this),
          writes = Array(result)
        )(context)
      case _ =>
        mapFallback(f, result)
    }
    result.asInstanceOf[That]
  }
}
class CLArray[A](
  val length: Int, 
  protected[scalacl] val buffers: Array[CLGuardedBuffer[Any]]
)(
  implicit val context: ScalaCLContext,
  val dataIO: CLDataIO[A]
)
  extends IndexedSeqLike[A, CLIndexedSeq[A, _]]
  with IndexedSeqOptimized[A, CLIndexedSeq[A, _]]
  with CLIndexedSeq[A, CLIndexedSeq[A, _]]
  with MappableToCLArray[A, CLIndexedSeq[A, _]]
{
  def this(length: Int)(implicit context: ScalaCLContext, dataIO: CLDataIO[A]) =
    this(length, dataIO.createBuffers(length))

  override def eventBoundComponents = buffers
  
  type Repr = CLIndexedSeq[A, _]
  
  assert(buffers.forall(_.buffer.getElementCount == length))

  import CLArray._

  override def newBuilder: Builder[A, CLArray[A]] = CLArray.newBuilder[A]

  override def toArray = dataIO.toArray(buffers)
  override def copyToArray[B >: A](out: Array[B], start: Int, len: Int): Unit = {
    dataIO.copyToArray(buffers, out, start, len)
  }

  override def toCLArray = this

  override def apply(index: Int): A =
    dataIO.extract(buffers, index).get

  def update(index: Int, value: A): Unit =
    dataIO.store(value, buffers, index)

  def update(f: A => A): CLArray[A] =
    map(f, this)/*(new CLCanBuildFrom[Repr, A, CLArray[A]] {
      override def dataIO = CLArray.this.dataIO
      override def apply() = newBuilder
      override def apply(from: Repr) = newBuilder
    })*/

  override def clone: CLArray[A] =
    new CLArray(length, buffers.map(_.clone)) // TODO map in parallel

  override def size = length

  protected override def mapFallback[B](f: A => B, result: CLArray[B]) = {
    for (i <- 0 until length)
      result(i) = f(this(i))
  }

  override def foreach[U](f: A => U): Unit =
    toArray(dataIO.t) foreach f

  override def filter(p: A => Boolean) =
    filter(p, new CLFilteredArray[A](length))//.toCLArray

  override def filterFallback[That <: CLCollection[A, _]](p: A => Boolean, out: That)(implicit ff: CLCanFilterFrom[Repr, A, That]) = {
    import scala.concurrent.ops._

    out match {
      case filteredOut: CLFilteredArray[A] =>
        val copy = if (filteredOut.array == this) null else future {
          copyTo(filteredOut.array)
        }

        val presenceArr = new Array[Boolean](length)
        for (i <- 0 until length) {
          val value = this(i)
          presenceArr(i) = p(value)
        }
        filteredOut.presence.update(presenceArr)
        Option(copy).foreach(_())
    }
  }

  def copyTo(other: CLArray[A]) = {
    import scala.concurrent.ops._

    assert(buffers.length == other.buffers.length)
    for ((from, to) <- buffers.zip(other.buffers))
      from.copyTo(to)
  }
}

