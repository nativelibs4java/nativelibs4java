package com.nativelibs4java.scalacl

abstract sealed class Operator(op: String)
case class UnaryOperator(op: String) extends Operator(op)
case class BinaryOperator(op: String) extends Operator(op)

//case class AssignmentOp(op: String) extends BinaryOperator(op)
//case class ArithOp(op: String) extends BinaryOperator(op)

//case class Plus extends ArithOp("+")
