/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package scalacl

import com.nativelibs4java.opencl._

import org.bridj.Pointer
import org.bridj.Pointer._
import org.bridj.SizeT

//class ViewCol
trait CLView[T, C] extends CLCol[T] {
  def force: C
}

class CLArrayView[A, T, C](
  col: CLArray[A],
  start: Long,
  end: Long,
  filter: CLFunction[A, Boolean],
  map: CLFunction[A, T],
  filterFun: A => Boolean,
  mapFun: A => T
)(
  implicit 
  aIO: CLDataIO[A],
  dataIO: CLDataIO[T],
  val context: ScalaCLContext
)
extends CLView[T, C]
{
  type ThisCol[T] = CLArrayView[A, T, C]
  
  override def slice(from: Long, to: Long) = {
    if (filter == null) {
      new CLArrayView(col, start + from, start + to, filter, map, filterFun, mapFun)
    } else
      error("TODO : slicing filtered views")
  }

  override def filter(f: T => Boolean)(implicit dataIO: CLDataIO[T]): CLArrayView[A, T, CLFilteredArray[T]] = {
    val mp: A => T = if (map == null) a => a.asInstanceOf[T] else mapFun
    val other: A => Boolean = v => f(mp(v))
    val newFilterFun: A => Boolean = if (filterFun == null) other else v => filterFun(v) && other(v)
    new CLArrayView[A, T, CLFilteredArray[T]](col, start, end, mixError, mixError, newFilterFun, mapFun)
  }

  override def map[V](f: T => V)(implicit tIO: CLDataIO[T], vIO: CLDataIO[V]): CLArrayView[A, V, CLCol[V]] = {
    val newMapFun: A => V = if (map == null) a => f(a.asInstanceOf[T]) else f.compose(mapFun)
    new CLArrayView[A, V, CLCol[V]](col, start, end, mixError, mixError, filterFun, newMapFun)
  }

  override def mapFun[V](f: CLFunction[T, V])(implicit dataIO: CLDataIO[T], vIO: CLDataIO[V]): CLArrayView[A, V, CLCol[V]] = {
    if (f.isOnlyInScalaSpace)
      map(f.function)
    else {
      val newFilter: CLFunction[A, Boolean] = filter
      val newMap: CLFunction[A, V] = if (map == null) f.asInstanceOf[CLFunction[A, V]] else f.compose(map)
      new CLArrayView[A, V, CLCol[V]](col, start, end, newFilter, newMap, mixErrorFun, mixErrorFun)
    }
  }
  protected def mixError[K, V]: CLFunction[K, V] = null
  protected def mixErrorFun[K, V]: K => V = _ => error("Cannot mix OpenCL functions and Scala functions in the same view !")

  override def filterFun(f: CLFunction[T, Boolean])(implicit dataIO: CLDataIO[T]): CLArrayView[A, T, CLFilteredArray[T]] = {
    if (f.isOnlyInScalaSpace)
      filter(f.function)
    else {
      val otherFilter: CLFunction[A, Boolean] = if (map == null) f.asInstanceOf[CLFunction[A, Boolean]] else f.compose(map)
      val newFilter: CLFunction[A, Boolean] = if (filter == null) otherFilter else filter.and(otherFilter)
      val newMap: CLFunction[A, T] = map
      new CLArrayView[A, T, CLFilteredArray[T]](col, start, end, newFilter, newMap, mixErrorFun, mixErrorFun)
    }
  }

  override def view: CLView[T, CLArrayView[A, T, C]] = notImp // view view !
  override def sizeFuture: CLFuture[Long] = new CLInstantFuture(col.size)

  override def force = {
    if (filter != null) {
      val out = CLArray[T](col.size)
      // TODO : use different classes for CLFilteredArrayView
      // TODO actually compute and write data here !
      out
    } else {
      val out = new CLFilteredArray[T](dataIO.createBuffers(col.size))
      // TODO actually compute and write data here !
      out
    }
  }.asInstanceOf[C]

  override def toCLArray: CLArray[T] = notImp
}