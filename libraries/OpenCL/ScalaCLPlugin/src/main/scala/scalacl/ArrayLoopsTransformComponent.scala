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

object ArrayLoopsTransformComponent {
  val runsAfter = List[String]("namer")
  val phaseName = "arrayloopstransform"
}
class ArrayLoopsTransformComponent(val global: Global)
extends PluginComponent
   with Transform
   with TypingTransformers
   with MiscMatchers
   with TreeBuilders
   with Printers
{
  import global._
  import global.definitions._
  import scala.tools.nsc.symtab.Flags._
  import typer.{typed, atOwner}    // methods to type trees

  override val runsAfter = ArrayLoopsTransformComponent.runsAfter
  override val phaseName = ArrayLoopsTransformComponent.phaseName

  def newTransformer(unit: CompilationUnit) = new TypingTransformer(unit) {
    override def transform(tree: Tree): Tree = tree match {
      case ArrayMap(array, componentType, mappedComponentType, paramName, body) =>
        //println("MATCHED ARRAY MAP " + tree)
        array.tpe = appliedType(ArrayClass.tpe, List(componentType.tpe))
        val mappedArrayTpe = appliedType(ArrayClass.tpe, List(mappedComponentType.tpe))
        try {
          val ssym = ArrayClass.typeConstructor.termSymbol
          val (aIdentGen, aSym, aDef) = newVariable(unit, "array$", currentOwner, tree.pos, false, array)
          val (iIdentGen, iSym, iDef) = newVariable(unit, "i$", currentOwner, tree.pos, true, Literal(Constant(0)).setType(IntClass.tpe))
          val (nIdentGen, nSym, nDef) = newVariable(unit, "n$", currentOwner, tree.pos, false, Select(aIdentGen(), nme.length).setSymbol(getMember(aSym, nme.length)).setType(IntClass.tpe))
          val (mIdentGen, mSym, mDef) = newVariable(unit, "m$", currentOwner, tree.pos, false, typed {
            Apply(
              localTyper.typedOperator {
                Select(
                  New(
                    TypeTree(mappedArrayTpe)
                  ),
                  mappedArrayTpe.typeSymbol.primaryConstructor
                )
              },
              List(nIdentGen())
            )
          }
          )

          typed {
            super.transform(
              treeCopy.Block(
                tree,
                List(
                  aDef,
                  iDef,
                  nDef,
                  mDef,
                  whileLoop(
                    currentOwner,
                    unit,
                    tree,
                    binOp(
                      iIdentGen(),
                      IntClass.tpe.member(nme.LT),
                      nIdentGen()
                    ),
                    Block(
                      List(
                        {
                          val r = replace(
                            paramName.toString,
                            body,
                            typed {
                              Apply(
                                Select(
                                  aIdentGen(),
                                  N("apply")
                                ).setSymbol(getMember(array.symbol, nme.apply)),
                                List(iIdentGen())
                              )
                            },
                            unit
                          )
                          val u =
                            Apply(
                              Select(
                                mIdentGen(),
                                N("update")
                              ).setSymbol(getMember(array.symbol, nme.update)),
                              List(iIdentGen(), r)
                            )

                          unit.comment(tree.pos, "ScalaCL plugin transformed array map into equivalent while loop.")
                          println(tree.pos + ": transformed array map into equivalent while loop.")
                          //println("REPLACED <<<\n" + body + "\n>>> by <<<\n" + r + "\n>>>")
                          typed { u }
                        }
                      ),
                      incrementIntVar(iIdentGen, typed { Literal(Constant(1)) })
                    )
                  )
                ),
                mIdentGen()
              )
            )
          }
        } catch {
          case ex =>
            ex.printStackTrace
            tree
        }
      case ArrayForeach(array, componentType, paramName, body) =>
        array.tpe = appliedType(ArrayClass.tpe, List(componentType.tpe))
        try {
          val (aIdentGen, aSym, aDef) = newVariable(unit, "array$", currentOwner, tree.pos, false, array)
          val (iIdentGen, iSym, iDef) = newVariable(unit, "i$", currentOwner, tree.pos, true, Literal(Constant(0)).setType(IntClass.tpe))
          val (nIdentGen, nSym, nDef) = newVariable(unit, "n$", currentOwner, tree.pos, false, Select(aIdentGen(), nme.length).setSymbol(getMember(aSym, nme.length)).setType(IntClass.tpe))
          typed {
            super.transform(
              treeCopy.Block(
                tree,
                List(
                  aDef,
                  iDef,
                  nDef
                ),
                whileLoop(
                  currentOwner,
                  unit,
                  tree,
                  binOp(
                    iIdentGen(),
                    IntClass.tpe.member(nme.LT),
                    nIdentGen()
                  ),
                  Block(
                    List(
                      {
                        val r = replace(
                          paramName.toString,
                          body,
                          typed {
                            Apply(
                              Select(
                                aIdentGen(),
                                nme.apply
                              ).setSymbol(getMember(array.symbol, nme.apply)),
                              List(iIdentGen())
                            )
                          },
                          unit
                        )
                        unit.comment(tree.pos, "ScalaCL plugin transformed array foreach into equivalent while loop.")
                        println(tree.pos + ": transformed array foreach into equivalent while loop.")
                        //println("REPLACED <<<\n" + body + "\n>>> by <<<\n" + r + "\n>>>")
                        typed { r }
                      }
                    ),
                    incrementIntVar(iIdentGen, typed { Literal(Constant(1)) })
                  )
                )
              )
            )
          }
        } catch {
          case ex =>
            ex.printStackTrace
            tree
        }
      case _ =>
        super.transform(tree)
    }
  }
}
