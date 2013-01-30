/*
 * ScalaCL - putting Scala on the GPU with JavaCL / OpenCL
 * http://scalacl.googlecode.com/
 *
 * Copyright (c) 2009-2013, Olivier Chafik (http://ochafik.com/)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Olivier Chafik nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY OLIVIER CHAFIK AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package scalacl
import scalacl.impl._
import com.nativelibs4java.opencl.CLMem
import scala.collection.mutable.ArrayBuffer
import com.nativelibs4java.opencl.CLEvent
import org.bridj.Pointer

import language.experimental.macros

object CLArray {
  def apply[T](values: T*)(implicit io: DataIO[T], context: Context, m: ClassManifest[T]) = {
    val valuesArray = values.toArray
    val length = valuesArray.length
    new CLArray[T](length, io.allocateBuffers(length, valuesArray))
  }
}

class CLArray[T](
  val length: Long, 
  protected val buffers: Array[ScheduledBuffer[_]]
)(
  implicit io: DataIO[T], 
  val context: Context, 
  m: ClassManifest[T]
)
extends ScheduledBufferComposite 
{
  def this(length: Long)(implicit io: DataIO[T], context: Context, m: ClassManifest[T]) = {
    this(length, io.allocateBuffers(length))
  }

  def apply(index: Long): T = sys.error("not implemented")
  def update(index: Long, value: T): Unit = sys.error("not implemented")
  
  override def clone: CLArray[T] =
    new CLArray[T](length, buffers.map(_.clone))

  private[scalacl] override def foreachBuffer(f: ScheduledBuffer[_] => Unit) {
    buffers.foreach(f)
  }

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
    
  def toList: List[T] =
    toArray.toList
  
  def toSeq: Seq[T] = 
    toArray.toSeq

  def foreach(f: T => Unit): Unit =
    macro CLArrayMacros.foreachImpl[T]
  
  private[scalacl] def foreach(f: CLFunction[T, Unit]) {
    execute(f, null)
  }

  def map[U](f: T => U)(implicit io2: DataIO[U], m2: ClassManifest[U]): CLArray[U] =
    macro CLArrayMacros.mapImpl[T, U]
    
  private[scalacl] 
  def map[U](f: CLFunction[T, U])
            (implicit io2: DataIO[U], m2: ClassManifest[U]): CLArray[U] = {
	val output = new CLArray[U](length)
    execute(f, output)
    output
  }
  
  private def execute[U](f: CLFunction[T, U], output: CLArray[U]) {
    val clf = f.asInstanceOf[CLFunction[T, U]]
    val params = KernelExecutionParameters(Array(length))
    clf.apply(context, params, this, output)
  }

  def filter(f: T => Boolean): CLFilteredArray[T] = macro CLArrayMacros.filterImpl[T]
  private[scalacl] def filter(f: CLFunction[T, Boolean]): CLFilteredArray[T] = {
    val presenceMask = new CLArray[Boolean](length)
    execute(f, presenceMask)
    new CLFilteredArray[T](this.clone, presenceMask)
  }

  def reduce(f: (T, T) => T): T = sys.error("not implemented")

  def zip[U](col: CLArray[U])(implicit m2: ClassManifest[U], io: DataIO[(T, U)]): CLArray[(T, U)] = 
	new CLArray[(T, U)](length, buffers.clone ++ col.buffers.clone)
	
  def zipWithIndex: CLArray[(T, Int)] = sys.error("not implemented")

  def copyTo(pointer: Pointer[T]): Unit = sys.error("not implemented")

  def sum: T = sys.error("not implemented")
  def product: T = sys.error("not implemented")
  def min: T = sys.error("not implemented")
  def max: T = sys.error("not implemented")
}