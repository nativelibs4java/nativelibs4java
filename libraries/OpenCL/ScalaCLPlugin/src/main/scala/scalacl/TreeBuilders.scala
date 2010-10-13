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

import scala.tools.nsc.plugins.PluginComponent
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
    val fileLine = new File(pos.source.path).getName + ":" + pos.line
    val prefix = "[scalacl] " + fileLine + " "
    try {
      val r = v
      unit.comment(pos, text)
      val str = prefix + text
      global.log(str)
      println(str)
      r
    } catch {
      case ex =>
        var str = 
          """An unexpected error occurred while attempting an optimization
  Attempted optimization : '"""+ text + """'
  You can skip this line with the following environment variable :
    SCALACL_SKIP=""" + fileLine

        if (ScalaCLPlugin.trace) {
          str += "\n\tError : " + ex
          
        } else {
          str += """
  To display the error and help debug the ScalaCL compiler plugin, please set the following environment variable :
    SCALACL_TRACE=1
  You can help by filing bugs here (with [ScalaCLPlugin] in the title) :
    http://code.google.com/p/nativelibs4java/issues/entry"""
        }
        str = prefix + str.replaceAll("\n", "\n" + prefix)

        global.warning(str)
        println(str)

        if (ScalaCLPlugin.trace)
          ex.printStackTrace
        throw ex
    }
  }

  type TreeGen = () => Tree

  def replaceOwners(sym: Symbol, ownerReplacements: Map[Symbol, Symbol]): Symbol = {
    if (sym == null || sym == NoSymbol || sym.owner == null || sym.owner == NoSymbol)
      sym
    else
      ownerReplacements.get(sym.owner) match {
        case Some(replacement) =>
          if (replacement == NoSymbol)
            sym
          else
            sym.cloneSymbol(replacement)
        case None =>
          val p = replaceOwners(sym.owner, ownerReplacements)
          if (sym.owner != p)
            sym.cloneSymbol(p)
          else
            sym
      }
  }
  def replaceOccurrences(tree: Tree, mappings: Map[Symbol, TreeGen], ownerReplacements: Map[Symbol, Symbol], unit: CompilationUnit) = new TypingTransformer(unit) {
    override def transform(tree: Tree): Tree = {
      val rep = tree match {
        case Ident(n) if tree.symbol != NoSymbol =>
          mappings.get(tree.symbol).map(_()).getOrElse(super.transform(tree))
        case _ =>
          //if (tree.symbol != null && tree.symbol.ownerChain.exists(_.isMethod))
          //  println("Found method symbol that's suspect: " + tree.symbol.ownerChain + " for " + tree)
          super.transform(tree)
      }
      if (false) {
        val sym = rep.symbol
        val repSym = replaceOwners(sym, ownerReplacements)
        try {
          if (repSym != sym)
            rep.setSymbol(repSym)
        } catch {
          case ex =>
            ex.printStackTrace
            print("ERROR failed to replace occurrence's symbol : " + ex)
        }
      }

      /*
      val sym = rep.symbol
      if (sym != null) {
        val symOwnerChain = sym.ownerChain

        //println("symOwnerChain = " + symOwnerChain)
        for ((disappearing, replacement) <- ownerReplacements) {
          if (sym.owner == disappearing)
            rep.setSymbol(sym.cloneSymbol(replacement))
          else if (symOwnerChain.contains(disappearing)) {
            println("[scalacl] We've got a problem with symbol " + sym + " for tree " + tree + ": ownerChain = " + symOwnerChain.toSeq + ", replacing " + mappings.keys.toSeq)
          }
        }
      }*/
      rep
    }
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
          ).setSymbol(getMember(a.symbol, nme.apply)),
          List(index)
        )
      }
    }
  }
  def newUpdate(pos: Position, array: => Tree, index: => Tree, value: => Tree) = {
    val a = array
    assert(a.tpe != null)
    val sym = getMember(a.symbol, nme.update)//
        //getMember(a.tpe.typeSymbol, nme.update)
    typed {
      atPos(pos) {
        val t =
        Apply(
          Select(
            a,
            N("update")
          ).setSymbol(sym),
          List(index, value)
        ).setSymbol(sym)
        //println(nodeToString(t))
        //treeBrowsers.create.browse(t)
        t
      }
    }
  }

  def binOp(a: Tree, op: Symbol, b: Tree) = typed {
    Apply(Select(a, op), List(b))
  }
  def newLogicAnd(a: Tree, b: Tree) = typed {
    binOp(a, BooleanClass.tpe.member(nme.AMPAMP), b)
  } 
  def ident(sym: Symbol, n: Name) = {
    val v = Ident(n)
    v.symbol = sym
    v.tpe = sym.tpe
    v
  }

  def boolNot(a: => Tree) = {
    val sym = BooleanClass.tpe.member(nme.UNARY_!)
    //Apply(
      Select(a, nme.UNARY_!).setSymbol(sym)//, Nil).setSymbol(sym).setType(BooleanClass.tpe)
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
    //labSym.setFlag(SYNTHETIC)

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

