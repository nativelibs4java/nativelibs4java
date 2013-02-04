/*
 * ScalaCL - putting Scala on the GPU with JavaCL / OpenCL
 * http://scalacl.googlecode.com/
 *
 * Copyright (c) 2009-2013, Olivier Chafik (http://ochafik.com/)
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
package scalaxy.common

import scala.collection.immutable.Stack
import scala.collection.mutable.ArrayBuffer

import scala.reflect.NameTransformer
import scala.reflect.api.Universe

object HasSideEffects

trait CodeAnalysis
extends MiscMatchers
   with TreeBuilders
   with TupleAnalysis
{
  val global: Universe
  import global._
  import definitions._
  
  def getTreeChildren(tree: Tree): Seq[Tree] = {
    var out = ArrayBuffer[Tree]()
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
  
  case class SymbolsInfo(
    tree: Tree, 
    symbolDefinitions: Seq[DefTree], 
    definedSymbols: Set[Symbol],
    preKnownSymbols: Set[Symbol], 
    unknownReferences: Seq[RefTree] 
  ) {
    //lazy val definedSymbols = symbolDefinitions.map(_.symbol).toSet
    lazy val unknownReferencesBySymbol = unknownReferences.groupBy(_.symbol)
    lazy val unknownSymbols = unknownReferencesBySymbol.keys.toSet
      
  }
  
  def getUnknownSymbolInfo(tree: Tree, filter: Tree => Boolean = _ => true, preKnownSymbols: Set[Symbol] = Set()): SymbolsInfo = {
    val symbolDefinitions = 
      getSymbolDefinitions(tree)
      
    val definedSymbols = 
      symbolDefinitions.map(_.symbol).toSet
    
    val unknownReferences = 
      getRawUnknownSymbolReferences(tree, s => definedSymbols.contains(s) || preKnownSymbols.contains(s), filter)
    
    SymbolsInfo(tree, symbolDefinitions, definedSymbols, preKnownSymbols, unknownReferences)
  }
  
  def isSideEffectFree(tree: Tree) =
    getSideEffects(tree).isEmpty
    
  def getSideEffects(tree: Tree) =
    createSideEffectsEvaluator(tree, cached = false).evaluate(tree)
  
  
  protected def createSideEffectsEvaluator(tree: Tree, cached: Boolean = true, preKnownSymbols: Set[Symbol] = Set()) = {
    //println("Creating original SideEffectsEvaluator")
    new SideEffectsEvaluator(tree, cached, preKnownSymbols)
  }
  
  type SideEffects = Seq[Tree]
  class SideEffectsEvaluator(tree: Tree, cached: Boolean = true, preKnownSymbols: Set[Symbol] = Set()) 
  extends SeqEvaluator
  {
    protected val cache = collection.mutable.Map[Tree, SideEffects]()
    
    protected val symbolsInfo = 
      getUnknownSymbolInfo(tree, preKnownSymbols = preKnownSymbols)
      
    protected val unknownSymbols = 
      symbolsInfo.unknownSymbols
    
    //println("#\n# unknownSymbols = " + unknownSymbols + "\n#")
    
    protected def isKnownTerm(symbol: Symbol) =
      symbolsInfo.definedSymbols.contains(symbol) ||
      !unknownSymbols.contains(symbol)
      
    protected def isSideEffectFreeMethod(target: Tree, symbol: MethodSymbol): Boolean = {
      val owner = symbol.owner
      val name = symbol.name
      
      symbol.isGetter ||
      isSideEffectFreeOwner(target.tpe.typeSymbol) || 
      isSideEffectFreeOwner(owner) ||
      owner != null && {
        name == (applyName: Name) && {
          ArrayClass == owner ||
          SeqClass == owner ||
          {
            val ownerStr = owner.toString
            ownerStr == ArrayModule.toString ||
            ownerStr == SeqModule.toString ||
            {
              if (verbose)
                println("Apply method not recognized as side-effect-free with owner " + owner + " : " + symbol)
              false 
            }
          }
        }
      }
    }
    
    private lazy val sideEffectFreeOwnerSymbols: Set[Symbol] = Set(
      StringClass,
      StringOpsClass,
      IntClass,
      ShortClass,
      LongClass,
      ByteClass,
      CharClass,
      BooleanClass,
      DoubleClass,
      IntClass,
      PredefModule,
      ScalaMathPackage,
      ScalaMathPackageClass,
      ScalaMathCommonClass,
      SeqClass,
      VectorClass,
      ListClass,
      IndexedSeqClass
    )
    
    protected def isSideEffectFreeOwner(symbol: Symbol): Boolean = {
      RichWrappers.contains(symbol) ||
      sideEffectFreeOwnerSymbols.contains(symbol)
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
      val sym = tree.symbol
      tree match {
        // TODO accept accesses to non-lazy vals
        case (_: New) =>
          //println("That was a new : " + tree)
          if (isPureCaseClass(tree.tpe))
            Seq()
          else
            Seq(tree)
        case This(_) | Select(_, SELF | THIS | thisName() | superName() | nme.CONSTRUCTOR) =>
          //println("That was a this : " + tree)
          Seq()
        case Select(TupleSelect(), applyName()) =>
          Seq()
        case Select(TreeWithType(_, TypeRef(_, c, List(_))), applyName()) if c == ArrayClass =>
          Seq()
        case Select(target, methodName) =>//if target.symbol.isInstanceOf[MethodSymbol] =>
          //val msg = "That was a select (" + tree + " @ " + tree.symbol + ": " + tree.symbol.getClass.getName + ") : \n\t" + tree
          //println(msg)
          //global.warning(msg)
          if (isSideEffectFreeOwner(sym))
            Seq()
          else if (isPureCaseClass(target.tpe))
            Seq()
          else if (!sym.isMethod)
            Seq(tree)
          else {
            val ms = sym.asMethod
            if (isSideEffectFreeMethod(target, ms))
              Seq()
            else
              Seq(tree)
          }
        case Assign(lhs, rhs) =>
          //println("That was an assign : " + tree + " on symbol " + lhs.symbol + "\n\tSymbols info :\n\t" + symbolsInfo.toString)
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
