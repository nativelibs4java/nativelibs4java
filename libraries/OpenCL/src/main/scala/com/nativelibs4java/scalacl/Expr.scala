package com.nativelibs4java.scalacl
import java.nio._
import scala.collection.immutable._
import com.nativelibs4java.opencl.OpenCL4Java._

trait CLValue 

abstract class Expr {
	def typeDesc: TypeDesc;

	def accept(visitor: (Expr, Stack[Expr]) => Unit, stack: Stack[Expr]) : Unit;
	def accept(visitor: (Expr, Stack[Expr]) => Unit) : Unit = accept(visitor, new Stack[Expr]);

	def ~(other: Expr) = BinOp("=", this, other)
	def +~(other: Expr) = BinOp("+=", this, other)
	def -~(other: Expr) = BinOp("-=", this, other)
	def *~(other: Expr) = BinOp("*=", this, other)
	def /~(other: Expr) = BinOp("/=", this, other)
	def ==(other: Expr) = BinOp("==", this, other)
	def <=(other: Expr) = BinOp("<=", this, other)
	def >=(other: Expr) = BinOp(">=", this, other)
	def <(other: Expr) = BinOp("<", this, other)
	def >(other: Expr) = BinOp(">", this, other)
	
	def !() = UnOp("!", true, this)
	
	//def apply(index: Expr) = ArrayIndex(this, index)
	
	def +(other: Expr) = BinOp("+", this, other)
	def -(other: Expr) = BinOp("-", this, other)
	def *(other: Expr) = BinOp("*", this, other)
	def /(other: Expr) = BinOp("/", this, other)
	def %(other: Expr) = BinOp("%", this, other)
	def >>(other: Expr) = BinOp(">>", this, other)
	def <<(other: Expr) = BinOp("<<", this, other)
	
	def includes : List[String] = {
			this match {
			case Fun(name, outType, vars, include) => List(include);
			case BinOp(op, first, second) => first.includes ++ second.includes;
			case UnOp(op, isPrefix, operand) => operand.includes
			case _ => Nil
			}
	}
	def variables: List[AbstractVar] = {
			this match {
			case Fun(name, outType, vars, include) => vars(0).variables//TODO (vars map (x => x variables)) collapse
			case BinOp(op, first, second) => first.variables ++ second.variables
			case UnOp(op, isPrefix, operand) => operand.variables
			case v: AbstractVar => List(v)
			case _ => Nil
			}
	}
}
abstract class TypedExpr(td: TypeDesc) extends Expr {
	override def typeDesc = td
	override def accept(visitor: (Expr, Stack[Expr]) => Unit, stack: Stack[Expr]) : Unit = {
	  visitor(this, stack)
	}
}

/*
abstract class VarOld[G](var context: Context, typeDesc: TypeDesc) extends TypedExpr(typeDesc)
{
	var value: G
	var name: String = ""
	var parallelIndexName: String = ""
 	var isRead = false
 	var isWritten = false
 	var isAggregated = false
 	var clKernel: CLKernel = null;
 	var clMem: CLMem = null;
	var argumentIndex = -1;
	def setup : Unit;
	override def toString = name + (if (parallelIndexName == "") "" else "[" + parallelIndexName + "]")
	//override def accept(visitor: Expr => Unit) = visitor(this)
	//override def inferLocalType = localClass
}

abstract class ArrayVarOld[G <: Buffer, L <: Number](context: Context, typeDesc: TypeDesc) extends VarOld(context, typeDesc)
{
	override def setup: Unit = {
		var mem: CLMem = null;
		var byteCount = getSizeInBytes(value);
		if (isRead && isWritten)
			mem = context.clContext.createInputOutput(byteCount);
		else if (isRead)
			mem = context.clContext.createInput(byteCount);
		else if (isWritten)
			mem = context.clContext.createOutput(byteCount);
		if (mem != null) {
			clKernel.setArg(argumentIndex, mem);
			clMem = mem;
		}
	}
	def set(values: Array[L]) {
		//value.set(values);
		//value.reset()
	}
}
*/
case class BinOp(var op: String, var first: Expr, var second: Expr) extends Expr {
	override def toString() = first + " " + op + " " + second
	override def typeDesc = first.typeDesc combineWith second.typeDesc
	override def accept(visitor: (Expr, Stack[Expr]) => Unit, stack: Stack[Expr]): Unit = {
	  stack push this;
	  first.accept(visitor, stack)
	  second.accept(visitor, stack)
	  stack pop;
	  visitor(this, stack);
	  
	}

}

case class UnOp(var op: String, var isPrefix: Boolean, var value: Expr) extends Expr {
	override def toString() = { 
		if (isPrefix) op + " " + value 
		else value.toString() + " " + op
	}
	override def typeDesc = value.typeDesc
	override def accept(visitor: (Expr, Stack[Expr]) => Unit, stack: Stack[Expr]): Unit = {
	  stack push this
	  value.accept(visitor, stack)
	  stack pop;
	  visitor(this, stack);
	}

}

case class Fun(name: String, outType: PrimType, args: List[Expr], include: String) extends Expr {
  
			def implode(elts: List[String], sep: String) = {
			  if (elts == Nil) "" 
			  else elts reduceLeft { _ + sep + _ } //map { _.toString } 
			  	
			}
   
	override def toString() = name + "(" + implode(args map {_.toString}, ", ") + ")"
	override def typeDesc = {
		var argType = args map (a => a.typeDesc) reduceLeft {(a, b) => a combineWith b};
		TypeDesc(argType.channels, argType.valueType, outType)
	}
	override def accept(visitor: (Expr, Stack[Expr]) => Unit, stack: Stack[Expr]) : Unit = {
	  stack push this
	  args foreach { _.accept(visitor, stack) }
	  stack pop;
	  visitor(this, stack);
	}
}
//case class If(test: Expr, thenExpr: Expr, elseExpr: Expr) extends Expr {
//	override def toString() = "if (" + test + ") { " + thenExpr + "}" + (if (elseExpr == None) "" else " else { " + elseExpr + "}")  
//	override def typeDesc = {
//		var td = thenExpr typeDesc;
//		if (elseExpr != None) 
//			td combineWith elseExpr.typeDesc;
//		else
//			td
//	}
//}

