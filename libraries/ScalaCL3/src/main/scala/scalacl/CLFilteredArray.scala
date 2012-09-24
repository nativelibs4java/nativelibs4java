package scalacl

import scalacl.impl.ScheduledBuffer
import scalacl.impl.DataIO
import scalacl.impl.ScheduledBufferComposite
import scalacl.impl.DefaultScheduledData

case class CLFilteredArray[T](array: CLArray[T], presenceMask: CLArray[Boolean])(implicit io: DataIO[T], context: Context)
  extends ScheduledBufferComposite {

  override def foreachBuffer(f: ScheduledBuffer[_] => Unit): Unit = {
    array.foreachBuffer(f)
    presenceMask.foreachBuffer(f)
  }

  def map[U](f: T => U): CLFilteredArray[U] = error("not implemented")
  def compact: CLArray[T] = error("not implemented")

  def toCLArray = compact
  
  override def toString = "(" + array + ", " + presenceMask + ")"
}