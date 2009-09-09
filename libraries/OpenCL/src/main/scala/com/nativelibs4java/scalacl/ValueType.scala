package com.nativelibs4java.scalacl

case object Scalar extends ValueType
case object Parallel extends ValueType
case object Reduction extends ValueType

abstract sealed class ValueType {

	def combineWith(o: ValueType): ValueType = (this, o) match {
		case (Parallel, _) | (_, Parallel) => Parallel
		case _ => Scalar
	}
}
