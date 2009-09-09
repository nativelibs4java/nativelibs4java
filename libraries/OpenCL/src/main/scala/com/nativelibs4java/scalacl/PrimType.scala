package com.nativelibs4java.scalacl
import java.nio._


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