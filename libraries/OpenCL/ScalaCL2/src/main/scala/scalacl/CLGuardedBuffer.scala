/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package scalacl
import com.nativelibs4java.opencl._
import org.bridj.Pointer
import org.bridj.Pointer._

class CLGuardedBuffer[T](val buffer: CLBuffer[T])(implicit t: ClassManifest[T], context: ScalaCLContext) extends CLEventBound {
  lazy val elementClass = t.erasure.asInstanceOf[Class[T]]
  //val buffer = context.context.createBuffer[T](CLMem.Usage.InputOutput, elementClass, fixedSize)

  def this(size: Long)(implicit t: ClassManifest[T], context: ScalaCLContext) =
    this(context.context.createBuffer(CLMem.Usage.InputOutput, t.erasure.asInstanceOf[Class[T]], size))

  def args = Seq(buffer)

  def apply(index: Long): CLFuture[T] = {
    val b: Pointer[T] = allocate(elementClass)
    new CLPointerFuture[T](b, read(evts => buffer.read(context.queue, index, 1, b, false, evts:_*)))
  }

  def toPointer(implicit t: ClassManifest[T]): Pointer[T] =
    readValue(evts => buffer.read(context.queue, evts:_*))

  def toArray(implicit t: ClassManifest[T]): Array[T] = {
    val c = t.erasure
    val p = toPointer
    val s = size.asInstanceOf[Int]
    (
      if (c == classOf[Int])
        p.getInts(s)
      else if (c == classOf[Long])
        p.getLongs(s)
      else if (c == classOf[Short])
        p.getShorts(s)
      else if (c == classOf[Byte])
        p.getBytes(s)
      else if (c == classOf[Char])
        p.getChars(s)
      else if (c == classOf[Double])
        p.getDoubles(s)
      else if (c == classOf[Float])
        p.getFloats(s)
      else if (c == classOf[java.lang.Boolean] || c == classOf[Boolean])
        p.getBooleans(s)
      else if (classOf[Object].isAssignableFrom(c))
        p.toArray(new Array[T](s).asInstanceOf[Array[Object]])
    ).asInstanceOf[Array[T]]
  }
  
  val size = buffer.getElementCount

  override def clone: CLGuardedBuffer[T] = {
    val out = new CLGuardedBuffer(context.context.createBuffer(CLMem.Usage.InputOutput, elementClass, size))
    read(readEvents => {
        out.write(writeEvents => {
            assert(writeEvents.isEmpty)
            buffer.copyTo(context.queue, 0, size, out.buffer, 0, readEvents:_*)
        })
    })
    out
  }

  def clone(start: Long, end: Long): CLGuardedBuffer[T] = {
    val newSize = end - start
    assert(newSize > 0)
    assert(start > 0)
    assert(end <= size)
    val out = new CLGuardedBuffer(context.context.createBuffer(CLMem.Usage.InputOutput, elementClass, newSize))
    read(readEvents => {
      out.write(writeEvents => {
        assert(writeEvents.isEmpty)
        buffer.copyTo(context.queue, start, newSize, out.buffer, 0, readEvents:_*)
      })
    })
    out
  }
}