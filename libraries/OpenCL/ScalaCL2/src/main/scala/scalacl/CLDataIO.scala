/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package scalacl
import org.bridj.Pointer
import org.bridj.PointerIO

trait CLDataIO[T] {
  implicit val t: ClassManifest[T]
  val elementCount: Int
  //def valueManifest: ClassManifest[T]

  val pointerIO: PointerIO[T]

  def clType: String
  def createBuffers(length: Long)(implicit context: ScalaCLContext): Array[CLGuardedBuffer[Any]]

  def extract(arrays: Array[CLGuardedBuffer[Any]], index: Long): CLFuture[T] = {
    assert(elementCount == arrays.length)
    extract(arrays, 0, index)
  }
  def store(v: T, arrays: Array[CLGuardedBuffer[Any]], index: Long): Unit = {
    assert(elementCount == arrays.length)
    store(v, arrays, 0, index)
  }
  def extract(pointers: Array[Pointer[Any]], index: Long): T = {
    assert(elementCount == pointers.length)
    extract(pointers, 0, index)
  }
  def store(v: T, pointers: Array[Pointer[Any]], index: Long): Unit = {
    assert(elementCount == pointers.length)
    store(v, pointers, 0, index)
  }

  def extract(arrays: Array[CLGuardedBuffer[Any]], offset: Int, index: Long): CLFuture[T]
  def store(v: T, arrays: Array[CLGuardedBuffer[Any]], offset: Int, index: Long): Unit
  def extract(pointers: Array[Pointer[Any]], offset: Int, index: Long): T
  def store(v: T, pointers: Array[Pointer[Any]], offset: Int, index: Long): Unit
  def exprs(arrayExpr: String): Seq[String]
  //def toArray(arrays: Array[CLGuardedBuffer[Any]], offset: Int): Array[T]

  def toArray(arrays: Array[CLGuardedBuffer[Any]]) = {
    assert(elementCount == arrays.length)
    val pointers = arrays.map(_.toPointer)
    val size = pointers(0).getRemainingElements.toInt
    val out = new Array[T](size)
    for (i <- 0 until size)
      out(i) = extract(pointers, 0, i)
    out
  }
}

object CLTupleDataIO {
  lazy val builtInArities = Set(1, 2, 4, 8)
}
class CLTupleDataIO[T](ios: Array[CLDataIO[Any]], values: T => Array[Any], tuple: Array[Any] => T)(implicit override val t: ClassManifest[T]) extends CLDataIO[T] {
  override lazy val pointerIO: PointerIO[T] = error("Cannot create PointerIO for tuples !")

  override def clType = {
    val types = ios.map(_.clType)
    val uniqTypes = types.toSet
    val n = ios.size
    if (uniqTypes.size == 1 && CLTupleDataIO.builtInArities.contains(n))
      uniqTypes.head + n
    else
      "struct { " + types.reduceLeft(_ + "; " + _) + "; }"
  }

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

  override def createBuffers(length: Long)(implicit context: ScalaCLContext): Array[CLGuardedBuffer[Any]] =
    ios.flatMap(_.createBuffers(length))

    
  override def extract(arrays: Array[CLGuardedBuffer[Any]], offset: Int, index: Long): CLFuture[T] =
    new CLTupleFuture(ios.map(_.extract(arrays, offset, index)), tuple)
  
  override def store(v: T, arrays: Array[CLGuardedBuffer[Any]], offset: Int, index: Long): Unit =
    iosAndOffsets.zip(values(v)).foreach { case ((io, ioOffset), vi) => io.store(vi, arrays, offset + ioOffset, index) }

  override def extract(pointers: Array[Pointer[Any]], offset: Int, index: Long): T =
    tuple(iosAndOffsets.map { case (io, ioOffset) => io.extract(pointers, offset + ioOffset, index) })

  override def store(v: T, pointers: Array[Pointer[Any]], offset: Int, index: Long): Unit =
    iosAndOffsets.zip(values(v)).foreach { case ((io, ioOffset), vi) => io.store(vi, pointers, offset + ioOffset, index) }

  override def exprs(arrayExpr: String): Seq[String] =
    ios.zipWithIndex.flatMap { case (io, i) => io.exprs(arrayExpr + "._" + (i + 1)) }

  /*override def toArray(arrays: Array[CLGuardedBuffer[Any]], offset: Int): Array[T] =
    io1.toArray(arrays, offset).zip(io2.toArray(arrays, offset + io1.elementCount))*/
}

class CLValDataIO[T <: AnyVal](implicit override val t: ClassManifest[T]) extends CLDataIO[T] {
  override val elementCount = 1

  override lazy val pointerIO: PointerIO[T] = PointerIO.getInstance(t.erasure)

  override def clType = t.erasure.getSimpleName.toLowerCase match {
    case "sizet" => "size_t"
    case "boolean" => "char"
    case "character" => "short"
    case n => n
  }
  
  override def createBuffers(length: Long)(implicit context: ScalaCLContext): Array[CLGuardedBuffer[Any]] =
    Array(new CLGuardedBuffer[T](length).asInstanceOf[CLGuardedBuffer[Any]])

  override def extract(arrays: Array[CLGuardedBuffer[Any]], offset: Int, index: Long): CLFuture[T] =
    arrays(offset).asInstanceOf[CLGuardedBuffer[T]](index)

  override def extract(pointers: Array[Pointer[Any]], offset: Int, index: Long): T =
    pointers(offset).asInstanceOf[Pointer[T]].get(index)

  override def store(v: T, pointers: Array[Pointer[Any]], offset: Int, index: Long): Unit =
    pointers(offset).asInstanceOf[Pointer[T]].set(index, v)

  override def store(v: T, arrays: Array[CLGuardedBuffer[Any]], offset: Int, index: Long): Unit =
    arrays(offset).asInstanceOf[CLGuardedBuffer[T]](index) = v

  override def exprs(arrayExpr: String): Seq[String] =
    Seq(arrayExpr)

  override def toArray(arrays: Array[CLGuardedBuffer[Any]]): Array[T] = {
    assert(elementCount == arrays.length)
    arrays(0).asInstanceOf[CLGuardedBuffer[T]].toArray
  }
}