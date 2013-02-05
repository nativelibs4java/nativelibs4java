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

import language.experimental.macros
import scala.reflect.macros.Context

import scala.reflect.runtime.{ universe => ru }
import scala.reflect.runtime.{ currentMirror => cm }
import scala.tools.reflect.ToolBox

trait WithMacroContext {
  
  val context: Context
  lazy val global = context.universe
  import global._
  import global.definitions._
  
  def verbose = true
  
  def withSymbol[T <: Tree](sym: Symbol, tpe: Type = NoType)(tree: T): T = {
    try {
      tree.symbol = sym
    } catch { case _: Throwable =>
      // TODO: remove this ugly stuff. 
    }
    if (tpe != NoType)
      tree.tpe = tpe
    tree
  }
  def typed[T <: Tree](tree: T): T = 
    context.typeCheck(tree.asInstanceOf[context.universe.Tree]).asInstanceOf[T]
    
  def inferImplicitValue(pt: Type): Tree =
    context.inferImplicitValue(pt.asInstanceOf[context.universe.Type]).asInstanceOf[Tree]
    
  def setInfo(sym: Symbol, tpe: Type): Symbol = {
    //sym.setInfo(tpe)
    sym
  }
    
  def setType(sym: Symbol, tpe: Type): Symbol = {
    //sym.tpe = tpe
    sym
  }
    
  def setType(tree: Tree, tpe: Type): Tree = {
    tree.tpe = tpe
    tree
  }
    
  def setPos(tree: Tree, pos: Position): Tree = { 
    tree.pos = pos
    tree
  }
  
  def fresh(s: String) = 
    context.fresh(s)
  
  def typeCheck(x: Expr[_]): Tree = 
    context.typeCheck(x.tree.asInstanceOf[context.universe.Tree]).asInstanceOf[Tree]
    
  def typeCheck(tree: Tree, pt: Type): Tree = {
    if (tree.tpe =:= pt)
      tree
    else
      context.typeCheck(
        tree.asInstanceOf[context.universe.Tree],
        pt.asInstanceOf[context.universe.Type]
      ).asInstanceOf[Tree]
  }
}
