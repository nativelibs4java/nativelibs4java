/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package scalaapplication1

import java.nio._
import Functions._

import com.nativelibs4java.opencl.OpenCL4Java._
import SyntaxUtils._
import NIOUtils._

import java.nio._

import scala.reflect.Manifest
import SyntaxUtils._


object Functions {
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

trait Val1
trait Val2
trait Val4




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

object SyntaxUtils {


  def implode(elts: List[String], sep: String) = {
    if (elts == Nil) ""
    else elts reduceLeft { _ + sep + _ } //map { _.toString }

  }

  implicit def class2ClassUtils(c: Class[_]) = ClassUtils(c)
  case class ClassUtils(var target: Class[_]) {
    def isBuffer() = classOf[Buffer] isAssignableFrom target
    def isAnyOf(matches: Class[_]*) : Boolean = {
      for (m <- matches.elements)
      if (m isAssignableFrom target)
      return true
      return false
    }
  }

}

object NIOUtils {
  implicit def array2Buffer(a: Array[Int   ]) = IntBuffer.wrap(a)
  implicit def array2Buffer(a: Array[Short ]) = ShortBuffer.wrap(a)
  implicit def array2Buffer(a: Array[Long  ]) = LongBuffer.wrap(a)
  implicit def array2Buffer(a: Array[Byte  ]) = ByteBuffer.wrap(a)
  implicit def array2Buffer(a: Array[Char  ]) = CharBuffer.wrap(a)
  implicit def array2Buffer(a: Array[Double]) = DoubleBuffer.wrap(a)
  implicit def array2Buffer(a: Array[Float ]) = FloatBuffer.wrap(a)

  def directInts(n: Int) = ByteBuffer.allocateDirect(n * 4).asIntBuffer()
  def directShorts(n: Int) = ByteBuffer.allocateDirect(n * 2).asShortBuffer()
  def directLongs(n: Int) = ByteBuffer.allocateDirect(n * 8).asLongBuffer()
  def directBytes(n: Int) = ByteBuffer.allocateDirect(n)
  def directChars(n: Int) = ByteBuffer.allocateDirect(n * 2).asCharBuffer()
  def directDoubles(n: Int) = ByteBuffer.allocateDirect(n * 8).asDoubleBuffer()
  def directFloats(n: Int) = ByteBuffer.allocateDirect(n * 4).asFloatBuffer()

}

class Context(var clContext: CLContext)
object Context {
  def GPU = new Context(CLContext.createContext(CLDevice.listGPUDevices()));
  def CPU = new Context(CLContext.createContext(CLDevice.listCPUDevices()));
  def BEST =
  try {
    GPU
  } catch {
    case _ => CPU
  }
}

abstract class Program(context: Context) {
  var root: Expr

  var source: String = null;

  private def generateSources(variables: List[AbstractVar]) : String = {

      var doc = new StringBuilder;

      doc ++ implode(root.includes.map { "#include <" + _ + ">" }, "\n")
      doc ++ "\n"
      var argDefs = variables.map { v =>
        "__global " +
        (if (v.mode == WriteMode || v.mode == AggregatedMode) "" else "const ") +
        v.typeDesc.globalCType + " " + v.name
      }

      doc ++ ("void function(" + implode(argDefs, ", ") + ")\n");
      doc ++ "{\n\t"
      //doc ++ "int gid = get_global_id(0);\n\t"
      doc ++ root.toString
      doc ++ "\n}\n"

      doc.toString
  }
  def ! = {
	  setup
  }
  def setup = {
    if (source == null) {
      val variables = root.variables;

      (variables zipWithIndex) foreach { case (v, i) => {
          v.argIndex = i;
          v.name = "var" + i;
          //if (v.variable.typeDesc.valueType == Parallel)
          //  v.parallelIndexName = "gid"
      } }

      root accept { (x, stack) => x match {
          case v: AbstractVar => v.mode = v.mode union ReadMode
            //			  case BinOp(""".*~""", v: Var[_], _) => v.isWritten = true
          case BinOp("=", v: AbstractVar, _) => v.mode = v.mode union WriteMode
          case _ =>
        } }

      variables filter { _.mode == AggregatedMode } foreach {
          throw new UnsupportedOperationException("Reductions not implemented yet !")
      }

      source = generateSources(variables)

      var kernel = context.clContext.createProgram(source).build().createKernel("function");

      variables foreach { v =>
          v.kernel = kernel
          v.setup
      }
    }
    println(source)
  }
}


import java.nio._
import scala.collection.mutable.Stack
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

abstract case class PrimScal(v: Number, t: PrimType) extends TypedExpr(TypeDesc(1, Scalar, t)) with CLValue {
  override def toString = v.toString
}

case class Dim(size: Int) extends PrimScal(size, IntType) with Val1


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




import scala.reflect.Manifest



/// fwd declare
class CLEvent

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
    var ch = {
      if (valueType.isInstanceOf[Val1]) 1
      else if (valueType.isInstanceOf[Val2]) 2
      else if (valueType.isInstanceOf[Val4]) 4
      else throw new IllegalArgumentException("Unable to guess channels for valueType " + valueType)
    }
    var c = t.erasure
    var pt = {
      if (c.isAnyOf(classOf[Int1], classOf[Int2], classOf[Int4])) IntType
      else if (c.isAnyOf(classOf[Double1], classOf[Double2], classOf[Double4])) DoubleType
      else throw new IllegalArgumentException("Unable to guess primType for class " + c.getName)
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
        0
      else if (c == classOf[Double])
        0.0
      else if (c == classOf[Float])
        0.0f
      else if (c == classOf[Long])
        0l
      else if (c == classOf[Short])
        0.asInstanceOf[Short]
      else
        c.newInstance()
    ).asInstanceOf[K]
  }
  override def setup = {
    var value = if (this.value == None) defaultValue[T] else this.value
    kernel.setObjectArg(argIndex, value)
  }
}
import java.nio._

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
  override def toString() = array + "[" + index + "]"
  override def accept(visitor: (Expr, Stack[Expr]) => Unit, stack: Stack[Expr]) : Unit = {
    stack push this
    visitor(array, stack)
    visitor(index, stack)
    stack pop;
    visitor(this, stack)
  }

}

import java.awt.Image
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


object ScalaCL {
//  implicit def Int2Dim(v: Int1) = Dim(v)
  implicit def Int2Int1(v: Int) = Int1(v)
  implicit def Int12Int(v: Int1) = v.value
  implicit def Double2Double1(v: Double) = Double1(v)
  implicit def Double12Double(v: Double1) = v.value
}


object Main {
    import ScalaCL._
    class MyProg(i: Dim) extends Program(Context.BEST) {

      val a = new ArrayVar[Double]
      val b = new ArrayVar[Double]
      val o = new ArrayVar[Double]
      val globalTot = new Var[Double4]
      val localTot = new Var[Double4]
      val x = new Var[Int]
      val img = new ImageVar

      override var root: Expr = o(i) ~ a(i) * sin(b(i)) + 1
      //(
                    //localTot ~ 0,
                    //For(j, 0, j.size,
                    //	localTot +~ img(i, j)
                    //)
                    //globalTot +~ localTot
            //)
    }

    def main(args: Array[String]) : Unit = {
        var prog = new MyProg(Dim(10000))
	    //prog.a.value.set(1, 0)
	    prog !;
	    prog.source

    }
}



