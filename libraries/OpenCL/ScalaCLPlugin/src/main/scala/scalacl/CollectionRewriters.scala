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
  )
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

  trait CollectionRewriters {
    protected var currentOwner: Symbol
    val unit: CompilationUnit

    abstract sealed class CollectionRewriter {
      val supportsRightVariants: Boolean
      def filters: List[Tree] = Nil
      def newBuilder(collection: Tree, componentType: Symbol, manifestGetter: Type => Tree): (Tree, TreeGen => Tree)
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
      }
    }

    trait HasBufferBuilder {
      def getBuilderType(componentType: Symbol): Type
      def bufferIdentGenToCol(bufferIdentGen: TreeGen, manifestGetter: Type => Tree, componentType: Symbol): Tree
      def newBuilder(collection: Tree, componentType: Symbol, manifestGetter: Type => Tree): (Tree, TreeGen => Tree) = {
        val builderType = getBuilderType(componentType)
        val builderCreation = Apply(
          Select(
            New(TypeTree(builderType)),
            builderType.typeSymbol.primaryConstructor
          ),
          Nil
        )
        (builderCreation, bufferIdentGenToCol(_, manifestGetter, componentType))
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
            loopOuters.statements ++
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
            if (loopOuters.finalReturnValue == null)
              newUnit
            else
              loopOuters.finalReturnValue
          )
        }
      }
    }
    trait ArrayBuilderTargetRewriter {
      def getBuilderType(componentType: Symbol) = primArrayBuilderClasses.get(componentType) match {
        case Some(t) =>
          t.tpe
        case None =>
          appliedType(ArrayBuilderClass.tpe, List(componentType.tpe))
      }
      def bufferIdentGenToCol(bufferIdentGen: TreeGen, manifestGetter: Type => Tree, componentType: Symbol) = {
        val bufferIdent = bufferIdentGen()
        val sym = bufferIdent.tpe member resultName
        //Apply(
        //  TypeApply(
        Apply(
            Select(
              bufferIdentGen(),
              resultName
            ).setSymbol(sym),
            Nil
        ).setSymbol(sym)
          //  List(TypeTree(componentType.tpe))
          //).setSymbol(sym),
          //List(manifestGetter(componentType.tpe))
        //).setSymbol(sym)
      }
    }
    case object ArrayRewriter extends CollectionRewriter with HasBufferBuilder with ArrayBuilderTargetRewriter {
      override val supportsRightVariants = true
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
            loopOuters.statements ++
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
            if (loopOuters.finalReturnValue == null)
              newUnit
            else
              loopOuters.finalReturnValue
          )
        }
      }
    }
    case object ListRewriter extends CollectionRewriter with HasBufferBuilder {
      override val supportsRightVariants = false
      override def getBuilderType(componentType: Symbol) = appliedType(ListBufferClass.tpe, List(componentType.tpe))
      override def bufferIdentGenToCol(bufferIdentGen: TreeGen, manifestGetter: Type => Tree, componentType: Symbol): Tree = {
        val bufferIdent = bufferIdentGen()
        Select(
          bufferIdent,
          resultName
        ).setSymbol(bufferIdent.tpe member resultName)
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
            loopOuters.statements ++
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
                      Select(aIdentGen(), tailName).setSymbol(sym)//.setType(colTpe)
                    )//.setType(UnitClass.tpe)
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