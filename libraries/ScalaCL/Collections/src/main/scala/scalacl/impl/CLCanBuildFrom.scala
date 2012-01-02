package scalacl

package impl

import scalacl._

import scala.collection.generic.CanBuildFrom
import com.nativelibs4java.opencl._

import org.bridj.Pointer
import org.bridj.PointerIO
import scala.math._
import scala.collection.mutable.Builder

trait CLCanBuildFrom[-From, Elem, +To] extends CanBuildFrom[From, Elem, To] {
  def dataIO: CLDataIO[Elem]
}

trait CLCanFilterFrom[-From, Elem, +To] {
  def context: Context
  def dataIO: CLDataIO[Elem]

  def rawLength(from: From): Int
  def newFilterResult(from: From): To
  //def apply(from: From): Builder[Elem, To]
  //def apply(): Builder[Elem, To]
}


