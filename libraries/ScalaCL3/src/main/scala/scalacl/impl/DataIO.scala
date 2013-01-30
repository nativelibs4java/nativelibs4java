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

import com.nativelibs4java.opencl.{ CLMem, CLEvent }
import org.bridj.{ Pointer, PointerIO }
import scala.collection.mutable.ArrayBuffer

private[scalacl] trait DataIO[T] {
  private[scalacl] def typeString: String
  private[scalacl] def bufferCount: Int
  private[scalacl] def foreachScalar(f: ScalarDataIO[_] => Unit): Unit
  private[scalacl] def allocateBuffers(length: Long)(implicit context: Context): Array[ScheduledBuffer[_]] = {
    val buffers = new ArrayBuffer[ScheduledBuffer[_]]
    allocateBuffers(length, buffers)
    buffers.toArray
  }
  
  private[scalacl] def toArray(length: Int, buffers: Array[ScheduledBuffer[_]]): Array[T]
  
  private[scalacl] def allocateBuffers(length: Long, values: Array[T])(implicit context: Context, m: ClassManifest[T]): Array[ScheduledBuffer[_]] = {
    val pointersBuf = new ArrayBuffer[Pointer[_]]
    foreachScalar(io => pointersBuf += Pointer.allocateArray(io.pointerIO, length))
    
    val pointers = pointersBuf.toArray
    for (i <- 0 until length.toInt) {
      set(i, pointers, 0, values(i))
    }
    
    pointers.map(pointer => new ScheduledBuffer(context.context.createBuffer(CLMem.Usage.InputOutput, pointer)))
  }
  private[scalacl] def allocateBuffers(length: Long, out: ArrayBuffer[ScheduledBuffer[_]])(implicit context: Context): Unit
  private[scalacl] def get(index: Long, buffers: Array[Pointer[_]], bufferOffset: Int): T
  private[scalacl] def set(index: Long, buffers: Array[Pointer[_]], bufferOffset: Int, value: T): Unit
}
