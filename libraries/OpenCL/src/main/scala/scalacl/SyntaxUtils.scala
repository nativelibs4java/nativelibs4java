/*
 * SyntaxUtils.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package scalacl
import java.nio._

object SyntaxUtils {


  def implode(elts: List[String], sep: String) = {
    if (elts == Nil) ""
    else elts reduceLeft { _ + sep + _ } //map { _.toString }

  }

  implicit def array2Buffer(a: Array[Int   ]) = IntBuffer.wrap(a)
  implicit def array2Buffer(a: Array[Short ]) = ShortBuffer.wrap(a)
  implicit def array2Buffer(a: Array[Long  ]) = LongBuffer.wrap(a)
  implicit def array2Buffer(a: Array[Byte  ]) = ByteBuffer.wrap(a)
  implicit def array2Buffer(a: Array[Char  ]) = CharBuffer.wrap(a)
  implicit def array2Buffer(a: Array[Double]) = DoubleBuffer.wrap(a)
  implicit def array2Buffer(a: Array[Float ]) = FloatBuffer.wrap(a)

  def directInts(n: Int) = ByteBuffer.allocateDirect(n * 4).asIntBuffer()
  def directShorts(n: Int) = ByteBuffer.allocateDirect(n * 2).asShortBuffer()
  def directLongs(n: Int) = ByteBuffer.allocateDirect(n * 8).asLongBuffer()
  def directBytes(n: Int) = ByteBuffer.allocateDirect(n)
  def directChars(n: Int) = ByteBuffer.allocateDirect(n * 2).asCharBuffer()
  def directDoubles(n: Int) = ByteBuffer.allocateDirect(n * 8).asDoubleBuffer()
  def directFloats(n: Int) = ByteBuffer.allocateDirect(n * 4).asFloatBuffer()

  implicit def class2ClassUtils(c: Class[_]) = ClassUtils(c)
  case class ClassUtils(var target: Class[_]) {
    def isBuffer() = classOf[Buffer] isAssignableFrom target
    def isAnyOf(matches: Class[_]*) : Boolean = {
      for (m <- matches.elements)
      if (m isAssignableFrom target)
      return true
      return false
    }
  }

}
