/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package scalacl

import org.bridj.StructObject

class CLEmptyCol[T](implicit val context: ScalaCLContext, dataIO: CLDataIO[T]) extends CLCol[T] {
  type ThisCol[T] <: CLEmptyCol[T]

  override def filterFun(f: CLFunction[T, Boolean])(implicit dataIO: CLDataIO[T]): CLCol[T] = this
  override def filter(f: T => Boolean)(implicit dataIO: CLDataIO[T]): CLCol[T] = this

  override def mapFun[V](f: CLFunction[T, V])(implicit dataIO: CLDataIO[T], vIO: CLDataIO[V]): CLCol[V] = new CLEmptyCol[V]
  override def map[V](f: T => V)(implicit dataIO: CLDataIO[T], vIO: CLDataIO[V]): CLCol[V] = new CLEmptyCol[V]

  override def toCLArray: CLArray[T] = CLArray[T](0)
  override def toArray: Array[T] = {
    implicit val t = dataIO.t
    new Array[T](0)
  }
  override def toSeq = Seq[T]()
  override def toSet = Set[T]()
  override def toIndexedSeq = scala.collection.immutable.IndexedSeq[T]()

  //override def view: CLView[T, CLEmptyCol[T]] = notImp//new CLEmptyView[T]
  override def slice(from: Long, to: Long): CLCol[T] = {
    if (from != 0 || to != 0)
      error("Invalid slice for empty collection : " + (from, to))
    this
  }
  
  override def zip[V](other: CLCol[V])(implicit dataIO: CLDataIO[T], vIO: CLDataIO[V]): ThisCol[(T, V)] = {
      assert(other.isEmpty)
      new CLEmptyCol[(T, V)].asInstanceOf[ThisCol[(T, V)]] // TODO !!!
  }

  override def sizeFuture: CLFuture[Long] = new CLInstantFuture(0L)
}
/*
class CLEmptyView[T](implicit context: ScalaCLContext, dataIO: CLDataIO[T]) extends CLEmptyCol[T] with CLView[T, CLEmptyCol[T]] {
  type ThisCol[T] <: CLEmptyView[T]

  override def force = new CLEmptyCol[T]
}*/
