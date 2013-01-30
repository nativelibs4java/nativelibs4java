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
package impl

import scala.reflect.ClassTag
import scala.collection.mutable.ArrayBuffer

import com.nativelibs4java.opencl.CLMem
import org.bridj.{ Pointer, PointerIO }

abstract class ScalarDataIO[T : Manifest](io: PointerIO[_]) extends DataIO[T] {
  override val typeString = implicitly[ClassTag[T]].runtimeClass.getSimpleName
  override def bufferCount = 1
  
  private[scalacl] val pointerIO: PointerIO[T] = io.asInstanceOf[PointerIO[T]]
  
  private[scalacl] override def foreachScalar(f: ScalarDataIO[_] => Unit) {
    f(this)
  }
    
  override def toArray(length: Int, buffers: Array[ScheduledBuffer[_]]): Array[T] = {
    val Array(buffer: ScheduledBuffer[T]) = buffers
    buffer.read().getArray.asInstanceOf[Array[T]]
  }
  
  override def allocateBuffers(length: Long, values: Array[T])(implicit context: Context, m: ClassTag[T]): Array[ScheduledBuffer[_]] = {
    val pointer = Pointer.pointerToArray[T](values)
    Array(new ScheduledBuffer(context.context.createBuffer(CLMem.Usage.InputOutput, pointer)))
  }
  private[scalacl] def allocateBuffer(length: Long)(implicit context: Context) =
    context.context.createBuffer(CLMem.Usage.InputOutput, pointerIO, length)
    
  override def allocateBuffers(length: Long, out: ArrayBuffer[ScheduledBuffer[_]])(implicit context: Context) = {
    out += new ScheduledBuffer(allocateBuffer(length))
  }
}

object IntDataIO extends ScalarDataIO[Int](PointerIO.getIntInstance) {
  override def get(index: Long, buffers: Array[Pointer[_]], bufferOffset: Int) =
    buffers(bufferOffset).getIntAtIndex(index)
  override def set(index: Long, buffers: Array[Pointer[_]], bufferOffset: Int, value: Int) =
    buffers(bufferOffset).setIntAtIndex(index, value)
}

object FloatDataIO extends ScalarDataIO[Float](PointerIO.getFloatInstance) {
  override def get(index: Long, buffers: Array[Pointer[_]], bufferOffset: Int) =
    buffers(bufferOffset).getFloatAtIndex(index)
  override def set(index: Long, buffers: Array[Pointer[_]], bufferOffset: Int, value: Float) =
    buffers(bufferOffset).setFloatAtIndex(index, value)
}

object BooleanDataIO extends ScalarDataIO[Boolean](PointerIO.getBooleanInstance) {
  override def get(index: Long, buffers: Array[Pointer[_]], bufferOffset: Int) =
    buffers(bufferOffset).getByteAtIndex(index) != 0
  override def set(index: Long, buffers: Array[Pointer[_]], bufferOffset: Int, value: Boolean) =
    buffers(bufferOffset).setByteAtIndex(index, (if (value) 1 else 0).asInstanceOf[Byte])
}