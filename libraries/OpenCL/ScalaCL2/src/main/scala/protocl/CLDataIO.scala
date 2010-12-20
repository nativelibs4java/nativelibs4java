/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package protocl
import org.bridj.Pointer
import org.bridj.PointerIO
import scala.math._

trait CLDataIO[T] {
  implicit val t: ClassManifest[T]
  val elementCount: Int
  val pointerIO: PointerIO[T]
  def elements: Seq[CLDataIO[Any]]
  def clType: String
  def createBuffers(length: Int)(implicit context: ScalaCLContext): Array[CLGuardedBuffer[Any]]

  def openCLKernelArgDeclarations(input: Boolean, offset: Int): Seq[String]
  def openCLKernelNthItemExprs(input: Boolean, offset: Int, n: String): Seq[(String, List[Int])]
  //def openCLIntermediateKernelNthItemExprs(input: Boolean, offset: Int, n: String): Seq[String]
  def openCLIntermediateKernelTupleElementsExprs(expr: String): Seq[(String, List[Int])]
  
  def openCLIthTupleElementNthItemExpr(input: Boolean, offset: Int, indexes: List[Int], n: String): String
  //def openCLTupleShuffleNthFieldExprs(input: Boolean, offset: Int, i: String, shuffleExpr: String): Seq[String]

  def extract(arrays: Array[CLGuardedBuffer[Any]], index: Int): CLFuture[T] = {
    assert(elementCount == arrays.length)
    extract(arrays, 0, index)
  }
  def store(v: T, arrays: Array[CLGuardedBuffer[Any]], index: Int): Unit = {
    assert(elementCount == arrays.length)
    store(v, arrays, 0, index)
  }
  def extract(pointers: Array[Pointer[Any]], index: Int): T = {
    assert(elementCount == pointers.length)
    extract(pointers, 0, index)
  }
  def store(v: T, pointers: Array[Pointer[Any]], index: Int): Unit = {
    assert(elementCount == pointers.length)
    store(v, pointers, 0, index)
  }

  private[protocl]
  def extract(arrays: Array[CLGuardedBuffer[Any]], offset: Int, index: Int): CLFuture[T]
  
  private[protocl]
  def store(v: T, arrays: Array[CLGuardedBuffer[Any]], offset: Int, index: Int): Unit
  
  private[protocl]
  def extract(pointers: Array[Pointer[Any]], offset: Int, index: Int): T
  
  private[protocl]
  def store(v: T, pointers: Array[Pointer[Any]], offset: Int, index: Int): Unit
  
  private[protocl]
  def exprs(arrayExpr: String): Seq[String]
  //def toArray(arrays: Array[CLGuardedBuffer[Any]], offset: Int): Array[T]

  def toArray(arrays: Array[CLGuardedBuffer[Any]]): Array[T] = toArray(arrays, null)
  def toArray(arrays: Array[CLGuardedBuffer[Any]], out: Array[T], start: Int = 0, length: Int = -1): Array[T] = {
    assert(elementCount == arrays.length)
    val pointers = arrays.map(_.toPointer)
    val size = pointers(0).getValidElements.toInt
    val actualOut = if (out == null) new Array[T](size) else out
    var i = start
    val sup = if (length < 0) size else min(size, start + length)
    while (i < sup) {
      actualOut(i.toInt) = extract(pointers, 0, i)
      i += 1
    }
    actualOut
  }
  
}

object CLTupleDataIO {
  lazy val builtInArities = Set(1, 2, 4, 8)
}
class CLTupleDataIO[T](ios: Array[CLDataIO[Any]], values: T => Array[Any], tuple: Array[Any] => T)(implicit override val t: ClassManifest[T]) extends CLDataIO[T] {

  override lazy val pointerIO: PointerIO[T] =
    error("Cannot create PointerIO for tuples !")

  override def openCLKernelArgDeclarations(input: Boolean, offset: Int): Seq[String] =
    iosAndOffsets.flatMap { case (io, ioOffset) => io.openCLKernelArgDeclarations(input, offset + ioOffset) }

  override def openCLKernelNthItemExprs(input: Boolean, offset: Int, n: String): Seq[(String, List[Int])] =
    iosAndOffsets.zipWithIndex.flatMap {
      case ((io, ioOffset), i) =>
        io.openCLKernelNthItemExprs(input, offset + ioOffset, n).map {
          case (s, indexes) => (s, i :: indexes)
        }
    }

  //override def openCLIntermediateKernelNthItemExprs(input: Boolean, offset: Int, n: String): Seq[String] =
  //  iosAndOffsets.flatMap { case (io, ioOffset) => io.openCLIntermediateKernelNthItemExprs(input, offset + ioOffset, n) }

  /*override def openCLTupleShuffleNthFieldExprs(input: Boolean, offset: Int, n: String, shuffleExpr: String): Seq[String] =
    if (!isOpenCLTuple)
      error("Type " + this + " is not an OpenCL tuple !")
    else
      shuffleExpr.view.map(_ match {
        case 'x' => 0
        case 'y' => 1
        case 'z' => 2
        case 'w' => 3
      }).flatMap(i => {
        val (io, ioOffset) = iosAndOffsets(i)
        io.openCLKernelNthItemExprs(input, offset + ioOffset, n).map(_._1)
      })
  */
  override def openCLIthTupleElementNthItemExpr(input: Boolean, offset: Int, indexes: List[Int], n: String): String = {
    val (io, ioOffset) = iosAndOffsets(indexes.head)
    io.openCLIthTupleElementNthItemExpr(input, offset + ioOffset, indexes.tail, n)
  }

  override def openCLIntermediateKernelTupleElementsExprs(expr: String): Seq[(String, List[Int])] = {
    ios.zipWithIndex.flatMap {
      case (io, i) =>
        io.openCLIntermediateKernelTupleElementsExprs(expr + "._" + (i + 1)).map {
          case (x, indexes) =>
            (x, i :: indexes)
        }
    }
  }

  
  override def elements: Seq[CLDataIO[Any]] =
    ios.flatMap(_.elements)

  lazy val types = ios.map(_.clType)
  lazy val uniqTypes = types.toSet

  lazy val isOpenCLTuple = {
    uniqTypes.size == 1 && 
    CLTupleDataIO.builtInArities.contains(ios.size) &&
    ios(0).isInstanceOf[CLValDataIO[_]]
  }
  override def clType = {
    if (isOpenCLTuple)
      uniqTypes.head + ios.size
    else
      "struct { " + types.reduceLeft(_ + "; " + _) + "; }"
  }

  override def createBuffers(length: Int)(implicit context: ScalaCLContext): Array[CLGuardedBuffer[Any]] =
    ios.flatMap(_.createBuffers(length))

  lazy val (iosAndOffsets, elementCount) = {
    var off = 0
    (
      ios.map(io => {
        val o = off
        off += io.elementCount
        (io, o)
      }),
      off
    )
  }
    
  override def extract(arrays: Array[CLGuardedBuffer[Any]], offset: Int, index: Int): CLFuture[T] =
    new CLTupleFuture(iosAndOffsets.map { case (io, ioOffset) => io.extract(arrays, offset + ioOffset, index) }, tuple)
  
  override def store(v: T, arrays: Array[CLGuardedBuffer[Any]], offset: Int, index: Int): Unit =
    iosAndOffsets.zip(values(v)).foreach { case ((io, ioOffset), vi) => io.store(vi, arrays, offset + ioOffset, index) }

  override def extract(pointers: Array[Pointer[Any]], offset: Int, index: Int): T =
    tuple(iosAndOffsets.map { case (io, ioOffset) => io.extract(pointers, offset + ioOffset, index) })

  override def store(v: T, pointers: Array[Pointer[Any]], offset: Int, index: Int): Unit =
    iosAndOffsets.zip(values(v)).foreach { case ((io, ioOffset), vi) => io.store(vi, pointers, offset + ioOffset, index) }

  override def exprs(arrayExpr: String): Seq[String] =
    ios.zipWithIndex.flatMap { case (io, i) => io.exprs(arrayExpr + "._" + (i + 1)) }

  override def toString = "(" + ios.map(_.toString).reduceLeft(_ + ", " + _) + ")"
}

class CLValDataIO[T <: AnyVal](implicit override val t: ClassManifest[T]) extends CLDataIO[T] {

  override val elementCount = 1
  
  override lazy val pointerIO: PointerIO[T] =
    PointerIO.getInstance(t.erasure)
  
  override def elements: Seq[CLDataIO[Any]] =
    Seq(this.asInstanceOf[CLDataIO[Any]])

  /*override def openCLTupleShuffleNthFieldExprs(input: Boolean, offset: Int, n: String, shuffleExpr: String): Seq[String] =
    error("Calling tuple shuffle field '" + shuffleExpr + "' on scalar type " + this)*/
  
  override def openCLKernelArgDeclarations(input: Boolean, offset: Int): Seq[String] =
    Seq("__global " + (if (input) " const " + clType + "* in" else clType + "* out") + offset)

  override def openCLKernelNthItemExprs(input: Boolean, offset: Int, n: String) =
    Seq(("(" + (if (input) "in" else "out") + offset + "[" + n + "])", List(0)))

  override def openCLIntermediateKernelTupleElementsExprs(expr: String): Seq[(String, List[Int])] = 
    Seq((expr, List(0)))

  override def openCLIthTupleElementNthItemExpr(input: Boolean, offset: Int, indexes: List[Int], n: String): String = {
    if (indexes != List(0))
        error("There is only one item in this array of " + this + " (trying to access item " + indexes + ")")
    openCLKernelNthItemExprs(input, offset, n)(0)._1
  }
  
  override def clType = t.erasure.getSimpleName.toLowerCase match {
    case "sizet" => "size_t"
    case "boolean" => "char"
    case "character" => "short"
    case n => n
  }

  override def toString = t.erasure.getSimpleName + " /* " + clType + "*/"

  override def createBuffers(length: Int)(implicit context: ScalaCLContext): Array[CLGuardedBuffer[Any]] =
    Array(new CLGuardedBuffer[T](length).asInstanceOf[CLGuardedBuffer[Any]])

  override def extract(arrays: Array[CLGuardedBuffer[Any]], offset: Int, index: Int): CLFuture[T] =
    arrays(offset).asInstanceOf[CLGuardedBuffer[T]](index)

  override def extract(pointers: Array[Pointer[Any]], offset: Int, index: Int): T =
    pointers(offset).asInstanceOf[Pointer[T]].get(index)

  override def store(v: T, pointers: Array[Pointer[Any]], offset: Int, index: Int): Unit =
    pointers(offset).asInstanceOf[Pointer[T]].set(index, v)

  override def store(v: T, arrays: Array[CLGuardedBuffer[Any]], offset: Int, index: Int): Unit =
    arrays(offset).asInstanceOf[CLGuardedBuffer[T]](index) = v

  override def exprs(arrayExpr: String): Seq[String] =
    Seq(arrayExpr)

  override def toArray(arrays: Array[CLGuardedBuffer[Any]]): Array[T] = {
    assert(elementCount == arrays.length)
    arrays(0).asInstanceOf[CLGuardedBuffer[T]].toArray
  }
}



class CLIntRangeDataIO(implicit val t: ClassManifest[Int]) extends CLDataIO[Int] {

  override val elementCount = 1

  override lazy val pointerIO: PointerIO[Int] =
    PointerIO.getInstance(t.erasure)

  override def elements: Seq[CLDataIO[Any]] =
    Seq(this.asInstanceOf[CLDataIO[Any]])

  override def openCLKernelArgDeclarations(input: Boolean, offset: Int): Seq[String] = {
    assert(input)
    Seq("int rangeLow" + offset)
  }

  override def openCLKernelNthItemExprs(input: Boolean, offset: Int, n: String) =
    Seq(("(rangeLow" + offset + " + " + n + ")", List(0)))

  override def openCLIntermediateKernelTupleElementsExprs(expr: String): Seq[(String, List[Int])] =
    Seq((expr, List(0))) // TODO ?

  override def openCLIthTupleElementNthItemExpr(input: Boolean, offset: Int, indexes: List[Int], n: String): String = {
    if (indexes != List(0))
        error("There is only one item in this array of " + this + " (trying to access item " + indexes + ")")
    openCLKernelNthItemExprs(input, offset, n)(0)._1
  }

  override def clType = "int"

  override def toString = "int range"

  override def createBuffers(length: Int)(implicit context: ScalaCLContext): Array[CLGuardedBuffer[Any]] =
    Array(new CLGuardedBuffer[Int](2).asInstanceOf[CLGuardedBuffer[Any]])

  override def extract(arrays: Array[CLGuardedBuffer[Any]], offset: Int, index: Int): CLFuture[Int] = {
    val arr = arrays(offset).asInstanceOf[CLGuardedBuffer[Int]]
    val range = CLIntRange.toRange(arr)
    new CLInstantFuture[Int](range.start + range.step * index.toInt)
  }

  override def extract(pointers: Array[Pointer[Any]], offset: Int, index: Int): Int =
    pointers(offset).asInstanceOf[Pointer[Int]].get(0) + index.toInt

  override def store(v: Int, pointers: Array[Pointer[Any]], offset: Int, index: Int): Unit =
    error("Int ranges are immutable !")

  override def store(v: Int, arrays: Array[CLGuardedBuffer[Any]], offset: Int, index: Int): Unit =
    error("Int ranges are immutable !")

  override def exprs(arrayExpr: String): Seq[String] =
    Seq(arrayExpr)

  override def toArray(arrays: Array[CLGuardedBuffer[Any]]): Array[Int] = {
    assert(elementCount == arrays.length)
    val Array(low, length, by) = arrays(0).asInstanceOf[CLGuardedBuffer[Int]].toArray
    (low.toInt until length.toInt).toArray
  }
}