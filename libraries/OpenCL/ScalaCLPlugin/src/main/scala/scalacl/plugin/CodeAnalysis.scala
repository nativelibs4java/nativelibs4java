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

  def getUnknownSymbolReferences(tree: Tree, filter: Tree => Boolean = _ => true, preKnownSymbols: Set[Symbol] = Set()): Seq[Tree] = {
    import collection._
    val defines = new mutable.HashSet[Symbol]
    defines ++= preKnownSymbols
    
    new ForeachTreeTraverser(t => t match {
      case _: DefTree =>
        defines += t.symbol
      case _ =>
    }).traverse(tree)

    val unknown = new ArrayBuffer[Tree]
    new ForeachTreeTraverser(t => t match {
      case _: RefTree =>
        if (!defines.contains(t.symbol) && filter(t))
          unknown += t
      case _ =>
    }).traverse(tree)

    unknown.toSeq
  }
  
  def isSideEffectFree(tree: Tree) = {
    val analyzer = new SideEffectsAnalysis(tree)
    analyzer.traverse(tree)
    analyzer.isSideEffectFree
  }
  class SideEffectsAnalysis(tree: Tree, preKnownSymbols: Set[Symbol] = Set()) extends Traverser {
    protected val unknownSymbols = 
      getUnknownSymbolReferences(tree, preKnownSymbols = preKnownSymbols)
    
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
    var isSideEffectFree = true
    var sideEffectTrees = Seq[Tree]()
    
    protected def hasSideEffects(tree: Tree): Unit = {
      sideEffectTrees :+= tree
      isSideEffectFree = false
      //println("Has side effects : " + tree + " (sym = " + tree.symbol + ", tpe = " + tree.tpe + ", tpe.sym = " + tree.tpe.typeSymbol + ")\n\t" + nodeToString(tree))
    }
    override def traverse(tree: Tree) = {
      super.traverse(tree)
      //println("TRAVERSING " + tree)
      tree match {
        // TODO accept accesses to non-lazy vals
        case (_: New) =>
          hasSideEffects(tree) // TODO refine this !!!
        case Select(target, methodName) =>//if target.symbol.isInstanceOf[MethodSymbol] =>
          if (target.symbol.isInstanceOf[MethodSymbol])
            if (!isSideEffectFreeMethod(target.symbol.asInstanceOf[MethodSymbol]))
              hasSideEffects(tree)
        case Assign(lhs, rhs) =>
          //println("Found assign : " + tree)
          if (!isKnownTerm(lhs.symbol)) 
            hasSideEffects(tree)
          //else
          //  println("Is known symbol : " + lhs.symbol)
        case _ =>
      }
    }
  }
}
