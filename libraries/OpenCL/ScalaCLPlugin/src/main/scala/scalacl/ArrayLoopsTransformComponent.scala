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

/**
 * Transforms the following constructs into their equivalent while loops :
 * - Array[T].foreach(x => body)
 * - Array[T].map(x => body)
 * - Array[T].reduceLeft((x, y) => body) / reduceRight
 * - Array[T].foldLeft((x, y) => body) / foldRight
 * - Array[T].scanLeft((x, y) => body) / scanRight
 */
class ArrayLoopsTransformComponent(val global: Global)
extends PluginComponent
   with Transform
   with TypingTransformers
   with MiscMatchers
   with TreeBuilders
{
  import global._
  import global.definitions._
  import scala.tools.nsc.symtab.Flags._
  import typer.{typed, atOwner}    // methods to type trees

  override val runsAfter = ArrayLoopsTransformComponent.runsAfter
  override val phaseName = ArrayLoopsTransformComponent.phaseName

  
  def newTransformer(unit: CompilationUnit) = new TypingTransformer(unit) {

    def arrayApply(array: => Tree, index: => Tree) = {
      val a = array
      typed {
        Apply(
          Select(
            a,
            N("apply")
          ).setSymbol(getMember(a.symbol, nme.apply)),
          List(index)
        )
      }
    }

    /// payload is produced by function that creates the outer definitions and passed to the function that creates the inner statements
    def arrayForeach[Payload](
      tree: Tree,
      array: Tree,
      reverseOrder: Boolean,
      outerStatementsReturnAndPayloadFromArrayAndLengthIdent: (IdentGen, IdentGen) => (List[Tree], Tree, Payload),
      innerStatementsFromIndexItemIdentAndPayload: (IdentGen, IdentGen, Payload) => List[Tree]
    ) = {
      val pos = tree.pos
      val (aIdentGen, aSym, aDef) = newVariable(unit, "array$", currentOwner, pos, false, array)
      val (nIdentGen, _, nDef) = newVariable(unit, "n$", currentOwner, pos, false, Select(aIdentGen(), nme.length).setSymbol(getMember(aSym, nme.length)).setType(IntClass.tpe))
      val (iIdentGen, _, iDef) = newVariable(unit, "i$", currentOwner, pos, true, if (reverseOrder) nIdentGen() else newInt(0))
      val (itemIdentGen, itemSym, itemDef) = newVariable(unit, "item$", currentOwner, pos, false, arrayApply(aIdentGen(), iIdentGen()))
      val (outerStatements, returnValue, payload) = 
        outerStatementsReturnAndPayloadFromArrayAndLengthIdent(aIdentGen, nIdentGen)
      
      typed {
        treeCopy.Block(
          tree,
          List(
            aDef,
            nDef,
            iDef
          ) ++
          outerStatements ++
          List(
            whileLoop(
              currentOwner,
              unit,
              tree,
              (
                if (reverseOrder) // while (i > 0) { i--; statements }
                  binOp(
                    iIdentGen(),
                    IntClass.tpe.member(nme.GT),
                    newInt(0)
                  )
                else // while (i < n) { statements; i++ }
                  binOp(
                    iIdentGen(),
                    IntClass.tpe.member(nme.LT),
                    nIdentGen()
                  )
              ),
              typed {
                val itemAndInnerStats =
                  List(itemDef) ++
                  innerStatementsFromIndexItemIdentAndPayload(iIdentGen, itemIdentGen, payload)

                if (reverseOrder)
                  Block(
                    List(incrementIntVar(iIdentGen, newInt(-1))) ++
                    itemAndInnerStats,
                    newUnit
                  )
                else
                  Block(
                    itemAndInnerStats,
                    incrementIntVar(iIdentGen, newInt(1))
                  )
              }
            )
          ),
          if (returnValue == null)
            newUnit
          else
            returnValue
        )
      }
    }
    /// print a message only if the operation succeeded :
    def msg[V](pos: Position, text: String)(v: => V): V = {
      val r = v
      unit.comment(pos, text)
      println(pos + ": " + text)
      r
    }
    def newArray(arrayType: Type, length: => Tree) =
      typed {
        Apply(
          localTyper.typedOperator {
            Select(
              New(TypeTree(arrayType)),
              arrayType.typeSymbol.primaryConstructor
            )
          },
          List(length)
        )
      }

    override def transform(tree: Tree): Tree = try {
      tree match {
        case ArrayMap(array, componentType, mappedComponentType, paramName, body) =>
          array.tpe = appliedType(ArrayClass.tpe, List(componentType.tpe))
          typed {
            super.transform(
              arrayForeach[IdentGen](
                tree,
                array,
                false,
                (_, nIdentGen) => {
                  val mappedArrayTpe = appliedType(ArrayClass.tpe, List(mappedComponentType.tpe))
                  val (mIdentGen, _, mDef) = newVariable(unit, "m$", currentOwner, tree.pos, false, newArray(mappedArrayTpe, nIdentGen()))
                  (List(mDef), mIdentGen(), mIdentGen)
                },
                (iIdentGen, itemIdentGen, mIdentGen) => List(
                  msg(tree.pos, "transformed array map into equivalent while loop.") {
                    typed {
                      Apply(
                        Select(
                          mIdentGen(),
                          N("update")
                        ).setSymbol(getMember(array.symbol, nme.update)),
                        List(
                          iIdentGen(),
                          replace(
                            body,
                            Map(paramName -> itemIdentGen()),
                            unit
                          )
                        )
                      )
                    }
                  }
                )
              )
            )
          }
        case ArrayForeach(array, componentType, paramName, body) =>
          array.tpe = appliedType(ArrayClass.tpe, List(componentType.tpe))
          typed {
            super.transform(
              arrayForeach[Unit](
                tree,
                array,
                false,
                (_, _) => (Nil, null, ()), // no extra outer statement
                (_, itemIdentGen, _) => List(
                  msg(tree.pos, "transformed array foreach into equivalent while loop.") {
                    replace(
                      body,
                      Map(paramName -> itemIdentGen()),
                      unit
                    )
                  }
                )
              )
            )
          }
        case TraversalOp(array, componentType, resultType, accParamName, newParamName, op, isLeft, body, initialValue) =>
          array.tpe = appliedType(ArrayClass.tpe, List(componentType.tpe))
          typed {
            super.transform(
              op match {
                case Reduce | Fold =>
                  arrayForeach[IdentGen](
                    tree,
                    array,
                    !isLeft,
                    (aIdentGen, nIdentGen) => {
                      assert((initialValue == null) == (op == Reduce)) // no initial value for reduce only
                      val (totIdentGen, _, totDef) = newVariable(unit, "tot$", currentOwner, tree.pos, true, 
                        if (initialValue == null)
                          arrayApply(aIdentGen(), if (isLeft) newInt(0) else intAdd(nIdentGen(), newInt(-1)))
                        else
                          initialValue
                      )
                      (List(totDef), totIdentGen(), totIdentGen)
                    },
                    (iIdentGen, itemIdentGen, totIdentGen) => List(
                      msg(tree.pos, "transformed array foldLeft into equivalent while loop.") {
                        Assign(
                          totIdentGen(),
                          replace(
                            body,
                            Map(
                              accParamName -> totIdentGen(),
                              newParamName -> itemIdentGen()
                            ),
                            unit
                          )
                        ).setType(UnitClass.tpe)
                      }
                    )
                  )
                case Scan =>
                  super.transform(tree)
                  /* TODO FIX THE UPDATE !
                  val mappedArrayTpe = appliedType(ArrayClass.tpe, List(resultType.tpe))
                  arrayForeach[(IdentGen, IdentGen)](
                    tree,
                    array,
                    !isLeft,
                    (aIdentGen, nIdentGen) => {
                      val (mIdentGen, _, mDef) = newVariable(unit, "m$", currentOwner, tree.pos, false, newArray(mappedArrayTpe, nIdentGen()))
                      val (totIdentGen, _, totDef) = newVariable(unit, "tot$", currentOwner, tree.pos, true, initialValue)//.setType(IntClass.tpe))
                      (List(totDef, mDef), totIdentGen(), (mIdentGen, totIdentGen))
                    },
                    {
                      case (iIdentGen, itemIdentGen, (mIdentGen, totIdentGen)) =>
                        msg(tree.pos, "transformed array foldLeft into equivalent while loop.") {
                          List(
                            Apply(
                              typed {
                                val mIdent = mIdentGen()
                                Select(
                                  mIdent,
                                  N("update")
                                ).setSymbol(getMember(mIdent.symbol, nme.update))
                              },
                              List(
                                iIdentGen(),
                                totIdentGen()
                              )
                            ).setType(UnitClass.tpe),
                            Assign(
                              totIdentGen(),
                              replace(
                                body,
                                Map(
                                  accParamName -> totIdentGen(),
                                  newParamName -> itemIdentGen()
                                ),
                                unit
                              )
                            ).setType(UnitClass.tpe)
                          )
                        }
                    }
                  )*/
              }
            )
          }
        case _ =>
          super.transform(tree)
      }
    } catch {
      case ex =>
        ex.printStackTrace
        super.transform(tree)
    }
  }
}
