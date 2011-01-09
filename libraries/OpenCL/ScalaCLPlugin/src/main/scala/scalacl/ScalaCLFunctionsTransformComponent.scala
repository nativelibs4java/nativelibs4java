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

import scala.tools.nsc.Global

import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.{Transform, TypingTransformers}
import scala.tools.nsc.typechecker.Analyzer
import scala.tools.nsc.typechecker.Contexts
//import scala.tools.nsc.typechecker.Contexts._

object ScalaCLFunctionsTransformComponent {
  val runsAfter = List[String](
    "namer"
  )
  val runsBefore = List[String](
    "refchecks",
    LoopsTransformComponent.phaseName
  )
  val phaseName = "scalacl-functionstransform"
}

class ScalaCLFunctionsTransformComponent(val global: Global, val options: ScalaCLPlugin.PluginOptions)
extends PluginComponent
   with Transform
   with TypingTransformers
   with MiscMatchers
   with TreeBuilders
  //with Analyzer
   with OpenCLConverter
   with WithOptions
{
  import global._
  import global.definitions._
  import gen._
  import scala.tools.nsc.symtab.Flags._
  import typer.{typed}    // methods to type trees
  import analyzer.{SearchResult, ImplicitSearch}

  override val runsAfter = ScalaCLFunctionsTransformComponent.runsAfter
  override val runsBefore = ScalaCLFunctionsTransformComponent.runsBefore
  override val phaseName = ScalaCLFunctionsTransformComponent.phaseName

  val ScalaCLPackage       = getModule("scalacl")
  val ScalaCLPackageClass  = ScalaCLPackage.tpe.typeSymbol
  val CLDataIOClass = definitions.getClass("scalacl.impl.CLDataIO")
  val CLArrayClass = definitions.getClass("scalacl.CLArray")
  val CLIntRangeClass = definitions.getClass("scalacl.CLIntRange")
  val CLCollectionClass = definitions.getClass("scalacl.CLCollection")
  val CLFilteredArrayClass = definitions.getClass("scalacl.CLFilteredArray")
  val scalacl_ = N("scalacl")
  val getCachedFunctionName = N("getCachedFunction")
  
  def nodeToStringNoComment(tree: Tree) =
    nodeToString(tree).replaceAll("\\s*//.*\n", "\n").replaceAll("\\s*\n\\s*", " ").replaceAll("\\(\\s+", "(").replaceAll("\\s+\\)", "")

  def newTransformer(unit: CompilationUnit) = new TypingTransformer(unit) {
    var currentClassName: Name = null

    def createImplicitDataIO(context: analyzer.Context, tree: Tree, tpe: Type) = {
      val applicableViews: List[SearchResult] = 
        new ImplicitSearch(tree, tpe, isView = false, context.makeImplicit(reportAmbiguousErrors = false)).allImplicits
      for (view <- applicableViews) {

      }
      null: Tree
    }

    override def transform(tree: Tree): Tree = {
      //println(".")
      if (!shouldOptimize(tree))
        super.transform(tree)
      else {
        tree match {
          case TraversalOp(traversalOp) if traversalOp.op.f != null =>
            import traversalOp._
            try {
              val colTpe = collection.tpe.widen.dealias.deconst
              //if (colTpe.matches(CLCollectionClass.tpe)) {
              if (colTpe.toString.startsWith("scalacl.")) { // TODO
                op match {
                  case opType @ (TraversalOp.Map(_, _) | TraversalOp.Filter(_, false)) =>
                    msg(unit, tree.pos, "associated equivalent OpenCL source to " + colTpe + "." + op + "'s function argument.") {
                      val Func(List(uniqueParam), body) = op.f
                      val context = localTyper.context1
                      val sourceTpe = uniqueParam.symbol.tpe
                      val mappedTpe = body.tpe
                      val Array(
                        sourceDataIO, 
                        mappedDataIO
                      ) = Array(
                        sourceTpe, 
                        mappedTpe
                      ).map(tpe => {
                        val dataIOTpe = appliedType(CLDataIOClass.tpe, List(tpe))
                        analyzer.inferImplicit(tree, dataIOTpe, false, false, context).tree
                      })
                      
                      val (statements, values) = convertExpr(Map(uniqueParam.name.toString -> "_"), body)
                      val uniqueSignature = Literal(Constant(
                        (
                          Array(
                            tree.symbol.outerSource, tree.symbol.tag + "|" + tree.symbol.pos,
                            sourceTpe, mappedTpe, statements
                          ) ++ values
                        ).map(_.toString).mkString("|")
                      ))
                      val uniqueId = uniqueSignature.hashCode // TODO !!!
                      
                      if (options.verbose)
                        println("[scalacl] Converted <<< " + body + " >>> to <<< \"" + statements + "\n(" + values.mkString(", ") + ")\" >>>")
                      val getCachedFunctionSym = ScalaCLPackage.tpe member getCachedFunctionName
                      val clFunction = 
                        typed {
                          Apply(
                            Apply(
                              TypeApply(
                                Select(
                                  Ident(scalacl_) setSymbol ScalaCLPackage,
                                  getCachedFunctionName
                                ),
                                List(TypeTree(sourceTpe), TypeTree(mappedTpe))
                              ).setSymbol(getCachedFunctionSym),
                              List(
                                newInt(uniqueId),
                                op.f,
                                newSeqApply(TypeTree(StringClass.tpe), Literal(Constant(statements))),
                                newSeqApply(TypeTree(StringClass.tpe), values.map(value => Literal(Constant(value))):_*),
                                newSeqApply(TypeTree(AnyClass.tpe)) // args TODO
                              )
                            ).setSymbol(getCachedFunctionSym),
                            List(
                              sourceDataIO,
                              mappedDataIO
                            )
                          ).setSymbol(getCachedFunctionSym)
                        }
  
                      val rep = replaceOccurrences(super.transform(tree), Map(), Map(), Map(op.f -> (() => clFunction)), unit)
                      //println("REP = " + nodeToString(rep))
                      rep
                    }
                  case _ =>
                    super.transform(tree)
                }
              } else {
                super.transform(tree)
              }
            } catch { case ex => 
              ex.printStackTrace
              super.transform(tree)
            }
          case _ =>
            super.transform(tree)
        }
      }
    }
  }
}
