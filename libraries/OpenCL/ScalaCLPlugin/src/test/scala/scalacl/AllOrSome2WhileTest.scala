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
package scalacl

import java.io.File
import org.junit._
import Assert._

class AllOrSome2WhileTest extends TestUtils {

  
  @Test
  def simpleForall {
    ensurePluginCompilesSnippetsToSameByteCode("simpleForall", 
      """ val a = new Array[Double](10)
          val s = a.forall(_ != 3)
      """,
      """ val a = new Array[Double](10)
          val s = {
            val aa = a
            val n = aa.length
            var i = 0
            var t = 0.0
            var hasTrue = true
            while (i < n && hasTrue) {
                val item = aa(i)
                hasTrue = item != 3
                i += 1
            }
            hasTrue
          }
      """
    )
  }

  
  @Test
  def simpleExists {
    ensurePluginCompilesSnippetsToSameByteCode("simpleExists", 
      """ val a = new Array[Double](10)
          val s = a.exists(_ != 3)
      """,
      """ val a = new Array[Double](10)
          val s = {
            val aa = a
            val n = aa.length
            var i = 0
            var t = 0.0
            var hasTrue = false
            while (i < n && !hasTrue) {
                val item = aa(i)
                hasTrue = item != 3
                i += 1
            }
            hasTrue
          }
      """
    )
  }
}
