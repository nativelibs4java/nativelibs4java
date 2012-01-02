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
package com.nativelibs4java.scalace ; package common
import pluginBase._

import java.io.File

import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.ast.TreeDSL
import scala.tools.nsc.transform.TypingTransformers
import Function.tupled

trait TreeBuilders
extends MiscMatchers
   with TypingTransformers
   with TreeDSL
{
  this: PluginComponent with WithOptions =>

  import global._
  import global.definitions._
  import scala.tools.nsc.symtab.Flags._
  import typer.{typed, atOwner}    // methods to type trees
  import CODE._

  def nodeToStringNoComment(tree: Tree) =
    nodeToString(tree).replaceAll("\\s*//.*\n", "\n").replaceAll("\\s*\n\\s*", " ").replaceAll("\\(\\s+", "(").replaceAll("\\s+\\)", "")

  /// print a message only if the operation succeeded :
  def msg[V](unit: CompilationUnit, pos: Position, text: => String)(v: => V): V = {
    val fileLine = new File(pos.source.path).getName + ":" + pos.line
    val prefix = "[scalacl] " + fileLine + " "
    try {
      val r = v
      //unit.comment(pos, text)
      if (options.verbose) {
        val str = prefix + text
        // Global.log(String) was removed or modified in Scala's trunk version... too bad !
        //global.log(str)
        println(str)
      }
      r
    } catch {
      case ex: UnsupportedOperationException =>
        throw ex
      case ex =>
        var str = 
          """An unexpected error occurred while attempting an optimization
  Attempted optimization : '"""+ text + """'
  You can skip this line with the following environment variable :
    SCALACL_SKIP=""" + fileLine

        if (options.trace) {
          ex.printStackTrace
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

        throw ex
    }
  }

  type TreeGen = () => Tree

  def replaceOccurrences(tree: Tree, mappingsSym: Map[Symbol, TreeGen], symbolReplacements: Map[Symbol, Symbol], treeReplacements: Map[Tree, TreeGen], unit: CompilationUnit) = {
    def key(s: Symbol) = s.ownerChain.map(_.toString)
    val mappings = mappingsSym.map({ case (sym, treeGen) => (key(sym), (sym, treeGen)) })
    val result = new TypingTransformer(unit) {
      override def transform(tree: Tree): Tree = {
        treeReplacements.get(tree).map(_()).getOrElse(
          tree match {
            case Ident(n) if tree.symbol != NoSymbol =>
              val treeKey = key(tree.symbol)
              mappings.get(treeKey).map({ case (sym, treeGen) =>
                treeGen().setType(tree.symbol.tpe)
              }).getOrElse(super.transform(tree))
            case _ =>
              super.transform(tree)
          }
        )
      }
    }.transform(tree)

    for ((fromSym, toSym) <- symbolReplacements)
      new ChangeOwnerTraverser(fromSym, toSym).traverse(result)
      
    typed {
      result
    }
  }
  
  // TreeGen.mkIsInstanceOf adds an extra Apply (and does not set all symbols), which makes it apparently useless in our case(s)
  def newIsInstanceOf(tree: Tree, tpe: Type) = {
    try {
      val tt = TypeTree(tpe)
      tt.tpe = tpe
      TypeApply(
        Select(
          tree,
          N("isInstanceOf")
        ).setSymbol(Any_isInstanceOf),
        List(tt)
      ).setSymbol(Any_isInstanceOf)
    } catch { case ex =>
      ex.printStackTrace
      throw new RuntimeException(ex)
    }
  }
  def newApply(pos: Position, array: => Tree, index: => Tree) = {
    val a = array
    assert(a.tpe != null)
    typed {
      atPos(pos) {
        //a.DOT(N("apply"))(index)
        val sym = getMember(a.symbol, nme.apply).filter(_.paramss.size == 1)
        Apply(
          Select(
            a,
            N("apply")
          ).setSymbol(sym),
          List(index)
        ).setSymbol(sym)
      }
    }
  }
  
  def newSelect(target: Tree, name: Name, typeArgs: List[Tree] = Nil) =
    newApply(target, name, typeArgs, null)
    
  def newApply(target: Tree/*, targetType: Type*/, name: Name, typeArgs: List[Tree] = Nil, args: List[Tree] = Nil) = {
    val targetType = 
      if (target.tpe == NoType || target.tpe == null) 
        target.symbol.tpe 
      else 
        target.tpe
        
    val sym = targetType member name
    typed {
      val select = Select(target, name).setSymbol(sym)
      if (!typeArgs.isEmpty)
        Apply(
          TypeApply(select, typeArgs).setSymbol(sym),
          args
        )
      else if (args != null)
        Apply(select, args)
      else
        select
    }
  }
  
  def newInstance(tpe: Type, constructorArgs: List[Tree]) = {
    val sym = tpe.typeSymbol.primaryConstructor
    typed {
      Apply(
        Select(
          New(TypeTree(tpe)),//.setSymbol(tpe.typeSymbol),
          sym
        ).setSymbol(sym),
        constructorArgs
      ).setSymbol(sym)
    }
  }
    
  def newCollectionApply(collectionModuleTree: => Tree, typeExpr: Tree, values: Tree*) =
    newApply(collectionModuleTree, applyName, List(typeExpr), values.toList)
    
  def newScalaPackageTree =  
    Ident(N("scala")).setSymbol(ScalaPackage).setType(ScalaPackage.tpe)
    
  def newScalaCollectionPackageTree =
    Select(newScalaPackageTree, N("collection")).setSymbol(ScalaCollectionPackage).setType(ScalaCollectionPackage.tpe)
    
  def newSomeModuleTree = typed {
    Select(newScalaPackageTree, N("Some")).setSymbol(SomeModule)
  }
    
  def newNoneModuleTree = typed {
    Select(newScalaPackageTree, N("None")).setSymbol(NoneModule)
  }
    
  def newSeqModuleTree = typed {
    Select(newScalaCollectionPackageTree, N("Seq")).setSymbol(SeqModule)
  }
    
  def newSetModuleTree = typed {
    Select(newScalaCollectionPackageTree, N("Set")).setSymbol(SetModule)
  }
    
  def newArrayModuleTree = typed {
    Select(newScalaPackageTree, N("Array")).setSymbol(ArrayModule)
  }
    
  def newSeqApply(typeExpr: Tree, values: Tree*) =
    newApply(newSeqModuleTree, applyName, List(typeExpr), values.toList)
    
  def newTypeTree(tpe: Type) =
    TypeTree(tpe).setType(tpe)
    
  def newSomeApply(tpe: Type, value: Tree) =
    newApply(newSomeModuleTree, applyName, List(newTypeTree(tpe)), List(value))
    
  def newArrayApply(typeExpr: Tree, values: Tree*) =
    newApply(newArrayModuleTree, applyName, List(typeExpr), values.toList)
  
  def newArrayMulti(arrayType: Type, componentTpe: Type, lengths: => List[Tree], manifest: Tree) =
      typed {
        val sym = (ArrayModule.tpe member "ofDim" alternatives).filter(_.paramss.flatten.size == lengths.size + 1).head
        Apply(
          Apply(
            TypeApply(
              Select(
                Ident(
                  ArrayModule
                ),
                N("ofDim")
              ).setSymbol(sym),
              List(TypeTree(componentTpe))
            ),
           lengths
          ).setSymbol(sym),
          List(manifest)
        ).setSymbol(sym)
      }

    def newArray(componentType: Type, length: => Tree) =
      newArrayWithArrayType(appliedType(ArrayClass.tpe, List(componentType)), length)

    def newArrayWithArrayType(arrayType: Type, length: => Tree) =
      typed {
        //NEW(TypeTree(arrayType), length)
        val sym = arrayType.typeSymbol.primaryConstructor
        Apply(
          Select(
            New(TypeTree(arrayType)),//.setSymbol(arrayType.typeSymbol),
            sym
          ).setSymbol(sym),
          List(length)
        ).setSymbol(sym)
      }

    
  def newUpdate(pos: Position, array: => Tree, index: => Tree, value: => Tree) = {
    val a = array
    assert(a.tpe != null)
    val sym = getMember(a.symbol, nme.update)
    typed {
      atPos(pos) {
        //a.DOT(N("update"))(index, value)
        Apply(
          Select(
            a,
            N("update")
          ).setSymbol(sym).setType(sym.tpe),
          List(index, typed { value })
        ).setSymbol(sym).setType(UnitClass.tpe)
        //println(nodeToString(t))
        //treeBrowsers.create.browse(t)
      }
    }
  }

  def binOp(a: Tree, op: Symbol, b: Tree) = typed {
    assert(op != NoSymbol)
    Apply(Select(a, op).setSymbol(op), List(b)).setSymbol(op)
  }

  def newIsNotNull(target: Tree) = typed {
    binOp(target, AnyRefClass.tpe.member(nme.NE/*N("neq")*/), newNull(target.tpe))//nme.NE)
  }
  
  def newArrayLength(a: Tree) =
    //a.DOT(nme.length)
    Select(a, nme.length).setSymbol(getMember(a.symbol, nme.length)).setType(IntClass.tpe)
  
  def boolAnd(a: Tree, b: Tree) = typed {
    if (a == null)
      b
    else if (b == null)
      a
    else
      binOp(a, BooleanClass.tpe.member(nme.ZAND /* AMPAMP */), b)
  }
  def boolOr(a: Tree, b: Tree) = typed {
    if (a == null)
      b
    else if (b == null)
      a
    else
      binOp(a, BooleanClass.tpe.member(nme.ZOR), b)
  }
  def ident(sym: Symbol, n: Name, pos: Position = NoPosition) = {
    assert(sym != NoSymbol)
    val v = Ident(n)
    v.symbol = sym
    if (sym.hasRawInfo)
      v.tpe = sym.tpe
    v.pos = pos
    v
  }

  def boolNot(a: => Tree) = {
    val sym = BooleanClass.tpe.member(nme.UNARY_!)
    //Apply(
    Select(a, nme.UNARY_!).setSymbol(sym).setType(BooleanClass.tpe)//, Nil).setSymbol(sym).setType(BooleanClass.tpe)
  }

  def intAdd(a: => Tree, b: => Tree) =
    binOp(a, IntClass.tpe.member(nme.PLUS), b)

  def intDiv(a: => Tree, b: => Tree) =
    binOp(a, IntClass.tpe.member(nme.DIV), b)

  def intSub(a: => Tree, b: => Tree) =
    binOp(a, IntClass.tpe.member(nme.MINUS), b)

  def newAssign(target: IdentGen, value: Tree) =
    Assign(target(), value).setType(UnitClass.tpe)
    
  def incrementIntVar(identGen: IdentGen, value: Tree = newInt(1)) =
    newAssign(identGen, intAdd(identGen(), value))

  def decrementIntVar(identGen: IdentGen, value: Tree) =
    //identGen() === intSub(identGen(), value)
    Assign(
      identGen(),
      intSub(identGen(), value)
    ).setType(UnitClass.tpe)

  def whileLoop(owner: Symbol, unit: CompilationUnit, tree: Tree, cond: Tree, body: Tree): Tree =
    whileLoop(owner, unit, tree.pos, cond, body)
    
  def whileLoop(owner: Symbol, unit: CompilationUnit, pos: Position, cond: Tree, body: Tree): Tree = {
    val lab = unit.fresh.newName(body.pos, "while$")
    val labTyp = MethodType(Nil, UnitClass.tpe)
    val labSym = owner.newLabel(pos, N(lab)).setInfo(labTyp).setFlag(SYNTHETIC | LOCAL)
   
    typed {
      //body.tpe = UnitClass.tpe
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
              ident(labSym, lab, pos),
              Nil
            )
          ),
          newUnit
        )
      ).setSymbol(labSym)
    }
  }

  type IdentGen = () => Ident
  
  private val anyValTypeInfos = Seq[(Class[_], Type, AnyVal)](
    ( classOf[java.lang.Boolean], BooleanClass.tpe, false ),
    ( classOf[java.lang.Integer], IntClass.tpe, 0),
    ( classOf[java.lang.Long], LongClass.tpe, 0: Long),
    ( classOf[java.lang.Short], ShortClass.tpe, 0: Short),
    ( classOf[java.lang.Byte], ByteClass.tpe, 0: Byte),
    ( classOf[java.lang.Character], CharClass.tpe, 0.asInstanceOf[Char]),
    ( classOf[java.lang.Double], DoubleClass.tpe, 0.0),
    ( classOf[java.lang.Float], FloatClass.tpe, 0.0f)
  )
  val classToType: Map[Class[_], Type] =
    (anyValTypeInfos.map { case (cls, tpe, defVal) => cls -> tpe }).toMap
    
  val typeToDefaultValue: Map[Type, AnyVal] =
    (anyValTypeInfos.map { case (cls, tpe, defVal) => tpe -> defVal }).toMap
  
  def newConstant(v: Any, tpe: Type = null) = {
    Literal(Constant(v)).setType(
      if (tpe != null) 
        tpe
      else if (v.isInstanceOf[String])
        StringClass.tpe
      else
        classToType(v.getClass)
    )
  }

  def newBool(v: Boolean) =   newConstant(v)
  def newInt(v: Int) =        newConstant(v)
  def newLong(v: Long) =      newConstant(v)

  def newNull(tpe: Type) =    newConstant(null, tpe)

  def newDefaultValue(tpe: Type) = {
    if (isAnyVal(tpe))
      newConstant(typeToDefaultValue(tpe), tpe)
    else
      newNull(tpe)
  }

  def newOneValue(tpe: Type) = {
    assert(isAnyVal(tpe))
    Literal(Constant(1: Byte)).setType(tpe)
  }

  def newUnit() = 
    Literal(Constant()).setType(UnitClass.tpe)

  case class VarDef(rawIdentGen: IdentGen, symbol: Symbol, definition: ValDef) {
    var identUsed = false
    val identGen: IdentGen = () => {
      identUsed = true
      rawIdentGen()
    }
    def tpe = definition.tpe
    def apply() = identGen()

    def defIfUsed = if (identUsed) Some(definition) else None
    def ifUsed[V](v: => V) = if (identUsed) Some(v) else None
  }
  implicit def VarDev2IdentGen(vd: VarDef) = if (vd == null) null else vd.identGen
  
  def simpleBuilderResult(builder: Tree): Tree = typed {
    val resultMethod = builder.tpe member resultName
    Apply(
      Select(
        builder,
        resultName
      ).setSymbol(resultMethod),//.setType(resultMethod.tpe),
      Nil
    ).setSymbol(resultMethod)
  }
  
  def addAssign(target: Tree, toAdd: Tree) = {
    val sym = (target.tpe member addAssignName).alternatives.head// filter (_.paramss.size == 1)
    Apply(
      Select(
        target,
        addAssignName
      ).setSymbol(sym).setType(sym.tpe),
      List(toAdd)
    ).setSymbol(sym).setType(UnitClass.tpe)
  }
  
  def toArray(tree: Tree, componentType: Type, localTyper: analyzer.Typer) = typed {
    val manifest = localTyper.findManifest(componentType, false).tree
    assert(manifest != EmptyTree, "Failed to get manifest for " + componentType)
    
    val method = tree.tpe member toArrayName
    Apply(
      TypeApply(
        Select(
          tree,
          toArrayName
        ).setSymbol(method).setType(method.tpe),
        List(TypeTree(componentType))
      ),
      List(manifest)
    ).setSymbol(method)
  }
  
  def newIf(cond: Tree, thenTree: Tree, elseTree: Tree = null) = {
    typed { thenTree }
    if (elseTree != null)
      typed { elseTree }
      
    If(cond, thenTree, Option(elseTree).getOrElse(EmptyTree)).setType {
      if (elseTree == null)
        UnitClass.tpe
      else if (thenTree.tpe == elseTree.tpe)
        thenTree.tpe
      else
        throw new RuntimeException("Mismatching types between then and else : " + thenTree.tpe + " vs. " + elseTree.tpe)
    }
  }
  
  def newVariable(
    unit: CompilationUnit,
    prefix: String,
    symbolOwner: Symbol,
    pos: Position,
    mutable: Boolean,
    initialValue: Tree
  ): VarDef = {
    typed { initialValue }
    var tpe = initialValue.tpe
    if (tpe.isInstanceOf[ConstantType])
      tpe = tpe.widen
    val name = unit.fresh.newName(pos, prefix)
    val sym = (
      if (mutable)
        symbolOwner.newVariable(pos, name)
      else
        symbolOwner.newValue(pos, name)
    ).setFlag(SYNTHETIC | LOCAL)
    if (tpe != null && tpe != NoType)
      sym.setInfo(tpe)
      
    VarDef(
      () => ident(sym, name, pos), 
      sym,
      ValDef(
        Modifiers(if (mutable) MUTABLE else 0), 
        name, 
        TypeTree(tpe), 
        initialValue
      ).setType(tpe).setSymbol(sym)
    )
  }
}

