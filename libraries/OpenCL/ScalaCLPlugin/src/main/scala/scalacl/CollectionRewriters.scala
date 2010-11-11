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

import scala.tools.nsc.ast.TreeDSL
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.TypingTransformers

trait RewritingPluginComponent {

  this: PluginComponent with TreeBuilders =>
  import global._
  import gen._
  import definitions._
  import typer.typed
  import CODE._

  /// describes the outside of the loop (before - statements - and after - final return value)
  case class LoopOutersEnv(
    aVar: VarDef,
    nVar: VarDef,
    outputSizeVar: VarDef
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
    aVar: VarDef,
    nVar: VarDef,
    iVar: VarDef,
    outputIndexVar: VarDef,
    itemVar: VarDef,
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
  ) {
    def setOrAdd(builder: TreeGen, index: TreeGen, value: TreeGen) =
      if (set != null && index != null)
        set(builder, index, value)
      else
        add(builder, value)
  }

  trait CollectionRewriters {
    protected var currentOwner: Symbol
    val unit: CompilationUnit

    abstract sealed class CollectionRewriter {
      val supportsRightVariants: Boolean
      def filters: List[Tree] = Nil
      def isSafeRewrite(op: TraversalOpType) = true
      def colToString(tpe: Type): String
      def newBuilderInstance(componentType: Type, knownSize: TreeGen, localTyper: analyzer.Typer): (Type, Tree)
      def newBuilder(pos: Position, componentType: Type, collectionType: Type, knownSize: TreeGen, localTyper: analyzer.Typer): CollectionBuilder
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
      def newBuilderInstance(componentType: Type, knownSize: TreeGen, localTyper: analyzer.Typer): (Type, Tree)
      def newBuilder(pos: Position, componentType: Type, collectionType: Type, knownSize: TreeGen, localTyper: analyzer.Typer) = {
        val (builderTpe, builderInstance) = newBuilderInstance(componentType, knownSize, localTyper)
        CollectionBuilder(
          builder = builderInstance,
          set = null,//(bufferIdentGen, indexIdentGen) => null,
          add = (bufferIdentGen, itemIdentGen) => {
            val addAssignMethod = (builderTpe member addAssignName).alternatives.head// filter (_.paramss.size == 1)
            typed {
              val bufferIdent = bufferIdentGen()
              val t = Apply(
                Select(
                  bufferIdent,
                  addAssignName
                ).setSymbol(addAssignMethod).setType(addAssignMethod.tpe),
                List(itemIdentGen())
              ).setSymbol(addAssignMethod).setType(UnitClass.tpe)
              //println(nodeToString(t))
              t
            }
          },
          result = bufferIdentGen => {
            val resultMethod = builderTpe member resultName
            typed {
              Apply(
                Select(
                  bufferIdentGen(),
                  resultName
                ).setSymbol(resultMethod).setType(resultMethod.tpe),//.setType(UnitClass.tpe),
                Nil
              ).setSymbol(resultMethod).setType(collectionType)//.setType(collectionType)//.setType(UnitClass.tpe)
            }
          }
        )
      }
    }
    case class IntRangeRewriter(from: Tree, to: Tree, byValue: Int, isUntil: Boolean, filtersList: List[Tree]) 
    extends CollectionRewriter 
       with HasBufferBuilder 
       with ArrayBuilderTargetRewriter
    {
      override val supportsRightVariants = false
      override def filters: List[Tree] = filtersList
      override def colToString(tpe: Type) = "Range"
      override def isSafeRewrite(op: TraversalOpType) = {
        import TraversalOp._
        op match {
          case Reduce(_) | Min | Max =>
            false
          case _: FilterWhile =>
            // dropWhile
            false
          case _ =>
            true
        }
      }

      override def newArrayBuilderInfo(componentType: Type, knownSize: TreeGen) = {
        //if (knownSize == null)
        //error("should not pass here now !");
          //(appliedType(WrappedArrayBuilderClass.tpe, List(componentType)), Nil, true, true)
          (appliedType(VectorBuilderClass.tpe, List(componentType)), Nil, false, false)
        //else
        //  super.newArrayBuilderInfo(componentType, knownSize)
      }

      /*
      override def newBuilder(pos: Position, componentType: Type, collectionType: Type, knownSize: TreeGen, localTyper: analyzer.Typer) = {
        val cb = if (knownSize != null)
          ArrayRewriter.newBuilder(pos, componentType, collectionType, knownSize, localTyper)
        else
          super.newBuilder(pos, componentType, collectionType, knownSize, localTyper)

        if (knownSize == null)
          cb
        else
          cb.copy(result = bufferIdentGen => {
            val r = cb.result(bufferIdentGen)
            typed {
              (
                if (knownSize == null)
                  r
                else
                  //mkWrapArray(mkCast(r, appliedType(ArrayClass.tpe, List(componentType))), componentType)
                  mkWrapArray(r, componentType)
              ).DOT(N("toIndexedSeq"))
            }
          })
      }*/
     
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
        //assert(!reverseOrder)
        val fromVar = newVariable(unit, "from$", currentOwner, tree.pos, true, from.setType(IntClass.tpe))
        val toVar = newVariable(unit, "to$", currentOwner, tree.pos, false, to.setType(IntClass.tpe))
        val iVar = newVariable(unit, "i$", currentOwner, tree.pos, true, fromVar())
        val nVar = newVariable(unit, "n$", currentOwner, tree.pos, false, toVar())

        val outputSize = {
          val span = intSub(toVar(), fromVar())
          val width = if (isUntil) 
            span
          else
            intAdd(span, newInt(1))
          
          if (byValue == 1)
            width
          else
            intDiv(width, newInt(byValue))
        }
        val outputSizeVar = newVariable(unit, "outputSize$", currentOwner, tree.pos, false, outputSize)
        val outputIndexVar = newVariable(unit, "outputIndex$", currentOwner, tree.pos, true, if (reverseOrder) intSub(outputSizeVar(), newInt(1)) else newInt(0))
        val loopOuters = outerStatements(new LoopOutersEnv(
          null,
          nVar,
          outputSizeVar
        ))
        val loopInners = new LoopInnersEnv[Payload](null, nVar, iVar, outputIndexVar, iVar, loopOuters.payload)
        val LoopInners(statements, extraTest) = innerStatements(loopInners)
        typed {
          treeCopy.Block(
            tree,
            List[Tree](
              fromVar.definition,
              toVar.definition,
              nVar.definition,
              iVar.definition
            ) ++
            outputSizeVar.defIfUsed ++
            outputIndexVar.defIfUsed ++
            loopOuters.typedStatements ++
            List(
              whileLoop(
                currentOwner,
                unit,
                tree,
                boolAnd(
                  binOp(
                    iVar(),
                    IntClass.tpe.member(
                      if (isUntil) {
                        if (byValue < 0) nme.GT else nme.LT
                      } else {
                        if (byValue < 0) nme.GE else nme.LE
                      }
                    ),
                    nVar()
                  ),
                  extraTest
                ),
                typed {
                  Block(
                    statements ++
                    outputIndexVar.ifUsed(incrementIntVar(outputIndexVar, newInt(if (reverseOrder) -1 else 1))),
                    incrementIntVar(iVar, newInt(byValue))
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
      def newArrayBuilderInfo(componentType: Type, knownSize: TreeGen) = primArrayBuilderClasses.get(componentType) match {
        case Some(t) =>
          (t.tpe, Nil, false, false)
        case None =>
          if (componentType <:< AnyRefClass.tpe)
            (appliedType(RefArrayBuilderClass.tpe, List(componentType)), Nil, true, false)
          else
            (appliedType(ArrayBufferClass.tpe, List(componentType)), List(newInt(16)), false, false)
      }
      def newBuilderInstance(componentType: Type, knownSize: TreeGen, localTyper: analyzer.Typer): (Type, Tree) = {
        val (builderType, mainArgs, needsManifest, manifestIsInMain) = newArrayBuilderInfo(componentType, knownSize);
        (
          builderType,
          typed {
            val manifestList = if (needsManifest) List(localTyper.findManifest(componentType, false).tree) else null
            val sym = builderType.typeSymbol.primaryConstructor
            val n = Apply(
              Select(
                New(TypeTree(builderType)),
                sym
              ).setSymbol(sym),
              if (needsManifest && manifestIsInMain)
                manifestList
              else
                mainArgs
            ).setSymbol(sym)
            if (needsManifest && !manifestIsInMain)
              Apply(
                n,
                manifestList
              ).setSymbol(sym)
            else
              n
          }
        )
      }
    }
    case object ArrayRewriter extends CollectionRewriter with HasBufferBuilder with ArrayBuilderTargetRewriter {
      override val supportsRightVariants = true
      override def colToString(tpe: Type) = tpe.toString
      
      override def newBuilder(pos: Position, componentType: Type, collectionType: Type, knownSize: TreeGen, localTyper: analyzer.Typer) = {
        if (knownSize != null) {
          CollectionBuilder(
            builder = newArray(
              componentType,
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
          super.newBuilder(pos, componentType, collectionType, knownSize, localTyper)
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
        val aVar = newVariable(unit, "array$", currentOwner, pos, false, collection)
        val nVar = newVariable(unit, "n$", currentOwner, pos, false, newArrayLength(aVar()))
        val iVar = newVariable(unit, "i$", currentOwner, pos, true,
          if (reverseOrder) {
            if (skipFirst)
              intSub(nVar(), newInt(1))
            else
              nVar()
          } else {
            if (skipFirst)
              newInt(1)
            else
              newInt(0)
          }
        )
        val itemVar = newVariable(unit, "item$", currentOwner, pos, false, newApply(tree.pos, aVar(), iVar()))
        //val outputIndexVar = newVariable(unit, "outputIndex$", currentOwner, tree.pos, false, if (reverseOrder) intSub(outputSizeVar(), newInt(1)) else newInt(0))
        val loopOuters = outerStatements(new LoopOutersEnv(aVar, nVar, nVar))
        val loopInners = new LoopInnersEnv[Payload](aVar, nVar, iVar, outputIndexVar = null, itemVar, loopOuters.payload)
        val LoopInners(statements, extraTest) = innerStatements(loopInners)
        typed {
          treeCopy.Block(
            tree,
            List(
              aVar.definition,
              nVar.definition,
              iVar.definition
            ) ++
            loopOuters.typedStatements ++
            List(
              whileLoop(
                currentOwner,
                unit,
                tree,
                boolAnd(
                  if (reverseOrder) // while (i > 0) { i--; statements }
                    binOp(
                      iVar(),
                      IntClass.tpe.member(nme.GT),
                      newInt(0)
                    )
                  else // while (i < n) { statements; i++ }
                    binOp(
                      iVar(),
                      IntClass.tpe.member(nme.LT),
                      nVar()
                    ),
                  extraTest
                ),
                typed {
                  val itemAndInnerStats =
                    List(itemVar.definition) ++
                    statements

                  if (reverseOrder)
                    Block(
                      List(decrementIntVar(iVar, newInt(1))) ++
                      itemAndInnerStats,
                      newUnit
                    )
                  else
                    Block(
                      itemAndInnerStats,
                      incrementIntVar(iVar, newInt(1))
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
      override def colToString(tpe: Type) = tpe.toString

      override def isSafeRewrite(op: TraversalOpType) = {
        import TraversalOp._
        op match {
          //case Map | Sum | Fold | _: AllOrSome =>
          //  true
          case _: FilterWhile =>
            false
          case _ =>
            true
        }
      }

      override def newBuilderInstance(componentType: Type, knownSize: TreeGen, localTyper: analyzer.Typer): (Type, Tree) = {
        val builderType = appliedType(ListBufferClass.tpe, List(componentType))
        (
          builderType,
          typed {
            val sym = builderType.typeSymbol.primaryConstructor
            Apply(
              Select(
                New(TypeTree(builderType)),
                builderType.typeSymbol.primaryConstructor
              ).setSymbol(sym),//.setType(sym.tpe),
              Nil
            ).setSymbol(sym)//.setType(builderType)
          }
        )
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
        assert(!reverseOrder)
        val pos = tree.pos
        val colTpe = collection.tpe
        val aVar = newVariable(unit, "list$", currentOwner, pos, true, collection)
        val itemVar = newVariable(unit, "item$", currentOwner, pos, false, typed {
            Select(aVar(), headName).setSymbol(colTpe.member(headName))//.setType(componentType.tpe)
        })
        val loopOuters = outerStatements(new LoopOutersEnv(aVar, null, null))
        val loopInners = new LoopInnersEnv[Payload](aVar, null, null, null, itemVar, loopOuters.payload)
        val LoopInners(statements, extraTest) = innerStatements(loopInners)
        typed {
          treeCopy.Block(
            tree,
            List(
              aVar.definition
            ) ++
            loopOuters.typedStatements ++
            List(
              whileLoop(
                currentOwner,
                unit,
                tree,
                boolAnd(boolNot(Select(aVar(), isEmptyName).setSymbol(colTpe.member(isEmptyName)).setType(BooleanClass.tpe)), extraTest),
                typed {
                  val itemAndInnerStats =
                    List(itemVar.definition) ++
                    statements
                  val sym = colTpe member tailName
                  Block(
                    itemAndInnerStats,//.map(typed),
                    Assign(
                      aVar(),
                      Select(aVar(), tailName).setSymbol(sym).setType(sym.tpe)//.setType(colTpe)
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