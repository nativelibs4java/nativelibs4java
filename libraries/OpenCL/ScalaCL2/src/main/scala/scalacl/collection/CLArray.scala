package scalacl
package collection
import impl._

import scala.collection.generic.CanBuildFrom
import com.nativelibs4java.opencl._
import scala.collection.IndexedSeqLike
import scala.collection.mutable.{ArrayBuffer, IndexedSeqOptimized, Builder}
import scala.collection.generic._
import scala.annotation.unchecked.uncheckedVariance

object CLArray {
  
  lazy val indexCode = new CLSimpleCode("""
    __kernel void indexCode(int size, __global int* out) {
      int i = get_global_id(0);
      if (i >= size)
        return;

      out[i] = i;
    }
  """)
  
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

trait MappableToCLArray[A, +Repr <: CLCollectionLike[A, Repr] with CLCollection[A]] {
  //self =>
  this: Repr =>
  //this: CLIndexedSeq[A] with CLIndexedSeqLike[A, Repr] => //with WithScalaCLContext with CLEventBoundContainer => // CLIndexedSeqLike[A, Repr] =>
  
  def length: Int
  protected def mapFallback[B](f: A => B, out: CLArray[B]): Unit
  override def map[B, That](f: A => B, out: That)(implicit bf: CanBuildFrom[Repr, B, That]): That = {
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
  extends CLIndexedSeq[A]//IndexedSeq[A]
  with GenericTraversableTemplate[A, CLArray]
  with IndexedSeqOptimized[A, CLIndexedSeq[A]]
  with CLIndexedSeqLike[A, CLIndexedSeq[A]]
  with MappableToCLArray[A, CLIndexedSeq[A]]
{
  def this(length: Int)(implicit context: ScalaCLContext, dataIO: CLDataIO[A]) =
    this(length, if (length > 0) dataIO.createBuffers(length) else null)

  override def companion: GenericCompanion[CLArray] = new SeqFactory[CLArray] {
    def newBuilder[AA]: Builder[AA, CLArray[AA]] =
      CLArray.newBuilder[AA](context, dataIO.asInstanceOf[CLDataIO[AA]])
  }
  
  override def release = if (length > 0) buffers.foreach(_.release)
  
  override def eventBoundComponents = if (length > 0) buffers else Seq()
  
  type Repr = CLIndexedSeq[A]
  
  assert(length == 0 || buffers.forall(_.buffer.getElementCount == length))

  import CLArray._
  import dataIO.t

  override def newBuilder: Builder[A, CLArray[A]] = CLArray.newBuilder[A]

  override def toArray = if (length > 0) dataIO.toArray(buffers) else Array[A]()
  override def copyToArray[B >: A](out: Array[B], start: Int, len: Int): Unit = if (length > 0) {
    dataIO.copyToArray(buffers, out, start, len)
  }

  override def toCLArray = this

  override def apply(index: Int): A =
    if (length > 0) 
      dataIO.extract(buffers, index).get
    else
      throw new ArrayIndexOutOfBoundsException("Empty CLArray !")

  def update(index: Int, value: A): Unit =
    if (length > 0)
      dataIO.store(value, buffers, index)
    else
      throw new ArrayIndexOutOfBoundsException("Empty CLArray !")

  def update(f: A => A): CLArray[A] =
    map(f, this)(new CLCanBuildFrom[Repr, A, CLArray[A]] {
      override def dataIO = CLArray.this.dataIO
      override def apply() = newBuilder.asInstanceOf[Builder[A, CLArray[A]]]
      override def apply(from: Repr) = newBuilder.asInstanceOf[Builder[A, CLArray[A]]]
    })

  override def zip[A1 >: A, B, That](that: Iterable[B])(implicit bf: CanBuildFrom[Repr, (A1, B), That]): That = {
    if (!that.isInstanceOf[CLArray[_]])
      super.zip(that)
    else {
      val thatArray = that.asInstanceOf[CLArray[B]]
      //val result = reuse(out, new CLArray[B](length)(context, bf.dataIO))
      /*
      val result = new CLArray[(A1, B)](length)(context, bf.dataIO)
      assert(result.buffers.length == buffers.length + thatArray.length)
      buffers.zip(result.buffers.take(buffers.length)).map(p => p._1.copyTo(p._2))
      thatArray.buffers.zip(result.buffers.drop(buffers.length)).map(p => p._1.copyTo(p._2))
      result.asInstanceOf[That]
      */
      new CLArray[(A1, B)](length, (buffers ++ thatArray.buffers).map(_.clone))(context, bf.dataIO).asInstanceOf[That]
    }
  }
  
  override def zipWithIndex[A1 >: A, That](implicit bf: CanBuildFrom[Repr, (A1, Int), That]): That = {
    val indexBuffer = new CLGuardedBuffer[Int](length)
    val kernel = indexCode.getKernel(context)
    val globalSizes = 
    kernel.synchronized {
      kernel.setArgs(length.asInstanceOf[Object], indexBuffer.buffer)
      CLEventBound.syncBlock(Array(), Array(indexBuffer), evts => {
        kernel.enqueueNDRange(context.queue, Array(size), evts:_*)
      })
    }
    new CLArray[(A, Int)](length, buffers.map(_.clone) ++ Array(indexBuffer.asInstanceOf[CLGuardedBuffer[Any]])).asInstanceOf[That]
  }
    
  override def clone: CLArray[A] =
    new CLArray[A](length, if (length > 0) buffers.map(_.clone) else null) // TODO map in parallel

  override def size = length

  protected override def mapFallback[B](f: A => B, result: CLArray[B]) = {
    for (i <- 0 until length)
      result(i) = f(this(i))
  }

  override def foreach[U](f: A => U): Unit =
    toArray(dataIO.t) foreach f

  override def filter(p: A => Boolean) =
    filter(p, new CLFilteredArray[A](length))//.toCLArray

  override def filterFallback[That <: CLCollection[A]](p: A => Boolean, out: That)(implicit ff: CLCanFilterFrom[Repr, A, That]) = {
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

    assert(length == other.length)
    if (length > 0) {
      assert(buffers.length == other.buffers.length)
      for ((from, to) <- buffers.zip(other.buffers))
        from.copyTo(to)
    }
  }
}

