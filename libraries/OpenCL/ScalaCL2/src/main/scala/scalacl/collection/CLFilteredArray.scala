package scalacl
package collection
import impl._

import scala.collection.generic.CanBuildFrom
import com.nativelibs4java.opencl._
import scala.collection.IndexedSeqLike
import scala.collection.mutable.Builder

import com.nativelibs4java.opencl._
import org.bridj.Pointer
import org.bridj.Pointer._
import org.bridj.SizeT
import scala.collection.JavaConversions._

object CLFilteredArray {
  def newBuilder[A](implicit context: ScalaCLContext, dataIO: CLDataIO[A]): Builder[A, CLFilteredArray[A]] =
    error("Not implemented")
}
class CLFilteredArray[A](
  protected[scalacl] val array: CLArray[A],
  protected[scalacl] val presence: CLGuardedBuffer[Boolean]
)(
  implicit val context: ScalaCLContext,
  val dataIO: CLDataIO[A]
)
extends CLCollection[A, CLFilteredArray[A]]
  with IndexedSeqLike[A, CLFilteredArray[A]]
  with CLIndexedSeq[A, CLFilteredArray[A]]
{
  def this(array: CLArray[A])(implicit context: ScalaCLContext, dataIO: CLDataIO[A]) =
      this(array, new CLGuardedBuffer[Boolean](array.length))

  def this(length: Int)(implicit context: ScalaCLContext, dataIO: CLDataIO[A]) =
        this(new CLArray[A](length))

  def this(length: Int, presence: CLGuardedBuffer[Boolean])(implicit context: ScalaCLContext, dataIO: CLDataIO[A]) =
        this(new CLArray[A](length), presence)

  override def eventBoundComponents = array.eventBoundComponents ++ Seq(presence)

  import array._

  override def length = array.length
  
//  assert(buffers.forall(_.buffer.getElementCount == length))
  assert(array != null)
  assert(presence != null)

  override def newBuilder = CLFilteredArray.newBuilder[A]

  //lazy val buffersList = buffers.toList
  lazy val presencePrefixSum = new CLGuardedBuffer[Int](length)

  override def clone =
    new CLFilteredArray[A](array.clone, presence.clone)

  def apply(index: Int) = {
    // TODO: find some faster way to run this in non-OpenCL mode :
    toCLArray.apply(index)
  }
  def update(index: Int, value: A) = {
    // TODO: make this faster
    toCLArray.update(index, value)
  }
  
  def toCLArray: CLArray[A] = {
    val prefixSum = updatedPresencePrefixSum
    val size = this.size
    new CLArray(length, buffers.map(b => {
      val out = new CLGuardedBuffer[Any](size)(b.dataIO, context)
      PrefixSum.copyPrefixed(size, prefixSum, b, out)(b.t, context)
      out
    }))
  }

  var prefixSumUpToDate = false
  def updatedPresencePrefixSum = this.synchronized {
    if (!prefixSumUpToDate) {
      PrefixSum.prefixSum(presence, presencePrefixSum)
      prefixSumUpToDate = true
    }
    presencePrefixSum
  }

  def map[B, That](f: A => B, out: That)(implicit bf: CanBuildFrom[CLFilteredArray[A], B, That]): That = {
    implicit val dataIO = bf.dataIO
    val result = reuse(out, new CLFilteredArray[B](length, presence.clone))

    f match {
      case clf: CLFunction =>
        clf.apply(
          dims = Array(size),
          args = Array(result, this),
          reads = Array(this),
          writes = Array(result)
        )
      case _ =>
        import scala.concurrent.ops._
        
        val copyPres = if (result == this) null else future {
          presence.copyTo(result.presence)
        }
        val presenceArr = presence.toArray
        for (i <- 0 until length) {
          if (presenceArr(i))
            result.array(i) = f(array(i))
        }
        Option(copyPres).foreach(_())
    }
    result.asInstanceOf[That]
  }

  override def filterFallback[That <: CLCollection[A, _]](p: A => Boolean, out: That)(implicit ff: CLCanFilterFrom[CLFilteredArray[A], A, That]) = {
    import scala.concurrent.ops._

    out match {
      case filteredOut: CLFilteredArray[A] =>
        val copy = if (filteredOut == this) null else future {
          array.copyTo(filteredOut.array)
        }
        val presenceArr = presence.toArray
        for (i <- 0 until length) {
          if (presenceArr(i)) {
            val value = array(i)
            if (!p(value))
              presenceArr(i) = false
          }
        }
        filteredOut.presence.update(presenceArr)
        Option(copy).foreach(_())
    }
  }

  /// Overridden because apply and update are just terribly slow
  override def foreach[U](f: A =>  U): Unit = {
    val presenceArr = presence.toArray
    for (i <- 0 until length) {
      if (presenceArr(i))
        f(array(i))
    }
  }
}
