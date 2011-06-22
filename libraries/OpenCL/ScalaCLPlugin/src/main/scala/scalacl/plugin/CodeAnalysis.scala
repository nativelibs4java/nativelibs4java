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

  def getUnknownSymbols(tree: Tree, filter: Tree => Boolean = _ => true, preKnownSymbols: Set[Symbol] = Set()) = {
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

    unknown.toArray
  }
}
