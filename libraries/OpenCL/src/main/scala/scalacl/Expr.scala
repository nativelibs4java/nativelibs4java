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
  
  def visit[T <: Node](info: VisitInfo, children: T*) = {
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

case class Statements(stats: Seq[Stat]) extends Stat {
  override def toString = stats.implode("\n\t")
  override def accept(info: VisitInfo): Unit = visit(info, stats: _*)
}
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

  private def dieMinMax2(op: String): (Double, Double) => Double = throw new RuntimeException("Binary operator " + op + " does not define a minMax function")

  def :=(other: Expr) = Assignment("=", this, other)
  def +=(other: Expr) = Assignment("+=", this, other)
  def -=(other: Expr) = Assignment("-=", this, other)
  def *=(other: Expr) = Assignment("*=", this, other)
  def /=(other: Expr) = Assignment("/=", this, other)


  def !=(other: Expr) = Assignment("!=", this, other)
  def ==(other: Expr) = BinOp("==", this, other, dieMinMax2("=="))
  def <=(other: Expr) = BinOp("<=", this, other, dieMinMax2("<="))
  def >=(other: Expr) = BinOp(">=", this, other, dieMinMax2(">="))
  def <(other: Expr) = BinOp("<", this, other, dieMinMax2("<"))
  def >(other: Expr) = BinOp(">", this, other, dieMinMax2(">"))

  def !() = UnOp("!", true, this, (x: Double) => if (x == 0) 1 else 0)

  def +(other: Expr) = BinOp("+", this, other, _ + _)
  def -(other: Expr) = BinOp("-", this, other, _ - _)
  def *(other: Expr) = BinOp("*", this, other, _ * _)
  def /(other: Expr) = BinOp("/", this, other, _ / _)
  def %(other: Expr) = BinOp("%", this, other, _ % _)
  def >>(other: Expr) = BinOp(">>", this, other, _.asInstanceOf[Int] >> _.asInstanceOf[Int])
  def <<(other: Expr) = BinOp("<<", this, other, _.asInstanceOf[Int] << _.asInstanceOf[Int])

  def computeMinMax: MinMax
}
object Expr {
  def computeMinMax(mm1: MinMax, mm2: MinMax, f: (Double, Double) => Double): MinMax = {
    var res = new scala.collection.jcl.TreeSet[Double]
    res + f(mm1.min, mm2.min)
    res + f(mm1.min, mm2.max)
    res + f(mm1.max, mm2.min)
    res + f(mm1.max, mm2.max)
    var arr = res.toArray
    MinMax(arr(0), arr(arr.length - 1))
  }
  def computeMinMax(mm: MinMax, f: (Double) => Double): MinMax = {
    val v1 = f(mm.min)
    val v2 = f(mm.max)
    if (v1 < v2)
      MinMax(v1, v2)
    else
      MinMax(v2, v1)
  }
}
case class MinMax(min: Double, max: Double) {
  def union(mm: MinMax) = MinMax(Math.min(min, mm.min), Math.max(max, mm.max))
}

abstract class TypedExpr(td: TypeDesc) extends Expr {
  override def typeDesc = td
  override def accept(info: VisitInfo) : Unit = visit(info);
  override def computeMinMax: MinMax = throw new UnsupportedOperationException("Can't infer min max values from typed expression")

}

abstract case class PrimScal(v: Number, t: PrimType) extends TypedExpr(TypeDesc(1, Scalar, t)) with CLValue {
  override def toString = v.toString
  override def computeMinMax: MinMax = {
    val x = v.doubleValue
    MinMax(x, x)
  }

}

class Dim(var size: Int) extends PrimScal(size, IntType) with Val1 {
  var name: String = null
  var dimIndex = -1
  override def toString = if (name == null) "unnamedDim(" + size + ")" else name
  override def computeMinMax: MinMax = {
    if (size < 0)
      throw new RuntimeException("Unable to compute minmax with a dim size < 0")
    MinMax(0, size - 1)
  }
}
object Dim {
  def apply(size: Int) = new Dim(size)
}

case class BinOp(op: String, first: Expr, second: Expr, minMaxFunction: (Double, Double) => Double) extends Expr {
  override def toString() = "(" + first + ") " + op + " (" + second + ")"
  override def typeDesc = first.typeDesc combineWith second.typeDesc
  override def accept(info: VisitInfo): Unit = visit(info, first, second)
  override def computeMinMax: MinMax = Expr.computeMinMax(first.computeMinMax, second.computeMinMax, minMaxFunction)
}
case class UnOp(op: String, isPrefix: Boolean, value: Expr, minMaxFunction: (Double) => Double) extends Expr {
  override def toString() = {
    if (isPrefix) "(" + op + " " + value + ")"
    else "(" + value.toString() + " " + op + ")"
  }
  override def typeDesc = value.typeDesc
  override def accept(info: VisitInfo): Unit = visit(info, value)
  override def computeMinMax: MinMax = {
    val mm = value.computeMinMax
    val v1 = minMaxFunction(mm.min)
    val v2 = minMaxFunction(mm.max)
    if (v1 < v2)
      MinMax(v1, v2)
    else
      MinMax(v2, v1)
  }
}

abstract case class Fun(name: String, outType: PrimType, args: Expr*) extends Expr {
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

case class Fun1(nam: String, outTyp: PrimType, arg1: Expr, f: (Double) => Double) extends Fun(nam, outTyp, arg1) {
  override def computeMinMax: MinMax = Expr.computeMinMax(arg1.computeMinMax, f)
}
case class Fun2(nam: String, outTyp: PrimType, arg1: Expr, arg2: Expr, f: (Double, Double) => Double) extends Fun(nam, outTyp, arg1, arg2) {
  override def computeMinMax: MinMax = Expr.computeMinMax(arg1.computeMinMax, arg2.computeMinMax, f)
}
case class Fun3(nam: String, outTyp: PrimType, arg1: Expr, arg2: Expr, arg3: Expr, f: (Double, Double, Double) => Double) extends Fun(nam, outTyp, arg1, arg2, arg3) {
  override def computeMinMax: MinMax = throw new RuntimeException("Not implemented")
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

case class Long1(value: Long) extends PrimScal(value, LongType) with Val1
case class Long2(x: Long, y: Long) extends TypedExpr(TypeDesc(2, Scalar, LongType)) with CLValue with Val2
case class Long4(x: Long, y: Long, z: Long, w: Long) extends TypedExpr(TypeDesc(4, Scalar, LongType)) with CLValue  with Val4 {
  def this(xy: Long2, zw: Long2) = {
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

  //def setup
  def realloc
  def alloc
  def bind
  override def toString() = name
  override def accept(info: VisitInfo) : Unit = visit(info)

  override def computeMinMax: MinMax = throw new UnsupportedOperationException("Can't infer min max values from variables")
  def local = {
    scope = LocalScope
    this
  }
  def priv = {
    scope = LocalScope
    this
  }
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
      else if (c.isAnyOf(classOf[Float], classOf[Float1], classOf[Float2], classOf[Float4])) FloatType
      else if (c.isAnyOf(classOf[Long], classOf[Long1], classOf[Long2], classOf[Long4])) LongType
      else if (c.isAnyOf(classOf[Short], classOf[Short1], classOf[Short2], classOf[Short4])) ShortType
      else if (c.isAnyOf(classOf[Byte], classOf[Byte1], classOf[Byte2], classOf[Byte4])) ByteType
      else throw new IllegalArgumentException("Unable to guess primType for class " + c.getName + " and class " + c)
    }
    TypeDesc(ch, valueType, pt)
  }
}


class FloatVar extends Var[Float](classOf[Float])
class ByteVar extends Var[Byte  ](classOf[Byte  ])
class ShortVar extends Var[Short ](classOf[Short ])
class IntVar extends Var[Int   ](classOf[Int   ])
class LongVar extends Var[Long  ](classOf[Long  ])
class DoubleVar extends Var[Double](classOf[Double])

class Var[T](t: Class[T]) extends AbstractVar {
  private var value: Option[T] = None
  def apply() : T = {
    value getOrElse { throw new RuntimeException("Cannot get variable value before setting things up !")}
  }
  override def typeDesc = getTypeDesc[T](t, Scalar)

  def defaultValue[K](implicit k: Manifest[K]): K = defaultValue[K](k.erasure.asInstanceOf[Class[K]])

  def defaultValue[K](c: Class[K]): K = {
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
  override def bind = {
    var value = if (this.value == None) defaultValue(t) else this.value
    kernel.setObjectArg(argIndex, value)
  }
  override def realloc = {}
  override def alloc = {}
}

class BytesVar(size: Int) extends ArrayVar[Byte, ByteBuffer](classOf[Byte], classOf[ByteBuffer], size) {
  def this() = this(-1)
  def this(sizeExpr: Expr) = {
    this(-1)
    this.sizeExpr = Some(sizeExpr)
  }
  def get(index: Int) = this().get(index)
  def set(index: Int, v: Byte) = this().put(index, v)
}
class ShortsVar(size: Int) extends ArrayVar[Short, ShortBuffer](classOf[Short], classOf[ShortBuffer], size) {
  def this() = this(-1)
  def this(sizeExpr: Expr) = {
    this(-1)
    this.sizeExpr = Some(sizeExpr)
  }
  def get(index: Int) = this().get(index)
  def set(index: Int, v: Short) = this().put(index, v)
}
class IntsVar(size: Int) extends ArrayVar[Int, IntBuffer](classOf[Int], classOf[IntBuffer], size) {
  def this() = this(-1)
  def this(sizeExpr: Expr) = {
    this(-1)
    this.sizeExpr = Some(sizeExpr)
  }
  def get(index: Int) = this().get(index)
  def set(index: Int, v: Int) = this().put(index, v)
}
class LongsVar(size: Int) extends ArrayVar[Long, LongBuffer](classOf[Long], classOf[LongBuffer], size) {
  def this() = this(-1)
  def this(sizeExpr: Expr) = {
    this(-1)
    this.sizeExpr = Some(sizeExpr)
  }
  def get(index: Int) = this().get(index)
  def set(index: Int, v: Long) = this().put(index, v)
}
class FloatsVar(size: Int) extends ArrayVar[Float, FloatBuffer](classOf[Float], classOf[FloatBuffer], size) {
  def this() = this(-1)
  def this(sizeExpr: Expr) = {
    this(-1)
    this.sizeExpr = Some(sizeExpr)
  }
  def get(index: Int) = this().get(index)
  def set(index: Int, v: Float) = this().put(index, v)
}
class DoublesVar(size: Int) extends ArrayVar[Double, DoubleBuffer](classOf[Double], classOf[DoubleBuffer], size) {
  def this() = this(-1)
  def this(sizeExpr: Expr) = {
    this(-1)
    this.sizeExpr = Some(sizeExpr)
  }
  def get(index: Int) = this().get(index)
  def set(index: Int, v: Double) = this().put(index, v)
}

class ArrayVar[V, B <: Buffer](v: Class[V], b: Class[B], var size: Int) extends AbstractVar {
  def this(v: Class[V], b: Class[B], sizeExpr: Expr) = {
    this(v, b, -1)
    this.sizeExpr = Some(sizeExpr)
  }
  var sizeExpr: Option[Expr] = None
  private var buffer: Option[B] = None
  var indexUsages = new scala.collection.mutable.ListBuffer[Expr]
  def apply() : B = {
    if (buffer == None)
      buffer = Some(newBuffer[B](b)(size))
    if (stale)
      read(buffer.get)

    buffer.get
  }
  private var mem: CLMem = null
  var implicitDim: Option[Dim] = None

  private def checkAlloc = if (mem == null) {
    if (size > 0)
      alloc
    else
      throw new IllegalThreadStateException("Array variable was not allocated. Please call alloc on it or on its program before writing to or reading from it")
  }
  def read(out: B) : Unit = {
    checkAlloc
    mem.read(out, queue, true)
  }
  def write(in: B) : Unit = {
    checkAlloc
    mem.write(in, queue, true)
  }

  override def toString = implicitDim match {
    case Some(x) => name + "[" + x + "]"
    case None => name
  }
  override def realloc = {
    mem = null
    alloc
  }
  def inferSize = {
    var exprs = sizeExpr match {
      case Some(x) => List(x)
      case None => indexUsages
    }
    val usagesMinMax = exprs.map { iu => try { Some(iu.computeMinMax) } catch { case x => None } }.filter (_ != None).map(_.get)
    if (usagesMinMax.length > 0) {
      val mm = usagesMinMax.reduceLeft(_ union _);
      if (mm.min < 0)
        throw new RuntimeException("Inferred weird array usage for array variable '" + name + "' (" + mm + "). Please allocate it explicitely in its constructor.")
      else {
        size = (Math.ceil(mm.max) + 1).asInstanceOf[Int];
        println("Info: Inferred size of array '" + name + "' : " + size)
      }
    }
    if (size < 0)
      implicitDim match {
        case Some(d) => size = d.size
        case _ => throw new RuntimeException("Array variable was not allocated, and no implicit dimension can be safely inferred to help.")
      }
  }
  override def alloc = if (mem == null)
  {
    val td = typeDesc
    if (size < 0)
      inferSize

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
  }
  override def bind = {
    alloc
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
  override def computeMinMax: MinMax = throw new UnsupportedOperationException("Can't infer min max values from array elements")
  override def toString() = array.name + "[" + index.toString + "]"
  override def accept(info: VisitInfo) : Unit = visit(info, array, index)
}

class ImageVar[T](var width: Int, var height: Int) extends AbstractVar {
  var implicitDimX: Option[Dim] = None
  var implicitDimY: Option[Dim] = None
  def die = throw new UnsupportedOperationException("IMPLEMENT ME: ImageVar.setup")

  override def alloc = die
  override def realloc = die
  override def bind = die
  override def typeDesc : TypeDesc = throw new UnsupportedOperationException("IMPLEMENT ME: ImageVar.typeDesc")
  def apply(x: Expr, y: Expr) = new Pixel[T](this, x, y)
}
class Pixel[T](/*implicit t: Manifest[T], */var image: ImageVar[T], var x: Expr, var y: Expr) extends Expr {
  override def typeDesc = {
    var td = image.typeDesc
    TypeDesc(td.channels, Scalar, td.primType)
  }
  override def computeMinMax: MinMax = throw new UnsupportedOperationException("Can't infer min max values from pixel accesses")
  override def toString() = image + "[" + x + ", " + y + "]"
  override def accept(info: VisitInfo) : Unit = visit(info, image, x, y)
}

