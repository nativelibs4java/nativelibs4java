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

import scala.collection.immutable.Stack
import scala.reflect.generic.Names
import scala.reflect.generic.Trees
import scala.tools.nsc.Global
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.symtab.Definitions

import scala.reflect.NameTransformer
import scala.tools.nsc.Global
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.{Transform, TypingTransformers}

object ScalaCLFunctionsTransformComponent {
  val runsAfter = List[String](
    "namer",
    RangeForeach2WhileTransformComponent.phaseName,
    ArrayLoopsTransformComponent.phaseName
  )
  val phaseName = "scalaclfunctionstransform"
}

class ScalaCLFunctionsTransformComponent(val global: Global)
extends PluginComponent
   with Transform
   with TypingTransformers
   with MiscMatchers
   with TreeBuilders
   with OpenCLConverter
{
  import global._
  import global.definitions._
  import scala.tools.nsc.symtab.Flags._
  import typer.{typed, atOwner}    // methods to type trees

  override val runsAfter = ScalaCLFunctionsTransformComponent.runsAfter
  override val phaseName = ScalaCLFunctionsTransformComponent.phaseName

  def nodeToStringNoComment(tree: Tree) =
    nodeToString(tree).replaceAll("\\s*//.*\n", "\n").replaceAll("\\s*\n\\s*", " ").replaceAll("\\(\\s+", "(").replaceAll("\\s+\\)", "")

  def newTransformer(unit: CompilationUnit) = new TypingTransformer(unit) {
    var currentClassName: Name = null

    override def transform(tree: Tree): Tree = tree match {
      // Transform inline functions into OpenCL mixed functions / expression code
      case
        Apply(
          Apply(
            TypeApply(
              Select(collectionExpr, functionName @ (mapName() | filterName() | updateName())),
              funTypeArgs @ List(outputType @ TypeTree())
            ),
            List(functionExpr @ Func1(List(ValDef(paramMods, paramName, inputType @ TypeTree(), rhs)), body))
          ),
          implicitArgs @ List(io1, io2)
        ) =>
        /*
         * if functionExpr is :
         *    ((x$1: Int) => (x$1: Int).*(2.0)),
         * functionOpenCLExprString will be :
         *    "_ * 2.0"
         */
        val uniqueSignature = Literal(Constant(tree.symbol.outerSource + "" + tree.symbol.tag + tree.symbol.pos)) // TODO
        val functionOpenCLExprString = convertExpr(Map(paramName.toString -> "_"), body).toString
        println("Converted <<< " + body + " >>> to <<< \"" + functionOpenCLExprString + "\" >>>")
        def seqExpr(typeExpr: Tree, values: Tree*) =
          Apply(
            TypeApply(
              Select(
                Select(Select(Ident(N("scala")), N("collection")), N("Seq")),
                N("apply")
              ),
              List(typeExpr)
            ),
            values.toList
          )

        val newArg =
          Apply(
            Apply(
              TypeApply(
                Select(
                  Select(
                    Ident(N("scalacl")),
                    N("package")
                  ),
                  N("CLFullFun")
                ),
                List(inputType, outputType)
              ),
              List(
                uniqueSignature,
                functionExpr,
                seqExpr(TypeTree(StringClass.tpe)), // statements TODO
                seqExpr(TypeTree(StringClass.tpe), Literal(Constant(functionOpenCLExprString))) // expressions TODO
              )
            ),
            implicitArgs
          )
          Apply(
            Apply(
              TypeApply(
                Select(collectionExpr, N("mapFun")),
                funTypeArgs
              ),
              List(newArg)
            ),
            implicitArgs
          )
      case _ =>
        super.transform(tree)
    }

  }
}
