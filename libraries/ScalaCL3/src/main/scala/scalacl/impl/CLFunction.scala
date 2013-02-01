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
package scalacl.impl

import scala.reflect.ClassTag
import scala.collection.mutable.ArrayBuffer
import com.nativelibs4java.opencl.CLEvent

import scalacl.CLArray
import scalacl.Context

case class CLFunction[U, V](
    f: U => V, 
    kernel: Kernel, 
    captures: Captures = Captures())
  extends Function1[U, V] {

  def apply(u: U) = f(u)
  def apply()(implicit ev1: U =:= Unit) = f({}.asInstanceOf[U])
  
  // Task
  def apply(context: Context)(implicit ev1: U =:= Unit, ev2: V =:= Unit): CLEvent = {
    apply(context, params = null, input = null, output = null)
  }

  // Kernel with no explicit input and output
  def apply(context: Context, params: KernelExecutionParameters)(implicit ev1: U =:= Unit, ev2: V =:= Unit): CLEvent = {
    apply(context, params = params, input = null, output = null)
  }

  private def append[T : ClassTag](a: Array[T], v: T): Array[T] = {
    if (v == null) a
    else if (a != null) a :+ v
    else Array(v)
  }
  
  // Task or NDRange
  def apply(
      context: Context, 
      params: KernelExecutionParameters, 
      input: CLArray[U],
      output: CLArray[V]): CLEvent = 
  {
    //println(s"Executing kernel with params = $params")
    ScheduledData.schedule(
      append(captures.inputs, input),
      append(captures.outputs, output),
      eventsToWaitFor => {
        val args = new ArrayBuffer[AnyRef]
        val addArg = (b: ScheduledBuffer[_]) => { args += b.buffer }: Unit
          
        if (input != null)
          input.foreachBuffer(addArg)
        
        if (output != null)
          output.foreachBuffer(addArg)
        
        if (captures.inputs != null)
          captures.inputs.foreach(_.foreachBuffer(addArg))
        
        if (captures.outputs != null)
          captures.outputs.foreach(_.foreachBuffer(addArg))
        
        if (captures.constants != null)
          args ++= captures.constants
          
        kernel.enqueue(context, params, args.toArray, eventsToWaitFor)
      })
  }
}