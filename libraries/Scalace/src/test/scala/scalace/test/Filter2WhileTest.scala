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
package com.nativelibs4java.scalace ; package test
import plugin._

import java.io.File
import org.junit._
import Assert._
import Function.{tupled, untupled}

class Filter2WhileTest extends ScalaceTestUtils with TypeUtils {

  @Test
  def simplePrimitiveArrayFilter {
    ensurePluginCompilesSnippetsToSameByteCode(
      """
          val a = Array(1, 2, 3, 4)
          val m = a.filter(_ != 0)
      """,
      """
          val a = Array(1, 2, 3, 4)
          val m = {
            val array1 = a
            val n1 = array1.length
            var i1 = 0
            val builder1 = new scala.collection.mutable.ArrayBuilder.ofInt
            while (i1 < n1)
            {
              val item1 = array1(i1)
              if (item1 != 0) {
                builder1 += item1
              }
              i1 += 1
            }
            builder1.result
          }
      """
    )
  }
  
  @Test
  def simpleRefArrayFilter {
    ensurePluginCompilesSnippetsToSameByteCode(
      """
          val a = Array("1", "2", "3", "4")
          val m = a.filter(_ != "")
      """,
      """
          val a = Array("1", "2", "3", "4")
          val m = {
            val array1 = a
            val n1 = array1.length
            var i1 = 0
            val builder1 = new scala.collection.mutable.ArrayBuilder.ofRef[String]
            while (i1 < n1)
            {
              val item1 = array1(i1)
              if (item1 != "") {
                builder1 += item1
              }
              i1 += 1
            }
            builder1.result
          }
      """
    )
  }
  
  @Test
  def tupleArrayFilter {
    ensurePluginCompilesSnippetsToSameByteCode(
      """
          val a = Array((1, 2), (10, 20), (100, 200))
          val m = a.filter(_._1 < 30)
      """,
      """
          val a = Array((1, 2), (10, 20), (100, 200))
          val m = {
            val array1 = a
            val n1 = array1.length
            var i1 = 0
            val builder1 = new scala.collection.mutable.ArrayBuilder.ofRef[(Int, Int)]
            while (i1 < n1)
            {
              val item1 = array1(i1)
              if (item1._1 < 30) {
                builder1 += item1
              }
              i1 += 1
            }
            builder1.result
          }
      """
    )
  }


  def simpleArrayFilter(typeStr: String, valueStr: String) = {
    (
      """
          val a = Array(""" + valueStr + """)
          val m = a.filter(_ != """ + valueStr + """)
      """,
      """
          val m = {
            val array1 = a
            val n1 = array1.length
            var i1 = 0
            val builder1 = new scala.collection.mutable.ArrayBuilder.of""" + (if (primTypeNames.contains(typeStr)) typeStr else "Ref[" + typeStr + "]") + """
            while (i1 < n1)
            {
              val item1 = array1(i1)
              if (item1 != """ + valueStr + """) {
                builder1 += item1
              }
              i1 += 1
            }
            builder1.result
          }
      """
    )
  }

  @Test
  def simpleListFilter { if (options.deprecated)
    ensurePluginCompilesSnippetsToSameByteCode(
      """
          val a = List(1, 2, 3, 4)
          val m = a.filter(_ != 0)
      """,
      """
          val a = List(1, 2, 3, 4)
          val m = {
            var list1 = a
            val builder1 = new scala.collection.mutable.ListBuffer[Int]
            //while (!list1.isEmpty) {
            while (list1.isInstanceOf[::[Int]]) {
                val item1 = list1.head
                if (item1 != 0) {
                  builder1 += item1
                }
                list1 = list1.tail
            }
            builder1.result
          }
      """
    )
  }
}
