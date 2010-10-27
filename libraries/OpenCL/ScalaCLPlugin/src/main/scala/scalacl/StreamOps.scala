/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package scalacl

import scala.tools.nsc.Global

trait StreamOps extends TreeBuilders {
  val global: Global
  import global._

  type ArgTuplePath = IntPath[Int]

  class TupleField(val tuple: Tree, val fieldIndex: Int)
  object TupleField {
    val rx = "_(\\d+)".r
    def unapply(tree: Tree) = tree match {
      case Select(target, fieldName) =>
        fieldName.toString match {
          case rx(n) =>
            if (target.symbol.owner.toString.matches("class Tuple\\d+"))
              Some(new TupleField(target, n.toInt - 1))
            else
              None
          case _ =>
            None
        }
      case _ =>
        None
    }
  }
  case class IntPath[T](target: T, path: List[Int]) {
    def appendPath(postPath: List[Int]) = new IntPath[T](target, path ++ postPath)
    def prependPath(prePath: List[Int]) = new IntPath[T](target, prePath ++ path)
    def map[V](f: T => V) = new IntPath[V](f(target), path)
  }
  object TuplePath {
    def unapply(tree: Tree): Option[IntPath[Tree]] = tree match {
      case TupleField(tf) =>
        tf.tuple match {
          case TuplePath(IntPath(originalTuple, path)) =>
            Some(new IntPath[Tree](originalTuple, path ++ List(tf.fieldIndex)))
          case _ =>
            Some(new IntPath[Tree](tf.tuple, List(tf.fieldIndex)))
        }
      case _ =>
        None
    }
  }
  class ArgTuplePathsHarvester(var defs: Map[Symbol, ArgTuplePath]) extends Traverser {
    override def traverse(tree: Tree) = {
      tree match {
        case ValDef(mods, name, tpt, TuplePath(IntPath(target, subPath))) =>
          defs.get(target.symbol) match {
            case Some(path) =>
              defs += target.symbol -> path.appendPath(subPath)
            case None =>
          }
        case _ =>
      }
      super.traverse(tree)
    }
    def apply(tree: Tree) = {
      this.traverse(tree)
      defs
    }
  }

  trait StreamOp {

    val isTransform: Boolean
    val isFilter: Boolean
    val argSymbols: Array[Symbol] = Array()
    val body: Tree = null

    def toTree(unit: CompilationUnit, currentOwner: Symbol, argsIdentGens: Map[ArgTuplePath, TreeGen], zippedIndexGen: TreeGen, resultUses: Set[ArgTuplePath]): StreamOpResult

    lazy val tupleUses: Map[Symbol, ArgTuplePath] = {
      val map = argSymbols.zipWithIndex.map({ case (argSymbol, argIndex) => argSymbol -> new ArgTuplePath(argIndex, Nil) }).toMap
      new ArgTuplePathsHarvester(map)(body)
    }
  }
  case class StreamOpResult(
    predefs: List[Tree],
    innerContentsPre: List[Tree],
    innerContentsPost: List[Tree],
    resultIdentGens: Map[ArgTuplePath, TreeGen],
    resultZippedIndexGen: TreeGen
  )
  class FilterStreamOp(not: Boolean, arg: ValDef, override val body: Tree) extends StreamOp {
    override val argSymbols = Array(arg.symbol)
    override val isTransform = false
    override val isFilter = true
    override def toTree(unit: CompilationUnit, currentOwner: Symbol, argsIdentGens: Map[ArgTuplePath, TreeGen], zippedIndexGen: TreeGen, resultUses: Set[ArgTuplePath]) = {
      null
    }
  }
  class ZipWithIndexStreamOp(not: Boolean, arg: ValDef, override val body: Tree) extends StreamOp {
    override val argSymbols = Array(arg.symbol)
    override val isTransform = true
    override val isFilter = false
    override def toTree(unit: CompilationUnit, currentOwner: Symbol, argsIdentGens: Map[ArgTuplePath, TreeGen], zippedIndexGen: TreeGen, resultUses: Set[ArgTuplePath]) = {
      val resultsIdentGens: TreeGen => Map[ArgTuplePath, TreeGen] = (g: TreeGen) => argsIdentGens.map { case (path, identGen) => (path.prependPath(List(0)), identGen) } ++ Map(new ArgTuplePath(0, List(1)) -> g)
      if (zippedIndexGen != null) {
        StreamOpResult(Nil, Nil, Nil, resultsIdentGens(zippedIndexGen), zippedIndexGen)
      } else {
        val (indexIdentGen, indexSym, indexDef) = newVariable(unit, "index$", currentOwner, NoPosition, true, newInt(0))
        StreamOpResult(
          List(indexDef),
          Nil,
          List(incrementIntVar(indexIdentGen, newInt(1))),
          resultsIdentGens(indexIdentGen),
          indexIdentGen
        )
      }
    }
  }
  class LinkedStreamOp(op: StreamOp, next: StreamOp) extends StreamOp {
    override val isTransform = {
      if (op.isTransform || next == null)
        true
      else
        next.isTransform
    }
    override val isFilter = {
      if (op.isFilter || next == null)
        true
      else
        next.isFilter
    }
    override def toTree(unit: CompilationUnit, currentOwner: Symbol, argsIdentGens: Map[ArgTuplePath, TreeGen], zippedIndexGen: TreeGen, resultUses: Set[ArgTuplePath]): StreamOpResult = {
      val res = op.toTree(unit, currentOwner, argsIdentGens, zippedIndexGen, if (next == null) resultUses else next.tupleUses.values.toSet)
      if (next == null)
        res
      else {
        val StreamOpResult(predefs, innerContents, outerContents, resultIdentGens, zippedIndexGen) = res
        val StreamOpResult(nextPredefs, nextInnerContents, nextOuterContents, nextResultIdentGens, nextZippedIndexGen) = 
          next.toTree(unit, currentOwner, resultIdentGens, if (op.isFilter) null else zippedIndexGen, resultUses)
        StreamOpResult(predefs ++ nextPredefs, innerContents ++ nextInnerContents, outerContents ++ nextOuterContents, nextResultIdentGens, nextZippedIndexGen)
      }
    }
    override lazy val tupleUses = {
      val uses = op.tupleUses
      val nextUses = if (op.isTransform || next == null)
        Map()
      else
        next.tupleUses

      uses ++ nextUses
    }
  }
}