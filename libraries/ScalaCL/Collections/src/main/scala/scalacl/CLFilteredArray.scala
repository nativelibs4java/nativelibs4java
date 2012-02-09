package scalacl

import impl._

import scala.collection.generic.CanBuildFrom
import com.nativelibs4java.opencl._
import scala.collection.IndexedSeqLike
import scala.collection.mutable.Builder
import scala.collection.generic._

import com.nativelibs4java.opencl._
import org.bridj.Pointer
import org.bridj.Pointer._
import scala.collection.JavaConversions._

object CLFilteredArray {
  def newBuilder[A](implicit context: Context, dataIO: CLDataIO[A]): Builder[A, CLFilteredArray[A]] = 
    error("Not implemented") // TODO

  /*
  type PresenceType = Boolean
  val presenceCLType = "char"
  @inline def toBool(p: PresenceType) = p
  @inline def toPresence(b: Boolean) = b
  //*/
  //*
  type PresenceType = Int
  val presenceCLType = "int"
  @inline def toBool(p: PresenceType) = p != 0
  @inline def toPresence(b: Boolean) = if (b) 1 else 0
  //*/
}
import CLFilteredArray.{PresenceType, toBool, toPresence}

class CLFilteredArray[A](
  protected[scalacl] val array: CLArray[A],
  protected[scalacl] val presence: CLGuardedBuffer[PresenceType]
)(
  implicit val context: Context,
  val dataIO: CLDataIO[A]
)
extends IndexedSeq[A]
  with GenericTraversableTemplate[A, CLFilteredArray]
  with CLIndexedSeq[A]
  //with IndexedSeqLike[A, CLFilteredArray[A]]
  with CLIndexedSeqLike[A, CLFilteredArray[A]]
  with CLWithForeach[A, CLFilteredArray[A]]
{
  def this(array: CLArray[A])(implicit context: Context, dataIO: CLDataIO[A]) =
      this(array, if (array.length > 0) new CLGuardedBuffer[PresenceType](array.length + 1) else null)

  def this(length: Int)(implicit context: Context, dataIO: CLDataIO[A]) =
        this(new CLArray[A](length))

  def this(length: Int, presence: CLGuardedBuffer[PresenceType])(implicit context: Context, dataIO: CLDataIO[A]) =
        this(new CLArray[A](length), presence)

  assert(presence.size == array.length + 1)
  
  override def companion: GenericCompanion[CLFilteredArray] = new SeqFactory[CLFilteredArray] {
    def newBuilder[AA]: Builder[AA, CLFilteredArray[AA]] = 
      CLFilteredArray.newBuilder[AA](context, dataIO.asInstanceOf[CLDataIO[AA]]) // TODO fix this hack !
  }
  override def eventBoundComponents = array.eventBoundComponents ++ (if (presence == null) Seq() else Seq(presence))

  import array._

  override def release = {
    array.release
    if (presence != null)
      presence.release
  }
  
  override def length = sizeFuture.get
  protected def sizeFuture = {
    if (presence == null)
      new CLInstantFuture(0)
    else {
      val ps = updatedPresencePrefixSum
      ps(array.length) // presence prefix sum buffer is of length array.length + 1
    }
  }
  
//  assert(buffers.forall(_.buffer.getElementCount == length))
  assert(array != null)
  assert(array.length == 0 || presence != null)

  override def newBuilder = CLFilteredArray.newBuilder[A]

  //lazy val buffersList = buffers.toList
  lazy val presencePrefixSum = if (presence == null) null else new CLGuardedBuffer[Int](array.length + 1)

  override def clone =
    new CLFilteredArray[A](array.clone, if (presence == null) null else presence.clone)

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
    //println("SIZE = " + size)
    //println("presence = " + presence.toArray.toSeq)
    //println("updatedPresencePrefixSum = " + updatedPresencePrefixSum.toArray.toSeq)
    new CLArray[A](size, if (size == 0) null else buffers.map(b => {
      val out = new CLGuardedBuffer[Any](size)(context, b.dataIO)
      PrefixSum.copyPrefixed(size, prefixSum, b, out)(b.t, context)
      out
    }))
  }

  var prefixSumUpToDate = false
  def updatedPresencePrefixSum = this.synchronized {
    if (!prefixSumUpToDate) {
      if (presence != null)
        //PrefixSum.prefixSumByte(presence, presencePrefixSum)
        PrefixSum.prefixSumInt(presence, presencePrefixSum)
      prefixSumUpToDate = true
    }
    presencePrefixSum
  }

  override def sum[B >: A](implicit num: Numeric[B]): B =
    toCLArray.sum[B] // TODO !
    
  def map[B, That](f: A => B, out: That)(implicit bf: CanBuildFrom[CLFilteredArray[A], B, That]): That = {
    if (!bf.isInstanceOf[CLCanBuildFrom[Repr, B, That]])
      return toSeq.map(f)(bf.asInstanceOf[CanBuildFrom[Seq[A], B, That]])
      
    implicit val dataIO = bf.dataIO
    val result = reuse(out, new CLFilteredArray[B](array.length, if (presence == null) null else presence.clone))

    if (presence != null)
      f match {
        case clf: CLRunnable if !clf.isOnlyInScalaSpace && !useScalaFunctions =>
          clf.run(
            args = Array(this, result),
            reads = Array(this),
            writes = Array(result)
          )(
            dims = Array(array.length)
          )
        case _ =>
          mapFallback(f, result)
      }
    result.asInstanceOf[That]
  }
  
  protected def mapFallback[B](f: A => B, result: CLFilteredArray[B]): Unit = {
    import scala.concurrent.ops._
    executingScalaFallbackOperation("mapFallback")
      
    val copyPres = if (result == this) null else future {
      presence.copyTo(result.presence)
    }
    val presenceArr = presence.toArray
    for (i <- 0 until length) {
      if (toBool(presenceArr(i)))
        result.array(i) = f(array(i))
    }
    Option(copyPres).foreach(_())
  }
  override def filterFallback[That <: CLCollection[A]](p: A => Boolean, out: That)(implicit ff: CLCanFilterFrom[CLFilteredArray[A], A, That]) = {
    import scala.concurrent.ops._
    executingScalaFallbackOperation("mapFallback")
    if (presence != null)
      out match {
        case filteredOut: CLFilteredArray[A] =>
          val copy = if (filteredOut == this) null else future {
            array.copyTo(filteredOut.array)
          }
          val presenceArr = presence.toArray
          for (i <- 0 until length) {
            if (toBool(presenceArr(i))) {
              val value = array(i)
              if (!p(value))
                presenceArr(i) = toPresence(false)
            }
          }
          filteredOut.presence.update(presenceArr)
          Option(copy).foreach(_())
      }
  }

  protected override def foreachFallback[U](f: A =>  U): Unit = if (presence != null) {
    executingScalaFallbackOperation("foreachFallback")
    val presenceArr = presence.toArray
    for (i <- 0 until length) {
      if (toBool(presenceArr(i)))
        f(array(i))
    }
  }
}
