/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package scalacl



import org.bridj.StructObject

trait CLCol[T] extends CLEventBound {
  implicit val context: ScalaCLContext
  protected type ThisCol[T] <: CLCol[T]

  def filterFun(f: CLFunction[T, Boolean])(implicit dataIO: CLDataIO[T]): CLCol[T]
  def filter(f: T => Boolean)(implicit dataIO: CLDataIO[T]): CLCol[T]

  def mapFun[V](f: CLFunction[T, V])(implicit dataIO: CLDataIO[T], vIO: CLDataIO[V]): CLCol[V]
  def map[V](f: T => V)(implicit dataIO: CLDataIO[T], vIO: CLDataIO[V]): CLCol[V]

  protected def notImp = error("Not implemented")
  def toCLArray: CLArray[T]
  def toArray: Array[T] = toCLArray.toArray
  def toSeq = toArray.toSeq
  def toSet = toArray.toSet
  def toIndexedSeq = toArray.toIndexedSeq

  @Deprecated
  def toMap[K <: StructObject, V <: StructObject](implicit kvi: T =:= (K, V)) = notImp
  
  def view: CLView[T, ThisCol[T]] = notImp
  def slice(from: Long, to: Long): CLCol[T]
  def take(n: Long): CLCol[T] = slice(n, size)
  def drop(n: Long): CLCol[T] = slice(0, size - n)
  
  def zip[V](other: CLCol[V])(implicit dataIO: CLDataIO[T], vIO: CLDataIO[V]): ThisCol[(T, V)] = notImp
  def zipWithIndex(implicit dataIO: CLDataIO[T]): CLCol[(T, Long)] = zip(new CLLongRange(0, size))
  
  def sizeFuture: CLFuture[Long]
  def size: Long = sizeFuture.get
  def isEmpty = size == 0L
  
}

trait CLUpdatableCol[T] {
  this: CLCol[T] =>

  @Deprecated
  def updateFun(f: CLFunction[T, T])(implicit dataIO: CLDataIO[T]): ThisCol[T]

  def update(f: T => T)(implicit dataIO: CLDataIO[T]): ThisCol[T]
}
trait CLUpdatableFilteredCol[T] {
  this: CLCol[T] =>
  
  @Deprecated
  def refineFilterFun(f: CLFunction[T, Boolean])(implicit dataIO: CLDataIO[T]): ThisCol[T]

  def refineFilter(f: T => Boolean)(implicit dataIO: CLDataIO[T]): ThisCol[T]
}