package scalacl

import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.TypingTransformers

trait RewritingPluginComponent {

  this: PluginComponent with TreeBuilders =>
  import global._
  import definitions._
  import typer.typed


  /// describes the outside of the loop (before - statements - and after - final return value)
  class LoopOutersEnv(
    val aIdentGen: IdentGen,
    val nIdentGen: IdentGen
  )
  /// describes the outside of the loop (before - statements - and after - final return value). Includes payload that will be visible from the inside
  class LoopOuters[Payload](
    val statements: List[Tree],
    val finalReturnValue: Tree,
    val payload: Payload
  )
  /// describes the environment available to the inside of the loop
  class LoopInnersEnv[Payload](
    val aIdentGen: IdentGen,
    val nIdentGen: IdentGen,
    val iIdentGen: IdentGen,
    val itemIdentGen: IdentGen,
    val itemSym: Symbol,
    val payload: Payload
  )

  trait CollectionRewriters {
    protected var currentOwner: Symbol
    val unit: CompilationUnit

    abstract sealed class CollectionRewriter {
      val supportsRightVariants: Boolean
      def filters: List[Tree] = Nil
      def foreach[Payload](
        tree: Tree,
        array: Tree,
        componentType: Symbol,
        reverseOrder: Boolean,
        skipFirst: Boolean,
        outerStatements: LoopOutersEnv => LoopOuters[Payload],
        innerStatements: LoopInnersEnv[Payload] => List[Tree]
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
      }
    }

    case class IntRangeRewriter(from: Tree, to: Tree, byValue: Int, isUntil: Boolean, filtersList: List[Tree]) extends CollectionRewriter {
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
        innerStatements: LoopInnersEnv[Payload] => List[Tree]
      ): Tree = {
        val pos = tree.pos
        assert(!reverseOrder)
        val (iIdentGen, iSym, iDef) = newVariable(unit, "i$", currentOwner, tree.pos, true, from.setType(IntClass.tpe))
        val (nIdentGen, nSym, nDef) = newVariable(unit, "n$", currentOwner, tree.pos, false, to.setType(IntClass.tpe))
        val loopOuters = outerStatements(new LoopOutersEnv(null, nIdentGen))
        val loopInners = new LoopInnersEnv[Payload](null, nIdentGen, iIdentGen, iIdentGen, iSym, loopOuters.payload)
        typed {
          treeCopy.Block(
            tree,
            List(
              nDef,
              iDef
            ) ++
            loopOuters.statements ++
            List(
              whileLoop(
                currentOwner,
                unit,
                tree,
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
                typed {
                  Block(
                    innerStatements(loopInners),
                    incrementIntVar(iIdentGen, newInt(byValue))
                  )
                }
              )
            ),
            if (loopOuters.finalReturnValue == null)
              newUnit
            else
              loopOuters.finalReturnValue
          )
        }
      }
    }
    case object ArrayRewriter extends CollectionRewriter {
      override val supportsRightVariants = true
      override def foreach[Payload](
        tree: Tree,
        collection: Tree,
        componentType: Symbol,
        reverseOrder: Boolean,
        skipFirst: Boolean,
        outerStatements: LoopOutersEnv => LoopOuters[Payload],
        innerStatements: LoopInnersEnv[Payload] => List[Tree]
      ): Tree = {
        val pos = tree.pos
        val (aIdentGen, aSym, aDef) = newVariable(unit, "array$", currentOwner, pos, false, collection)
        val (nIdentGen, _, nDef) = newVariable(unit, "n$", currentOwner, pos, false, Select(aIdentGen(), nme.length).setSymbol(getMember(aSym, nme.length)).setType(IntClass.tpe))
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
        val loopOuters = outerStatements(new LoopOutersEnv(aIdentGen, nIdentGen))
        val loopInners = new LoopInnersEnv[Payload](aIdentGen, nIdentGen, iIdentGen, itemIdentGen, itemSym, loopOuters.payload)
        typed {
          treeCopy.Block(
            tree,
            List(
              aDef,
              nDef,
              iDef
            ) ++
            loopOuters.statements ++
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
                    innerStatements(loopInners)

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
            if (loopOuters.finalReturnValue == null)
              newUnit
            else
              loopOuters.finalReturnValue
          )
        }
      }
    }
    case object ListRewriter extends CollectionRewriter {
      override val supportsRightVariants = false
      override def foreach[Payload](
        tree: Tree,
        collection: Tree,
        componentType: Symbol,
        reverseOrder: Boolean,
        skipFirst: Boolean,
        outerStatements: LoopOutersEnv => LoopOuters[Payload],
        innerStatements: LoopInnersEnv[Payload] => List[Tree]
      ): Tree = {
        assert(!reverseOrder)
        val pos = tree.pos
        val colTpe = collection.tpe
        val (aIdentGen, aSym, aDef) = newVariable(unit, "list$", currentOwner, pos, true, collection)
        val (itemIdentGen, itemSym, itemDef) = newVariable(unit, "item$", currentOwner, pos, false, typed {
            Select(aIdentGen(), headName).setSymbol(colTpe.member(headName)).setType(componentType.tpe)
        })
        val loopOuters = outerStatements(new LoopOutersEnv(aIdentGen, null))
        val loopInners = new LoopInnersEnv[Payload](aIdentGen, null, null, itemIdentGen, itemSym, loopOuters.payload)
        typed {
          treeCopy.Block(
            tree,
            List(
              aDef
            ) ++
            loopOuters.statements ++
            List(
              whileLoop(
                currentOwner,
                unit,
                tree,
                boolNot(Select(aIdentGen(), isEmptyName).setSymbol(colTpe.member(isEmptyName)).setType(BooleanClass.tpe)),
                typed {
                  val itemAndInnerStats =
                    List(itemDef) ++
                    innerStatements(loopInners)

                  Block(
                    itemAndInnerStats,
                    Assign(
                      aIdentGen(),
                      Select(aIdentGen(), tailName).setSymbol(colTpe.member(tailName)).setType(colTpe)
                    ).setType(UnitClass.tpe)
                  )
                }
              )
            ),
            if (loopOuters.finalReturnValue == null)
              newUnit
            else
              loopOuters.finalReturnValue
          )
        }
      }
    }
  }
}