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

class PastBugsTest extends TestUtils {

  /// http://code.google.com/p/nativelibs4java/issues/detail?id=34
  @Test
  def issue34_optimInTrait {
    ensurePluginCompilesSnippet("""
      trait Bug {
        for (i <- 0 to 10) {
          val crash = i
        }
      }
    """)
  }

  @Test
  def yieldTuplesFromTuplesList {
    ensurePluginCompilesSnippet("""
      val l1 = (1 to 5).toList.zipWithIndex
      for ((a, b) <- l1)
          yield (a, b)
    """)
  }
  
  /// http://code.google.com/p/nativelibs4java/issues/detail?id=40
  @Test
  def issue40_tuplesArrayFilteredForeach {
    ensureCodeWithSameResult("""
      (for ((i, j) <- Array(1 -> 1, 2 -> 2)) yield (i, j)).toSeq
    """)
  }
  
  /// http://code.google.com/p/nativelibs4java/issues/detail?id=41
  @Test
  def issue41_arrayListToArray {
    ensureCodeWithSameResult("""
      List(Array(1,2), Array(3,4)).toArray.map(_.toSeq).toSeq
    """)
  }
  
  /// http://code.google.com/p/nativelibs4java/issues/detail?id=42
  @Test
  def issue42_anyListToArray {
    ensureCodeWithSameResult("""
      def f(a: List[Any]) = a.toArray
      f(List(1, 2, 3)).toSeq
    """)
  }
  
  /// http://code.google.com/p/nativelibs4java/issues/detail?id=43
  @Test
  def issue43_mapWithBreakOut {
    ensureCodeWithSameResult("""
      (
        Array(1).map(_ + 1)(collection.breakOut): Set[Int],
        List(1).map(_ + 1)(collection.breakOut): Set[Int],
        (0 until 10).map(_ + 1)(collection.breakOut): Set[Int]
      )
    """)
  }
  
  val a: Set[Int] = List(1).map(_ + 1)(collection.breakOut)
  
  @Test
  def yieldTuplesInMap {
    ensurePluginCompilesSnippetsToSameByteCode(
      """
          val a = Array(1, 2)
          for (v <- a) yield (v, v)
      """,
      """
          val a = Array(1, 2);
          {
            val array = a
            val n = array.length
            var i = 0
            val m = new Array[(Int, Int)](n)
            while (i < n) {
              val item = array(i)
              m(i) = (item, item)
              i += 1
            }
            m
          }
      """
    )
  }
  
  @Test
  def yieldCaseClassFromTuples {
    ensurePluginCompilesSnippet(
      """
          case class T(a: Int, b: Int)
          for ((a, b) <- Array((1, 2), (2, 3))) yield T(a, b)
      """
    )
  }
        
  @Test
  def lambdaLiftNestedMap {
    ensurePluginCompilesSnippetsToSameByteCode(
      """
          val a = Array(1, 2)
          a.map(xx => a.map(x => { def f = x ; f }))
      """,
      """
          val a = Array(1, 2);
          {
            val array1 = a
            val n1 = array1.length
            var i1 = 0
            val m1 = new Array[Array[Int]](n1)
            while (i1 < n1) {
              val item1 = array1(i1)
              m1(i1) = {
                val array2 = a
                val n2 = array2.length
                var i2 = 0
                val m2 = new Array[Int](n2)
                while (i2 < n2) {
                  val item2 = array2(i2)
                  m2(i2) = {
                    def f = item2
                    f
                  }
                  i2 += 1
                }
                m2
              }
              i1 += 1
            }
            m1
          }
      """
    )
  }
}
