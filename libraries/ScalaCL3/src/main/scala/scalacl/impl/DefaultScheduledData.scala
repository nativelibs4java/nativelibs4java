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
import com.nativelibs4java.opencl._
import java.util.concurrent.locks._
import collection.mutable.ArrayBuffer

private[scalacl] trait DefaultScheduledData extends ScheduledData {
  private[impl] val scheduleLock = new ReentrantLock

  private def locked[V](block: => V): V = {
    scheduleLock.lock
    try {
      block
    } finally {
      scheduleLock.unlock
    }
  }
  override def finish = {
    val events = new ArrayBuffer[CLEvent]
    locked {
      if (dataWrite != null)
        events += dataWrite
      events ++= dataReads
    }
    CLEvent.waitFor(events.toArray: _*)
    locked {
      events.foreach(doEventCompleted(_))
    }
  }
  private def doEventCompleted(event: CLEvent) {
    if (event.equals(dataWrite)) {
	    dataWrite = null
	    dataReads.clear
	  } else {
	    dataReads -= event
	  }
  } 
  override def eventCompleted(event: CLEvent) {
    locked {
      doEventCompleted(event)
    }
  }
  override def startRead(out: ArrayBuffer[CLEvent]) = {
    scheduleLock.lock
    if (dataWrite != null)
      out += dataWrite
  }

  override def endRead(event: CLEvent) {
    if (event != null)
      dataReads += event
    scheduleLock.unlock
  }

  override def startWrite(out: ArrayBuffer[CLEvent]) = {
    scheduleLock.lock
    out ++= dataReads
    if (dataWrite != null)
      out += dataWrite
  }

  override def endWrite(event: CLEvent) {
    if (event != null) {
      // write will wait for all reads to complete anyway.
      dataReads.clear()
      dataWrite = event
    }
    scheduleLock.unlock
  }

  private[impl] var dataWrite: CLEvent = _
  private[impl] val dataReads = new ArrayBuffer[CLEvent]
}
