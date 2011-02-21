import org.bridj._
import org.bridj.Pointer._
import annotation.unchecked.uncheckedVariance

package bridj {
  
  class HasNIOBuffer[T, B <: java.nio.Buffer] {
    //def setValues(pointer: Pointer[T], values: B): Unit
  }
  
  class RichPointer[@specialized T](pointer: org.bridj.Pointer[T]) {
    def getArray: Array[T] = pointer.getArray().asInstanceOf[Array[T]]
    def getArray(length: Int): Array[T] = pointer.getArray(length).asInstanceOf[Array[T]]
    def getArrayAtOffset(byteOffset: Long, length: Int): Array[T] = pointer.getArrayAtOffset(byteOffset, length).asInstanceOf[Array[T]]
    
    def setArray(a: Array[T]) = pointer.setArray(a)
    def setArrayAtOffset(byteOffset: Long, a: Array[T]) = pointer.setArrayAtOffset(byteOffset, a)
    
    def getBuffer[B <: java.nio.Buffer](implicit hnb: HasNIOBuffer[T, B]): B = pointer.getBuffer.asInstanceOf[B]
    def getBuffer[B <: java.nio.Buffer](length: Int)(implicit hnb: HasNIOBuffer[T, B]): B = pointer.getBuffer(length).asInstanceOf[B]
    def getBufferAtOffset[B <: java.nio.Buffer](byteOffset: Long, length: Int)(implicit hnb: HasNIOBuffer[T, B]): B = pointer.getBufferAtOffset(byteOffset, length).asInstanceOf[B]
    
    def setValues[B <: java.nio.Buffer](vs: B)(implicit hnb: HasNIOBuffer[T, B]): Pointer[T] = pointer.setValues(vs)
    def setValuesAtOffset[B <: java.nio.Buffer](byteOffset: Long, vs: B)(implicit hnb: HasNIOBuffer[T, B]): Pointer[T] = pointer.setValuesAtOffset(byteOffset, vs)
    def setValuesAtOffset[B <: java.nio.Buffer](byteOffset: Long, vs: B, valuesOffset: Int, length: Int)(implicit hnb: HasNIOBuffer[T, B]): Pointer[T] = pointer.setValuesAtOffset(byteOffset, vs, valuesOffset, length)
  }
}
package object bridj {
  type Pointer[+T] = org.bridj.Pointer[T @uncheckedVariance]
  //val Pointer: org.bridj.Pointer[_] = null
  
  type SizeT = org.bridj.SizeT
  
  def allocate[T](implicit io: PointerIO[T]): Pointer[T] = org.bridj.Pointer.allocate(io)
  def allocateArray[T](length: Long)(implicit io: PointerIO[T]): Pointer[T] = org.bridj.Pointer.allocateArray(io, length)
  
  def pointer2RichPointer[T](pointer: Pointer[T]) = new RichPointer(pointer)
  
  implicit def booleanPointerIO = PointerIO.getBooleanInstance
  implicit def     intPointerIO = PointerIO.getIntInstance
  implicit def    longPointerIO = PointerIO.getLongInstance
  implicit def   shortPointerIO = PointerIO.getShortInstance
  implicit def  doublePointerIO = PointerIO.getDoubleInstance
  implicit def   floatPointerIO = PointerIO.getFloatInstance
  implicit def    charPointerIO = PointerIO.getCharInstance
  implicit def    bytePointerIO = PointerIO.getByteInstance
  implicit def pointerIO[T](implicit t: ClassManifest[T]): PointerIO[T] = PointerIO.getInstance(t.erasure.asInstanceOf[Class[T]])
  
  import java.nio._
  implicit def hasNIOIntBuffer = new HasNIOBuffer[Int, IntBuffer]
  implicit def hasNIOShortBuffer = new HasNIOBuffer[Short, ShortBuffer]
  implicit def hasNIOLongBuffer = new HasNIOBuffer[Long, LongBuffer]
  implicit def hasNIOByteBuffer = new HasNIOBuffer[Byte, ByteBuffer]
  implicit def hasNIOCharBuffer = new HasNIOBuffer[Char, CharBuffer]
  implicit def hasNIOFloatBuffer = new HasNIOBuffer[Float, FloatBuffer]
  implicit def hasNIODoubleBuffer = new HasNIOBuffer[Double, DoubleBuffer]
}