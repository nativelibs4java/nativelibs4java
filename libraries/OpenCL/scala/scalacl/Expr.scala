package scalacl
import java.nio._

trait CLValue 

abstract class Expr {
	def typeDesc: TypeDesc;

	def accept(visitor: Expr => Unit);

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
	def variables: List[Var[_]] = {
			this match {
			case Fun(name, outType, vars, include) => vars(0).variables//TODO (vars map (x => x variables)) collapse
			case BinOp(op, first, second) => first.variables ++ second.variables
			case UnOp(op, isPrefix, operand) => operand.variables
			case v: Var[_] => List(v)
			case _ => Nil
			}
	}
}
abstract class TypedExpr(td: TypeDesc) extends Expr {
	override def typeDesc = td
	override def accept(visitor: Expr => Unit) = {
	  visitor(this)
	}
}

abstract class Var[G](typeDesc: TypeDesc) extends TypedExpr(typeDesc) 
{
	var value: G
	var name: String = ""
	var parallelIndexName: String = ""
 	var isRead = false
 	var isWritten = false
 	var isAggregated = false
	var argumentIndex = -1;
	override def toString = name + (if (parallelIndexName == "") "" else "[" + parallelIndexName + "]")
	//override def accept(visitor: Expr => Unit) = visitor(this)
	//override def inferLocalType = localClass
}

abstract class ArrayVar[G <: Buffer, L <: Number](typeDesc: TypeDesc) extends Var(typeDesc) 
{
	def set(values: Array[L]) {
		//value.set(values);
		//value.reset()
	}
}

case class BinOp(var op: String, var first: Expr, var second: Expr) extends Expr {
	override def toString() = first + " " + op + " " + second
	override def typeDesc = first.typeDesc combineWith second.typeDesc
	override def accept(visitor: Expr => Unit) = {
	  first.accept(visitor)
	  second.accept(visitor)
	  visitor(this)
	}

}

case class UnOp(var op: String, var isPrefix: Boolean, var value: Expr) extends Expr {
	override def toString() = { 
		if (isPrefix) op + " " + value 
		else value.toString() + " " + op
	}
	override def typeDesc = value.typeDesc
	override def accept(visitor: Expr => Unit) = {
	  value.accept(visitor)
	  visitor(this)
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
	override def accept(visitor: Expr => Unit) = {
	  args foreach { _.accept(visitor) }
	  visitor(this)
	}

}
object Expr {
	def cos	(x: Expr) = Fun("cos", DoubleType, List(x), "math.h")
	def sin	(x: Expr) = Fun("sin", DoubleType, List(x), "math.h")
	def tan	(x: Expr) = Fun("tan", DoubleType, List(x), "math.h")
	def atan(x: Expr) = Fun("atan", DoubleType, List(x), "math.h")
	def acos(x: Expr) = Fun("acos", DoubleType, List(x), "math.h")
	def asin(x: Expr) = Fun("asin", DoubleType, List(x), "math.h")
	def cosh(x: Expr) = Fun("cosh", DoubleType, List(x), "math.h")
	def sinh(x: Expr) = Fun("sinh", DoubleType, List(x), "math.h")
	def tanh(x: Expr) = Fun("tanh", DoubleType, List(x), "math.h")
	def atan2(x: Expr, y: Expr) = Fun("atan2", DoubleType, List(x, y), "math.h");
	
	//def implode[T](elts: List[T], sep: String) = elts map (x => x.toString()) reduceLeft ((a, b) => a + sep + b)
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

