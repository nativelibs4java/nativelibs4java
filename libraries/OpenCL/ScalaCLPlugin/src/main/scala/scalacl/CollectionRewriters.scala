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

import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.TypingTransformers

trait RewritingPluginComponent {

  this: PluginComponent with TreeBuilders =>
  import global._
  import definitions._
  import typer.typed


  /// describes the outside of the loop (before - statements - and after - final return value)
  case class LoopOutersEnv(
    aIdentGen: IdentGen,
    aSym: Symbol,
    nIdentGen: IdentGen
  )
  /// describes the outside of the loop (before - statements - and after - final return value). Includes payload that will be visible from the inside
  case class LoopOuters[Payload](
    statements: List[Tree],
    finalReturnValue: Tree,
    payload: Payload
  ) {
    def typedStatements = statements.map(typed)
    def finalReturnValueOrUnit =
      if (finalReturnValue == null)
        newUnit
      else
        typed { finalReturnValue }
  }
  /// describes the environment available to the inside of the loop
  case class LoopInnersEnv[Payload](
    aIdentGen: IdentGen,
    nIdentGen: IdentGen,
    iIdentGen: IdentGen,
    itemIdentGen: IdentGen,
    itemSym: Symbol,
    payload: Payload
  )
  case class LoopInners(
    statements: List[Tree],
    extraLoopTest: Tree = null
  )

  case class CollectionBuilder(
    builder: Tree,
    set: (TreeGen, TreeGen, TreeGen) => Tree,
    add: (TreeGen, TreeGen) => Tree,
    result: TreeGen => Tree
  )

  trait CollectionRewriters {
    protected var currentOwner: Symbol
    val unit: CompilationUnit

    abstract sealed class CollectionRewriter {
      val supportsRightVariants: Boolean
      def filters: List[Tree] = Nil
      def newBuilder(pos: Position, componentType: Symbol, collectionType: Symbol = null, knownSize: TreeGen = null): CollectionBuilder
      def foreach[Payload](
        tree: Tree,
        array: Tree,
        componentType: Symbol,
        reverseOrder: Boolean,
        skipFirst: Boolean,
        outerStatements: LoopOutersEnv => LoopOuters[Payload],
        innerStatements: LoopInnersEnv[Payload] => LoopInners
      ): Tree
    }
    object CollectionRewriter {
      def unapply(tree: Tree): Option[(CollectionRewriter, Type, Tree, Symbol)] = tree match {
        case ArrayTree(array, componentType) =>
          Some((ArrayRewriter, appliedType(ArrayClass.tpe, List(componentType.tpe)), array, componentType))
        case ListTree(componentType) =>
          Some((ListRewriter, appliedType(ListClass.tpe, List(componentType.tpe)), tree, componentType))
        case IntRange(from, to, by, isUntil, filters) =>
          (
            by match {
              case None =>
                Some(1)
              case Some(Literal(Constant(v: Int))) =>
                Some(v)
              case _ =>
                None
            }
          ) match {
            case Some(byValue) =>
              Some((IntRangeRewriter(from, to, byValue, isUntil, filters), appliedType(ArrayClass.tpe, List(IntClass.tpe)), null, IntClass))
            case _ =>
              None
          }
        case _ =>
          None
      }
    }

    trait HasBufferBuilder {
      def getBuilderType(componentType: Symbol): Type
      def newBuilder(pos: Position, componentType: Symbol, collectionType: Symbol, knownSize: TreeGen) = {
        val builderTpe = getBuilderType(componentType)
        CollectionBuilder(
          builder = Apply(
            Select(
              New(TypeTree(builderTpe)),
              builderTpe.typeSymbol.primaryConstructor
            ),
            Nil
          ),
          set = null,//(bufferIdentGen, indexIdentGen) => null,
          add = (bufferIdentGen, itemIdentGen) => {
            val addAssignMethod = builderTpe member addAssignName
            Apply(
              Select(
                bufferIdentGen(),
                addAssignName
              ).setSymbol(addAssignMethod),
              List(itemIdentGen())
            ).setSymbol(addAssignMethod)
          },
          result = bufferIdentGen => {
            val resultMethod = builderTpe member resultName
            Apply(
              Select(
                bufferIdentGen(),
                resultName
              ).setSymbol(resultMethod),
              Nil
            ).setSymbol(resultMethod)
          }
        )
      }
    }
    case class IntRangeRewriter(from: Tree, to: Tree, byValue: Int, isUntil: Boolean, filtersList: List[Tree]) 
    extends CollectionRewriter 
       with HasBufferBuilder 
       with ArrayBuilderTargetRewriter {
      //IntRange(from, to, by, isUntil, filters), f @ Func(List(param), body))
      override val supportsRightVariants = true
      override def filters: List[Tree] = filtersList
      
      override def foreach[Payload](
        tree: Tree,
        collection: Tree,
        componentType: Symbol,
        reverseOrder: Boolean,
        skipFirst: Boolean,
        outerStatements: LoopOutersEnv => LoopOuters[Payload],
        innerStatements: LoopInnersEnv[Payload] => LoopInners
      ): Tree = {
        val pos = tree.pos
        assert(!reverseOrder)
        val (iIdentGen, iSym, iDef) = newVariable(unit, "i$", currentOwner, tree.pos, true, from.setType(IntClass.tpe))
        val (nIdentGen, nSym, nDef) = newVariable(unit, "n$", currentOwner, tree.pos, false, to.setType(IntClass.tpe))
        val loopOuters = outerStatements(new LoopOutersEnv(null, null /* TODO */, nIdentGen))
        val loopInners = new LoopInnersEnv[Payload](null, nIdentGen, iIdentGen, iIdentGen, iSym, loopOuters.payload)
        val LoopInners(statements, extraTest) = innerStatements(loopInners)
        typed {
          treeCopy.Block(
            tree,
            List(
              nDef,
              iDef
            ) ++
            loopOuters.typedStatements ++
            List(
              whileLoop(
                currentOwner,
                unit,
                tree,
                newLogicAnd(
                  binOp(
                    iIdentGen(),
                    IntClass.tpe.member(
                      if (isUntil) {
                        if (byValue < 0) nme.GT else nme.LT
                      } else {
                        if (byValue < 0) nme.GE else nme.LE
                      }
                    ),
                    nIdentGen()
                  ),
                  extraTest
                ),
                typed {
                  Block(
                    statements,
                    incrementIntVar(iIdentGen, newInt(byValue))
                  )
                }
              )
            ),
            loopOuters.finalReturnValueOrUnit
          )
        }
      }
    }
    trait ArrayBuilderTargetRewriter {
      def getBuilderType(componentType: Symbol) = primArrayBuilderClasses.get(componentType) match {
        case Some(t) =>
          t.tpe
        case None =>
          appliedType(RefArrayBuilderClass.tpe, List(componentType.tpe))
      }
    }
    case object ArrayRewriter extends CollectionRewriter with HasBufferBuilder with ArrayBuilderTargetRewriter {
      override val supportsRightVariants = true

      override def newBuilder(pos: Position, componentType: Symbol, collectionType: Symbol, knownSize: TreeGen) = {
        if (knownSize != null && collectionType != null) {
          CollectionBuilder(
            builder = newArray(//WithArrayType(
              //collectionType.tpe,
              componentType.tpe,
              knownSize()
            ),
            set = (bufferIdentGen, indexIdentGen, contentGen) => {
              newUpdate(
                pos,
                bufferIdentGen(),
                indexIdentGen(),
                contentGen()
              )
            },
            add = null,
            result = bufferIdentGen => bufferIdentGen()
          )
        } else
          super.newBuilder(pos, componentType, collectionType, knownSize)
      }

      override def foreach[Payload](
        tree: Tree,
        collection: Tree,
        componentType: Symbol,
        reverseOrder: Boolean,
        skipFirst: Boolean,
        outerStatements: LoopOutersEnv => LoopOuters[Payload],
        innerStatements: LoopInnersEnv[Payload] => LoopInners
      ): Tree = {
        val pos = tree.pos
        val (aIdentGen, aSym, aDef) = newVariable(unit, "array$", currentOwner, pos, false, collection)
        val (nIdentGen, _, nDef) = newVariable(unit, "n$", currentOwner, pos, false, newArrayLength(aIdentGen()))
        val (iIdentGen, _, iDef) = newVariable(unit, "i$", currentOwner, pos, true,
          if (reverseOrder) {
            if (skipFirst)
              intSub(nIdentGen(), newInt(1))
            else
              nIdentGen()
          } else {
            if (skipFirst)
              newInt(1)
            else
              newInt(0)
          }
        )
        val (itemIdentGen, itemSym, itemDef) = newVariable(unit, "item$", currentOwner, pos, false, newApply(tree.pos, aIdentGen(), iIdentGen()))
        val loopOuters = outerStatements(new LoopOutersEnv(aIdentGen, aSym, nIdentGen))
        val loopInners = new LoopInnersEnv[Payload](aIdentGen, nIdentGen, iIdentGen, itemIdentGen, itemSym, loopOuters.payload)
        val LoopInners(statements, extraTest) = innerStatements(loopInners)
        typed {
          treeCopy.Block(
            tree,
            List(
              aDef,
              nDef,
              iDef
            ) ++
            loopOuters.typedStatements ++
            List(
              whileLoop(
                currentOwner,
                unit,
                tree,
                newLogicAnd(
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
                    ),
                  extraTest
                ),
                typed {
                  val itemAndInnerStats =
                    List(itemDef) ++
                    statements

                  if (reverseOrder)
                    Block(
                      List(decrementIntVar(iIdentGen, newInt(1))) ++
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
            loopOuters.finalReturnValueOrUnit
          )
        }
      }
    }
    case object ListRewriter extends CollectionRewriter with HasBufferBuilder {
      override val supportsRightVariants = false
      override def getBuilderType(componentType: Symbol) = appliedType(ListBufferClass.tpe, List(componentType.tpe))
      override def foreach[Payload](
        tree: Tree,
        collection: Tree,
        componentType: Symbol,
        reverseOrder: Boolean,
        skipFirst: Boolean,
        outerStatements: LoopOutersEnv => LoopOuters[Payload],
        innerStatements: LoopInnersEnv[Payload] => LoopInners
      ): Tree = {
        assert(!reverseOrder)
        val pos = tree.pos
        val colTpe = collection.tpe
        val (aIdentGen, aSym, aDef) = newVariable(unit, "list$", currentOwner, pos, true, collection)
        val (itemIdentGen, itemSym, itemDef) = newVariable(unit, "item$", currentOwner, pos, false, typed {
            Select(aIdentGen(), headName).setSymbol(colTpe.member(headName)).setType(componentType.tpe)
        })
        val loopOuters = outerStatements(new LoopOutersEnv(aIdentGen, aSym, null))
        val loopInners = new LoopInnersEnv[Payload](aIdentGen, null, null, itemIdentGen, itemSym, loopOuters.payload)
        val LoopInners(statements, extraTest) = innerStatements(loopInners)
        typed {
          treeCopy.Block(
            tree,
            List(
              aDef
            ) ++
            loopOuters.typedStatements ++
            List(
              whileLoop(
                currentOwner,
                unit,
                tree,
                newLogicAnd(boolNot(Select(aIdentGen(), isEmptyName).setSymbol(colTpe.member(isEmptyName)).setType(BooleanClass.tpe)), extraTest),
                typed {
                  val itemAndInnerStats =
                    List(itemDef) ++
                    statements
                  val sym = colTpe member tailName
                  Block(
                    itemAndInnerStats,
                    Assign(
                      aIdentGen(),
                      Select(aIdentGen(), tailName).setSymbol(sym).setType(colTpe)
                    ).setType(UnitClass.tpe)
                  )
                }
              )
            ),
            loopOuters.finalReturnValueOrUnit
          )
        }
      }
    }
  }
}