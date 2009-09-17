package scalacl


import scala.collection.mutable.Stack
import scala.reflect.Manifest

import java.nio._

import com.nativelibs4java.opencl.OpenCL4Java._
import SyntaxUtils._

trait CLValue
trait Val1 extends CLValue
trait Val2 extends CLValue
trait Val4 extends CLValue

abstract class Expr {
	def typeDesc: TypeDesc;

	def accept(visitor: (Expr, Stack[Expr]) => Unit, stack: Stack[Expr]) : Unit;
	def accept(visitor: (Expr, Stack[Expr]) => Unit) : Unit = accept(visitor, new Stack[Expr]);

        def find[C](implicit c: Manifest[C]) : List[C] = {
          val list = new scala.collection.mutable.ListBuffer[C]()
          accept { (x, stack) => if (x != null && c.erasure.isInstance(x)) list + x.asInstanceOf[C] }
          return list.toList
        }
        
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

  /*
	def includes : List[String] = this match {
          case Fun(name, outType, vars, include) => List(include);
          case BinOp(op, first, second) => first.includes ++ second.includes;
          case UnOp(op, isPrefix, operand) => operand.includes
          case (ae: ArrayElement[_]) => ae.array.includes ++ ae.index.includes
          case _ => Nil
	}
	def variables: List[AbstractVar] = this match {
          case Fun(name, outType, vars, include) => vars(0).variables//TODO (vars map (x => x variables)) collapse
          case BinOp(op, first, second) => first.variables ++ second.variables
          case UnOp(op, isPrefix, operand) => operand.variables
          case (ae: ArrayElement[_]) => ae.array.variables ++ ae.index.variables
          case v: AbstractVar => List(v)
          case _ => {
              println("Unhandled case in Expr.variable : " + this + ": " + getClass.getName)
              Nil
          }
	}*/
}
abstract class TypedExpr(td: TypeDesc) extends Expr {
	override def typeDesc = td
	override def accept(visitor: (Expr, Stack[Expr]) => Unit, stack: Stack[Expr]) : Unit = {
	  visitor(this, stack)
	}
}

abstract case class PrimScal(v: Number, t: PrimType) extends TypedExpr(TypeDesc(1, Scalar, t)) with CLValue {
  override def toString = v.toString
}

class Dim(size: Int) extends PrimScal(size, IntType) with Val1 {
  var name: String = null
  override def toString = if (name == null) "unnamedDim(" + size + ")" else name
}


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

	override def toString() = name + "(" + args.map(_.toString).implode(", ") + ")"
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


case class Int1(value: Int) extends PrimScal(value, IntType) with Val1
case class Int2(x: Int, y: Int) extends TypedExpr(TypeDesc(2, Scalar, IntType)) with CLValue with Val2
case class Int4(x: Int, y: Int, z: Int, w: Int) extends TypedExpr(TypeDesc(4, Scalar, IntType)) with CLValue with Val4 {
  def this(xy: Int2, zw: Int2) = {
    this(xy.x, xy.y, zw.x, zw.y)
  }
}

case class Double1(value: Double) extends PrimScal(value, DoubleType) with Val1
case class Double2(x: Double, y: Double) extends TypedExpr(TypeDesc(2, Scalar, DoubleType)) with CLValue with Val2
case class Double4(x: Double, y: Double, z: Double, w: Double) extends TypedExpr(TypeDesc(4, Scalar, DoubleType)) with CLValue  with Val4 {
  def this(xy: Int2, zw: Int2) = {
    this(xy.x, xy.y, zw.x, zw.y)
  }
}


abstract sealed class VarMode {
  def union(mode: VarMode): VarMode;
}
case object UnknownMode extends VarMode {
  override def union(mode: VarMode) = mode
}

case object ReadMode extends VarMode {
  override def union(mode: VarMode) = if (mode == AggregatedMode) AggregatedMode else if (mode == WriteMode) ReadWriteMode else this
}
case object WriteMode extends VarMode {
  override def union(mode: VarMode) = if (mode == AggregatedMode) AggregatedMode else if (mode == WriteMode) ReadWriteMode else this
}
case object ReadWriteMode extends VarMode {
  override def union(mode: VarMode) = if (mode == AggregatedMode) AggregatedMode else this
}
case object AggregatedMode extends VarMode {
  override def union(mode: VarMode) = this
}

abstract class AbstractVar extends Expr {
  var kernel: CLKernel = null;
  var argIndex = -2;
  var name: String = null;
  var mode: VarMode = UnknownMode;

  def setup
  override def toString() = name;
  override def accept(visitor: (Expr, Stack[Expr]) => Unit, stack: Stack[Expr]) : Unit = {
    visitor(this, stack)
  }

  def getTypeDesc[T](implicit t: Manifest[T], valueType: ValueType) = {
    var c = t.erasure
    var ch = {
      if (c.isPrimitive || c.isAnyOf(classOf[java.lang.Number], classOf[Number])) 1
      else if (classOf[Val1].isAssignableFrom(c)) 1
      else if (classOf[Val2].isAssignableFrom(c)) 2
      else if (classOf[Val4].isAssignableFrom(c)) 4
      else throw new IllegalArgumentException("Unable to guess channels for valueType " + valueType + " and manifest " + t)
    }
    var pt = {
      if (c.isAnyOf(classOf[Int], classOf[Int1], classOf[Int2], classOf[Int4])) IntType
      else if (c.isAnyOf(classOf[Double], classOf[Double1], classOf[Double2], classOf[Double4])) DoubleType
      else throw new IllegalArgumentException("Unable to guess primType for class " + c.getName + " and manifest " + t)
    }
    TypeDesc(ch, valueType, pt)
  }
}


class Var[T](implicit t: Manifest[T]) extends AbstractVar {
  private var value: Option[T] = None;
  def apply() : T = {
    value getOrElse { throw new RuntimeException("Cannot get variable value before setting things up !")}
  }
  override def typeDesc = getTypeDesc[T](t, Scalar)

  def defaultValue[K](implicit k: Manifest[K]): K = {
    var c = k.erasure;
    (
      if (c == classOf[Int])
        new java.lang.Integer(0)
      else if (c == classOf[Double])
        new java.lang.Double(0.0)
      else if (c == classOf[Float])
        new java.lang.Float(0.0f)
      else if (c == classOf[Long])
        new java.lang.Long(0l)
      else if (c == classOf[Short])
        new java.lang.Short(0.asInstanceOf[Short])
      else if (c == classOf[Byte])
        new java.lang.Byte(0.asInstanceOf[Byte])
      else
        c.newInstance()
    ).asInstanceOf[K]
  }
  override def setup = {
    var value = if (this.value == None) defaultValue[T] else this.value
    kernel.setObjectArg(argIndex, value)
  }
}

class ArrayVar[T](implicit t: Manifest[T]) extends AbstractVar {
  var dim: Option[Dim] = None
  
  override def setup = {

  }
  override def typeDesc = getTypeDesc[T](t, Parallel)
  private var mem: Option[CLMem] = None;
  private var value: Option[T] = None;

  def apply(index: Expr) : Expr = new ArrayElement[T](this, index)

  def apply(index: Int) : T = {
    var value = this.value getOrElse { throw new RuntimeException("Cannot get variable value before setting things up !")}
    return value.asInstanceOf[DoubleBuffer].get(index).asInstanceOf[T];
  }
}

class ArrayElement[T](/*implicit t: Manifest[T], */var array: ArrayVar[T], var index: Expr) extends Expr {
  override def typeDesc = {
    var td = array.typeDesc
    TypeDesc(td.channels, Scalar, td.primType)
  }
  override def toString() = array.toString + "[" + index.toString + "]"
  override def accept(visitor: (Expr, Stack[Expr]) => Unit, stack: Stack[Expr]) : Unit = {
    stack push this
    visitor(array, stack)
    visitor(index, stack)
    stack pop;
    visitor(this, stack)
  }

}

class ImageVar[T] extends AbstractVar {
  var width: Option[Dim] = None
  var height: Option[Dim] = None
  override def setup = throw new UnsupportedOperationException("IMPLEMENT ME: ImageVar.setup")
  override def typeDesc : TypeDesc = throw new UnsupportedOperationException("IMPLEMENT ME: ImageVar.typeDesc")
  def apply(x: Expr, y: Expr) = new Pixel[T](this, x, y)
}
class Pixel[T](/*implicit t: Manifest[T], */var image: ImageVar[T], var x: Expr, var y: Expr) extends Expr {
  override def typeDesc = {
    var td = image.typeDesc
    TypeDesc(td.channels, Scalar, td.primType)
  }
  override def toString() = image + "[" + x + ", " + y + "]"
  override def accept(visitor: (Expr, Stack[Expr]) => Unit, stack: Stack[Expr]) : Unit = {
    stack push this
	visitor(image, stack)
    visitor(x, stack)
    visitor(x, stack)
    stack pop;
    visitor(this, stack)
  }

}

