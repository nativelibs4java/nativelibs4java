package scalacl
package collection
import impl._
import scala.collection.IndexedSeqLike
import scala.collection.generic.CanBuildFrom


object CLIntRange {
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

      int from = range[0], by = range[2];
      out[i] = from + by * i;
    }
  """)
  def copyToCLArray(range: CLGuardedBuffer[Int], output: CLGuardedBuffer[Int])(implicit context: ScalaCLContext) = {
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


  implicit def canFilterFrom(implicit ctx: ScalaCLContext, io: CLDataIO[Int]): CLCanFilterFrom[CLIntRange, Int, CLFilteredArray[Int]] =
    new CLCanFilterFrom[CLIntRange, Int, CLFilteredArray[Int]] {
      override def dataIO = io
      override def context = ctx
      def rawLength(from: CLIntRange): Int = from.size
      def newFilterResult(from: CLIntRange) = new CLFilteredArray[Int](from.size)(context, io)
    }
  
}
class CLIntRange(
  protected[scalacl] val buffer: CLGuardedBuffer[Int]
)(
  implicit val context: ScalaCLContext
)
  extends IndexedSeq[Int]
  with CLIndexedSeq[Int]
  with CLIndexedSeqLike[Int, CLIntRange]
  with MappableToCLArray[Int, CLIntRange]
{
  def this(range: Range)(implicit context: ScalaCLContext) =
    this(new CLGuardedBuffer[Int](Array(range.start, range.end, range.step, if (range.isInclusive) 1 else 0)))

  override def eventBoundComponents = Seq(buffer)
  override def release = buffer.release
  
  import CLIntRange._

  override def toArray: Array[Int] = toRange.toArray

  override def newBuilder = error("Not implemented")
  override def toCLArray = {
    val size = length
    val outBuffer = new CLGuardedBuffer[Int](size)
    val out = new CLArray[Int](size, Array(outBuffer.asInstanceOf[CLGuardedBuffer[Any]]))
    copyToCLArray(buffer, outBuffer)
    out
  }

  protected override def mapFallback[B](f: Int => B, result: CLArray[B]) = {
    var offset = 0
    for (i <- toRange) {
      result(offset) = f(i)
      offset += 1
    }
  }

  override def filterFallback[That <: CLCollection[Int]](p: Int => Boolean, out: That)(implicit ff: CLCanFilterFrom[CLIntRange, Int, That]) = {
    import scala.concurrent.ops._

    out match {
      case filteredOut: CLFilteredArray[Int] =>
        val copy = future {
          copyToCLArray(buffer, filteredOut.array.buffers.head.asInstanceOf[CLGuardedBuffer[Int]])
        }
        val range = toRange
        val presenceArr = new Array[Boolean](range.size)
        var offset = 0
        for (i <- range) {
          if (p(i))
            presenceArr(offset) = true
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

  override def slice(from: Int, to: Int) = new CLIntRange(toRange.slice(from, to))
  //override def sizeFuture: CLFuture[Int] = new CLInstantFuture[Int](toRange.size)
}

