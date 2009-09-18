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

case class VisitInfo(visitor: (Node, Stack[Node]) => Unit, stack: Stack[Node])
  
abstract class Node {
  def accept(info: VisitInfo) : Unit;
  def accept(visitor: (Node, Stack[Node]) => Unit) : Unit = accept(VisitInfo(visitor, new Stack[Node]));
  
  def visit(info: VisitInfo, children: Node*) = {
    info.visitor(this, info.stack)
    if (children != Nil) {
      info.stack push this;
      children foreach {x => x.accept(info)}
      info.stack pop;
    }
  }
  def find[C](implicit c: Manifest[C]) : List[C] = {
    val list = new scala.collection.mutable.ListBuffer[C]()
    accept { (x, stack) => if (x != null && c.erasure.isInstance(x)) list + x.asInstanceOf[C] }
    return list.toList
  }
}

abstract class Stat extends Node
case class Assignment(op: String, target: Expr, value: Expr) extends Stat {
  override def toString = target + " " + op + " " + value + ";"
  override def accept(info: VisitInfo): Unit = visit(info, target, value);
}
case class ExprStat(expr: Expr) extends Stat {
  override def toString = expr.toString + ";"
  override def accept(info: VisitInfo): Unit = visit(info, expr)
}

/*object Expr {
  protected def wrapparen(expr: Expr) = {

  }
}*/
abstract class Expr extends Node {
  def typeDesc: TypeDesc;

  def ~(other: Expr) = Assignment("=", this, other)
  def +~(other: Expr) = Assignment("+=", this, other)
  def -~(other: Expr) = Assignment("-=", this, other)
  def *~(other: Expr) = Assignment("*=", this, other)
  def /~(other: Expr) = Assignment("/=", this, other)
  def ==(other: Expr) = BinOp("==", this, other)
  def <=(other: Expr) = BinOp("<=", this, other)
  def >=(other: Expr) = BinOp(">=", this, other)
  def <(other: Expr) = BinOp("<", this, other)
  def >(other: Expr) = BinOp(">", this, other)

  def !() = UnOp("!", true, this)

  def +(other: Expr) = BinOp("+", this, other)
  def -(other: Expr) = BinOp("-", this, other)
  def *(other: Expr) = BinOp("*", this, other)
  def /(other: Expr) = BinOp("/", this, other)
  def %(other: Expr) = BinOp("%", this, other)
  def >>(other: Expr) = BinOp(">>", this, other)
  def <<(other: Expr) = BinOp("<<", this, other)
}
abstract class TypedExpr(td: TypeDesc) extends Expr {
  override def typeDesc = td
  override def accept(info: VisitInfo) : Unit = visit(info);
}

abstract case class PrimScal(v: Number, t: PrimType) extends TypedExpr(TypeDesc(1, Scalar, t)) with CLValue {
  override def toString = v.toString
}

class Dim(var size: Int) extends PrimScal(size, IntType) with Val1 {
  var name: String = null
  var dimIndex = -1
  override def toString = if (name == null) "unnamedDim(" + size + ")" else name
}
object Dim {
  def apply(size: Int) = new Dim(size)
}

case class BinOp(var op: String, var first: Expr, var second: Expr) extends Expr {
  override def toString() = "(" + first + ") " + op + " (" + second + ")"
  override def typeDesc = first.typeDesc combineWith second.typeDesc
  override def accept(info: VisitInfo): Unit = visit(info, first, second)
}

case class UnOp(var op: String, var isPrefix: Boolean, var value: Expr) extends Expr {
  override def toString() = {
    if (isPrefix) "(" + op + " " + value + ")"
    else "(" + value.toString() + " " + op + ")"
  }
  override def typeDesc = value.typeDesc
  override def accept(info: VisitInfo): Unit = visit(info, value)
}

case class Fun(name: String, outType: PrimType, args: Expr*) extends Expr {

  override def toString() = name + "(" + args.map(_.toString).implode(", ") + ")"
  override def typeDesc = {
    var argType = args map (a => a.typeDesc) reduceLeft {(a, b) => a combineWith b};
    TypeDesc(argType.channels, argType.valueType, outType)
  }
  override def accept(info: VisitInfo) : Unit = {
    info.visitor(this, info.stack);
    info.stack push this
    args foreach { _.accept(info) }
    info.stack pop;
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
  def this(xy: Double2, zw: Double2) = {
    this(xy.x, xy.y, zw.x, zw.y)
  }
}

case class Float1(value: Float) extends PrimScal(value, FloatType) with Val1
case class Float2(x: Float, y: Float) extends TypedExpr(TypeDesc(2, Scalar, FloatType)) with CLValue with Val2
case class Float4(x: Float, y: Float, z: Float, w: Float) extends TypedExpr(TypeDesc(4, Scalar, FloatType)) with CLValue  with Val4 {
  def this(xy: Float2, zw: Float2) = {
    this(xy.x, xy.y, zw.x, zw.y)
  }
}


case class Short1(value: Short) extends PrimScal(value, ShortType) with Val1
case class Short2(x: Short, y: Short) extends TypedExpr(TypeDesc(2, Scalar, ShortType)) with CLValue with Val2
case class Short4(x: Short, y: Short, z: Short, w: Short) extends TypedExpr(TypeDesc(4, Scalar, ShortType)) with CLValue  with Val4 {
  def this(xy: Short2, zw: Short2) = {
    this(xy.x, xy.y, zw.x, zw.y)
  }
}


case class Byte1(value: Byte) extends PrimScal(value, ByteType) with Val1
case class Byte2(x: Byte, y: Byte) extends TypedExpr(TypeDesc(2, Scalar, ByteType)) with CLValue with Val2
case class Byte4(x: Byte, y: Byte, z: Byte, w: Byte) extends TypedExpr(TypeDesc(4, Scalar, ByteType)) with CLValue  with Val4 {
  def this(xy: Byte2, zw: Byte2) = {
    this(xy.x, xy.y, zw.x, zw.y)
  }
}



sealed class VarMode
{
  var read = false
  var write = false
  var reduction = false
  
  def hintName = {
    if (read && write)
      "inOut"
    else if (read)
      "in"
    else if (write)
      "out"
    else
      "var"
  }
}

abstract sealed class VarScope
case object LocalScope extends VarScope
case object GlobalScope extends VarScope
case object PrivateScope extends VarScope

abstract class AbstractVar extends Expr {
  var stale = false
  var kernel: CLKernel = null
  var queue: CLQueue = null
  var argIndex = -2
  var scope: VarScope = GlobalScope
  var name: String = null
  var mode = new VarMode

  def setup
  override def toString() = name
  override def accept(info: VisitInfo) : Unit = visit(info)

  def getTypeDesc[T](implicit t: Manifest[T], valueType: ValueType) : TypeDesc = getTypeDesc[T](t.erasure.asInstanceOf[Class[T]], valueType)
  def getTypeDesc[T](c: Class[T], valueType: ValueType) : TypeDesc = {
    var ch = {
      if (c.isPrimitive || c.isAnyOf(classOf[java.lang.Number], classOf[Number])) 1
      else if (classOf[Val1].isAssignableFrom(c)) 1
      else if (classOf[Val2].isAssignableFrom(c)) 2
      else if (classOf[Val4].isAssignableFrom(c)) 4
      else throw new IllegalArgumentException("Unable to guess channels for valueType " + valueType + " and class " + c)
    }
    var pt = {
      if (c.isAnyOf(classOf[Int], classOf[Int1], classOf[Int2], classOf[Int4])) IntType
      else if (c.isAnyOf(classOf[Double], classOf[Double1], classOf[Double2], classOf[Double4])) DoubleType
      else throw new IllegalArgumentException("Unable to guess primType for class " + c.getName + " and class " + c)
    }
    TypeDesc(ch, valueType, pt)
  }
}


class Var[T](implicit t: Manifest[T]) extends AbstractVar {
  private var value: Option[T] = None
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

class BytesVar(size: Int) extends ArrayVar[Byte, ByteBuffer](classOf[Byte], classOf[ByteBuffer], size) {
  def this() = this(-1)
  def this(dim: Dim) = this(dim.size)
  def get(index: Int) = this().get(index)
  def set(index: Int, v: Byte) = this().put(index, v)
}
class ShortsVar(size: Int) extends ArrayVar[Short, ShortBuffer](classOf[Short], classOf[ShortBuffer], size) {
  def this() = this(-1)
  def this(dim: Dim) = this(dim.size)
  def get(index: Int) = this().get(index)
  def set(index: Int, v: Short) = this().put(index, v)
}
class IntsVar(size: Int) extends ArrayVar[Int, IntBuffer](classOf[Int], classOf[IntBuffer], size) {
  def this() = this(-1)
  def this(dim: Dim) = this(dim.size)
  def get(index: Int) = this().get(index)
  def set(index: Int, v: Int) = this().put(index, v)
}
class LongsVar(size: Int) extends ArrayVar[Long, LongBuffer](classOf[Long], classOf[LongBuffer], size) {
  def this() = this(-1)
  def this(dim: Dim) = this(dim.size)
  def get(index: Int) = this().get(index)
  def set(index: Int, v: Long) = this().put(index, v)
}
class FloatsVar(size: Int) extends ArrayVar[Float, FloatBuffer](classOf[Float], classOf[FloatBuffer], size) {
  def this() = this(-1)
  def this(dim: Dim) = this(dim.size)
  def get(index: Int) = this().get(index)
  def set(index: Int, v: Float) = this().put(index, v)
}
class DoublesVar(size: Int) extends ArrayVar[Double, DoubleBuffer](classOf[Double], classOf[DoubleBuffer], size) {
  def this() = this(-1)
  def this(dim: Dim) = this(dim.size)
  def get(index: Int) = this().get(index)
  def set(index: Int, v: Double) = this().put(index, v)
}

class ArrayVar[V, B <: Buffer](v: Class[V], b: Class[B], var size: Int) extends AbstractVar {
  private var buffer: Option[B] = None
  def apply() : B = {
    if (buffer == None)
      buffer = Some(newBuffer[B](b)(size))
    if (stale)
      read(buffer.get)

    buffer.get
  }
  private var mem: CLMem = null
  var implicitDim: Option[Dim] = None

  def read(out: B) : Unit = mem.read(out, queue, true)
  def write(in: B) : Unit = mem.write(in, queue, true)

  def alloc(size: Int) = {
    this.size = size
    this
  }
  override def toString = implicitDim match {
    case Some(x) => name + "[" + x + "]"
    case None => name
  }
  override def setup = {
    val td = typeDesc
    if (size < 0) implicitDim match {
      case Some(d) => size = d.size
      case _ => throw new RuntimeException("Array variable was not allocated, and no implicit dimension can be safely inferred to help.")
    }
    val bytes = td.channels * td.primType.bytes * size
    val cx = kernel.getProgram.getContext
    mem = 
      if (mode.read && mode.write)
        cx.createInputOutput(bytes)
      else if (mode.read)
        cx.createInput(bytes)
      else if (mode.write)
        cx.createOutput(bytes)
      else
        throw new UnsupportedOperationException("Unsupported variable mode : " + this)

    kernel.setArg(argIndex, mem)
  }
  override def typeDesc = getTypeDesc[V](v, Parallel)
  
  def apply(index: Expr) : Expr = new ArrayElement[V, B](this, index)

  /*def apply(index: Int) : V = {
    var value = this.value getOrElse { throw new RuntimeException("Cannot get variable value before setting things up !")}
    return value.asInstanceOf[DoubleBuffer].get(index).asInstanceOf[T];
  }*/
}

class ArrayElement[T, B <: Buffer](/*implicit t: Manifest[T], */var array: ArrayVar[T, B], var index: Expr) extends Expr {
  override def typeDesc = {
    var td = array.typeDesc
    TypeDesc(td.channels, Scalar, td.primType)
  }
  override def toString() = array.name + "[" + index.toString + "]"
  override def accept(info: VisitInfo) : Unit = visit(info, array, index)
}

class ImageVar[T](var width: Int, var height: Int) extends AbstractVar {
  var implicitDimX: Option[Dim] = None
  var implicitDimY: Option[Dim] = None
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
  override def accept(info: VisitInfo) : Unit = visit(info, image, x, y)
}

