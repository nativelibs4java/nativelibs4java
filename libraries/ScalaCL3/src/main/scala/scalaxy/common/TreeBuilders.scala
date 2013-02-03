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

import scala.reflect.api.Universe

trait TreeBuilders
extends MiscMatchers
{
  val global: Universe

  import global._
  import global.definitions._
  
  type TreeGen = () => Tree
  
  def withSymbol[T <: Tree](sym: Symbol, tpe: Type = NoType)(tree: T): T
  def typed[T <: Tree](tree: T): T
  def typeCheck(tree: Tree, pt: Type): Tree
  def fresh(s: String): String
  def inferImplicitValue(pt: Type): Tree
  def setInfo(sym: Symbol, tpe: Type): Symbol
  def setType(sym: Symbol, tpe: Type): Symbol
  def setType(tree: Tree, tpe: Type): Tree
  def setPos(tree: Tree, pos: Position): Tree
  
  
  def primaryConstructor(tpe: Type): Symbol = {
    tpe.members.iterator
      .find(s => s.isMethod && s.asMethod.isPrimaryConstructor)
      .getOrElse(sys.error("No primary constructor for " + tpe))
  }
  
  def apply(sym: Symbol)(target: Tree, args: List[Tree]) = {
    withSymbol(sym) {
      Apply(
        withSymbol(sym) {
          target
        },
        args
      )
    }
  }
  def typeApply(sym: Symbol)(target: Tree, targs: List[TypeTree]) = {
    withSymbol(sym) {
      TypeApply(
        withSymbol(sym) {
          target
        },
        targs
      )
    }
  }
  def newTypeTree(tpe: Type): TypeTree =
    withSymbol(tpe.typeSymbol, tpe) { 
      TypeTree(tpe) 
    }
  
  // TreeGen.mkIsInstanceOf adds an extra Apply (and does not set all symbols), which makes it apparently useless in our case(s)
  def newIsInstanceOf(tree: Tree, tpe: Type) = {
    try {
      typeApply(AnyClass.asType.toType.member(newTermName("isInstanceOf")))(
        Select(
          tree,
          N("isInstanceOf")
        ),
        List(newTypeTree(tpe))
      )
    } catch { case ex: Throwable =>
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
        val sym = 
          (a.tpe member applyName())
            .filter(s => s.isMethod && s.asMethod.paramss.size == 1)
        apply(sym)(
          Select(
            a,
            N("apply")
          ),
          List(index)
        )
      }
    }
  }
  
  def newSelect(target: Tree, name: Name, typeArgs: List[TypeTree] = Nil) =
    newApply(target, name, typeArgs, null)
    
  def newApply(target: Tree/*, targetType: Type*/, name: Name, typeArgs: List[TypeTree] = Nil, args: List[Tree] = Nil) = {
    val targetType = 
      if (target.tpe == NoType || target.tpe == null) 
        target.symbol.typeSignature 
      else 
        target.tpe
        
    val sym = targetType.member(name)
    typed {
      val select = withSymbol(sym) { Select(target, name) }
      if (!typeArgs.isEmpty)
        Apply(
          typeApply(sym)(select, typeArgs),
          args
        )
      else if (args != null)
        Apply(select, args)
      else
        select
    }
  }
  
  def newInstance(tpe: Type, constructorArgs: List[Tree]) = {
    val sym = primaryConstructor(tpe)
    typed {
      apply(sym)(
        Select(
          New(newTypeTree(tpe)),
          sym
        ),
        constructorArgs
      )
    }
  }
    
  def newCollectionApply(collectionModuleTree: => Tree, typeExpr: TypeTree, values: Tree*) =
    newApply(collectionModuleTree, applyName, List(typeExpr), values.toList)
    
  def newScalaPackageTree = 
    Ident(ScalaPackage)
    
  def newScalaCollectionPackageTree =
    Ident(ScalaCollectionPackage)/*
    withSymbol(ScalaCollectionPackage) { 
      Select(newScalaPackageTree, N("collection")) 
    }*/
    
  def newSomeModuleTree = typed {
    Ident(SomeModule)
    /*withSymbol(SomeModule) { 
      Select(newScalaPackageTree, N("Some"))
    }*/
  }
    
  def newNoneModuleTree = typed {
    Ident(NoneModule)/*
    withSymbol(NoneModule) {
      Select(newScalaPackageTree, N("None"))
    }*/
  }
    
  def newSeqModuleTree = typed {
    Ident(SeqModule)/*
    withSymbol(SeqModule) {
      Select(newScalaCollectionPackageTree, N("Seq"))
    }*/
  }
    
  def newSetModuleTree = typed {
    Ident(SetModule)/*
    withSymbol(SetModule) {
      Select(newScalaCollectionPackageTree, N("Set"))
    }*/
  }
    
  def newArrayModuleTree = typed {
    Ident(ArrayModule)/*
    withSymbol(ArrayModule) {
      Select(newScalaPackageTree, N("Array"))
    }*/
  }
    
  def newSeqApply(typeExpr: TypeTree, values: Tree*) =
    newApply(newSeqModuleTree, applyName, List(typeExpr), values.toList)
    
  def newSomeApply(tpe: Type, value: Tree) =
    newApply(newSomeModuleTree, applyName, List(newTypeTree(tpe)), List(value))
    
  def newArrayApply(typeExpr: TypeTree, values: Tree*) =
    newApply(newArrayModuleTree, applyName, List(typeExpr), values.toList)
  
  def newArrayMulti(arrayType: Type, componentTpe: Type, lengths: => List[Tree], manifest: Tree) =
      typed {
        val sym = (ArrayModule.asModule.moduleClass.asType.toType member newTermName("ofDim"))
          .suchThat(s => s.isMethod && s.asMethod.paramss.flatten.size == lengths.size + 1)
          //.getOrElse(sys.error("No Array.ofDim found"))
        withSymbol(sym) {
          Apply(
            withSymbol(sym) {
              Apply(
                TypeApply(
                  withSymbol(sym) {
                    Select(
                      Ident(
                        ArrayModule
                      ),
                      N("ofDim")
                    )
                  },
                  List(newTypeTree(componentTpe))
                ),
               lengths
              )
            },
            List(manifest)
          )
        }
      }

    def newArray(componentType: Type, length: => Tree) =
      newArrayWithArrayType(appliedType(ArrayClass.asType.toType, List(componentType)), length)

    def newArrayWithArrayType(arrayType: Type, length: => Tree) =
      typed {
        val sym = primaryConstructor(arrayType)
        apply(sym)(
          Select(
            New(newTypeTree(arrayType)),
            sym
          ),
          List(length)
        )
      }

    
  def newUpdate(pos: Position, array: => Tree, index: => Tree, value: => Tree) = {
    val a = array
    assert(a.tpe != null)
    val sym = a.tpe member updateName()
    typed {
      atPos(pos) {
        apply(sym)(
          Select(
            a,
            N("update")
          ),
          List(index, typed { value })
        )
      }
    }
  }

  def binOp(a: Tree, op: Symbol, b: Tree) = typed {
    assert(op != NoSymbol)
    //withSymbol(op) {
      Apply(
        //withSymbol(op) {
          Select(a, op),
        //}, 
        List(b)
      )
    //}
  }

  def newIsNotNull(target: Tree) = typed {
    binOp(target, AnyRefClass.asType.toType.member(NE), newNull(target.tpe))
  }
  
  def newArrayLength(a: Tree) =
    withSymbol(a.tpe.member(lengthName()), IntTpe) {
      Select(a, lengthName())
    }
  
  def boolAnd(a: Tree, b: Tree) = typed {
    if (a == null)
      b
    else if (b == null)
      a
    else
      binOp(a, BooleanTpe.member(ZAND /* AMPAMP */), b)
  }
  def boolOr(a: Tree, b: Tree) = typed {
    if (a == null)
      b
    else if (b == null)
      a
    else
      binOp(a, BooleanTpe.member(ZOR), b)
  }
  def ident(sym: Symbol, tpe: Type, n: Name, pos: Position = NoPosition): Ident = {
    assert(sym != NoSymbol)
    typed {
      Ident(sym)
    }
    /*val v = Ident(sym)
    //val tpe = sym.typeSignature
    setPos(v, pos)
    withSymbol(sym, tpe) { v }
    */
  }

  def boolNot(a: => Tree) = {
    val sym = BooleanTpe.member(UNARY_!)
    //Apply(
    withSymbol(sym, BooleanTpe) { Select(a, UNARY_!) }
  }

  def intAdd(a: => Tree, b: => Tree) =
    binOp(a, IntTpe.member(PLUS), b)

  def intDiv(a: => Tree, b: => Tree) =
    binOp(a, IntTpe.member(DIV), b)

  def intSub(a: => Tree, b: => Tree) =
    binOp(a, IntTpe.member(MINUS), b)

  def newAssign(target: IdentGen, value: Tree) = typed {
    Assign(target(), value)
  }
    
  def incrementIntVar(identGen: IdentGen, value: Tree = newInt(1)) =
    newAssign(identGen, intAdd(identGen(), value))

  def decrementIntVar(identGen: IdentGen, value: Tree) = typed {
    //identGen() === intSub(identGen(), value)
    Assign(
      identGen(),
      intSub(identGen(), value)
    )
  }

  def whileLoop(owner: Symbol, tree: Tree, cond: Tree, body: Tree): Tree =
    whileLoop(owner, tree.pos, cond, body)
    
  def whileLoop(owner: Symbol, pos: Position, cond: Tree, body: Tree): Tree = {
    val lab = newTermName(fresh("while$"))
    val labTyp = MethodType(Nil, UnitTpe)
    val labSym = setInfo(owner.newTermSymbol(lab, pos, Flag.LOCAL), labTyp)
   
    typed {
      withSymbol(labSym) {
        LabelDef(
          lab,
          Nil,
          If(
            cond,
            Block(
              if (body == null)
                Nil
              else
                List(body),
              Apply(
                ident(labSym, NoType, lab, pos),
                Nil
              )
            ),
            newUnit
          )
        )
      }
    }
  }

  type IdentGen = () => Ident
  
  private lazy val anyValTypeInfos = Seq[(Class[_], Type, AnyVal)](
    ( classOf[java.lang.Boolean], BooleanTpe, false ),
    ( classOf[java.lang.Integer], IntTpe, 0),
    ( classOf[java.lang.Long], LongTpe, 0: Long),
    ( classOf[java.lang.Short], ShortTpe, 0: Short),
    ( classOf[java.lang.Byte], ByteTpe, 0: Byte),
    ( classOf[java.lang.Character], CharTpe, 0.asInstanceOf[Char]),
    ( classOf[java.lang.Double], DoubleTpe, 0.0),
    ( classOf[java.lang.Float], FloatTpe, 0.0f)
  )
  lazy val classToType: Map[Class[_], Type] =
    (anyValTypeInfos.map { case (cls, tpe, defVal) => cls -> tpe }).toMap
    
  lazy val typeToDefaultValue: Map[Type, AnyVal] =
    (anyValTypeInfos.map { case (cls, tpe, defVal) => tpe -> defVal }).toMap
  
  def newConstant(v: Any, tpe: Type = null) = typed {
    Literal(Constant(v))
  }/*.setType(
      if (tpe != null) 
        tpe
      else if (v.isInstanceOf[String])
        StringClass.tpe
      else
        classToType(v.getClass)
    )
  }*/

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
    newConstant(1: Byte)
  }

  def newUnit() = 
    newConstant(())

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
    apply(resultMethod)(
      Select(
        builder,
        resultName
      ),
      Nil
    )
  }
  
  def addAssign(target: Tree, toAdd: Tree) = {
    val sym = (target.tpe member addAssignName())
    apply(sym)(
      Select(
        target,
        addAssignName()
      ),
      List(toAdd)
    )
  }
  
  lazy val TypeRef(manifestPre, manifestSym, _) = typeOf[Manifest[Int]]
  def toArray(tree: Tree, componentType: Type) = typed {
    val manifest = inferImplicitValue(typeRef(manifestPre, manifestSym, List(componentType)))
    assert(manifest != EmptyTree, "Failed to get manifest for " + componentType)
    
    val method = tree.tpe member toArrayName
    apply(method)(
      typeApply(method)(
        Select(
          tree,
          toArrayName
        ),
        List(newTypeTree(componentType))
      ),
      List(manifest)
    )
  }
  
  def newIf(cond: Tree, thenTree: Tree, elseTree: Tree = null) = {
    typed { thenTree }
    if (elseTree != null)
      typed { elseTree }
      
    val tpe = {
      if (elseTree == null)
        UnitTpe
      else if (thenTree.tpe == elseTree.tpe)
        thenTree.tpe
      else
        throw new RuntimeException("Mismatching types between then and else : " + thenTree.tpe + " vs. " + elseTree.tpe)
    }
    
    withSymbol(NoSymbol, tpe) {
      If(cond, thenTree, Option(elseTree).getOrElse(EmptyTree))
    }
  }
  
  def newVariable(
    prefix: String,
    symbolOwner: Symbol,
    pos: Position,
    mutable: Boolean,
    initialValue: Tree
  ): VarDef = {
    typed { initialValue }
    var tpe = initialValue.tpe
    //if (ConstantType.unapply(tpe))//.isInstanceOf[ConstantType])
      tpe = normalize(tpe)
    val name = fresh(prefix)
    val sym = 
      symbolOwner.newTermSymbol(name, pos, (if (mutable) Flag.MUTABLE else NoFlags) | Flag.LOCAL)
    if (tpe != null && tpe != NoType)
      setInfo(sym, tpe)
      
    VarDef(
      () => ident(sym, tpe, newTermName(name), pos), 
      sym,
      withSymbol(sym, tpe) {
        ValDef(
          Modifiers(if (mutable) Flag.MUTABLE else NoFlags), 
          name, 
          newTypeTree(tpe), 
          initialValue
        )
      }
    )
  }
}

