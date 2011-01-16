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
import Function.{tupled, untupled}

class IntRangeForeach2WhileTest extends TestUtils with TypeUtils {
  
  @Test
  def simpleUntilFilterLoop {
    ensurePluginCompilesSnippetsToSameByteCode(
      """ var t = 0
          for (i <- 0 until 100; if i < 10)
            t += 2 * i
      """,
      """ var t = 0
          val n = 100
          var i = 0
          while (i < n)
          {
            if (i < 10) {
              t += 2 * i
            }
            i += 1
          }
      """
    )
  }
  
  @Test
  def simpleRange2PrimitiveMap =
    ensurePluginCompilesSnippetsToSameByteCode(primValues map tupled { simpleRangeMap })

  @Test
  def simpleRange2RefMap =
    ensurePluginCompilesSnippetsToSameByteCode(trivialRefValues map tupled { simpleRangeMap })

  def simpleRangeMap(typeStr: String, valueStr: String) = {
    (
      """
          val m =
            for (i <- 0 until 100)
              yield """ + valueStr + """
      """,
      """
          val m = {
            val from = 0
            val to = 100
            val n = to
            var i = from
            var builder1 = new scala.collection.immutable.VectorBuilder[""" + typeStr + """]
            while (i < n)
            {
              builder1 +=(""" + valueStr + """)
              i += 1
            }
            builder1.result
          }
      """
    )
  }

  @Test
  def simpleRangeFilter: Unit = {
    ensurePluginCompilesSnippetsToSameByteCode( 
      """
          (0 until 100).filter(_ != 50)
      """,
      """
          {
            val from = 0
            val to = 100
            val n = to
            var i = from
            var builder1 = new scala.collection.immutable.VectorBuilder[Int]
            while (i < n)
            {
              if (i != 50)
                builder1 += i
              i += 1
            }
            builder1.result
          }
      """
    )
  }

  @Test
  def simpleToLoop {
    ensurePluginCompilesSnippetsToSameByteCode( 
      """
          var t = 0
          for (i <- 0 to 100)
            t += 2 * i
      """,
      """
          var t = 0
          val n = 100
          var i = 0
          while (i <= n)
          {
            t += 2 * i
            i += 1
          }
      """
    )
  }


  @Test
  def simpleUntilLoop {
    ensurePluginCompilesSnippetsToSameByteCode(
      """ var t = 0
          for (i <- 0 until 100)
            t += 2 * i
      """,
      """ var t = 0
          val n = 100
          var i = 0
          while (i < n)
          {
            t += 2 * i
            i += 1
          }
      """
    )
  }

  
  @Test
  def simpleToByLoop {
    ensurePluginCompilesSnippetsToSameByteCode( 
      """ var t = 0
          for (j <- 50 to 200 by 3)
            t += j / 2
      """,
      """ var t = 0
          val m = 200
          var j = 50
          while (j <= m)
          {
            t += j / 2
            j += 3
          }
      """
    )
  }

  @Test
  def reverseToByLoop {
    ensurePluginCompilesSnippetsToSameByteCode(
      """ var t = 0
          for (j <- 200 to 50 by -3)
            t += j / 2
      """,
      """ var t = 0
          val m = 50
          var j = 200
          while (j >= m)
          {
            t += j / 2
            j += -3
          }
      """
    )
  }

  @Test
  def simpleUntilByLoop {
    ensurePluginCompilesSnippetsToSameByteCode( 
      """ var t = 0
          for (j <- 50 until 200 by 3)
            t += j / 2
      """,
      """ var t = 0
          val m = 200
          var j = 50
          while (j < m)
          {
            t += j / 2
            j += 3
          }
      """
    )
  }
  @Test
  def testNestedLoop {
    ensurePluginCompilesSnippetsToSameByteCode( 
      """ var t = 0
          for (i <- 0 to 100 by 5; j <- 0 until 1000)
            t += 2 * (i + j)
      """,
      """ var t = 0
          val n = 100
          var i = 0
          while (i <= n)
          {
              val m = 1000
              var j = 0
              while (j < m)
              {
                t += 2 * (i + j)
                j += 1
              }
              i += 5
          }
      """
    )
  }
  
  @Test
  def testNestedLoopWithExtRefs {
    ensurePluginCompilesSnippetsToSameByteCode( 
      """ var t = 0
          def f(x: Int) = x + 1
          def g(x: Int) = x - 1
          for (i <- 0 to 100 by 5; j <- 0 until 1000)
            t += 2 * (f(i) + g(j))
      """,
      """ var t = 0
          def f(x: Int) = x + 1
          val n = 100
          var i = 0
          while (i <= n)
          {
              def g(x: Int) = x - 1
              val m = 1000
              var j = 0
              while (j < m)
              {
                t += 2 * (f(i) + g(j))
                j += 1
              }
              i += 5
          }
      """
    )
  }

}
