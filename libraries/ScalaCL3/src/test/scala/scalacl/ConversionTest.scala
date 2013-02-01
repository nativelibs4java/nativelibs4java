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
package impl

import org.junit._
import Assert._
import org.hamcrest.CoreMatchers._

class ConversionTest extends CodeConversion with WithRuntimeUniverse {
  import global._
  
  private val context = reify { null: Context }
  private val NotVectorizable: Option[Expr[Unit]] = None
  private val Vectorizable = not(NotVectorizable)
  
  private def conv(block: Expr[Unit], explicitParamDescs: Seq[ParamDesc] = Seq()) = {
    convertCode(typeCheck(block.tree), explicitParamDescs)
  }
  
  def assertParamDesc(d: ParamDesc, name: String, tpe: Type, usage: UsageKind, kind: ParamKind) = {
    assertEquals(name, d.symbol.name.toString)
    assertEquals(tpe, d.tpe)
    assertEquals(kind, d.mode)
    assertEquals(usage, d.usage)
  }
  
  @Test
  def simpleCaptures {
    val in: CLArray[Int] = null
    val out: CLArray[Int] = null
    val f = 10
    val c = conv(reify { out(1) = in(2) * f })
    assertEquals(
      "kernel void f(global const int* in, global int* out, int f) {\n" +
        "\tout[1] = (in[2] * f);\n" +
      "}",
      c.code
    )
    val Seq(inDesc) = c.capturedInputs
    assertParamDesc(inDesc, "in", typeOf[CLArray[Int]], UsageKind.Input, ParamKind.Normal)
    
    val Seq(outDesc) = c.capturedOutputs
    assertParamDesc(outDesc, "out", typeOf[CLArray[Int]], UsageKind.Output, ParamKind.Normal)
    
    val Seq(fDesc) = c.capturedConstants
    assertParamDesc(fDesc, "f", typeOf[Int], UsageKind.Input, ParamKind.Normal)
  }
  
  @Test
  def simpleTupleResult {
    val in: CLArray[Int] = null
    val out: CLArray[(Int, Float)] = null
    val c = conv(reify {
      out(0) = (in(0), in(2).toFloat)
    })
    assertEquals(
      "kernel void f(global const int* in, global int* out_1, global float* out_2) {\n" +
        "\tout_1[0] = in[0];\n" +
        "\tout_2[0] = ((float)in[2]);\n" +
      "}",
      c.code
    )
  }
  
  @Ignore
  @Test
  def simpleTuplesCaptures {
    val in: CLArray[(Int, (Float, Short))] = null
    val out: CLArray[Float] = null
    val c = conv(reify {
      val (i, (f, s)) = in(0)
      out(0) = i + f + s
    })
    assertEquals(
      "kernel void f(global const int* in, global int* out) {\n" +
        "\tout[1] = (in[2] * f);\n" +
      "}",
      c.code
    )
  }
  
  @Ignore
  @Test
  def aliasedTuplesCaptures {
    val in: CLArray[(Int, (Float, Short))] = null
    val out: CLArray[Float] = null
    val c = conv(reify {
      val (i, p @ (f, s)) = in(0)
      out(0) = i + f + s + p._1 + p._2
    })
    assertEquals(
      "kernel void f(global const int* in, global int* out) {\n" +
        "\tout[1] = (in[2] * f);\n" +
      "}",
      c.code
    )
  }
}
