package scalacl
package collection
package impl

import scalacl._

import scala.collection.generic.CanBuildFrom
import com.nativelibs4java.opencl._

import org.bridj.Pointer
import org.bridj.PointerIO
import scala.math._

trait CLCanBuildFrom[From, Elem, To] extends CanBuildFrom[From, Elem, To] {
  def dataIO: CLDataIO[Elem]
}

/*class CLAbstractCanBuildFrom[FromElem, From :< CLCollection[], Elem, To](implicit val dataIO: CLDataIO[Elem]) extends CLCanBuildFrom[From, Elem, To] {
	//def dataIO: CLDataIO[B]

  def apply(from: From): Builder[Elem, To]

  /** Creates a new builder from scratch.
   *
   *  @return a builder for collections of type `To` with element type `Elem`.
   *  @see scala.collection.breakOut
   */
  def apply(): Builder[Elem, To]
}*/

