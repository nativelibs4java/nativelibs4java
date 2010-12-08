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

class ListLoopsRewriteTest extends TestUtils with TypeUtils {

  @Test
  def simpleRefListForeach =
    ensurePluginCompilesSnippetsToSameByteCode("simpleRefListForeach", refTypeNames map simpleListForeach)

  @Test
  def simplePrimitiveListForeach =
    ensurePluginCompilesSnippetsToSameByteCode("simplePrimitiveListForeach", primTypeNames map simpleListForeach)
  
  def simpleListForeach(typeStr: String) = {
    (
      """
          val a = List[""" + typeStr + """]()
          a.foreach(println(_))
      """,
      """
          val a = List[""" + typeStr + """]();
          {
            var list = a
            while (list.isInstanceOf[::[Int]]) {
            //while (!list.isEmpty) {
                val item = list.head
                println(item)
                list = list.tail
            }
          }
      """
    )
  }

  @Test
  def simpleRefListMap =
    ensurePluginCompilesSnippetsToSameByteCode("simpleRefListMap", refTypeNames map simpleListMap)

  @Test
  def simplePrimitiveListMap =
    ensurePluginCompilesSnippetsToSameByteCode("simplePrimitiveListMap", primTypeNames map simpleListMap)

  def simpleListMap(typeStr: String) = {
    (
      """
          val a = List[""" + typeStr + """]()
          val m = a.map(_ + "...")
      """,
      """
          val a = List[""" + typeStr + """]();
          val m = {
            var list = a
            val builder = new scala.collection.mutable.ListBuffer[String]
            while (list.isInstanceOf[::[Int]]) {
            //while (!list.isEmpty) {
                val item = list.head
                builder += item + "..."
                list = list.tail
            }
            builder.result
          }
      """
    )
  }

}
