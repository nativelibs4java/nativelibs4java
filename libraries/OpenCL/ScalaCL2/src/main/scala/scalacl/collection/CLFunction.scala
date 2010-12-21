package scalacl
package collection
import impl._

import scala.collection.generic.CanBuildFrom
import com.nativelibs4java.opencl._


trait CLFunction {
  def apply(dims: Array[Int], args: Array[Any], reads: Array[CLEventBoundContainer], writes: Array[CLEventBoundContainer]): Unit
}
