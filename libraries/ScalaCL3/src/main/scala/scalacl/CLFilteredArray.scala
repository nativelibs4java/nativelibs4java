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

import scalacl.impl.ScheduledBuffer
import scalacl.impl.DataIO
import scalacl.impl.ScheduledBufferComposite
import scalacl.impl.DefaultScheduledData

case class CLFilteredArray[T](array: CLArray[T], presenceMask: CLArray[Boolean])(implicit io: DataIO[T], context: Context)
  extends ScheduledBufferComposite {

  private[scalacl] override def foreachBuffer(f: ScheduledBuffer[_] => Unit) {
    array.foreachBuffer(f)
    presenceMask.foreachBuffer(f)
  }

  def map[U](f: T => U): CLFilteredArray[U] = sys.error("not implemented")
  def compact: CLArray[T] = sys.error("not implemented")

  def toCLArray = compact
    
  def toArray: Array[T] =
    toCLArray.toArray
    
  def toList: List[T] =
    toArray.toList
  
  def toSeq: Seq[T] = 
    toArray.toSeq
  
  override def toString = "(" + array + ", " + presenceMask + ")"
}