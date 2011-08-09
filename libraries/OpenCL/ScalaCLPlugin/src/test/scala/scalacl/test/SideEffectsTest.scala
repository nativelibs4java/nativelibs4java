/*
 * ScalaCL - putting Scala on the GPU with JavaCL / OpenCL
 * http://scalacl.googlecode.com/
 *
 * Copyright (c) 2009-2010, Olivier Chafik (http://ochafik.free.fr/)
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
package scalacl ; package test
import plugin._

import java.io.File
import org.junit._
import Assert._

class SideEffectsTest extends TestUtils {

  def assertSideEffectFull(decls: String, code: String) =  
    assertSideEffect(false, decls, code)
    
  def assertSideEffectFree(decls: String, code: String) = 
    assertSideEffect(true, decls, code)
    
  def assertSideEffect(free: Boolean, decls: String, code: String) = {
    val rc = 
      compileCode(withPlugin = true, decls = decls, code = code)
      
    //println("rc.pluginOptions.testOutputs = " + rc.pluginOptions.testOutputs)
    val sef = rc.pluginOptions.testOutputs.get(HasSideEffects).orNull.asInstanceOf[collection.mutable.ArrayBuffer[_]]
    //getOrElse(null)
    
    assertEquals("Failed to analyze side-effects ! Side effects = \n\t" + Option(sef).map(_.mkString(",\n\t")).getOrElse("testOutputs = " + rc.pluginOptions.testOutputs), free, sef == null || sef.isEmpty)       
  }
  
  @Test
  def simpleSideEffectAssignment {
    assertSideEffectFull("""
      var v = 0
    """, """
      v = 10
    """)
  }
  
  @Test
  def simpleSideEffectFreeAssignment {
    assertSideEffectFree("""
      //no external declaration
    """, """
      var v = 0
      v = 10
    """)
  }
  
  @Test
  def simpleMethodCall {
    assertSideEffectFull("""
      var v = 0
      def some = v = 10
    """, """
      some
    """)
  }
  
  @Test
  def mediumSideEffects {
    assertSideEffectFree("", """
      import scala.math._
      val x = 10
      val p @ (a, b) = 
        if ((sin(x / 2) / 10).toInt % 3 < 100)
          (1: Float, 2.0)
        else
          (1.toFloat, 2: Double)
    """)
  }
}
