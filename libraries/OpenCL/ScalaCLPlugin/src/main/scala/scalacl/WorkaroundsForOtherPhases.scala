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
 * 
 *
 *
 * Contains portions of scala-2.8.0.final-sources/src/compiler/scala/tools/nsc/transform/ExplicitOuter.scala :
 * NSC -- new Scala compiler
 * Copyright 2005-2010 LAMP/EPFL
 * @author Martin Odersky
 * 
 */

package scalacl

import scala.reflect.NameTransformer
import scala.tools.nsc.Global
import scala.tools.nsc.ast.TreeDSL

trait WorkaroundsForOtherPhases extends TreeDSL with MiscMatchers {
  val global: Global 
  import global._
  import definitions._
  import treeInfo.{ methPart }
  import typer.typed
  import CODE._
  
  /**
   * This code was copied from the following source from the Scala Compiler : scala-2.8.0.final-sources/src/compiler/scala/tools/nsc/transform/ExplicitOuter.scala
   * To trigger the targetted "no-symbol has no owner" bug, try compiling Types.scala and Typers.scala :
   * SCALACL_NO_WORKAROUND_FOR_THIS_OUTERREF_NOSYMBOL=1 SRCS=/Users/ochafik/src/scala-2.8.x scalac -bootclasspath $SRCS/build/locker/classes/library -cp $SRCS/build/locker/classes/compiler $SRCS/src/compiler/scala/tools/nsc/symtab/Types.scala $SRCS/src/compiler/scala/tools/nsc/typechecker/Typers.scala
   */
  def assertNoThisWithNoSymbolOuterRef(tree: Tree, localTyper: analyzer.Typer): Unit = if ("1" == System.getenv("SCALACL_NO_WORKAROUND_FOR_THIS_OUTERREF_NOSYMBOL")) new Transformer {
    protected def outerPath(base: Tree, from: Symbol, to: Symbol): Tree = {
      //Console.println("outerPath from "+from+" to "+to+" at "+base+":"+base.tpe)
      //assert(base.tpe.widen.baseType(from.toInterface) != NoType, ""+base.tpe.widen+" "+from.toInterface)//DEBUG
      if (from == to || from.isImplClass && from.toInterface == to) base
      else outerPath(outerSelect(base), from.outerClass, to)
    }
    def outerAccessor(clazz: Symbol): Symbol = {
      val firstTry = clazz.info.decl(nme.expandedName(nme.OUTER, clazz))
      if (firstTry != NoSymbol && firstTry.outerSource == clazz) firstTry
      else clazz.info.decls find (_.outerSource == clazz) getOrElse NoSymbol
    }

    private def outerSelect(base: Tree): Tree = {
      val outerAcc = outerAccessor(base.tpe.typeSymbol.toInterface)
      val currentClass = this.currentClass //todo: !!! if this line is removed, we get a build failure that protected$currentClass need an override modifier
      // outerFld is the $outer field of the current class, if the reference can
      // use it (i.e. reference is allowed to be of the form this.$outer),
      // otherwise it is NoSymbol
      val outerFld =         
        if (outerAcc.owner == currentClass && 
            base.tpe =:= currentClass.thisType &&
            outerAcc.owner.isFinal) 
          outerField(currentClass) suchThat (_.owner == currentClass)
        else
          NoSymbol
      val path = 
        if (outerFld != NoSymbol) Select(base, outerFld)
        else Apply(Select(base, outerAcc), Nil)
        
      localTyper typed path
    }
    
    private def outerField(clazz: Symbol): Symbol = {
      val result = clazz.info.member(nme getterToLocal nme.OUTER)
      assert(result != NoSymbol, "no outer field in "+clazz+clazz.info.decls+" at "+phase)
      
      result
	  }
    
    protected def outerValue: Tree =
      if (outerParam != NoSymbol) ID(outerParam)
      else outerSelect(THIS(currentClass))

    protected var outerParam: Symbol = NoSymbol

    private def isInner(clazz: Symbol) =
      !clazz.isPackageClass && !clazz.outerClass.isStaticOwner

    override def transform(tree: Tree) = {
      val savedOuterParam = outerParam
      try {
        tree match {
          case Template(_, _, _) =>
            outerParam = NoSymbol
          case DefDef(_, _, _, vparamss, _, _) =>
            if (tree.symbol.isClassConstructor && isInner(tree.symbol.owner)) {
              outerParam = vparamss.head.head.symbol
              assert(outerParam.name startsWith nme.OUTER, outerParam.name)
            }
          case _ =>
        }
        
        if (tree.isInstanceOf[This]) {
          val This(qual) = tree
          val sym = tree.symbol

          import scala.tools.nsc._
          import symtab._
          import Flags.{ CASE => _, _ }
          if (sym == currentClass || (sym hasFlag MODULE) && sym.isStatic) tree
          else outerPath(outerValue, currentClass.outerClass, sym)
        }
        super.transform(tree)
      }
      finally {
        outerParam = savedOuterParam
      }
      
    }
  }.transform(tree)
  
}