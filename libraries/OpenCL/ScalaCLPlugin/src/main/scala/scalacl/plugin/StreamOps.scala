/*
 * Created by IntelliJ IDEA.
 * User: ochafik
 * Date: 10/05/11
 * Time: 21:40
 */
package scalacl ; package plugin

import tools.nsc.plugins.PluginComponent
import tools.nsc.Global

trait StreamOps extends PluginNames with Streams with StreamSinks {
  this: PluginComponent with WithOptions with WorkaroundsForOtherPhases =>
  
  val global: Global
  import global._
  import definitions._
  import treeInfo.{ methPart }
  import typer.typed

  sealed abstract class TraversalOpType {
    val needsInitialValue = false
    val needsFunction = false
    val loopSkipsFirst = false
    val f: Tree
  }

  class TraversalOp(
    val op: TraversalOpType,
    val collection: Tree,
    val resultType: Type,
    val mappedCollectionType: Type,
    val isLeft: Boolean,
    val initialValue: Tree
  ) {
    override def toString = "TraversalOp(" + Array(op, collection, resultType, mappedCollectionType, isLeft, initialValue).mkString(", ") + ")"
  }
  
  /// Matches one of the folding/scanning/reducing functions : (reduce|fold|scan)(Left|Right)
  /// Matches one of the folding/scanning/reducing functions : (reduce|fold|scan)(Left|Right)
  object TraversalOps {

    trait ScalarReduction extends Reductoid {
      override def resultKind = ScalarResult
      override def transformedValue(value: StreamValue, totalVar: VarDef, initVar: VarDef)(implicit loop: Loop): StreamValue = 
        null
    }
    trait SideEffectFreeScalarReduction extends ScalarReduction with SideEffectFreeStreamComponent
    
    trait FunctionTransformer extends StreamTransformer {
      def f: Tree
      
      override def analyzeSideEffectsOnStream(analyzer: SideEffectsAnalyzer) =
        analyzer.analyzeSideEffects(tree, f)
        // Initial value does not affect the stream :
        // && sideEffectsAnalyzer.isSideEffectFree(initialValue)
    }
    trait Function1Transformer extends FunctionTransformer {
      lazy val Func(List(arg), body) = f
      def transformedFunc(value: StreamValue)(implicit loop: Loop) = 
        replaceOccurrences(
          loop.transform(body),
          Map(
            arg.symbol -> (() => value.value())
          ),
          Map(f.symbol -> loop.currentOwner),
          Map(),
          loop.unit
        )
    }
    trait Function2Reduction extends Reductoid with FunctionTransformer {
      lazy val Func(List(leftParam, rightParam), body) = f
      
      override def updateTotalWithValue(total: TreeGen, value: TreeGen)(implicit loop: Loop): ReductionTotalUpdate = {
        import loop.{ unit, currentOwner }
        val result = replaceOccurrences(
          loop.transform(body),
          Map(
            leftParam.symbol -> total,
            rightParam.symbol -> value
          ),
          Map(f.symbol -> currentOwner),
          Map(),
          unit
        )
        //val resultVar = newVariable(unit, "res$", currentOwner, tree.pos, true,
        //  result
        //)
        //loop.inner += resultVar.definition
        ReductionTotalUpdate(result)//Var())
      }
    }
    trait Reductoid extends StreamTransformer {
      //def loopSkipsFirst: Boolean
      //def isLeft: Boolean
      //def initialValue: Option[Tree]
      def op: String = toString
      
      case class ReductionTotalUpdate(newTotalValue: Tree, conditionOpt: Option[Tree] = None)
      
      val initialValue: Tree
      def hasInitialValue(value: StreamValue) = 
        initialValue != null || value.extraFirstValue != None
        
      def updateTotalWithValue(total: TreeGen, value: TreeGen)(implicit loop: Loop): ReductionTotalUpdate
      
      //def hasInitialValue = false
      
      override def consumesExtraFirstValue = true
      def createInitialValue(value: StreamValue)(implicit loop: Loop): Tree = {
        //println("value.extraFirstValue = " + value.extraFirstValue)
        (Option(initialValue), value.extraFirstValue) match {
          case (Some(i), Some(e)) =>
            updateTotalWithValue(() => i, () => e()).newTotalValue
          case (None, Some(e)) =>
            e()
          case (Some(i), None) =>
            i
          case (None, None) =>
            newDefaultValue(value.tpe)
        }
      }
        
      def transformedValue(value: StreamValue, totalVar: VarDef, initVar: VarDef)(implicit loop: Loop): StreamValue
      
      override def transform(value: StreamValue)(implicit loop: Loop): StreamValue = {
        import loop.{ unit, currentOwner }
        
        val initVar = newVariable(unit, op + "$", currentOwner, tree.pos, true,
          createInitialValue(value)
        )
        val totVar = newVariable(unit, op + "$", currentOwner, tree.pos, true,
          initVar()
        )
        val hasTotVar = newVariable(unit, "has" + op + "$", currentOwner, tree.pos, true,
          newBool(hasInitialValue(value))
        )
        loop.preOuter += initVar.definition
        loop.preOuter += totVar.definition
        loop.preOuter += hasTotVar.definition
        
        val ReductionTotalUpdate(newTotalValue, conditionOpt) =
          updateTotalWithValue(totVar.identGen, value.value)
          
        val totAssign = newAssign(totVar, newTotalValue)
        
        val update = 
          conditionOpt.map(newIf(_, totAssign)).getOrElse(totAssign)
        
        loop.inner += (
          if (!hasInitialValue(value))
            newIf(
              boolNot(hasTotVar()),
              Block(
                Assign(hasTotVar(), newBool(true)),
                newAssign(totVar, value.value()),
                newUnit
              ),
              update
            )
          else
            update
        )
        
        loop.postOuter += totVar()
        
        transformedValue(
          if (producesExtraFirstValue)
            value.copy(extraFirstValue = Some(new DefaultTupleValue(initVar)))
          else
            value, 
          totVar,
          initVar
        )
      }
    }
    
    case class FoldOp(tree: Tree, f: Tree, override val initialValue: Tree, isLeft: Boolean) extends TraversalOpType with ScalarReduction with Function2Reduction {
      override def toString = "fold" + (if (isLeft) "Left" else "Right")
      override val needsInitialValue = true
      override val needsFunction: Boolean = true
      
      override def consumesExtraFirstValue = true
      
      override def order = SameOrder
    }
    case class ScanOp(tree: Tree, f: Tree, override val initialValue: Tree, isLeft: Boolean) extends TraversalOpType with Function2Reduction {
      override def toString = "scan" + (if (isLeft) "Left" else "Right")
      override val needsInitialValue = true
      override val needsFunction: Boolean = true
      
      override def consumesExtraFirstValue = true
      override def producesExtraFirstValue = true
      
      override def transformedValue(value: StreamValue, totalVar: VarDef, initVar: VarDef)(implicit loop: Loop): StreamValue = {
        value.copy(
          value = new DefaultTupleValue(totalVar),
          extraFirstValue = Some(new DefaultTupleValue(initVar))
        )
      }
        
      override def order = SameOrder
    }
    case class ReduceOp(tree: Tree, f: Tree, isLeft: Boolean) extends TraversalOpType with ScalarReduction with Function2Reduction {
      override def toString = "reduce" + (if (isLeft) "Left" else "Right")
      override val needsFunction: Boolean = true
      override val loopSkipsFirst = true
      override val initialValue = null
      
      override def consumesExtraFirstValue = true
      override def order = SameOrder
    }
    case class SumOp(tree: Tree) extends TraversalOpType with SideEffectFreeScalarReduction {
      override def toString = "sum"
      override val f = null
      override def order = Unordered
      override val initialValue = null
      
      override def updateTotalWithValue(totIdentGen: TreeGen, valueIdentGen: TreeGen)(implicit loop: Loop): ReductionTotalUpdate = {
        val totIdent = totIdentGen()
        val valueIdent = valueIdentGen()
        ReductionTotalUpdate(binOp(totIdent, totIdent.tpe.member(nme.PLUS), valueIdent))
      }
    }
    case class ProductOp(tree: Tree) extends TraversalOpType with SideEffectFreeScalarReduction {
      override def toString = "product"
      override val f = null
      override def order = Unordered
      override val initialValue = null
      
      override def updateTotalWithValue(totIdentGen: TreeGen, valueIdentGen: TreeGen)(implicit loop: Loop): ReductionTotalUpdate = {
        val totIdent = totIdentGen()
        val valueIdent = valueIdentGen()
        ReductionTotalUpdate(binOp(totIdent, totIdent.tpe.member(nme.STAR), valueIdent))
      }
    }
    case class CountOp(tree: Tree, f: Tree) extends TraversalOpType with Function1Transformer {
      override def toString = "count"
      override val needsFunction: Boolean = true
      
      override def order = Unordered
      override def resultKind = ScalarResult
      override def transform(value: StreamValue)(implicit loop: Loop): StreamValue = {
        import loop.{ unit, currentOwner }
        val countVar = newVariable(unit, "count$", currentOwner, tree.pos, true, newInt(0))
        loop.preOuter += countVar.definition
        loop.inner += 
          newIf(
            transformedFunc(value),
            incrementIntVar(countVar.identGen)
          )
        loop.postOuter += countVar()
        null
      }
    }
    case class MinOp(tree: Tree) extends TraversalOpType with SideEffectFreeScalarReduction {
      override def toString = "min"
      override val loopSkipsFirst = true
      override val f = null
      override def order = Unordered
      override val initialValue = null
      
      override def updateTotalWithValue(totIdentGen: TreeGen, valueIdentGen: TreeGen)(implicit loop: Loop): ReductionTotalUpdate = {
        val totIdent = totIdentGen()
        val valueIdent = valueIdentGen()
        ReductionTotalUpdate(valueIdent, conditionOpt = Some(binOp(valueIdent, totIdent.tpe.member(nme.LT), totIdent)))
      }
    }
    case class MaxOp(tree: Tree) extends TraversalOpType with SideEffectFreeScalarReduction {
      override def toString = "max"
      override val loopSkipsFirst = true
      override val f = null
      override def order = Unordered
      override val initialValue = null
      
      override def updateTotalWithValue(totIdentGen: TreeGen, valueIdentGen: TreeGen)(implicit loop: Loop): ReductionTotalUpdate = {
        val totIdent = totIdentGen()
        val valueIdent = valueIdentGen()
        ReductionTotalUpdate(valueIdent, conditionOpt = Some(binOp(valueIdent, totIdent.tpe.member(nme.GT), totIdent)))
      }
    }
    case class FilterOp(tree: Tree, f: Tree, not: Boolean) extends TraversalOpType with Function1Transformer {
      override def toString = if (not) "filterNot" else "filter"
      override def order = Unordered
      override def transform(value: StreamValue)(implicit loop: Loop): StreamValue = {
        val condVar = newVariable(loop.unit, "cond$", loop.currentOwner, loop.pos, false, transformedFunc(value))
        loop.inner += condVar.definition
          
        loop.innerIf(() => {
          if (not)
            boolNot(condVar())
          else
            condVar()
        })
        
        value.withoutSizeInfo
      }
    }
    case class FilterWhileOp(tree: Tree, f: Tree, take: Boolean) extends TraversalOpType with Function1Transformer {
      override def toString = if (take) "takeWhile" else "dropWhile"
      
      override def order = SameOrder
      override def transform(value: StreamValue)(implicit loop: Loop): StreamValue = {
        import loop.{ unit, currentOwner }
        
        val passedVar = newVariable(unit, "passed$", currentOwner, tree.pos, true, newBool(false) )
        loop.preOuter += passedVar.definition
        
        if (take)
          loop.tests += boolNot(passedVar())
          
        val cond = boolNot(transformedFunc(value))
        
        if (take) {
          loop.inner += newAssign(passedVar, cond)
          loop.innerIf(() => boolNot(passedVar()))
        } else {
          loop.innerIf(() => 
            boolOr(
              passedVar(),
              Block(
                List(
                  Assign(
                    passedVar(),
                    cond
                  ).setType(UnitClass.tpe)
                ),
                passedVar()
              ).setType(BooleanClass.tpe)
            )
          )
        }
        
        value.withoutSizeInfo
      }
    }
    case class MapOp(tree: Tree, f: Tree, canBuildFrom: Tree) extends TraversalOpType with Function1Transformer {
      override def toString = "map"
      override def order = Unordered
      override def transform(value: StreamValue)(implicit loop: Loop): StreamValue = {
        val mappedVar = newVariable(loop.unit, "mapped$", loop.currentOwner, loop.pos, false, transformedFunc(value))
        loop.inner += mappedVar.definition
        
        value.copy(value = new DefaultTupleValue(mappedVar))
      }
    }
    
    case class CollectOp(tree: Tree, f: Tree, canBuildFrom: Tree) extends TraversalOpType {
      override def toString = "collect"
    }
    case class UpdateAllOp(tree: Tree, f: Tree) extends TraversalOpType {
      override def toString = "update"
    }
    case class ForeachOp(tree: Tree, f: Tree) extends TraversalOpType  with Function1Transformer {
      override def toString = "foreach"
      override def order = SameOrder
      override def resultKind = NoResult
      override def transform(value: StreamValue)(implicit loop: Loop): StreamValue = {
        loop.inner += transformedFunc(value)
        null
      }
    }
    case class AllOrSomeOp(tree: Tree, f: Tree, all: Boolean) extends TraversalOpType with Function1Transformer {
      override def toString = if (all) "forall" else "exists"
      
      override def order = Unordered
      override def resultKind = ScalarResult
      override def transform(value: StreamValue)(implicit loop: Loop): StreamValue = {
        import loop.{ unit, currentOwner }
        
        val hasTrueVar = newVariable(unit, "hasTrue$", currentOwner, tree.pos, true, newBool(all))
        val countVar = newVariable(unit, "count$", currentOwner, tree.pos, true, newInt(0))
        loop.preOuter += hasTrueVar.definition
        loop.tests += (
          if (all)
            hasTrueVar()
          else
            boolNot(hasTrueVar())
        )
        loop.inner += newAssign(hasTrueVar, transformedFunc(value))
        loop.postOuter += hasTrueVar()
        null
      }
    }
    case class FindOp(tree: Tree, f: Tree) extends TraversalOpType {
      override def toString = "find"
    }
    case class ReverseOp(tree: Tree) extends TraversalOpType with StreamTransformer with SideEffectFreeStreamComponent {
      override def toString = "reverse"
      override val f = null
      
      override def order = ReverseOrder
      
      override def transform(value: StreamValue)(implicit loop: Loop): StreamValue = 
        value.copy(valueIndex = (value.valueIndex, value.valuesCount) match {
          case (Some(i), Some(n)) =>
            Some(() => intSub(intSub(n(), i()), newInt(1)))
          case _ =>
            None
        })
    }
    case class ZipOp(tree: Tree, zippedCollection: Tree) extends TraversalOpType {
      override def toString = "zip"
      override val f = null
    }

    abstract class ToCollectionOp(val colType: ColType) extends TraversalOpType with StreamTransformer with SideEffectFreeStreamComponent {
      override def toString = "to" + colType
      override val f = null
      override def transform(value: StreamValue)(implicit loop: Loop): StreamValue = 
        value
        
      override def order = SameOrder
    }
    case class ToListOp(tree: Tree) extends ToCollectionOp(ListType) with CanCreateListSink
    case class ToSeqOp(tree: Tree) extends ToCollectionOp(SeqType) with CanCreateListSink
    case class ToArrayOp(tree: Tree) extends ToCollectionOp(ArrayType) with CanCreateArraySink
    //case class ToOptionOp(tree: Tree, tpe: Type) extends ToCollectionOp(ListType) with CanCreateOptionSink
    case class ToVectorOp(tree: Tree) extends ToCollectionOp(VectorType) with CanCreateVectorSink
    case class ToIndexedSeqOp(tree: Tree) extends ToCollectionOp(IndexedSeqType) with CanCreateVectorSink
    
    case class ZipWithIndexOp(tree: Tree) extends TraversalOpType {
      override def toString = "zipWithIndex"
      override val f = null
    }
  }
}
