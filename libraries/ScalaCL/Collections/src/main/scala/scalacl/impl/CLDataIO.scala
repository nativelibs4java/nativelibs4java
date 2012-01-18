package scalacl

package impl

import scalacl._

import scala.collection.generic.CanBuildFrom
import com.nativelibs4java.opencl._
import com.nativelibs4java.opencl.util._

import org.bridj.Pointer
import org.bridj.PointerIO
import scala.math._

object CLDataIO {
  abstract sealed class ArgType
  object Value extends ArgType
  object InputPointer extends ArgType
  object OutputPointer extends ArgType
}

trait CLDataIO[T] {
  implicit val t: ClassManifest[T]
  val elementCount: Int
  val pointerIO: PointerIO[T]
  def elementSize: Int
  def elements: Seq[CLDataIO[Any]]
  def clType: String

  lazy val clearSource = new CLSimpleCode("""
    __kernel void clearKernel(int size, __global """ + clType + """* buffer) {
      int i = get_global_id(0);
      if (i >= size)
        return;

      buffer[i] = (""" + clType + """)0;
    }
  """)
  def clear(buffer: CLBuffer[T], evts: CLEvent*)(implicit context: Context): CLEvent = {
    val kernel = clearSource.getKernel(context)
    kernel.synchronized {
      val size = buffer.getElementCount.toInt
      kernel.setArgs(size, buffer)
      kernel.enqueueNDRange(context.queue, Array(size), evts:_*)
    }
  }
  def reductionType: (OpenCLType, Int) = error("Not a reductible type : " + this)
  
  def createBuffers(length: Int)(implicit context: Context): Array[CLGuardedBuffer[Any]]

  def openCLKernelArgDeclarations(nameBasis: String, argType: CLDataIO.ArgType, offset: Int): Seq[String]
  def openCLKernelNthItemExprs(nameBasis: String, argType: CLDataIO.ArgType, offset: Int, n: String): Seq[(String, List[Int])]
  def openCLIntermediateKernelTupleElementsExprs(expr: String): Seq[(String, List[Int])]
  
  def openCLIthTupleElementNthItemExpr(nameBasis: String, argType: CLDataIO.ArgType, offset: Int, indexes: List[Int], n: String): String
  
  def extract(arrays: Array[Array[Any]], index: Int): T = {
    assert(elementCount == arrays.length)
    extract(arrays, 0, index)
  }
  def extract(arrays: Array[CLGuardedBuffer[Any]], index: Int): CLFuture[T] = {
    assert(elementCount == arrays.length)
    extract(arrays, 0, index)
    //CLInstantFuture(extract(arrays.map(_.toArray.asInstanceOf[Array[Any]]), index))
  }
  def store(v: T, arrays: Array[CLGuardedBuffer[Any]], index: Int): Unit = {
    assert(elementCount == arrays.length)
    store(v, arrays, 0, index)
  }
  def extract(pointers: Array[Pointer[Any]], index: Int): T = {
    //extract(pointers.map(_.toArray.asInstanceOf[Array[Any]]), index)
    assert(elementCount == pointers.length)
    extract(pointers, 0, index)
  }
  def store(v: T, pointers: Array[Pointer[Any]], index: Int): Unit = {
    assert(elementCount == pointers.length)
    store(v, pointers, 0, index)
  }

  def extract(arrays: Array[CLGuardedBuffer[Any]], offset: Int, index: Int): CLFuture[T]
  
  def store(v: T, arrays: Array[CLGuardedBuffer[Any]], offset: Int, index: Int): Unit

  def extract(pointers: Array[Pointer[Any]], offset: Int, index: Int): T
  def extract(arrays: Array[Array[Any]], offset: Int, index: Int): T
  
  def store(v: T, pointers: Array[Pointer[Any]], offset: Int, index: Int): Unit
  
  def exprs(arrayExpr: String): Seq[String]

  def toArray(arrays: Array[CLGuardedBuffer[Any]]): Array[T] = {
    val size = arrays.head.buffer.getElementCount.toInt
    if (arrays.length == 1) {
      arrays.head.withReadablePointer(p => p.toArray.asInstanceOf[Array[T]]) 
    } else {
      val out = new Array[T](size)
      copyToArray(arrays, out, 0, size)
      out
    }
  }
  def copyToArray[B >: T](arrays: Array[CLGuardedBuffer[Any]], out: Array[B], start: Int = 0, length: Int = -1) = {
    assert(elementCount == arrays.length)
    val pointers = arrays.map(_.toPointer)
    val size = pointers(0).getValidElements.toInt
    var i = start
    val sup = if (length < 0) size else min(size, start + length)
    while (i < sup) {
      out(i) = extract(pointers, i)
      i += 1
    }
  }
  
}

object CLTupleDataIO {
  val builtInArities = Set(1, 2, 4, 8)
}
class CLTupleDataIO[T](ios: Array[CLDataIO[Any]], values: T => Array[Any], tuple: Array[Any] => T)(implicit override val t: ClassManifest[T]) extends CLDataIO[T] {

  assert(!ios.isEmpty)
    
  override lazy val pointerIO: PointerIO[T] =
    error("Cannot create PointerIO for tuples !")

  override def openCLKernelArgDeclarations(nameBasis: String, argType: CLDataIO.ArgType, offset: Int): Seq[String] =
    iosAndOffsets.flatMap { case (io, ioOffset) => io.openCLKernelArgDeclarations(nameBasis, argType, offset + ioOffset) }

  override def openCLKernelNthItemExprs(nameBasis: String, argType: CLDataIO.ArgType, offset: Int, n: String): Seq[(String, List[Int])] =
    iosAndOffsets.zipWithIndex.flatMap {
      case ((io, ioOffset), i) =>
        io.openCLKernelNthItemExprs(nameBasis, argType, offset + ioOffset, n).map {
          case (s, indexes) => (s, i :: indexes)
        }
    }

  //override def openCLIntermediateKernelNthItemExprs(argType: CLDataIO.ArgType, offset: Int, n: String): Seq[String] =
  //  iosAndOffsets.flatMap { case (io, ioOffset) => io.openCLIntermediateKernelNthItemExprs(argType, offset + ioOffset, n) }

  /*override def openCLTupleShuffleNthFieldExprs(argType: CLDataIO.ArgType, offset: Int, n: String, shuffleExpr: String): Seq[String] =
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
        io.openCLKernelNthItemExprs(argType, offset + ioOffset, n).map(_._1)
      })
  */
  override def openCLIthTupleElementNthItemExpr(nameBasis: String, argType: CLDataIO.ArgType, offset: Int, indexes: List[Int], n: String): String = {
    val (io, ioOffset) = iosAndOffsets(indexes.head)
    io.openCLIthTupleElementNthItemExpr(nameBasis, argType, offset + ioOffset, indexes.tail, n)
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

  override def elementSize = ios.map(_.elementSize).sum
  override def elements: Seq[CLDataIO[Any]] =
    ios.flatMap(_.elements)

  val types = ios.map(_.clType)
  val uniqTypes = types.toSet

  val isOpenCLTuple = {
    uniqTypes.size == 1 && 
    CLTupleDataIO.builtInArities.contains(ios.size) &&
    ios.head.isInstanceOf[CLValDataIO[_]]
  }
  override def clType = {
    if (isOpenCLTuple)
      uniqTypes.head + ios.size
    else
      "struct { " + types.reduceLeft(_ + "; " + _) + "; }"
  }
  
  override def reductionType = if (isOpenCLTuple)
    (ios.head.reductionType._1, ios.size)
  else
    super.reductionType

  override def createBuffers(length: Int)(implicit context: Context): Array[CLGuardedBuffer[Any]] =
    ios.flatMap(_.createBuffers(length))

  val (iosAndOffsets, elementCount) = {
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
    new CLTupleFuture(
      iosAndOffsets.map(p => {
        val (io, ioOffset) = p
        io.extract(arrays, offset + ioOffset, index) 
      }), 
      tuple
    )
  
  override def store(v: T, arrays: Array[CLGuardedBuffer[Any]], offset: Int, index: Int): Unit =
    iosAndOffsets.zip(values(v)).foreach { case ((io, ioOffset), vi) => io.store(vi, arrays, offset + ioOffset, index) }

  override def extract(pointers: Array[Pointer[Any]], offset: Int, index: Int): T = {
    var i = 0
    val length = iosAndOffsets.length
    val data = new Array[Any](length)
    
    while (i < length) {
      val (io, ioOffset) = iosAndOffsets(i)
      data(i) = io.extract(pointers, offset + ioOffset, index)
      i += 1
    }
    tuple(data)
    /*
    tuple(iosAndOffsets.map(p => {
      val (io, ioOffset) = p
      io.extract(pointers, offset + ioOffset, index) 
    }))
    */
  }
  
  override def extract(arrays: Array[Array[Any]], offset: Int, index: Int): T = {
    var i = 0
    val length = iosAndOffsets.length
    val data = new Array[Any](length)
    
    while (i < length) {
      val (io, ioOffset) = iosAndOffsets(i)
      data(i) = io.extract(arrays, offset + ioOffset, index)
      i += 1
    }
    tuple(data)
  }

  override def store(v: T, pointers: Array[Pointer[Any]], offset: Int, index: Int): Unit = {
    var i = 0
    val length = iosAndOffsets.length
    val vals = values(v)
    while (i < length) {
      val (io, ioOffset) = iosAndOffsets(i)
      val vi = vals(i)
      io.store(vi, pointers, offset + ioOffset, index)
      i += 1
    }
    //iosAndOffsets.zip(values(v)).foreach { case ((io, ioOffset), vi) => io.store(vi, pointers, offset + ioOffset, index) }
  }

  override def exprs(arrayExpr: String): Seq[String] =
    ios.zipWithIndex.flatMap { case (io, i) => io.exprs(arrayExpr + "._" + (i + 1)) }

  override def toString =
    "(" + ios.mkString(", ") + ")"
}

abstract class CLValDataIO[T <: AnyVal](implicit override val t: ClassManifest[T]) extends CLDataIO[T] {

  override val elementCount = 1
  
  override val pointerIO: PointerIO[T] =
    PointerIO.getInstance(t.erasure)
  
  override def elementSize = pointerIO.getTargetSize.toInt
  override def elements: Seq[CLDataIO[Any]] =
    Seq(this.asInstanceOf[CLDataIO[Any]])

  /*override def openCLTupleShuffleNthFieldExprs(argType: CLDataIO.ArgType, offset: Int, n: String, shuffleExpr: String): Seq[String] =
    error("Calling tuple shuffle field '" + shuffleExpr + "' on scalar type " + this)*/
  
  override def openCLKernelArgDeclarations(nameBasis: String, argType: CLDataIO.ArgType, offset: Int): Seq[String] = {
    Seq(
      (
        argType match {
          case CLDataIO.InputPointer =>
            "__global const " + clType + "* " + nameBasis
          case CLDataIO.OutputPointer =>
            "__global " + clType + "* " + nameBasis
          case CLDataIO.Value =>
            "const " + clType + " " + nameBasis
        }
      ) + offset
    )
  }

  override def openCLKernelNthItemExprs(nameBasis: String, argType: CLDataIO.ArgType, offset: Int, n: String) =
    Seq(
      (
        argType match {
          case CLDataIO.Value =>
            nameBasis + offset
          case CLDataIO.InputPointer =>
            nameBasis + offset + "[" + n + "]"
          case CLDataIO.OutputPointer =>
            nameBasis + offset + "[" + n + "]"
        },
        List(0)
      )
    )

  override def openCLIntermediateKernelTupleElementsExprs(expr: String): Seq[(String, List[Int])] = 
    Seq((expr, List(0)))

  override def openCLIthTupleElementNthItemExpr(nameBasis: String, argType: CLDataIO.ArgType, offset: Int, indexes: List[Int], n: String): String = {
    if (indexes != List(0))
        error("There is only one item in this array of " + this + " (trying to access item " + indexes + ")")
    openCLKernelNthItemExprs(nameBasis, argType, offset, n)(0)._1
  }
  
  override def clType = t.erasure.getSimpleName.toLowerCase match {
    case "sizet" => "size_t"
    case "boolean" => CLFilteredArray.presenceCLType
    case "character" => "short"
    case n => n
  }

  override def toString = t.erasure.getSimpleName + " /* " + clType + "*/"

  override def createBuffers(length: Int)(implicit context: Context): Array[CLGuardedBuffer[Any]] =
    Array(new CLGuardedBuffer[T](length)(context, this).asInstanceOf[CLGuardedBuffer[Any]])

  override def extract(arrays: Array[CLGuardedBuffer[Any]], offset: Int, index: Int): CLFuture[T] =
    arrays(offset).asInstanceOf[CLGuardedBuffer[T]].apply(index)

  //override def extract(pointers: Array[Pointer[Any]], offset: Int, index: Int): T =
  //  pointers(offset).asInstanceOf[Pointer[T]].get(index)

  //override def store(v: T, pointers: Array[Pointer[Any]], offset: Int, index: Int): Unit =
  //  pointers(offset).asInstanceOf[Pointer[T]].set(index, v)

  override def store(v: T, arrays: Array[CLGuardedBuffer[Any]], offset: Int, index: Int): Unit =
    arrays(offset).asInstanceOf[CLGuardedBuffer[T]].update(index, v)

  override def exprs(arrayExpr: String): Seq[String] =
    Seq(arrayExpr)

  override def toArray(arrays: Array[CLGuardedBuffer[Any]]): Array[T] = {
    assert(elementCount == arrays.length)
    arrays(0).asInstanceOf[CLGuardedBuffer[T]].toArray
  }
}

object CLIntDataIO extends CLValDataIO[Int] {
  override def reductionType = (OpenCLType.Int, 1)
  
  override def extract(pointers: Array[Pointer[Any]], offset: Int, index: Int): Int =
    pointers(offset).getIntAtOffset(index * 4)

  override def extract(arrays: Array[Array[Any]], offset: Int, index: Int): Int =
    arrays(offset).asInstanceOf[Array[Int]](index)

  override def store(v: Int, pointers: Array[Pointer[Any]], offset: Int, index: Int): Unit =
    pointers(offset).setIntAtOffset(index * 4, v)
}

object CLShortDataIO extends CLValDataIO[Short] {
  override def reductionType = (OpenCLType.Short, 1)
  
  override def extract(pointers: Array[Pointer[Any]], offset: Int, index: Int): Short =
    pointers(offset).getShortAtOffset(index * 2)

  override def extract(arrays: Array[Array[Any]], offset: Int, index: Int): Short =
    arrays(offset).asInstanceOf[Array[Short]](index)

  override def store(v: Short, pointers: Array[Pointer[Any]], offset: Int, index: Int): Unit =
    pointers(offset).setShortAtOffset(index * 2, v)
}

object CLByteDataIO extends CLValDataIO[Byte] {
  override def reductionType = (OpenCLType.Byte, 1)
  
  override def clType = "char"
  
  override def extract(pointers: Array[Pointer[Any]], offset: Int, index: Int): Byte =
    pointers(offset).getByteAtOffset(index * 1)

  override def extract(arrays: Array[Array[Any]], offset: Int, index: Int): Byte =
    arrays(offset).asInstanceOf[Array[Byte]](index)

  override def store(v: Byte, pointers: Array[Pointer[Any]], offset: Int, index: Int): Unit =
    pointers(offset).setByteAtOffset(index * 1, v)
}

object CLBooleanDataIO extends CLValDataIO[Boolean] {
  override def reductionType = (OpenCLType.Byte, 1)
  
  override def extract(pointers: Array[Pointer[Any]], offset: Int, index: Int): Boolean =
    pointers(offset).getByteAtOffset(index * 1) != 0

  override def extract(arrays: Array[Array[Any]], offset: Int, index: Int): Boolean =
    arrays(offset).asInstanceOf[Array[Boolean]](index)

  override def store(v: Boolean, pointers: Array[Pointer[Any]], offset: Int, index: Int): Unit =
    pointers(offset).setByteAtOffset(index * 1, if (v) 1 else 0)
}

object CLCharDataIO extends CLValDataIO[Char] {
  override def reductionType = (OpenCLType.Char, 1)
  
  override def extract(pointers: Array[Pointer[Any]], offset: Int, index: Int): Char =
    pointers(offset).getCharAtOffset(index * 2)

  override def extract(arrays: Array[Array[Any]], offset: Int, index: Int): Char =
    arrays(offset).asInstanceOf[Array[Char]](index)

  override def store(v: Char, pointers: Array[Pointer[Any]], offset: Int, index: Int): Unit =
    pointers(offset).setCharAtOffset(index * 2, v)
}

object CLLongDataIO extends CLValDataIO[Long] {
  override def reductionType = (OpenCLType.Long, 1)
  
  override def extract(pointers: Array[Pointer[Any]], offset: Int, index: Int): Long =
    pointers(offset).getLongAtOffset(index * 8)

  override def extract(arrays: Array[Array[Any]], offset: Int, index: Int): Long =
    arrays(offset).asInstanceOf[Array[Long]](index)

  override def store(v: Long, pointers: Array[Pointer[Any]], offset: Int, index: Int): Unit =
    pointers(offset).setLongAtOffset(index * 8, v)
}

object CLFloatDataIO extends CLValDataIO[Float] {
  override def reductionType = (OpenCLType.Float, 1)
  
  override def extract(pointers: Array[Pointer[Any]], offset: Int, index: Int): Float =
    pointers(offset).getFloatAtOffset(index * 4)

  override def extract(arrays: Array[Array[Any]], offset: Int, index: Int): Float =
    arrays(offset).asInstanceOf[Array[Float]](index)

  override def store(v: Float, pointers: Array[Pointer[Any]], offset: Int, index: Int): Unit =
    pointers(offset).setFloatAtOffset(index * 4, v)
}

object CLDoubleDataIO extends CLValDataIO[Double] {
  override def reductionType = (OpenCLType.Double, 1)
  
  override def extract(pointers: Array[Pointer[Any]], offset: Int, index: Int): Double =
    pointers(offset).getDoubleAtOffset(index * 8)

  override def extract(arrays: Array[Array[Any]], offset: Int, index: Int): Double =
    arrays(offset).asInstanceOf[Array[Double]](index)

  override def store(v: Double, pointers: Array[Pointer[Any]], offset: Int, index: Int): Unit =
    pointers(offset).setDoubleAtOffset(index * 8, v)
}

/*
class CLRangeDataIO(implicit val t: ClassManifest[Int]) extends CLDataIO[Int] {

  override val elementCount = 1

  override val pointerIO: PointerIO[Int] =
    PointerIO.getInstance(t.erasure)

  override def elements: Seq[CLDataIO[Any]] =
    Seq(this.asInstanceOf[CLDataIO[Any]])

  override def openCLKernelArgDeclarations(argType: CLDataIO.ArgType, offset: Int): Seq[String] = {
    assert(argType != )
    Seq("int rangeLow" + offset)
  }

  override def openCLKernelNthItemExprs(argType: CLDataIO.ArgType, offset: Int, n: String) =
    Seq(("(rangeLow" + offset + " + " + n + ")", List(0)))

  override def openCLIntermediateKernelTupleElementsExprs(expr: String): Seq[(String, List[Int])] =
    Seq((expr, List(0))) // TODO ?

  override def openCLIthTupleElementNthItemExpr(argType: CLDataIO.ArgType, offset: Int, indexes: List[Int], n: String): String = {
    if (indexes != List(0))
        error("There is only one item in this array of " + this + " (trying to access item " + indexes + ")")
    openCLKernelNthItemExprs(argType, offset, n)(0)._1
  }

  override def clType = "int"

  override def toString = "int range"

  override def createBuffers(length: Int)(implicit context: Context): Array[CLGuardedBuffer[Any]] =
    Array(new CLGuardedBuffer[Int](2).asInstanceOf[CLGuardedBuffer[Any]])

  override def extract(arrays: Array[CLGuardedBuffer[Any]], offset: Int, index: Int): CLFuture[Int] = {
    val arr = arrays(offset).asInstanceOf[CLGuardedBuffer[Int]]
    error("not implemented")
    
  }

  override def extract(pointers: Array[Pointer[Any]], offset: Int, index: Int): Int =
    pointers(offset).asInstanceOf[Pointer[Int]].get(0) + index.toInt

  override def extract(arrays: Array[Array[Any]], offset: Int, index: Int): Int =
    arrays(offset).asInstanceOf[Array[Int]](0) + index.toInt

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
*/
