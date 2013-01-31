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

import scala.reflect.api.Universe

trait Vectorization extends CodeGeneration with MiscMatchers {
  val global: Universe
  import global._
  import definitions._
  
  object PositiveIntConstantOrOne {
    def unapply(treeOpt: Option[Tree]): Option[Int] = Option(treeOpt) collect {
      case Some(Literal(Constant(n: Int))) => n
      case None => 1
    }
  }
  
  private[impl]
  def vectorize(context: Expr[scalacl.Context], block: Tree): Option[Expr[Unit]] = {
    Option(block) collect {
      case 
        Foreach(
          range @ IntRange(from, to, PositiveIntConstantOrOne(by), isUntil, Nil), 
          Function(List(param), body)
        ) 
        =>
        // TODO: get rid of that stupid range and compute the range size by our own means.
        val rangeValDef =
          freshVal("range", typeOf[Range], range)
        val fromValDef = 
          freshVal("from", IntTpe, Select(ident[Range](rangeValDef).tree, N("start")))
        //val toValDef = freshVal("to", IntTpe, to)
        
        val byValDef = freshVal("by", IntTpe, Literal(Constant(by)))
        
        def newSymbol(name: TermName) =
          NoSymbol.newTermSymbol(name)
        
        val paramDescs = Seq(
          ParamDesc(
            symbol = param.symbol,
            tpe = IntTpe,
            mode = ParamKind.RangeIndex,
            usage = UsageKind.Input,
            implicitIndexDimension = Some(0),
            rangeOffset = Some(newSymbol(fromValDef.name)),
            rangeStep = Some(newSymbol(byValDef.name)))
        )
        
        val f = generateCLFunction[Unit, Unit](
          f = blockToUnitFunction(block),
          kernelId = CLFunctionMacros.nextKernelId,
          body = body, 
          paramDescs = paramDescs 
        )
        
        expr[Unit](
          Block(
            rangeValDef,
            fromValDef,
            byValDef,
            reify(
              f.splice(context.splice, new KernelExecutionParameters(ident[Range](rangeValDef).splice.size))
            ).tree
          )
        )
    }
  }
}
