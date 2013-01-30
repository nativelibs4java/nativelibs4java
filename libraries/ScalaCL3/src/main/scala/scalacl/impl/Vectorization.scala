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
  
  private[impl]
  def vectorize(context: Expr[scalacl.Context], block: Expr[Unit], owner: Symbol): Expr[Unit] = {
    block.tree match {
      //Foreach(range @ IntRange(from, to, byOpt, isUntil, Nil), Function(List(param), body)) =>
      case
        Apply(
          TypeApply(
            Select(
              range @ IntRange(from, to, byOpt, isUntil, Nil), 
              foreachName()
            ),
            targs
          ), 
          List(Function(List(param), body))
        ) =>
        val (rangeSym, rangeValDef) = freshVal(owner, "range", typeOf[Range], range)
        val (fromSym, fromValDef) = freshVal(owner, "from", IntTpe, from)
        //val (toSym, toValDef) = freshVal(owner, "to", IntTpe, to)
        
        val by: Int = byOpt match {
          case None => 1
          case Some(Literal(Constant(n: Int))) if n > 0 => n
          case Some(by) => sys.error("Only positive constant steps are handled: " + by)
        }
        
        val (bySym, byValDef) = freshVal(owner, "by", IntTpe, Literal(Constant(by)))
        
        val paramDescs = Seq(
          ParamDesc(
            symbol = param.symbol,
            tpe = IntTpe,
            mode = ParamKind.RangeIndex,
            usage = UsageKind.Input,
            implicitIndexDimension = Some(0),
            rangeOffset = Some(fromSym),
            rangeStep = Some(bySym))
        )
        
        val f = generateCLFunction[Unit, Unit](
          f = blockToUnitFunction(block.tree),
          kernelId = CLFunctionMacros.nextKernelId,
          body = body, 
          paramDescs = paramDescs 
        )
        //println(f)
        //val f = generation.result.tree.asInstanceOf[c.Expr[CLFunction[Unit, Unit]]]
        //reify({})
        def ident[T](vd: ValDef) = expr[T](Ident(vd.name))//symbol))
        
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
      case Apply(TypeApply(Select(target, foreachName()), targs), List(function)) =>
        println("NOT MATCHED(target, args):\n\t" + target + "\n\t" + function)
        reify({})
      case Foreach(collection, function) =>
      //case Apply(TypeApply(Select(t, n), targs), args) =>
        //println("NOT MATCHED: " + t + " (: " + t.getClass.getName + ")")
        println("NOT MATCHED:\n\t" + collection + "\n\t" + function)
        reify({})
      //TypeApply(Select(collection, foreachName()), typeArgs), function @ Function(_, _)) =>
      case _ =>
        println("NOT MATCHED: " + block + " (: " + block.getClass.getName + ")")// + nodeToString(block))
        reify({})
    }
  }
}
