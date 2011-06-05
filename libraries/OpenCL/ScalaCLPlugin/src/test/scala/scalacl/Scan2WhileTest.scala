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

class Scan2WhileTest extends TestUtils {

  import options.experimental // SCALACL_EXPERIMENTAL
  
  @Test
  def simpleArrayScanLeft {
    ensurePluginCompilesSnippetsToSameByteCode(
      """ val a = new Array[Double](10)
          val s = a.scanLeft(0.0)(_ + _ * 0.01)
      """,
      """ val a = new Array[Double](10)
          val s = {
            val aa = a
            val n = aa.length
            var i = 0
            var t = 0.0
            val m = new Array[Double](n + 1)
            m(0) = t
            while (i < n) {
                val item = aa(i)
                t = t + item * 0.01
                m(i + 1) = t
                i += 1
            }
            m
          }
      """
    )
  }


  @Test
  def simpleListScanLeft { if (new ScalaCLPlugin.PluginOptions(null).deprecated)
    ensurePluginCompilesSnippetsToSameByteCode(
      """
          val a = List(1.0, 2.0)
          val s = a.scanLeft(0.0)(_ + _ * 0.01)
      """,
      """
          val a = List(1.0, 2.0)
          val m = {
            var list = a
            var t = 0.0
            val builder = new scala.collection.mutable.ListBuffer[Double]
            builder += t
            while (list.isInstanceOf[::[Int]]) {
            //while (!list.isEmpty) {
                val item = list.head
                t = t + item * 0.01
                builder += t
                list = list.tail
            }
            builder.result
          }
      """
    )
  }


  @Test
  def simpleArrayScanRight = if (experimental) {
    ensurePluginCompilesSnippetsToSameByteCode(
      """ val a = new Array[Double](10)
          val s = a.scanRight(0.0)(_ * 0.01 + _)
      """,
      """ val a = new Array[Double](10)
          val s = {
            val aa = a
            val n = aa.length
            var i = n
            var t = 0.0
            val m = new Array[Double](n + 1)
            m(0) = t
            while (i > 0) {
                i -= 1
                val iVal = i
                val item = aa(iVal)
                t = item * 0.01 + t
                m(n - iVal) = t
            }
            m
          }
      """
    )
  }
}
