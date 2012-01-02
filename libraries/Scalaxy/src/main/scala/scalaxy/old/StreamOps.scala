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
package com.nativelibs4java.scalaxy ; package old ; 
import common._
import pluginBase._

import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.Global

@deprecated("Not tested for long, please re-read")
trait StreamOps extends TreeBuilders {
  this: PluginComponent with WithOptions =>
  
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
  object TuplePath2 {
    def unapply(tree: Tree): Option[IntPath[Tree]] = tree match {
      case TupleField(tf) =>
        tf.tuple match {
          case TuplePath2(IntPath(originalTuple, path)) =>
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
        case ValDef(mods, name, tpt, TuplePath2(IntPath(target, subPath))) =>
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
        val indexVar = newVariable(unit, "index$", currentOwner, NoPosition, true, newInt(0))
        StreamOpResult(
          List(indexVar.definition),
          Nil,
          List(incrementIntVar(indexVar, newInt(1))),
          resultsIdentGens(indexVar),
          indexVar
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