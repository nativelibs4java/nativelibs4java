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
  private val random = new java.util.Random(System.currentTimeMillis)
  
  /// These ids are not necessarily unique, but their values should be dispersed well
  private[impl] def nextKernelId = random.nextLong
  
  private[impl]
  def convertFunction[A: c.WeakTypeTag, B: c.WeakTypeTag](c: Context)(f: c.Expr[A => B]): c.Expr[CLFunction[A, B]] = {
    import c.universe._
    import definitions._
    
    val outSymbol = c.enclosingMethod.symbol.newTermSymbol(newTermName(c.fresh("out")))
    
    val inputTpe = implicitly[c.WeakTypeTag[A]].tpe
    val outputTpe = implicitly[c.WeakTypeTag[B]].tpe
    
    def isUnit(t: Type) =
      t <:< UnitTpe || t == NoType

    val Function(params, body) = c.typeCheck(f.tree)
    
    val bodyToConvert = 
      if (isUnit(outputTpe)) {
        body
      } else {
        Assign(Ident(outSymbol).setType(outputTpe), body)
      }
    
    val generation = new CodeGeneration {
	    override val global = c.universe
      override def fresh(s: String) = c.fresh(s)
      
      import global._
      
      val inputParamDesc: Option[ParamDesc] = if (isUnit(inputTpe.asInstanceOf[global.Type])) None else Some({
        val List(param) = params
        ParamDesc(
          symbol = cast(param.symbol),
          tpe = cast(inputTpe),
          mode = ParamKind.ImplicitArrayElement,
          usage = UsageKind.Input,
          implicitIndexDimension = Some(0))
      })
      
      val outputParamDesc: Option[ParamDesc] = if (isUnit(outputTpe.asInstanceOf[global.Type])) None else Some({
        ParamDesc(
            symbol = cast(outSymbol), 
            tpe = cast(outputTpe),
            mode = ParamKind.ImplicitArrayElement,
            usage = UsageKind.Output,
            implicitIndexDimension = Some(0))
      })
      
	    val result = generateCLFunction[A, B](
        f = cast(f),
        kernelId = nextKernelId,
        body = cast(bodyToConvert), 
        paramDescs = inputParamDesc.toSeq ++ outputParamDesc.toSeq 
      )
    }
    generation.result.asInstanceOf[c.Expr[CLFunction[A, B]]]
  }
  
  private[impl]
  def convertTask(c: Context)(block: c.Expr[Unit]): c.Expr[CLFunction[Unit, Unit]] = {
    import c.universe._
    import definitions._
    
    val generation = new CodeGeneration {
	    override val global = c.universe
      override def fresh(s: String) = c.fresh(s)
      
	    // Create a fake Unit => Unit function.
	    val typedBlock = c.typeCheck(block.tree)
	    val f = blockToUnitFunction(cast(typedBlock))
	    val result = generateCLFunction[Unit, Unit](
        f = cast(f),
        kernelId = nextKernelId,
        body = cast(typedBlock), 
        paramDescs = Seq() 
      )
    }
    generation.result.asInstanceOf[c.Expr[CLFunction[Unit, Unit]]]
  }
}