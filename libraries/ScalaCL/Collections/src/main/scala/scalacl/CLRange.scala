package scalacl

import impl._
import scala.collection.IndexedSeqLike
import scala.collection.generic.CanBuildFrom


object CLRange {
  def convertToRange(buf: CLGuardedBuffer[Int]): Range = {
    val Array(from, to, by, inclusive) = buf.toArray
    if (inclusive == 0)
      from until to by by
    else
      from to to by by
  }
  lazy val toCLArrayCode = new CLSimpleCode("""
    __kernel void toCLArray(int size, __global const int* range, __global int* out) {
      int i = get_global_id(0);
      if (i >= size)
        return;

      // from, to, by, inclusive
      int from = range[0], by = range[2];
      out[i] = from + by * i;
    }
  """)
  def copyToCLArray(range: CLGuardedBuffer[Int], output: CLGuardedBuffer[Int])(implicit context: Context) = {
    val size = output.buffer.getElementCount.toInt
    val kernel = toCLArrayCode.getKernel(context)
    val globalSizes = Array(size)
    kernel.synchronized {
      kernel.setArgs(size.asInstanceOf[Object], range.buffer, output.buffer)
      CLEventBound.syncBlock(Array(range), Array(output), evts => {
        kernel.enqueueNDRange(context.queue, globalSizes, evts:_*)
      })
    }
  }


  implicit def canFilterFrom(implicit ctx: Context, io: CLDataIO[Int]): CLCanFilterFrom[CLRange, Int, CLFilteredArray[Int]] =
    new CLCanFilterFrom[CLRange, Int, CLFilteredArray[Int]] {
      override def dataIO = io
      override def context = ctx
      def rawLength(from: CLRange): Int = from.size
      def newFilterResult(from: CLRange) = new CLFilteredArray[Int](from.size)(context, io)
    }
  
}

import CLFilteredArray.{PresenceType, toBool, toPresence}

class CLRange(
  protected[scalacl] val buffer: CLGuardedBuffer[Int]
)(
  implicit val context: Context
)
  extends IndexedSeq[Int]
  with CopiableToCLArray[Int]
  with CLIndexedSeq[Int]
  with CLIndexedSeqLike[Int, CLIndexedSeq[Int]]
  with MappableToCLArray[Int, CLIndexedSeq[Int]]
{
  def this(range: Range)(implicit context: Context) =
    this(new CLGuardedBuffer[Int](Array(range.start, range.end, range.step, if (range.isInclusive) 1 else 0)))

  override def eventBoundComponents = Seq(buffer)
  override def release = buffer.release
  
  import CLRange._

  override def toArray: Array[Int] = toRange.toArray

  override def newBuilder = error("Not implemented")
  override def toCLArray = {
    val size = length
    val outBuffer = new CLGuardedBuffer[Int](size)
    val out = new CLArray[Int](size, Array(outBuffer.asInstanceOf[CLGuardedBuffer[Any]]))
    copyToCLArray(buffer, outBuffer)
    out
  }
  
  def copyTo(a: CLArray[Int]) = {
    assert(a.length == length)
    val Array(outBuffer) = a.buffers
    copyToCLArray(buffer, outBuffer.asInstanceOf[CLGuardedBuffer[Int]])
  }

  protected override def mapFallback[B](f: Int => B, result: CLArray[B]) = {
    executingScalaFallbackOperation("mapFallback")
    var offset = 0
    for (i <- toRange) {
      result(offset) = f(i)
      offset += 1
    }
  }
  
  protected override def foreachFallback[U](f: Int => U) = {
    executingScalaFallbackOperation("foreachFallback")
    for (i <- toRange) {
      f(i)
    }
  }

  override def filter(p: Int => Boolean): CLFilteredArray[Int] = // TODO ? toCLArray.filter(p)
    filter(p, new CLFilteredArray[Int](length))//.toCLArray

  override def filterFallback[That <: CLCollection[Int]](p: Int => Boolean, out: That)(implicit ff: CLCanFilterFrom[CLIndexedSeq[Int], Int, That]) = {
    import scala.concurrent.ops._
    executingScalaFallbackOperation("filterFallback")
    out match {
      case filteredOut: CLFilteredArray[Int] =>
        val copy = future {
          copyToCLArray(buffer, filteredOut.array.buffers.head.asInstanceOf[CLGuardedBuffer[Int]])
        }
        val range = toRange
        val presenceArr = new Array[PresenceType](range.size)
        var offset = 0
        for (i <- range) {
          if (p(i))
            presenceArr(offset) = toPresence(true)
          offset += 1
        }
        filteredOut.presence.update(presenceArr)
        copy()
    }
  }

  override def apply(index: Int): Int =
    toRange.apply(index)

  override def length = toRange.length
  override def size = length
  
  def toRange = convertToRange(buffer)

  override def foreach[U](f: Int => U): Unit =
    toRange foreach f

  //TODO Changed in Scala 2.9.0 : Range.slice no longer returns a Range !
  //override def slice(from: Int, to: Int) = new CLRange(toRange.slice(from, to))
  
  //override def sizeFuture: CLFuture[Int] = new CLInstantFuture[Int](toRange.size)
}

