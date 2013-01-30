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

import com.nativelibs4java.opencl._
import java.util.concurrent.locks._
import collection.mutable.ArrayBuffer

/**
 * Thin wrapper for OpenCL kernel sources, which can act as a fast cache key for the corresponding CLKernel
 */
class Kernel(protected val id: Long, protected val sources: String) {
  def getKernel(context: Context): CLKernel = {
    context.kernels(this, _.release) {
      println("sources = " + sources)
      val Array(k) = context.context.createProgram(sources).createKernels
      k
    }
  }
  def enqueue(context: Context, params: KernelExecutionParameters, args: Array[AnyRef], eventsToWaitFor: Array[CLEvent]): CLEvent = {
    var kernel = getKernel(context)
    kernel synchronized {
      kernel.setArgs(args: _*)
      if (params == null)
        kernel.enqueueTask(context.queue, eventsToWaitFor: _*)
      else
        kernel.enqueueNDRange(context.queue, params.globalOffsets, params.globalSizes, params.localSizes, eventsToWaitFor: _*)
    }
  }
  
  override def equals(o: Any) = o.isInstanceOf[Kernel] && {
    val k = o.asInstanceOf[Kernel]
    id == k.id && (sources eq k.sources) // identity test: assume interned strings coming from class resources!
  }
  
  override def hashCode = id.hashCode
  
  override def toString = "Kernel(" + sources + ")"
}