package scalacl

import impl._

import scala.collection.generic.CanBuildFrom
import com.nativelibs4java.opencl._
import com.nativelibs4java.opencl.util._
import scala.collection.IndexedSeqLike
import scala.collection.mutable.{ArrayBuffer, IndexedSeqOptimized, Builder}
import scala.collection.generic._
import scala.annotation.unchecked.uncheckedVariance

object CLArray {
  val MaxReductionSize = Option(System.getenv("SCALACL_MAX_REDUCTION_SIZE")).getOrElse("32").toInt
  
  lazy val indexCode = new CLSimpleCode("""
    __kernel void indexCode(int size, __global int* out) {
      int i = get_global_id(0);
      if (i >= size)
        return;

      out[i] = i;
    }
  """)
  
  def apply[A](values: A*)(implicit context: Context, dataIO: CLDataIO[A]) =
    fromSeq(values)

  def fromSeq[A](values: Seq[A])(implicit context: Context, dataIO: CLDataIO[A]) = {
    implicit val t = dataIO.t
    val valuesArray = values.toArray
    val length = valuesArray.length
    if (t.erasure.isPrimitive) {
      new CLArray[A](length, if (length == 0) null else Array(new CLGuardedBuffer[A](length).update(valuesArray).asInstanceOf[CLGuardedBuffer[Any]]))
    } else {
      val a = new CLArray[A](length)
      // TODO find a faster way to handle initial copy of complex types !!!
      for (i <- 0 until length)
        dataIO.store(values(i), a.buffers, 0)
      
      a
    }
  }

  def newBuilder[A](implicit context: Context, dataIO: CLDataIO[A]): Builder[A, CLArray[A]] =
    new ArrayBuffer[A].mapResult(b => fromSeq(b))
}
import CLFilteredArray.{PresenceType, toBool, toPresence}

trait CLWithForeach[A, +Repr <: CLCollectionLike[A, Repr] with CLCollection[A]] {
  //self =>
  this: Repr =>

  override def foreach[U](f: A => U): Unit = {
    f match {
      case clf: CLRunnable if !clf.isOnlyInScalaSpace && !useScalaFunctions =>
        clf.run(
          args = Array(this),
          reads = Array(this), // TODO handle captured arrays
          writes = Array() // TODO handle captured arrays
        )(
          dims = Array(length)
        )(context)
      case _ =>
        foreachFallback(f)
    }
  }
  protected def foreachFallback[U](f: A => U): Unit = {
    executingScalaFallbackOperation("foreachFallback")
    for (i <- 0 until length)
      f(this(i))
  }
}
trait MappableToCLArray[A, +Repr <: CLCollectionLike[A, Repr] with CLCollection[A]] 
extends CLWithForeach[A, Repr]
{
  //self =>
  this: Repr =>
  //this: CLIndexedSeq[A] with CLIndexedSeqLike[A, Repr] => //with WithContext with CLEventBoundContainer => // CLIndexedSeqLike[A, Repr] =>
  
  def length: Int
  protected def mapFallback[B](f: A => B, out: CLArray[B]): Unit
  override def map[B, That](f: A => B, out: That)(implicit bf: CanBuildFrom[Repr, B, That]): That = {
    if (!bf.isInstanceOf[CLCanBuildFrom[Repr, B, That]])
      return toSeq.map(f)(bf.asInstanceOf[CanBuildFrom[Seq[A], B, That]])
      
    val result = reuse(out, new CLArray[B](length)(context, bf.dataIO))

    f match {
      case clf: CLRunnable if !clf.isOnlyInScalaSpace && !useScalaFunctions =>
        clf.run(
          args = Array(this, result),
          reads = Array(this),
          writes = Array(result)
        )(
          dims = Array(length)
        )(context)
      case _ =>
        mapFallback(f, result)
    }
    result.asInstanceOf[That]
  }
}

/**
 * Array-like collection stored in OpenCL buffers, which uses CLFunction in map, filter operations.<br>
 * CLArray is a mutable yet asynchronous structure :
 * $ - `a.map(f).map(g)` returns an unfinished CLArray
 * $ - New reads wait for past writes to finish
 * $ - New writes wait for past reads and writes to finish
 */
class CLArray[A](
  val length: Int, 
  protected[scalacl] val buffers: Array[CLGuardedBuffer[Any]]
)(
  implicit val context: Context,
  val dataIO: CLDataIO[A]
)
  extends IndexedSeqOptimized[A, CLIndexedSeq[A]]
  with CopiableToCLArray[A]
  with CLIndexedSeq[A]//IndexedSeq[A]
  with GenericTraversableTemplate[A, CLArray]
  with CLIndexedSeqLike[A, CLIndexedSeq[A]]
  with MappableToCLArray[A, CLIndexedSeq[A]]
{
  def this(length: Int)(implicit context: Context, dataIO: CLDataIO[A]) =
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

  /**
   * Mutate a specific element of the array
   */
  def update(index: Int, value: A): Unit =
    if (length > 0)
      dataIO.store(value, buffers, index)
    else
      throw new ArrayIndexOutOfBoundsException("Empty CLArray !")

  /**
   * Mutate all items of this array with a CLFunction 
   */
  def update(f: A => A): CLArray[A] =
    map(f, this)(new CLCanBuildFrom[Repr, A, CLArray[A]] {
      override def dataIO = CLArray.this.dataIO
      override def apply() = newBuilder.asInstanceOf[Builder[A, CLArray[A]]]
      override def apply(from: Repr) = newBuilder.asInstanceOf[Builder[A, CLArray[A]]]
    })

  override def zip$into[A1 >: A, B, That](that: Iterable[B], out: That)(implicit bf: CanBuildFrom[Repr, (A1, B), That]): That = {
    if (!that.isInstanceOf[CLCollection[_]])
      super.zip(that)
    else {
      val thatArray = that.asInstanceOf[CLCollection[B]].toCLArray
      
      val result = reuse(out, new CLArray[(A1, B)](length)(context, bf.dataIO))
      (buffers ++ thatArray.buffers).zip(result.buffers).map(p => p._1.copyTo(p._2))
      result.asInstanceOf[That]
    }
  }
  
  def zip$ShareBuffers[A1 >: A, B, That](that: Iterable[B])(implicit bf: CanBuildFrom[Repr, (A1, B), That]): That = 
    zip$into[A1, B, That](that, new CLArray[(A1, B)](length, buffers ++ that.asInstanceOf[CLArray[B]].buffers)(context, bf.dataIO).asInstanceOf[That])
  
  override def zipWithIndex$into[A1 >: A, That](out: That)(implicit bf: CanBuildFrom[Repr, (A1, Int), That]): That = {
    val result = reuse(out, new CLArray[(A1, Int)](length)(context, bf.dataIO))
    val indexBuffer = result.buffers.last.asInstanceOf[CLGuardedBuffer[Int]]
    buffers.zip(result.buffers.take(buffers.size)).map(p => p._1.copyTo(p._2))
    
    val kernel = indexCode.getKernel(context)
    kernel.synchronized {
      kernel.setArgs(length.asInstanceOf[Object], indexBuffer.buffer)
      CLEventBound.syncBlock(Array(), Array(indexBuffer), evts => {
        kernel.enqueueNDRange(context.queue, Array(length), evts:_*)
      })
    }
    result.asInstanceOf[That]
  }
  
  def zipWithIndex$ShareBuffers[A1 >: A, That](implicit bf: CanBuildFrom[Repr, (A1, Int), That]): That =
    zipWithIndex$into[A1, That](new CLArray[(A1, Int)](length, buffers ++ Array(new CLGuardedBuffer[Int](length).asInstanceOf[CLGuardedBuffer[Any]]))(context, bf.dataIO).asInstanceOf[That])
  
    
  protected def reduce[B >: A](op: ReductionUtils.Operation): B = {
    assert(buffers.length == 1)
    val buffer = buffers.head.asInstanceOf[CLGuardedBuffer[A]]
    val (reductionType, channels) = dataIO.reductionType
    val reductor = ReductionUtils.createReductor(context, op, reductionType, channels).asInstanceOf[ReductionUtils.Reductor[A]]
    
    val reduction = org.bridj.Pointer.allocate(dataIO.pointerIO)
    CLEventBound.syncBlock(Array(buffer), Array(), evts => {
      reductor.reduce(context.queue, buffer.buffer, length: Long, reduction, MaxReductionSize, evts: _*).waitFor
      null
    })
    reduction.get
  }
  
  protected def isDefaultOrdering(cmp: Ordering[_]) =
    cmp == Boolean ||
    cmp == Byte ||
    cmp == Char ||
    cmp == Double ||
    cmp == Float ||
    cmp == Int ||
    cmp == Long ||
    cmp == Short
  
  protected def isDefaultNumeric(num: Numeric[_]) = {
    import Numeric._
    
    num == ByteIsIntegral ||
    num == CharIsIntegral ||
    num == DoubleIsFractional ||
    num == FloatIsFractional ||
    num == IntIsIntegral ||
    num == LongIsIntegral ||
    num == ShortIsIntegral
  }
    
  /**
   * Perform parallel sum of this array's values, if the implicit numeric is the default one (otherwise, perform slow sum that reads data back from OpenCL to Scala memory)
   */ 
  override def sum[B >: A](implicit num: Numeric[B]): B =
    if (isDefaultNumeric(num))
      reduce[B](ReductionUtils.Operation.Add)
    else
      super.sum[B]
    
  /**
   * Perform parallel product of this array's values, if the implicit numeric is the default one (otherwise, perform slow product that reads data back from OpenCL to Scala memory)
   */ 
  override def product[B >: A](implicit num: Numeric[B]): B =
    if (isDefaultNumeric(num))
      reduce[B](ReductionUtils.Operation.Multiply)
    else
      super.product[B]
  
  /**
   * Perform parallel min of this array's values, if the implicit ordering is the default one (otherwise, perform slow min that reads data back from OpenCL to Scala memory)
   */ 
  override def min[B >: A](implicit cmp: Ordering[B]): A =
    if (isDefaultOrdering(cmp))
      reduce[A](ReductionUtils.Operation.Min)
    else
      super.min[B]
    
  /**
   * Perform parallel max of this array's values, if the implicit ordering is the default one (otherwise, perform slow max that reads data back from OpenCL to Scala memory)
   */ 
  override def max[B >: A](implicit cmp: Ordering[B]): A =
    if (isDefaultOrdering(cmp))
      reduce[A](ReductionUtils.Operation.Max)
    else
      super.max[B]
    
  /**
   * Clone this CLArray<br>
   * TODO copy-on-write cloning
   */ 
  override def clone: CLArray[A] =
    new CLArray[A](length, if (length > 0) buffers.map(_.clone) else null) // TODO map in parallel

  override def size = length

  protected override def mapFallback[B](f: A => B, result: CLArray[B]) = {
    executingScalaFallbackOperation("mapFallback")
    for (i <- 0 until length)
      result(i) = f(this(i))
  }

  //override def foreach[U](f: A => U): Unit =
  //  toArray(dataIO.t) foreach f

  override def filter(p: A => Boolean) =
    filter(p, new CLFilteredArray[A](length))//.toCLArray

  override def filterFallback[That <: CLCollection[A]](p: A => Boolean, out: That)(implicit ff: CLCanFilterFrom[Repr, A, That]) = {
    import scala.concurrent.ops._
    executingScalaFallbackOperation("filterFallback")
    
    out match {
      case filteredOut: CLFilteredArray[A] =>
        val copy = if (filteredOut.array == this) null else future {
          copyTo(filteredOut.array)
        }

        val presenceArr = new Array[PresenceType](length)
        for (i <- 0 until length) {
          val value = this(i)
          presenceArr(i) = toPresence(p(value))
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

