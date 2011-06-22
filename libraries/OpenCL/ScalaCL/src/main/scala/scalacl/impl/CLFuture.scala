package scalacl

package impl

import com.nativelibs4java.opencl._

import org.bridj.Pointer
import org.bridj.Pointer._


trait CLFuture[T] {
  def get: T
}

trait CLEventFuture[T] extends CLEventBound with CLFuture[T] {
  protected def doGet: T
  override def get = readBlock { doGet }
  def apply = get
}

case class CLInstantFuture[T](value: T) extends CLFuture[T] {
  override def get = value
}
case class CLPointerFuture[T](ptr: Pointer[T], evt: CLEvent) extends CLEventFuture[T] {
  //evt.waitFor
  
  lastWriteEvent = evt
  
  protected override def doGet = ptr.get
}

class CLTupleFuture[T](ff: Array[CLFuture[Any]], tuple: Array[Any] => T) extends CLFuture[T] {
  override def get = tuple(ff.map(_.get))
}
