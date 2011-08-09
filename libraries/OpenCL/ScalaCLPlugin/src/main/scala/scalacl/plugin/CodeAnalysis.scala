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
package scalacl ; package plugin

import scala.collection.immutable.Stack
import scala.collection.mutable.ArrayBuffer
import scala.reflect.NameTransformer
import scala.reflect.generic.{Names, Trees, Types, Constants, Universe}
import scala.tools.nsc.Global
import tools.nsc.plugins.PluginComponent

object HasSideEffects

trait CodeAnalysis
extends MiscMatchers
   with TreeBuilders
   with TupleAnalysis
{
  this: PluginComponent with WithOptions =>

  import global._
  import global.definitions._
  import scala.tools.nsc.symtab.Flags._
  import typer.{typed}    // methods to type trees
  import CODE._
  
  import gen._
  import scala.tools.nsc.symtab.Flags._
  import analyzer.{SearchResult, ImplicitSearch}

  
  def getTreeChildren(tree: Tree): Seq[Tree] = {
    var out = collection.mutable.ArrayBuffer[Tree]()
    new Traverser {
      var level = 0
      override def traverse(tree: Tree) = {
        if (level == 1)
          out += tree
        else {
          level += 1
          super.traverse(tree)
          level -= 1
        }
      }
    }.traverse(tree)
    out.toArray.toSeq
  }
  
  abstract class Evaluator[@specialized(Boolean, Int) R](defaultValue: R, combine: (R, R) => R)
  extends Traverser {
    def evaluate(tree: Tree): R =
      if (tree eq EmptyTree)
        defaultValue
      else
        getTreeChildren(tree).map(evaluate(_)).foldLeft(defaultValue)(combine)
  }
  
  abstract class BooleanEvaluator extends Evaluator[Boolean](false, _ || _)
  abstract class IntEvaluator extends Evaluator[Int](0, _ + _)
  abstract class SeqEvaluator extends Evaluator[Seq[Tree]](Seq(), _ ++ _)
  
  def filterTree[V](tree: Tree)(f: PartialFunction[Tree, V]): Seq[V] = {
    val out = new ArrayBuffer[V]
    new Traverser {
      override def traverse(t: Tree) {
        if (f.isDefinedAt(t)) 
          out += f(t)
        super.traverse(t)
      }
    }.traverse(tree)
    out
  }
  
  def getSymbolDefinitions(tree: Tree): Seq[DefTree] = 
    filterTree(tree) { 
      case t: DefTree => t 
    }
  
  def getRawUnknownSymbolReferences(tree: Tree, isKnownSymbol: Symbol => Boolean, accept: Tree => Boolean): Seq[RefTree] =
    filterTree(tree) {
      case t: RefTree if !isKnownSymbol(t.symbol) && accept(t) => t
    }
  
  def getUnknownSymbolReferences(tree: Tree, filter: Tree => Boolean = _ => true, preKnownSymbols: Set[Symbol] = Set()): Seq[Tree] = {
    val symDefs = getSymbolDefinitions(tree)
    val syms = symDefs.map(_.symbol).toSet
    
    val unknown = getRawUnknownSymbolReferences(tree, s => syms.contains(s) || preKnownSymbols.contains(s), filter)
    unknown
  }
  
  def isSideEffectFree(tree: Tree) = {
    new SideEffectsEvaluator(tree, cached = false).evaluate(tree).isEmpty
  }
  
  type SideEffects = Seq[Tree]
  class SideEffectsEvaluator(tree: Tree, cached: Boolean = true, preKnownSymbols: Set[Symbol] = Set()) 
  extends SeqEvaluator
  {
    protected val cache = collection.mutable.Map[Tree, SideEffects]()
    
    protected val unknownSymbols: Set[Symbol] = 
      getUnknownSymbolReferences(tree, preKnownSymbols = preKnownSymbols).map(_.symbol).toSet
    
    protected def isKnownTerm(symbol: Symbol) = 
      !unknownSymbols.contains(symbol)
      
    protected def isSideEffectFreeMethod(symbol: MethodSymbol): Boolean = {
      val owner = symbol.owner
      val name = symbol.name
      isSideEffectFreeOwner(owner) || 
      name == (applyName: Name) && {
        owner == SeqModule ||
        owner == ArrayModule //||
        //owner == SetModule ||
        //owner == MapModule
      }
    }
    protected def isSideEffectFreeOwner(symbol: Symbol): Boolean = {
      symbol match {
        case IntClass | ShortClass | LongClass | ByteClass | CharClass | BooleanClass | DoubleClass | IntClass =>
          true
        case PredefModule =>
          true
        case ScalaMathPackage | ScalaMathPackageClass | ScalaMathCommonClass =>
          true
        case _ =>
          //println("NOT A SIDE-EFFECT-FREE OWNER : " + symbol)
          false
      }
    }
    def isPureCaseClass(tpe: Type) = 
      false // TODO 
    
    override def evaluate(tree: Tree) = { 
      if (cached)
        cache.getOrElseUpdate(tree, {
          uncachedEvaluation(tree)
        })
      else
        uncachedEvaluation(tree)
    }
    def uncachedEvaluation(tree: Tree) = {
      //println("EVALUATING " + tree)
      tree match {
        // TODO accept accesses to non-lazy vals
        case (_: New) =>
          if (isPureCaseClass(tree.tpe))
            Seq()
          else
            Seq(tree)
        case Select(target, methodName) =>//if target.symbol.isInstanceOf[MethodSymbol] =>
          if (isPureCaseClass(target.tpe))
            Seq()
          else if (!target.symbol.isInstanceOf[MethodSymbol])
            Seq(tree)
          else if (isSideEffectFreeMethod(target.symbol.asInstanceOf[MethodSymbol]))
            Seq()
          else
            Seq(tree)
        case Assign(lhs, rhs) =>
          //println("Found assign : " + tree)
          if (!isKnownTerm(lhs.symbol)) 
            Seq(tree)
          else
            Seq()
        case _ =>
          super.evaluate(tree)
      }
    }
  }
}
