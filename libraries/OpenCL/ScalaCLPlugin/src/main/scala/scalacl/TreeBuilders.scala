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
package scalacl

import java.io.File
import scala.reflect.generic.{Constants, Names, Trees, Types, Symbols}

import scala.tools.nsc.CompilationUnits
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.symtab.Definitions
import scala.tools.nsc.transform.TypingTransformers

trait TreeBuilders
extends MiscMatchers
   with TypingTransformers
{
  this: PluginComponent =>
  
  import global._
  import global.definitions._
  import scala.tools.nsc.symtab.Flags._
  import typer.{typed, atOwner}    // methods to type trees


  /// print a message only if the operation succeeded :
  def msg[V](unit: CompilationUnit, pos: Position, text: String)(v: => V): V = {
    val r = v
    unit.comment(pos, text)
    println("[scalacl] " + new File(pos.source.path).getName + ":" + pos.line + " " + text)
    r
  }

  type TreeGen = () => Tree
  def replaceOccurrences(tree: Tree, mappings: Map[Name, TreeGen], unit: CompilationUnit) = new TypingTransformer(unit) {
    override def transform(tree: Tree): Tree = //typed {
      tree match {
        case Ident(n) =>
          mappings.get(n).map(_()).getOrElse(super.transform(tree))
        case _ =>
          //if (tree.symbol != null && tree.symbol.ownerChain.exists(_.isMethod))
          //  println("Found method symbol that's suspect: " + tree.symbol.ownerChain + " for " + tree)
          super.transform(tree)
      }
    //}
  }.transform(tree)

  def newApply(pos: Position, array: => Tree, index: => Tree) = {
    val a = array
    assert(a.tpe != null)
    typed {
      atPos(pos) {
        Apply(
          Select(
            a,
            N("apply")
          ).setSymbol(getMember(a.tpe.typeSymbol, nme.apply)),
          List(index)
        )
      }
    }
  }
  def newUpdate(pos: Position, array: => Tree, index: => Tree, value: => Tree) = {
    val a = array
    assert(a.tpe != null)
    typed {
      atPos(pos) {
        Apply(
          Select(
            a,
            N("update")
          ).setSymbol(getMember(a.tpe.typeSymbol, nme.update)),
          List(index, value)
        )
      }
    }
  }

  def binOp(a: Tree, op: Symbol, b: Tree) = typed {
    Apply(Select(a, op), List(b))
  }
  def ident(sym: Symbol, n: Name) = {
    val v = Ident(n)
    v.symbol = sym
    v.tpe = sym.tpe
    v
  }

  def intAdd(a: => Tree, b: => Tree) =
    binOp(a, IntClass.tpe.member(nme.PLUS), b)

  def intSub(a: => Tree, b: => Tree) =
    binOp(a, IntClass.tpe.member(nme.MINUS), b)

  def incrementIntVar(identGen: IdentGen, value: Tree) =
    Assign(
      identGen(),
      intAdd(identGen(), value)
    ).setType(UnitClass.tpe)

  def decrementIntVar(identGen: IdentGen, value: Tree) =
    Assign(
      identGen(),
      intSub(identGen(), value)
    ).setType(UnitClass.tpe)

  def whileLoop(owner: Symbol, unit: CompilationUnit, tree: Tree, cond: Tree, body: Tree) = {
    val lab = unit.fresh.newName(body.pos, "while$")
    val labTyp = MethodType(Nil, UnitClass.tpe)
    val labSym = owner.newLabel(tree.pos, N(lab)) setInfo labTyp

    typed {
      LabelDef(
        N(lab),
        Nil,
        If(
          cond,
          Block(
            if (body == null)
              Nil
            else
              List(body),
            Apply(
              ident(labSym, lab),
              Nil
            )
          ),
          newUnit
        )
      ).setSymbol(labSym)
    }
  }

  type IdentGen = () => Ident

  def newInt(v: Int) = 
    Literal(Constant(v)).setType(IntClass.tpe)

  def newUnit() = 
    Literal(Constant()).setType(UnitClass.tpe)

  def newVariable(
    unit: CompilationUnit,
    prefix: String,
    symbolOwner: Symbol,
    pos: Position,
    mutable: Boolean,
    initialValue: Tree
  ): (IdentGen, Symbol, ValDef) = {
    var tpe = initialValue.tpe
    if (tpe.isInstanceOf[ConstantType])
      tpe = tpe.widen
    val name = unit.fresh.newName(pos, prefix)
    val sym = (
      if (mutable)
        symbolOwner.newVariable(pos, name)
      else
        symbolOwner.newValue(pos, name)
    ) setInfo tpe
    (() => ident(sym, name), sym, ValDef(Modifiers(if (mutable) MUTABLE else 0), name, TypeTree(tpe), initialValue).setType(tpe).setSymbol(sym))
  }
}

