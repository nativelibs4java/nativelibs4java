package scalacl
package collection
import impl._
import scala.collection.IndexedSeqLike
import scala.collection.generic.CanBuildFrom

class CLIntRange(buffer: CLGuardedBuffer[Int])(implicit val context: ScalaCLContext)
  extends CLCollection[Int, CLIntRange]
  with IndexedSeqLike[Int, CLIntRange]
  with MappableToCLArray[Int, CLIntRange]
{
  def this(range: Range)(implicit context: ScalaCLContext) =
    this(new CLGuardedBuffer[Int](Array(range.start, range.end, range.step, if (range.isInclusive) 1 else 0)))
    
  def toArray: Array[Int] = toRange.toArray

  override def newBuilder = error("Not implemented")
  override def toCLArray = error("Not implemented")

  protected override def mapFallback[B](f: Int => B, result: CLArray[B]) = {
    var offset = 0
    for (i <- toRange) {
      result(offset) = f(i)
      offset += 1
    }
  }

  override def apply(index: Int) =
    toRange.apply(index)

  override def length = toRange.length
  
  def toRange = CLIntRange.toRange(buffer)

  override def slice(from: Int, to: Int) = new CLIntRange(toRange.slice(from, to))
  //override def sizeFuture: CLFuture[Int] = new CLInstantFuture[Int](toRange.size)
}

object CLIntRange {
  def toRange(buf: CLGuardedBuffer[Int]): Range = {
    val Array(from, to, by, inclusive) = buf.toArray
    if (inclusive == 0)
      from until to by by
    else
      from to to by by
  }
}
