/*
 * Created by IntelliJ IDEA.
 * User: ochafik
 * Date: 10/05/11
 * Time: 21:40
 */
package scalacl ; package plugin

import tools.nsc.plugins.PluginComponent
import tools.nsc.Global

trait StreamOps extends PluginNames with Streams {
  this: PluginComponent with WithOptions =>
  
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

    case class FoldOp(tree: Tree, f: Tree, isLeft: Boolean) extends TraversalOpType {
      override def toString = "fold" + (if (isLeft) "Left" else "Right")
      override val needsInitialValue = true
      override val needsFunction: Boolean = true
    }
    case class ScanOp(tree: Tree, f: Tree, isLeft: Boolean) extends TraversalOpType {
      override def toString = "scan" + (if (isLeft) "Left" else "Right")
      override val needsInitialValue = true
      override val needsFunction: Boolean = true
    }
    case class ReduceOp(tree: Tree, f: Tree, isLeft: Boolean) extends TraversalOpType {
      override def toString = "reduce" + (if (isLeft) "Left" else "Right")
      override val needsFunction: Boolean = true
      override val loopSkipsFirst = true
    }
    case class SumOp(tree: Tree) extends TraversalOpType {
      override def toString = "sum"
      override val f = null
    }
    case class CountOp(tree: Tree, f: Tree) extends TraversalOpType {
      override def toString = "count"
      override val needsFunction: Boolean = true
    }
    case class MinOp(tree: Tree) extends TraversalOpType {
      override def toString = "min"
      override val loopSkipsFirst = true
      override val f = null
    }
    case class MaxOp(tree: Tree) extends TraversalOpType {
      override def toString = "max"
      override val loopSkipsFirst = true
      override val f = null
    }
    case class FilterOp(tree: Tree, f: Tree, not: Boolean) extends TraversalOpType with StreamTransformer {
      override def toString = if (not) "filterNot" else "filter"
      override def order = Unordered
      override def transform(value: StreamValue)(implicit loop: Loop): StreamValue = {
        val Func(List(arg), body) = f
        
        val cond = replaceOccurrences(
          body,
          Map(
            arg.symbol -> (() => value.value())
          ),
          Map(f.symbol -> loop.currentOwner),
          Map(),
          loop.unit
        )
        val condVar = newVariable(loop.unit, "cond$", loop.currentOwner, loop.pos, false, cond)
        loop.inner += condVar.definition
          
        loop.innerIf(() => {
          if (not)
            boolNot(condVar())
          else
            condVar()
        })
        
        StreamValue(
          value.value/*value = new DefaultTupleValue(
            cond.tpe,
            condVar // TODO
          )*/
        )
      }
    }
    case class FilterWhileOp(tree: Tree, f: Tree, take: Boolean) extends TraversalOpType {
      override def toString = if (take) "takeWhile" else "dropWhile"
    }
    case class MapOp(tree: Tree, f: Tree, canBuildFrom: Tree) extends TraversalOpType with StreamTransformer {
      override def toString = "map"
      override def order = Unordered
      override def transform(value: StreamValue)(implicit loop: Loop): StreamValue = {
        val Func(List(arg), body) = f
        
        val mapped = replaceOccurrences(
          body,
          Map(
            arg.symbol -> (() => value.value())
          ),
          Map(f.symbol -> loop.currentOwner),
          Map(),
          loop.unit
        )
        val mappedVar = newVariable(loop.unit, "mapped$", loop.currentOwner, loop.pos, false, mapped)
        loop.inner += mappedVar.definition
        
        value.copy(value = new DefaultTupleValue(
          mapped.tpe,
          mappedVar // TODO
        ))
      }
    }
    
    case class CollectOp(tree: Tree, f: Tree, canBuildFrom: Tree) extends TraversalOpType {
      override def toString = "collect"
    }
    case class UpdateAllOp(tree: Tree, f: Tree) extends TraversalOpType {
      override def toString = "update"
    }
    case class ForeachOp(tree: Tree, f: Tree) extends TraversalOpType {
      override def toString = "foreach"
    }
    case class AllOrSomeOp(tree: Tree, f: Tree, all: Boolean) extends TraversalOpType {
      override def toString = if (all) "forall" else "exists"
    }
    case class FindOp(tree: Tree, f: Tree) extends TraversalOpType {
      override def toString = "find"
    }
    case class ReverseOp(tree: Tree) extends TraversalOpType {
      override def toString = "reverse"
      override val f = null
    }
    case class ZipOp(tree: Tree, zippedCollection: Tree) extends TraversalOpType {
      override def toString = "zip"
      override val f = null
    }

    case class ToCollectionOp(tree: Tree, colType: ColType, tpe: Type) extends TraversalOpType {
      override def toString = "to" + colType
      override val f = null
    }
    case class ZipWithIndexOp(tree: Tree) extends TraversalOpType {
      override def toString = "zipWithIndex"
      override val f = null
    }
  }
}
