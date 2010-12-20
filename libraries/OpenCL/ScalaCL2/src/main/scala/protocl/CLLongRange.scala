/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package protocl

class CLIntRange(buffer: CLGuardedBuffer[Int])(implicit val context: ScalaCLContext) extends CLCol[Int] {
  //type ThisCol[T] = CLIntRange

  def this(range: Range)(implicit context: ScalaCLContext) =
    this(new CLGuardedBuffer[Int](Array(range.start, range.end, range.step, if (range.isInclusive) 1 else 0)))
    
  //lazy val Array(from, until, step) = fromUntilStep.toArray
  
  override def filterFun(f: CLFunction[Int, Boolean])(implicit dataIO: CLDataIO[Int]): CLCol[Int] = notImp
  override def filter(f: Int => Boolean)(implicit dataIO: CLDataIO[Int]): CLCol[Int] = notImp

  override def mapFun[V](f: CLFunction[Int, V])(implicit dataIO: CLDataIO[Int], vIO: CLDataIO[V]): CLCol[V] = notImp
  override def map[V](f: Int => V)(implicit dataIO: CLDataIO[Int], vIO: CLDataIO[V]): CLCol[V] = notImp

  override def toCLArray: CLArray[Int] = {
    val range = toRange
    new CLArray[Int](range.size).updateFun(CLFun[Int, Int](Seq(range.start + " + $i * " + range.step)))
  }
  override def toArray: Array[Int] = toRange.toArray

  def toRange = CLIntRange.toRange(buffer)

  //override def view: CLView[Int, ThisCol[Int]] = notImp
  override def slice(from: Int, to: Int) = new CLIntRange(toRange.slice(from, to))
  override def sizeFuture: CLFuture[Int] = new CLInstantFuture[Int](toRange.size)
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