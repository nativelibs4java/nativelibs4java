/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package scalacl

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
  lastWriteEvent = evt
  /*write(evts => {
    assert(evts.forall(_ == null))
    evt
  })*/
  protected override def doGet = ptr.get
}

class CLTupleFuture[T](ff: Array[CLFuture[Any]], tuple: Array[Any] => T) extends CLFuture[T] {
  override def get = tuple(ff.map(_.get))
}
/*
class CLTuple2Future[T1, T2](f1: CLFuture[T1], f2: CLFuture[T2]) extends CLFuture[(T1, T2)] {
  override def get = (f1.get, f2.get)
}*/