/*
 * SyntaxUtils.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package scalacl
import java.nio._

//class BufferIO[V <: Number, T <: { def get(a: Int): V; def put(a: Int, v: V): Int }]

object SyntaxUtils {
  def unique[C](list: List[C]) = (new scala.collection.mutable.ListBuffer[C]() ++ (new scala.collection.immutable.HashSet[C]() ++ list)).toList

  case class SeqUtils[T](seq: Seq[T]) {
    def implode(sep: String) = {
      if (seq == Nil) ""
      else seq.map(x => if (x == null) "" else x.toString) reduceLeft { _ + sep + _ }
    }
  }
  implicit def Seq2SeqUtils[T](seq: Seq[T]) = SeqUtils[T](seq)



  //implicit def range2Buffer(r: Range): IntBuffer =
  implicit def seq2Buffer(s: Seq[Int   ]): IntBuffer = array2Buffer(s.toArray)
  implicit def seq2Buffer(s: Seq[Short ]): ShortBuffer = array2Buffer(s.toArray)
  implicit def seq2Buffer(s: Seq[Long  ]): LongBuffer = array2Buffer(s.toArray)
  implicit def seq2Buffer(s: Seq[Byte  ]): ByteBuffer = array2Buffer(s.toArray)
  implicit def seq2Buffer(s: Seq[Char  ]): CharBuffer = array2Buffer(s.toArray)
  implicit def seq2Buffer(s: Seq[Double]): DoubleBuffer = array2Buffer(s.toArray)
  implicit def seq2Buffer(s: Seq[Float ]): FloatBuffer = array2Buffer(s.toArray)

  implicit def seq2Buffer(s: List[Int   ]): IntBuffer = array2Buffer(s.toArray)
  implicit def seq2Buffer(s: List[Short ]): ShortBuffer = array2Buffer(s.toArray)
  implicit def seq2Buffer(s: List[Long  ]): LongBuffer = array2Buffer(s.toArray)
  implicit def seq2Buffer(s: List[Byte  ]): ByteBuffer = array2Buffer(s.toArray)
  implicit def seq2Buffer(s: List[Char  ]): CharBuffer = array2Buffer(s.toArray)
  implicit def seq2Buffer(s: List[Double]): DoubleBuffer = array2Buffer(s.toArray)
  implicit def seq2Buffer(s: List[Float ]): FloatBuffer = array2Buffer(s.toArray)

  implicit def array2Buffer(a: Array[Int   ]) = IntBuffer.wrap(a)
  implicit def array2Buffer(a: Array[Short ]) = ShortBuffer.wrap(a)
  implicit def array2Buffer(a: Array[Long  ]) = LongBuffer.wrap(a)
  implicit def array2Buffer(a: Array[Byte  ]) = ByteBuffer.wrap(a)
  implicit def array2Buffer(a: Array[Char  ]) = CharBuffer.wrap(a)
  implicit def array2Buffer(a: Array[Double]) = DoubleBuffer.wrap(a)
  implicit def array2Buffer(a: Array[Float ]) = FloatBuffer.wrap(a)

  def directInts(n: Int) = directBytes(n * 4).asIntBuffer()
  def directShorts(n: Int) = directBytes(n * 2).asShortBuffer()
  def directLongs(n: Int) = directBytes(n * 8).asLongBuffer()
  def directBytes(n: Int) = ByteBuffer.allocateDirect(n).order(ByteOrder.nativeOrder())
  def directChars(n: Int) = directBytes(n * 2).asCharBuffer()
  def directDoubles(n: Int) = directBytes(n * 8).asDoubleBuffer()
  def directFloats(n: Int) = directBytes(n * 4).asFloatBuffer()

  def newBuffer[B <: Buffer](b: Class[B]) : Int => B = b.getName match {
    case "java.nio.IntBuffer"     => directInts(_).asInstanceOf[B]
    case "java.nio.ShortBuffer"   => directShorts(_).asInstanceOf[B]
    case "java.nio.LongBuffer"    => directLongs(_).asInstanceOf[B]
    case "java.nio.ByteBuffer"    => directBytes(_).asInstanceOf[B]
    case "java.nio.CharBuffer"    => directChars(_).asInstanceOf[B]
    case "java.nio.DoubleBuffer"  => directDoubles(_).asInstanceOf[B]
    case "java.nio.FloatBuffer"  => directFloats(_).asInstanceOf[B]
    case _ => throw new IllegalArgumentException("Unknown buffer type : " + b.getName)
  }
  
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
