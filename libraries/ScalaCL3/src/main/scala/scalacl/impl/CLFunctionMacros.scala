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
package scalacl.impl
import scalacl.CLArray
import scalacl.CLFilteredArray

import language.experimental.macros
import scala.reflect.macros.Context

private[impl] object CLFunctionMacros 
{
  def cast[A, B](a: A): B = a.asInstanceOf[B]
  
  private val random = new java.util.Random(System.currentTimeMillis)
  
  /// These ids are not necessarily unique, but their values should be dispersed well
  private def nextKernelId = random.nextLong
  
  private[impl] def convertFunction[T: c.WeakTypeTag, U: c.WeakTypeTag](c: Context)(f: c.Expr[T => U]): c.Expr[CLFunction[T, U]] = {
    import c.universe._
    import definitions._
    
    val Function(List(param), body) = c.typeCheck(f.tree)
    
    val outSymbol = c.enclosingMethod.symbol.newTermSymbol(newTermName(c.fresh("out")))
    
    val inputTpe = implicitly[c.WeakTypeTag[T]].tpe
    val outputTpe = implicitly[c.WeakTypeTag[U]].tpe
    
    val conversion = new CodeConversion {
	    override val u = c.universe
	    val (code, capturedParamDescs) = convertCode(
	      Assign(Ident(outSymbol).setType(outputTpe), body).asInstanceOf[u.Tree],
        Seq(
          ParamDesc(
            symbol = param.symbol.asInstanceOf[u.Symbol],
            tpe = inputTpe.asInstanceOf[u.Type],
            mode = ParamKind.ImplicitArrayElement,
            usage = UsageKind.Input,
            implicitIndexDimension = Some(0)),
          ParamDesc(
            symbol = outSymbol.asInstanceOf[u.Symbol], 
            tpe = outputTpe.asInstanceOf[u.Type],
            mode = ParamKind.ImplicitArrayElement,
            usage = UsageKind.Output,
            implicitIndexDimension = Some(0))
        ),
        s => c.fresh(s)
      )
    }
    val code = conversion.code
	  
    val src = c.Expr[String](Literal(Constant(code)))
    val id = c.Expr[Long](Literal(Constant(nextKernelId)))
    
    def arrayApply[A: TypeTag](values: List[Tree]): c.Expr[Array[A]] = {
      c.Expr[Array[A]](
        Apply(
          TypeApply(
            Select(Ident(ArrayModule), newTermName("apply")),
            List(TypeTree(typeOf[A]))
          ),
          values
        )
      )
    }
    val inputs = arrayApply[CLArray[_]](
      conversion.capturedParamDescs
        .filter(d => d.isArray && d.usage.isInput)
        .map(d => Ident(d.symbol.asInstanceOf[Symbol])).toList
    )
    val outputs = arrayApply[CLArray[_]](
      conversion.capturedParamDescs
        .filter(d => d.isArray && d.usage.isOutput)
        .map(d => Ident(d.symbol.asInstanceOf[Symbol])).toList
    )
    val constants = arrayApply[AnyRef](
      conversion.capturedParamDescs
        .filter(!_.isArray)
        .map(d => {
          val x = c.Expr[Array[AnyRef]](Ident(d.symbol.asInstanceOf[Symbol]))
          (c.universe.reify {
            x.splice.asInstanceOf[AnyRef]
          }).tree
        }).toList
    )
    c.universe.reify {
      new CLFunction[T, U](
        f.splice, 
        new Kernel(id.splice, src.splice),
        Captures(
          inputs = inputs.splice, 
          outputs = outputs.splice, 
          constants = constants.splice)
      )
    }
  }
}