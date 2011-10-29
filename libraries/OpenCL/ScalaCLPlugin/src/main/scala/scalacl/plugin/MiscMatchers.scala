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

import scala.reflect.NameTransformer
import scala.tools.nsc.Global

abstract sealed class ColType(name: String) {
  override def toString = name
}
case object SeqType extends ColType("Seq")
case object SetType extends ColType("Set")
case object ListType extends ColType("List")
case object VectorType extends ColType("Vector")
case object ArrayType extends ColType("Array")
case object IndexedSeqType extends ColType("IndexedSeq")
case object MapType extends ColType("Map")
case object OptionType extends ColType("Option")


trait MiscMatchers extends PluginNames with WithOptions {
  val global: Global
  import global._
  import definitions._
  import treeInfo.{ methPart }
  import typer.typed

  def isPackageReference(tree: Tree, packageName: String) = {
    tree.toString.matches(packageName + ".(package|`package`)")
  }
  
  /** Strips apply nodes looking for type application. */
  def typeArgs(tree: Tree): List[Tree] = tree match {
    case Apply(fn, _)              => typeArgs(fn)
    case TypeApply(fn, args)       => args
    case AppliedTypeTree(fn, args) => args
    case _                         => Nil
  }
  /** Smashes directly nested applies down to catenate the argument lists. */
  def flattenApply(tree: Tree): List[Tree] = tree match {
    case Apply(fn, args)  => flattenApply(fn) ++ args
    case _                => Nil
  }
  def flattenApplyGroups(tree: Tree): List[List[Tree]] = tree match {
    case Apply(fn, args)  => flattenApplyGroups(fn) :+ args
    case _                => Nil
  }
  /** Smashes directly nested selects down to the inner tree and a list of names. */
  def flattenSelect(tree: Tree): (Tree, List[Name]) = tree match {
    case Select(qual, name) => flattenSelect(qual) match { case (t, xs) => (t, xs :+ name) }
    case _                  => (tree, Nil)
  }
  /** Creates an Ident or Select from a list of names. */
  def mkSelect(names: Name*): Tree = names.toList match {
    case Nil        => EmptyTree
    case x :: Nil   => Ident(x)
    case x :: xs    => xs.foldLeft(Ident(x): Tree)(Select(_, _))
  }

  class Ids(start: Long = 1) {
    private var nx = start
    def next = this.synchronized {
      val v = nx
      nx += 1
      v
    }
  }

  object ScalaMathFunction {
    /** I'm all for avoiding "magic strings" but in this case it's hard to
     *  see the twice-as-long identifiers as much improvement.
     */
    def apply(functionName: String, args: List[Tree]) =
      Apply(mkSelect("scala", "math", "package", functionName), args)
        
    def unapply(tree: Tree): Option[(Type, Name, List[Tree])] = tree match {
      case Apply(f @ Select(left, name), args) =>
        if (isPackageReference(left, "scala.math"))
          Some((f.tpe, name, args))
        else
          None
      /*case
        Apply(
          f @ Select(
            Select(
              Select(
                Ident(scalaName()),
                mathName()
              ),
              packageName()
            ),
            funName
          ),
          args
        ) =>
        Some((f.tpe, funName, args))*/
      case _ =>
        None
    }
  }
  object IntRange {
    def apply(from: Tree, to: Tree, by: Option[Tree], isUntil: Boolean, filters: List[Tree]) = error("not implemented")

    def unapply(tree: Tree): Option[(Tree, Tree, Option[Tree], Boolean, List[Tree])] = tree match {
      case Apply(Select(Apply(Select(Predef(), intWrapperName()), List(from)), funToName @ (toName() | untilName())), List(to)) =>
        Option(funToName) collect {
          case toName() =>
            (from, to, None, false, Nil)
          case untilName() =>
            (from, to, None, true, Nil)
        }
      case Apply(Select(tg, n @ (byName() | withFilterName() | filterName())), List(arg)) =>
       tg match {
          case IntRange(from, to, by, isUntil, filters) =>
            Option(n) collect {
                case byName() if by == None =>
                    (from, to, Some(arg), isUntil, filters)
                case withFilterName() | filterName() if !options.stream =>
                    (from, to, by, isUntil, filters :+ arg)
            }
          case _ =>
            None
        }
      case _ => 
        None
    }
  }

  object TreeWithSymbol {
    def unapply(tree: Tree): Option[(Tree, Symbol)] = 
      Some(tree, tree.symbol)
  }
  object TreeWithType {
    def unapply(tree: Tree): Option[(Tree, Type)] =
      Some((tree, if (tree.tpe == null) null else normalize(tree.tpe)))
  }
  object SymbolWithOwnerAndName {
    def unapply(sym: Symbol): Option[(Symbol, Symbol, Name)] = 
      Some(sym, sym.owner, sym.name)
  }
  object TupleClass {
    def unapply(sym: Symbol): Boolean =
      isTupleSymbol(sym)
  }
  
  object tupleComponentName {
    val rx = "_(\\d+)".r
    def unapply(n: Name): Option[Int] = { 
      n.toString match {
        case rx(n) =>
          Some(n.toInt)
        case _ =>
          None
      }
    }
  }
  
  object TupleComponent {
    val rx = "_(\\d+)".r
    def unapply(tree: Tree) = tree match {
      /*case 
        TreeWithSymbol(
          Select(
            target, 
            tupleComponentName(_)
          ),
          SymbolWithOwnerAndName(
            _, 
            TupleClass(),
            tupleComponentName(n)
          )
        ) =>
        Some(target, n - 1)
      */
      case Select(target, tupleComponentName(n)) =>
        //fieldName.toString match {
        //  case rx(n) =>
            if (isTupleSymbol(tree.symbol.owner) || isTupleSymbol(target.tpe.typeSymbol)) {
              Some(target, n - 1)
            } else {
              println("ISSUE with tuple target symbol \n\t" + target.symbol + "\n\t" + target.tpe.typeSymbol)
              None
            }
          //case _ =>
          //  None
        //}
      case _ =>
        None
    }
  }
  object TuplePath {
    def unapply(tree: Tree) = {
      var lastTarget: Tree = tree
      var path: List[Int] = Nil
      var finished = false
      while (!finished) {
        lastTarget match {
          case TupleComponent(target, i) =>
            path = i :: path
            lastTarget = target
          case _ =>
            finished = true
        }
      }
      if (path.isEmpty)
        None
      else
        Some((lastTarget, path))
    }
  }
  object WhileLoop {
    def unapply(tree: Tree) = tree match {
      case
        LabelDef(
          lab,
          List(),
          If(
            condition,
            Block(
              content,
              Apply(
                Ident(lab2),
                List()
              )
            ),
            Literal(Constant(()))
          )
        ) if (lab == lab2) =>
        Some(condition, content)
      case _ =>
        None
    }
  }
  object TupleSelect {
    def unapply(tree: Tree) = tree match {
      case Select(Ident(nme.scala_), name) if name.toString.matches(""".*Tuple\d+""") =>
        true
      case _ =>
        false
      //name.toString.matches("""(_root_\.)?scala\.Tuple\d+""")
    }
  }
  def isTupleSymbol(sym: Symbol) =
    sym.toString.matches("class Tuple\\d+")
    
  def isAnyVal(tpe: Type) =
    tpe == IntClass.tpe ||
    tpe == ShortClass.tpe ||
    tpe == LongClass.tpe ||
    tpe == ByteClass.tpe ||
    tpe == DoubleClass.tpe ||
    tpe == FloatClass.tpe ||
    tpe == CharClass.tpe ||
    tpe == BooleanClass.tpe
    
  def getArrayType(dimensions: Int, componentType: Type): Type = dimensions match {
    case 1 => 
      appliedType(ArrayClass.tpe, List(componentType))
    case _ =>
      assert(dimensions > 1)
      appliedType(ArrayClass.tpe, List(getArrayType(dimensions - 1, componentType)))
  }
  
  
  object BasicTypeApply {
    def unapply(tree: Tree): Option[(Tree, Name, List[Tree], Seq[List[Tree]])] = tree match {
      case 
        Apply(
          TypeApply(
            Select(collection, name),
            typeArgs
          ),
          args
        )
        =>
        Some((collection, name, typeArgs, Seq(args)))
      case Apply(BasicTypeApply(collection, name, typeArgs, args), newArgs) =>
        Some((collection, name, typeArgs, args :+ newArgs))
      case _ =>
        None
    }
  }
  
  object TupleCreation {
    def unapply(tree: Tree): Option[List[Tree]] = Option(tree) collect {
      case Apply(TypeApply(Select(TupleSelect(), applyName()), types), components) =>
        components
      case Apply(tt @ TypeTree(), components) if isTupleSymbol(tree.tpe.typeSymbol) =>
        // TODO FIX THIS BROAD HACK !!! (test tt)
        //println("tt.tpe = (" + tt.tpe + ": " + tt.tpe.getClass.getName + ")")
        components
    }
  }
  class CollectionApply(colModule: Symbol, colClass: Symbol) {
    def apply(component: Tree) = error("not implemented")
    def unapply(tree: Tree): Option[(List[Tree], Type)] = tree match {
      case 
        TreeWithType(
          Apply(TypeApply(Select(colObject, applyName()), List(tpe)), components),
          TypeRef(_, colClass, List(componentType))
        )
      if colObject.symbol == colModule =>
        //normalize(tree.tpe) match {
        //  case TypeRef(_, colClass, List(componentType)) =>
            Some(components, componentType)
        //  case _ =>
        //    None
        //}
      case _ =>
        None
    }
  }
  
  object OptionApply extends CollectionApply(OptionModule, OptionClass)
  object ArrayApply extends CollectionApply(ArrayModule, ArrayClass)
  object SeqApply extends CollectionApply(SeqModule, SeqClass)
  object IndexedSeqApply extends CollectionApply(IndexedSeqModule, IndexedSeqClass)
  object ListApply extends CollectionApply(ListModule, ListClass)
  
  def normalize(tpe: Type) = {
    var t = tpe
    if (t != null) {
      var d = t.dealias
      if (d != null) {
        t = d
        d = t.deconst
        if (d != null) {
          t = d
          d = t.widen
          if (d != null)
            t = d
        }
      }
    }
    t
  }
  object OptionTree {
    def apply(componentType: Type) = error("not implemented")
    def unapply(tree: Tree): Option[Type] = {
      if (!tree.isInstanceOf[TypeTree])
        normalize(tree.tpe) match {
          case TypeRef(_, OptionClass, List(componentType)) => 
            //println("FOUND OPTION TREE " + tree)
            Some(componentType)
          case _ =>
            None
        }
      else
        None
    }
  }
  object Predef {
    lazy val RefArrayOps = this("refArrayOps")
    lazy val GenericArrayOps = this("genericArrayOps")
    lazy val IntWrapper  = this("intWrapper")
    lazy val println  = this("println")

    def contains(sym: Symbol)        = sym.owner == PredefModule.moduleClass
    def apply(name: String): Symbol  = PredefModule.tpe member name
    def unapply(tree: Tree): Boolean = tree.symbol == PredefModule
  }
  object ArrayOps {
    lazy val ArrayOpsClass    = definitions.getClass("scala.collection.mutable.ArrayOps")

    def unapply(tree: Tree): Option[Type] = tree match {
      case TypeApply(sel, List(arg))
        if sel.symbol == Predef.RefArrayOps || sel.symbol == Predef.GenericArrayOps =>
        Some(arg.tpe)
      case _  => tree.tpe match {
        case MethodType(_, TypeRef(_, ArrayOpsClass, List(param)))
          if Predef contains tree.symbol =>
          Some(param)
        case _ =>
          None
      }
    }
  }

  object ArrayTree {
    def unapply(tree: Tree) = tree match {
      case Apply(ArrayOps(componentType), List(array)) => Some(array, componentType)
      case _ => None
    }
  }
  object ArrayTyped extends HigherTypeParameterExtractor(ArrayClass)
  
  class HigherTypeParameterExtractor(ColClass: Symbol) {
    private def isCol(s: Symbol) = 
      s.tpe == ColClass.tpe || s.tpe.toString == ColClass.tpe.toString
    private def isCol2(s: Symbol) =
      isCol(s) || isCol(s.tpe.typeSymbol)
    
    def unapply(tpe: Type): Option[Type] = Option(normalize(tpe)) collect {
      case TypeRef(_, ColClass, List(param)) =>
        param
      case TypeRef(_, cc, List(param)) if isCol2(cc) =>//tree.symbol)  => 
        param
      case PolyType(Nil, TypeRef(_, cc, List(param))) if isCol2(cc) =>
        param
    }
    
    def unapply(tree: Tree): Option[Type] = if (tree == null) None else {
      unapply(tree.tpe) match {
        case Some(s) =>
          Some(s)
        case None =>
          if ((tree ne null) && (tree.symbol ne null))
            unapply(tree.symbol.tpe)
          else
            None
      }
    }
  }
  object ListTree extends HigherTypeParameterExtractor(ListClass)

  object ArrayTabulate {
    /** This is the one all the other ones go through. */
    lazy val tabulateSyms = (ArrayModule.tpe member "tabulate" alternatives).toSet//filter (_.paramss.flatten.size == 3)

    def apply(componentType: Tree, lengths: List[Tree], function: Tree, manifest: Tree) = error("not implemented")
    def unapply(tree: Tree): Option[(Tree, List[Tree], Tree, Tree)] = {
      if (!tabulateSyms.contains(methPart(tree).symbol))
        None
      else flattenApplyGroups(tree) match {
        case List(lengths, List(function), List(manifest)) =>
          Some((typeArgs(tree).headOption getOrElse EmptyTree, lengths, function, manifest))
        case _ =>
          None
      }
    }
  }



  object TrivialCanBuildFromArg {
    private def isCanBuildFrom(tpe: Type) =
      tpe != null && tpe.dealias.matches(CanBuildFromClass.tpe)
      //tpe.dealias.deconst <:< CanBuildFromClass.tpe

    val n1 = N("canBuildIndexedSeqFromIndexedSeq") // ScalaCL
    val n2 = N("canBuildArrayFromArray") // ScalaCL
    def unapply(tree: Tree) = if (!isCanBuildFrom(tree.tpe)) None else Option(tree) collect {
      case Apply(TypeApply(Select(comp, canBuildFromName()), List(resultType)), List(_)) =>
        (comp.symbol.companionClass, resultType)
      case Apply(TypeApply(Select(comp, n1() | n2()), List(resultType)), _) =>
        (comp.symbol.companionClass, resultType)
      case TypeApply(Select(comp, canBuildFromName()), List(resultType)) =>
        (comp.symbol.companionClass, resultType)
    }
  }
  object CanBuildFromArg {
    def unapply(tree: Tree) = tree match {
      case TrivialCanBuildFromArg(_, _) => true
      case _ => false
    }
  }

  object Func {
    def unapply(tree: Tree): Option[(List[ValDef], Tree)] = Option(tree) collect {
      case Block(List(), Func(params, body)) =>
        // method references def f(x: T) = y; col.map(f) (inside the .map = a block((), xx => f(xx))
        (params, body)
      case Function(params, body) =>
        (params, body)
    }
  }
}