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

import com.nativelibs4java.opencl.CLBuffer
import scala.collection.mutable.ArrayBuffer
import com.nativelibs4java.opencl.CLEvent
import org.bridj.Pointer
import com.nativelibs4java.opencl.CLMem

/*

ScheduledBuffer:
- shallowClones: ArrayBuffer[ScheduledBuffer]
- cloneModel: ScheduledBuffer

 */

private[scalacl] class ScheduledBuffer[T](initialBuffer: CLBuffer[T])(implicit context: Context) extends DefaultScheduledData {

  private var buffer_ = initialBuffer
  private var lazyClones = new ArrayBuffer[ScheduledBuffer[T]]
  private var lazyCloneModel: ScheduledBuffer[T] = _

  def buffer = buffer_
  
  def release() = this synchronized {
    if (lazyCloneModel == null)
      buffer.release
  }
  override def clone: ScheduledBuffer[T] = this synchronized {
    if (lazyCloneModel != null) {
      lazyCloneModel.clone
    } else {
      val c = new ScheduledBuffer(buffer)
      c.lazyCloneModel = this
      lazyClones += c
      c
    }
  }

  private def performClone() = this synchronized {
    if (lazyCloneModel != null) {
      lazyCloneModel synchronized {
        buffer_ = context.context.createBuffer(CLMem.Usage.InputOutput, initialBuffer.getIO, initialBuffer.getElementCount)
        lazyCloneModel.lazyClones -= this
        lazyCloneModel = null
        ScheduledData.schedule(
          Array(lazyCloneModel),
          Array(this),
          eventsToWaitFor => initialBuffer.copyTo(context.queue, buffer, eventsToWaitFor: _*))
      }
    }
  }
  
  override def startWrite(out: ArrayBuffer[CLEvent]) = this synchronized {
    performClone
    lazyClones.toArray.foreach(_.performClone)
    super.startWrite(out)
  }

  def write(in: Pointer[T]) {
    ScheduledData.schedule(
        Array(),
        Array(this),
        eventsToWaitFor => buffer.write(context.queue, in, false, eventsToWaitFor: _*)
  	)
  }
  def read(): Pointer[T] = {
    val queue = context.queue
    val p = buffer.allocateCompatibleMemory(queue.getDevice)
    
    val event = ScheduledData.schedule(
      Array(this),
      Array[ScheduledData](),
      eventsToWaitFor => buffer.read(queue, p, true, eventsToWaitFor: _*))
    p
  }
  def read(out: ArrayBuffer[CLEvent]): Pointer[T] = {
    val queue = context.queue
    val p = buffer.allocateCompatibleMemory(queue.getDevice)
    
    val event = ScheduledData.schedule(
      Array(this),
      Array[ScheduledData](),
      eventsToWaitFor => buffer.read(queue, p, false, eventsToWaitFor: _*))
    if (out != null)
      out += event
    p
  }
  
  def read(p: Pointer[T]) {
    val event = ScheduledData.schedule(
      Array(this),
      Array(),
      eventsToWaitFor => buffer.read(context.queue, p, false, eventsToWaitFor: _*))
    event.waitFor
  }
}
