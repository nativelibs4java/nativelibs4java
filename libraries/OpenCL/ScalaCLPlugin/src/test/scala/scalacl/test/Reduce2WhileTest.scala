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

class Reduce2WhileTest extends TestUtils {

  @Test
  def simpleReduceLeft {
    ensurePluginCompilesSnippetsToSameByteCode(
      """
          val a = new Array[Double](10)
          val s = a.reduceLeft(_ + _ * 0.01)
      """,
      """
          val a = new Array[Double](10)
          val s = {
            val aa = a
            val n = aa.length
            var i = 0
            var t = 0.0
            var isDefined = false
            while (i < n) {
                val item = aa(i)
                if (!isDefined) {
                  isDefined = true
                  t = item
                } else
                  t = t + item * 0.01
                i += 1
            }
            if (!isDefined)
              throw new ArrayIndexOutOfBoundsException(0)
            t
          }
      """
    )
  }

  @Test
  def simpleReduceRight {
    ensurePluginCompilesSnippetsToSameByteCode(
      """
          val a = new Array[Double](10)
          val s = a.reduceRight(_ * 0.01 + _)
      """,
      """
          val a = new Array[Double](10)
          val s = {
            val aa = a
            val n = aa.length
            var i = n// - 1
            var t = 0.0//aa(n - 1)
            var isDefined = false
            while (i > 0) {
                i -= 1
                val item = aa(i)
                if (!isDefined) {
                  isDefined = true
                  t = item
                } else
                  t = item * 0.01 + t
            }
            if (!isDefined)
              throw new ArrayIndexOutOfBoundsException(0)
            t
          }
      """
    )
  }
  @Test
  def simpleSum {
    ensurePluginCompilesSnippetsToSameByteCode(
      """
          val s1 = Array(1, 2, 3).sum
          val s2 = Array(1.0, 2.0, 3.0).sum
      """,
      """
          val s1 = {
            val a = Array(1, 2, 3)
            val n = a.length
            var i = 0
            var tot = 0.0
            while (i < n) {
                val item = a(i)
                tot += item
                i += 1
            }
            tot
          };
          val s2 = {
            val a = Array(1.0, 2.0, 3.0)
            val n = a.length
            var i = 0
            var tot = 0.0
            while (i < n) {
                val item = a(i)
                tot += item
                i += 1
            }
            tot
          }
      """
    )
  }
  @Test
  def simpleProduct {
    ensurePluginCompilesSnippetsToSameByteCode(
      """
          val s1 = Array(1, 2, 3).product
          val s2 = Array(1.0, 2.0, 3.0).product
      """,
      """
          val s1 = {
            val a = Array(1, 2, 3)
            val n = a.length
            var i = 0
            var tot = 1.0
            while (i < n) {
                val item = a(i)
                tot *= item
                i += 1
            }
            tot
          };
          val s2 = {
            val a = Array(1.0, 2.0, 3.0)
            val n = a.length
            var i = 0
            var tot = 1.0
            while (i < n) {
                val item = a(i)
                tot *= item
                i += 1
            }
            tot
          }
      """
    )
  }

  @Test
  def simpleMinMax {
    ensurePluginCompilesSnippetsToSameByteCode(
      """
          val s1 = Array(1, 2, 3).min
          val s2 = Array(1.0, 2.0, 3.0).max
      """,
      """
          val s1 = {
            val a = Array(1, 2, 3)
            val n = a.length
            var i = 0//1
            var tot = 0//a(0)
            var isDefined = false
            while (i < n) {
                val item = a(i)
                if (!isDefined) {
                  isDefined = true
                  tot = item
                } else {
                  if (item < tot)
                    tot = item
                }
                i += 1
            }
            if (!isDefined)
              throw new ArrayIndexOutOfBoundsException(0)
            tot
          };
          val s2 = {
            val a = Array(1.0, 2.0, 3.0)
            val n = a.length
            var i = 0//1
            var tot = 0.0//a(0)
            var isDefined = false
            while (i < n) {
                val item = a(i)
                if (!isDefined) {
                  isDefined = true
                  tot = item
                } else {
                  if (item > tot)
                    tot = item
                }
                i += 1
            }
            if (!isDefined)
              throw new ArrayIndexOutOfBoundsException(0)
            tot
          }
      """
    )
  }
}
