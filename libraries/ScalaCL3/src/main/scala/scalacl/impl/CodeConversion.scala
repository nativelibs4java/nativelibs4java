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

trait CodeConversion extends OpenCLConverter {
  val global: Universe
  def fresh(s: String): String
  
  import global._
  import definitions._
  
  def cast[A, B](a: A): B = a.asInstanceOf[B]
  
  case class ParamDesc(
    symbol: Symbol,
    tpe: Type,
    mode: ParamKind,
    usage: UsageKind,
    implicitIndexDimension: Option[Int] = None,
    rangeOffset: Option[Symbol] = None,
    rangeStep: Option[Symbol] = None
  ) {
    assert((mode == ParamKind.ImplicitArrayElement || mode == ParamKind.RangeIndex) == (implicitIndexDimension != None))
    def isArray = 
      mode == ParamKind.ImplicitArrayElement || mode == ParamKind.Normal && tpe <:< typeOf[CLArray[_]]
  }
  
  /*
  ParamDesc(i, ParamKindRangeIndex, Some(0))
    -> get_global_id(0)
  ParamDesc(i, ParamKindRangeIndex, Some(0), Some(from), Some(by))
    -> (from + get_global_id(0) * by)
  ParamDesc(x, ParamKindRead)
    -> x
  ParamDesc(x, ParamKindRead, Some(0))
    -> x[get_global_id(0)]
  */
  case class CodeConversionResult(
    code: String,
	  capturedInputs: Seq[ParamDesc],
	  capturedOutputs: Seq[ParamDesc],
	  capturedConstants: Seq[ParamDesc]
	)
	
  def convertCode(code: Tree, explicitParamDescs: Seq[ParamDesc]): CodeConversionResult = {
	  val externalSymbols =
	    getExternalSymbols(
	      code, 
	      knownSymbols = explicitParamDescs.map(_.symbol).toSet
	    )

	  val capturedParams: Seq[ParamDesc] = {
	    for (sym <- externalSymbols.capturedSymbols) yield {
	      val tpe = externalSymbols.symbolTypes(sym)
	      val usage = externalSymbols.symbolUsages(sym)
        ParamDesc(
          sym.asInstanceOf[Symbol],
          tpe.asInstanceOf[Type],
          mode = ParamKind.Normal,
          usage = usage)
	    }
	  }
	  
	  val capturedInputs = capturedParams.filter(p => p.isArray && !p.usage.isOutput)
	  val capturedOutputs = capturedParams.filter(p => p.isArray && p.usage.isOutput)
	  val capturedConstants =
	    capturedParams.filter(!_.isArray) ++
	    explicitParamDescs.filter(_.mode == ParamKind.RangeIndex).flatMap(d =>
        Seq(
          ParamDesc(
            symbol = d.rangeOffset.get,
            tpe = IntTpe,
            mode = ParamKind.Normal,
            usage = UsageKind.Input),
          ParamDesc(
            symbol = d.rangeStep.get,
            tpe = IntTpe,
            mode = ParamKind.Normal,
            usage = UsageKind.Input)
        )
      )
	  
	  val paramDescs =
      explicitParamDescs ++ 
	    capturedInputs ++ 
	    capturedOutputs ++ 
	    capturedConstants
	  val flat = convert(code)
	  
	  val globalIDIndexes =
	    paramDescs.flatMap(_.implicitIndexDimension).toSet
	  
	  val globalIDValNames: Map[Int, String] =
	    globalIDIndexes.map(i => i -> fresh("_global_id_" + i + "_")).toMap
	  
	  val replacements: Seq[String => String] = paramDescs.map(paramDesc => {
      val r = ("\\b(" + paramDesc.symbol.name + ")\\b").r
      // TODO handle composite types, with replacements of all possible fibers (x._1, x._2._1, x._2._2)
	    paramDesc match {
        case ParamDesc(_, _, ParamKind.ImplicitArrayElement, _, Some(i), None, None) =>
          (s: String) => r.replaceAllIn(s, "$1[" + globalIDValNames(i) + "]")
        case ParamDesc(_, _, ParamKind.RangeIndex, _, Some(i), Some(from), Some(by)) =>
          (s: String) => r.replaceAllIn(s, "(" + from.name + " + " + globalIDValNames(i) + " * " + by.name + ")")
        case _ =>
          (s: String) => s
      }
    })
    
    val globalIDStatements = globalIDValNames.toSeq.map { case (i, n) =>
      s"size_t $n = get_global_id($i);"
    }
    
	  val result = 
      (FlatCode[String](statements = globalIDStatements) ++ flat).mapEachValue(s => Seq(
        replacements.foldLeft(s)((v, f) => f(v))))
	  
    val params: Seq[String] = paramDescs.filter(_.mode != ParamKind.RangeIndex).map(paramDesc => {
      // TODO handle composite types, with fresh names for each fiber (x_1, x_2_1, x_2_2)
	    val t = convertTpe(paramDesc.tpe)

	    (if (paramDesc.isArray) "global " else "") +
	    (if (paramDesc.usage == UsageKind.Input && paramDesc.isArray) "const " else "") +
	    t +
	    (if (paramDesc.mode == ParamKind.ImplicitArrayElement) " *" else " ") +
	    paramDesc.symbol.name
    })
      
    val convertedCode =
      result.outerDefinitions.mkString("\n") +
      "kernel void f(" + params.mkString(", ") + ") {\n\t" +
        (result.statements ++ result.values.map(_ + ";")).mkString("\n\t") + "\n" +
      "}"
    CodeConversionResult(convertedCode, capturedInputs, capturedOutputs, capturedConstants)
	}
	
}
