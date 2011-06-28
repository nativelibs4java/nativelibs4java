/*
 * Created by IntelliJ IDEA.
 * User: ochafik
 * Date: 10/05/11
 * Time: 21:40
 */
package scalacl ; package plugin

import tools.nsc.plugins.PluginComponent
import tools.nsc.Global

trait TraversalOps extends PluginNames {
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
  object TraversalOps {

    case class Fold(f: Tree, isLeft: Boolean) extends TraversalOpType {
      override def toString = "fold" + (if (isLeft) "Left" else "Right")
      override val needsInitialValue = true
      override val needsFunction: Boolean = true
    }
    case class Scan(f: Tree, isLeft: Boolean) extends TraversalOpType {
      override def toString = "scan" + (if (isLeft) "Left" else "Right")
      override val needsInitialValue = true
      override val needsFunction: Boolean = true
    }
    case class Reduce(f: Tree, isLeft: Boolean) extends TraversalOpType {
      override def toString = "reduce" + (if (isLeft) "Left" else "Right")
      override val needsFunction: Boolean = true
      override val loopSkipsFirst = true
    }
    case object Sum extends TraversalOpType {
      override def toString = "sum"
      override val f = null
    }
    case class Count(f: Tree) extends TraversalOpType {
      override def toString = "count"
      override val needsFunction: Boolean = true
    }
    case object Min extends TraversalOpType {
      override def toString = "min"
      override val loopSkipsFirst = true
      override val f = null
    }
    case object Max extends TraversalOpType {
      override def toString = "max"
      override val loopSkipsFirst = true
      override val f = null
    }
    case class Filter(f: Tree, not: Boolean) extends TraversalOpType {
      override def toString = if (not) "filterNot" else "filter"
    }
    case class FilterWhile(f: Tree, take: Boolean) extends TraversalOpType {
      override def toString = if (take) "takeWhile" else "dropWhile"
    }
    case class Map(f: Tree, canBuildFrom: Tree) extends TraversalOpType {
      override def toString = "map"
    }
    case class Collect(f: Tree, canBuildFrom: Tree) extends TraversalOpType {
      override def toString = "collect"
    }
    case class Foreach(f: Tree) extends TraversalOpType {
      override def toString = "foreach"
    }
    case class AllOrSome(f: Tree, all: Boolean) extends TraversalOpType {
      override def toString = if (all) "forall" else "exists"
    }
    case class Find(f: Tree) extends TraversalOpType {
      override def toString = "find"
    }
    case object Reverse extends TraversalOpType {
      override def toString = "reverse"
      override val f = null
    }
    case class Zip(zippedCollection: Tree) extends TraversalOpType {
      override def toString = "zip"
      override val f = null
    }

    case class ToCollection(colType: ColType, tpe: Type) extends TraversalOpType {
      override def toString = "to" + colType
      override val f = null
    }
    case object ZipWithIndex extends TraversalOpType {
      override def toString = "zipWithIndex"
      override val f = null
    }
  }
}