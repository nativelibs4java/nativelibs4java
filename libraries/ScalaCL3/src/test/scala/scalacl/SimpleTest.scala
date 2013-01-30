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

import scalacl._
  
import com.nativelibs4java.opencl.CLMem
import scalacl.impl.Kernel
import scalacl.impl.CLFunction
import scalacl.impl.Captures

import org.junit._
import Assert._

/*

kernel void f(global float*a, int dim1Offset, int dim1Step) {
	int i = dim1Offset + global_index(0) * dim1Step;
}

 */

class SimpleTest {
  @Test
  def testHandWrittenKernels {
	  implicit val context = Context.best
	  val factor = 20.5f
	  val trans = new CLFunction[Int, Int](
	    v => (v * factor).toInt, 
	    new Kernel(
	      1, 
	      """
        kernel void f(global const int* input, global int* output, float factor) {
          int i = get_global_id(0);
          if (i >= get_global_size(0))
          return;
          output[i] = (int)(input[i] * factor);
        }
        """), 
        Captures(constants = Array(factor.asInstanceOf[AnyRef])))
        
	  val pred = new CLFunction[Int, Boolean](
	    v => v % 2 == 0,
	    new Kernel(
	      2,
	      """
        kernel void f(global const int* input, global char* output) {
          int i = get_global_id(0);
          if (i >= get_global_size(0))
          return;
          output[i] = input[i] % 2 == 0;
        }
        """))

	  val values = Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
	  val a = CLArray[Int](values: _*)
	  //val a = new CLArray[Int](1000000)
	  println(a)
		  
	  def doit(print: Boolean, check: Boolean = false) {
	    val b = a.map(trans)
	    val fil = a.map(pred)
		if (print) {
		  println(b)
		  println(fil)
		}
	    if (check) {
	      assertEquals(values.map(trans).toSeq, b.toArray.toSeq)
	    }
		  context.queue.finish()
	    b.release()
	  }
	  
	  doit(true)
	  for (i <- 0 until 10) {
	    val start = System.nanoTime
		doit(false)
	    val timeMicros = (System.nanoTime - start) / 1000
	    println((timeMicros / 1000.0) + " milliseconds")
	  }
	  context.release()
	}
	
  @Test
  def testSimpleScalarCapture {
	  implicit val context = Context.best
	  val f = 0.2f
	  
	  val array = Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
	  val clArray: CLArray[Int] = array.cl
	  
	  val clResult = clArray.map(x => x * 2 * f)
	  val result = array.map(x => x * 2 * f)
	  assertEquals(result.toList, clResult.toList)
	  
	  clArray.release()
	  clResult.release()
	  context.release()
	}
	
  @Test
  def testSimpleArrayCapture {
	  implicit val context = Context.best
	  
	  val clResult = {
      val f = CLArray(10, 20, 30, 40)
      val a = CLArray(0, 1, 2, 3)
      a.map(x => f(x) + x)
    }
	  val result = {
      val f = Array(10, 20, 30, 40)
      val a = Array(0, 1, 2, 3)
      a.map(x => f(x) + x)
    }
	  assertEquals(result.toList, clResult.toList)
	  
	  context.release()
	}
}