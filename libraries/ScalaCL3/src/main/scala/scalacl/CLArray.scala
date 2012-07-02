package scalacl
import scalacl.impl._
import com.nativelibs4java.opencl.CLMem
import scala.collection.mutable.ArrayBuffer
import com.nativelibs4java.opencl.CLEvent
import org.bridj.Pointer

object CLArray {
  def apply[T](values: T*)(implicit io: DataIO[T], context: Context, m: ClassManifest[T]) = {
    val valuesArray = values.toArray
    val length = valuesArray.length
    new CLArray[T](length, io.allocateBuffers(length, valuesArray))
  }
}
class CLArray[T](length: Long, protected val buffers: Array[ScheduledBuffer[_]])(implicit io: DataIO[T], context: Context, m: ClassManifest[T])
  extends ScheduledBufferComposite {

  def this(length: Long)(implicit io: DataIO[T], context: Context, m: ClassManifest[T]) = {
    this(length, io.allocateBuffers(length))
  }

  override def clone: CLArray[T] =
    new CLArray[T](length, buffers.map(_.clone))

  override def foreachBuffer(f: ScheduledBuffer[_] => Unit): Unit =
    buffers.foreach(f)

  override def toString =
    toArray.mkString("CLArray[" + io.typeString + "](", ", ", ")")

  def toPointer(implicit io: ScalarDataIO[T]): Pointer[T] = {
    val p: Pointer[T] = Pointer.allocateArray(io.pointerIO, length)
    val Array(buffer: ScheduledBuffer[T]) = buffers
    buffer.read(p)
    p
  }
  def toArray: Array[T] =
    io.toArray(length.toInt, buffers)
  
  def toSeq: Seq[T] = 
    toArray.toSeq

  def foreach(f: T => Unit): Unit =
    execute(f, null)

  def map[U](f: T => U)(implicit io2: DataIO[U], m2: ClassManifest[U]): CLArray[U] = {
    val output = new CLArray[U](length)
    execute(f, output)
    output
  }

  private def execute[U](f: T => U, output: CLArray[U]): Unit = {
    val clf = f.asInstanceOf[CLFunction[T, U]]
    val params = KernelExecutionParameters(Array(length))
    clf.apply(context.queue, params, this, output)
  }

  def filter(f: T => Boolean): CLFilteredArray[T] = {
    val clf = f.asInstanceOf[CLFunction[T, Boolean]]
    val presenceMask = new CLArray[Boolean](length)
    val params = KernelExecutionParameters(Array(length))
    clf.apply(context.queue, params, this, presenceMask)
    new CLFilteredArray[T](this.clone, presenceMask)
  }

  def reduce(f: (T, T) => T): T = error("not implemented")

  def zip[U](col: CLArray[U])(implicit m2: ClassManifest[U], io: DataIO[(T, U)]): CLArray[(T, U)] = 
	new CLArray[(T, U)](length, buffers.clone ++ col.buffers.clone)
	
  def zipWithIndex: CLArray[(T, Int)] = error("not implemented")

  def copyTo(pointer: Pointer[T]): Unit = error("not implemented")

  def sum: T = error("not implemented")
  def product: T = error("not implemented")
  def min: T = error("not implemented")
  def max: T = error("not implemented")
}