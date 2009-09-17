package com.nativelibs4java.scalacl

import java.nio._
import scala.reflect.Manifest
import SyntaxUtils._

case object IntType 	extends PrimType("int")
case object LongType 	extends PrimType("long long")
case object ShortType 	extends PrimType("short")
case object ByteType 	extends PrimType("byte")
case object FloatType 	extends PrimType("float")
case object DoubleType 	extends PrimType("double")

abstract sealed class PrimType(str: String) {
  override def toString = str
  implicit def class2ClassUtils(target: Class[_]) = ClassUtils(target)

  def combineWith(o: PrimType): PrimType = {
    for (t <- List(DoubleType, FloatType, LongType, IntType, ShortType, ByteType))
    if (o == t || this == t)
    return t;
    throw new IllegalArgumentException("Unhandled combination of primitive types : " + this + " with " + o)
  }
  def getPrimType(c: Class[_]) : PrimType = {
    if (c.isAnyOf(classOf[Int], classOf[IntBuffer]))
    return IntType;
    if (c isAnyOf(classOf[Long], classOf[LongBuffer]))
    return LongType;
    if (c isAnyOf(classOf[Short], classOf[ShortBuffer]))
    return ShortType;
    if (c isAnyOf(classOf[Byte], classOf[ByteBuffer]))
    return ByteType;
    if (c isAnyOf(classOf[Float], classOf[FloatBuffer]))
    return FloatType;
    if (c isAnyOf(classOf[Double], classOf[DoubleBuffer]))
    return FloatType;

    throw new IllegalArgumentException("No primitive type is associated to class " + c.getName());
  }
}

case object Scalar extends ValueType
case object Parallel extends ValueType
case object Reduction extends ValueType

abstract sealed class ValueType {

  def combineWith(o: ValueType): ValueType = (this, o) match {
    case (Parallel, _) | (_, Parallel) => Parallel
    case _ => Scalar
  }
}

case class TypeDesc(channels: Int, valueType: ValueType, primType: PrimType) {
  def combineWith(o: TypeDesc): TypeDesc = {
    var cn = 0;
    if (channels != o.channels) {
      if (o.channels == 1)
      cn = channels;
      else if (channels == 1)
      cn = o.channels;
      else
      throw new RuntimeException("Mismatching channels : " + channels +" vs. " + o.channels)
    } else
    cn = channels;

    return TypeDesc(cn, valueType combineWith o.valueType, primType combineWith o.primType)
  }

  def globalCType = primType.toString + (if (valueType != Scalar) "*" else "")
}
