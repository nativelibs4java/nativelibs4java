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

import scala.reflect.AppliedType
import scala.reflect.generic.{Names, Trees, Types, Constants, Symbols, StandardDefinitions, Universe}
import scala.tools.nsc.Global
import scala.tools.nsc.symtab.Definitions

trait MiscMatchers {
  //this: TreeBuilders =>
  val global: Universe//Trees with Names with Types with Constants with Definitions with Symbols with StandardDefinitions
  import global._
  import definitions._

  class Ids(start: Long = 1) {
    private var nx = start
    def next = this.synchronized {
      val v = nx
      nx += 1
      v
    }
  }
  
  class N(val s: String) {
    def unapply(n: Name): Boolean = n.toString == s
  }
  object N {
    def apply(s: String) = new N(s)
  }
  implicit def N2Name(n: N) = newTermName(n.s)
  
  val scalaName = N("scala")
  val ArrayName = N("Array")
  val PredefName = N("Predef")
  val intWrapperName = N("intWrapper")
  val toName = N("to")
  val byName = N("by")
  val withFilterName = N("withFilter")
  val untilName = N("until")
  val foreachName = N("foreach")
  val foldLeftName = N("foldLeft")
  val foldRightName = N("foldRight")
  val reduceLeftName = N("reduceLeft")
  val reduceRightName = N("reduceRight")
  val scanLeftName = N("scanLeft")
  val scanRightName = N("scanRight")
  val doubleArrayOpsName = N("doubleArrayOps")
  val floatArrayOpsName = N("floatArrayOps")
  val shortArrayOpsName = N("shortArrayOps")
  val intArrayOpsName = N("intArrayOps")
  val longArrayOpsName = N("longArrayOps")
  val byteArrayOpsName = N("byteArrayOps")
  val charArrayOpsName = N("charArrayOps")
  val refArrayOpsName = N("refArrayOps")
  val booleanArrayOpsName = N("booleanArrayOps")
  // TODO
  val mapName = N("map")
  val canBuildFromName = N("canBuildFrom")
  val filterName = N("filter")
  val updateName = N("update")
  val toSizeTName = N("toSizeT")
  val toLongName = N("toLong")
  val toIntName = N("toInt")
  val toShortName = N("toShort")
  val toByteName = N("toByte")
  val toCharName = N("toChar")
  val toDoubleName = N("toDouble")
  val toFloatName = N("toFloat")
  val mathName = N("math")
  val packageName = N("package")
    
  object ScalaMathFunction {
    def apply(functionName: String, args: List[Tree]) =
      Apply(Select(Select(Select(Ident(scalaName), mathName), packageName), N(functionName)), args)
        
    def unapply(tree: Tree): Option[(Name, List[Tree])] = tree match {
      case
        Apply(
          Select(
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
        Some((funName, args))
      case _ =>
        None
    }
  }
  object IntRange {
    def apply(from: Tree, to: Tree, by: Option[Tree], isUntil: Boolean, filters: List[Tree]) = error("not implemented")

	def unapply(tree: Tree): Option[(Tree, Tree, Option[Tree], Boolean, List[Tree])] = tree match {
      case Apply(Select(Apply(Select(Predef(), intWrapperName()), List(from)), funToName @ (toName() | untilName())), List(to)) =>
        funToName match {
          case toName() =>
            Some((from, to, None, false, Nil))
          case untilName() =>
            Some((from, to, None, true, Nil))
          case _ =>
            None
        }
      case Apply(Select(tg, n @ (byName() | withFilterName())), List(arg)) =>
       tg match {
          case IntRange(from, to, by, isUntil, filters) =>
            n match {
                case byName() if by == None =>
                    Some((from, to, Some(arg), isUntil, filters))
                case withFilterName() =>
                    Some((from, to, by, isUntil, filters ++ List(arg)))
                case _ =>
                    None
            }
          case _ =>
            None
        }
    }
  }
  object Foreach {
    def apply(target: Tree, function: Tree) = error("not implemented")

	def unapply(tree: Tree): Option[(Tree, Tree)] = tree match {
      case Apply(TypeApply(Select(target, foreachName()), List(fRetType)), List(function)) =>
        Some((target, function))
      case _ =>
        None
    }
  }

  object Predef {
    def unapply(tree: Tree) = tree match {
      case Select(This(scalaName()), PredefName()) => true
      case _ => false
    }
  }
  object PrimitiveArrayOps {
    def unapply(tree: Tree): Option[Symbol] = tree match {
      case 
        Select(
          Predef(),
          n
        ) =>
        n match {
          case doubleArrayOpsName() => Some(DoubleClass)
          case floatArrayOpsName() => Some(FloatClass)
          case intArrayOpsName() => Some(IntClass)
          case shortArrayOpsName() => Some(ShortClass)
          case longArrayOpsName() => Some(LongClass)
          case byteArrayOpsName() => Some(ByteClass)
          case charArrayOpsName() => Some(CharClass)
          case booleanArrayOpsName() => Some(BooleanClass)
          case _ => None
        }
      case _ =>
        None
    }
  }
  object RefArrayOps {
    def unapply(tree: Tree) = tree match {
      case
        TypeApply(
          Select(
            Predef(),
            refArrayOpsName()),
          List(tt @ TypeTree())
        ) =>
        Some(tt)
      case _ =>
        None
    }
  }
  object ArrayOps {
    def unapply(tree: Tree) = tree match {
      case PrimitiveArrayOps(componentType) =>
        Some(componentType)
      case RefArrayOps(componentType) =>
        Some(componentType.symbol)
      case _ =>
        None
    }
  }

  object ArrayForeach {
    def apply(array: Tree, componentType: Symbol, paramName: Name, body: Tree) = error("not implemented")
    def unapply(tree: Tree): Option[(Tree, Symbol, Name, Tree)] = tree match {
      case
        Apply(
          TypeApply(
            Select(
              Apply(
                ArrayOps(componentType),
                List(array)
              ),
              foreachName()
            ),
            List(functionReturnType)
          ),
          List(Func1(paramName, body))
        ) =>
        Some((array, componentType, paramName, body))
      case _ =>
        None
    }
  }

  object CanBuildFromArg {
    def unapply(tree: Tree) = tree match {
      case
        Apply(
          TypeApply(
            Select(
              xxx,
              canBuildFromName()
            ),
            yyy
          ),
          zzz
        ) =>
        true
      case _ =>
        false
    }
  }

  object ArrayMap {
    def apply(array: Tree, componentType: Symbol, mappedComponentType: Symbol, paramName: Name, body: Tree) = error("not implemented")
    def unapply(tree: Tree): Option[(Tree, Symbol, Symbol, Name, Tree)] = tree match {
      case 
        Apply(
          Apply(
            TypeApply(
              Select(
                Apply(
                  ArrayOps(componentType),
                  List(array)
                ),
                mapName()
              ),
              List(functionArgType, mappedArrayType)
            ),
            List(Func1(paramName, body))
          ),
          List(CanBuildFromArg())
        ) =>
        val tpe = array.tpe
        val sym = tpe.typeSymbol
        mappedArrayType.tpe match {
          case TypeRef(_, _, List(TypeRef(_, sym, args))) =>
            Some((array, componentType, sym, paramName, body))
          case _ =>
            None
        }
      case _ =>
        None
    }
  }

  object Func1 {
    def unapply(tree: Tree): Option[(Name, Tree)] = tree match {
      case Block(List(), Func1(paramName, body)) =>
        Some(paramName, body)
      case Function(List(ValDef(_, paramName, _, _)), body) =>
        Some(paramName, body)
      case _ =>
        None
    }
  }
  object Func2 {
    def unapply(tree: Tree): Option[(Name, Name, Tree)] = tree match {
      case Block(List(), Func2(paramName1, paramName2, body)) =>
        //if tree.symbol.toString == "trait Function2"
        Some(paramName1, paramName2, body)
      case Function(List(ValDef(_, paramName1, _, _), ValDef(_, paramName2, _, _)), body) => // paramMods, paramName, TypeTree(), rhs
        Some(paramName1, paramName2, body)
      case _ =>
        None
    }
  }
  sealed abstract class TraversalOpType
  case object Fold extends TraversalOpType {
    override def toString = "fold"
  }
  case object Scan extends TraversalOpType {
    override def toString = "scan"
  }
  case object Reduce extends TraversalOpType {
    override def toString = "reduce"
  }
  object ReduceName {
    def apply(isLeft: Boolean) = error("not implemented")
    def unapply(name: Name) = name match {
      case reduceLeftName() => Some(true)
      case reduceRightName() => Some(false)
      case _ => None
    }
  }
  object ScanName {
    def apply(isLeft: Boolean) = error("not implemented")
    def unapply(name: Name) = name match {
      case scanLeftName() => Some(true)
      case scanRightName() => Some(false)
      case _ => None
    }
  }
  object FoldName {
    def apply(isLeft: Boolean) = error("not implemented")
    def unapply(name: Name) = name match {
      case foldLeftName() => Some(true)
      case foldRightName() => Some(false)
      case _ => None
    }
  }

  /// Matches one of the folding/scanning/reducing functions : (reduce|fold|scan)(Left|Right)
  object TraversalOp {
    def apply(array: Tree, componentType: Symbol, resultType: Symbol, leftParamName: Name, rightParamName: Name, op: TraversalOpType, isLeft: Boolean, body: Tree, initialValue: Tree) = error("not implemented")
    def unapply(tree: Tree): Option[(Tree, Symbol, Symbol, Name, Name, TraversalOpType, Boolean, Tree, Tree)] = tree match {
      case // PRIMITIVE OR REF SCAN : scala.this.Predef.refArrayOps[A](array: Array[A]).scanLeft[B, Array[B]](initialValue)(function)(canBuildFromArg)
        Apply(
          Apply(
            Apply(
              TypeApply(
                Select(
                  Apply(
                    ArrayOps(componentType),
                    List(array)
                  ),
                  ScanName(isLeft)
                ),
                List(functionArgType, mappedArrayType)
              ),
              List(initialValue)
            ),
            List(Func2(leftParamName, rightParamName, body))
          ),
          List(CanBuildFromArg())
        ) =>
        val tpe = array.tpe
        mappedArrayType.tpe match {
          case TypeRef(_, _, List(TypeRef(_, sym, args))) =>
            Some((array, componentType, sym, leftParamName, rightParamName, Scan, isLeft, body, initialValue))
          case _ =>
            None
        }
      case // PRIMITIVE OR REF FOLD : scala.this.Predef.refArrayOps[A](array: Array[A]).foldLeft[B](initialValue)(function)
        Apply(
          Apply(
            TypeApply(
              Select(
                Apply(
                  ArrayOps(componentType),
                  List(array)
                ),
                FoldName(isLeft)
              ),
              List(functionResultType)
            ),
            List(initialValue)
          ),
          List(Func2(leftParamName, rightParamName, body))
        ) =>
        Some((array, componentType, functionResultType.symbol, leftParamName, rightParamName, Fold, isLeft, body, initialValue))
      case // PRIMITIVE OR REF REDUCE : scala.this.Predef.refArrayOps[A](array: Array[A]).reduceLeft[B](function)
        Apply(
          TypeApply(
            Select(
              Apply(
                ArrayOps(componentType),
                List(array)
              ),
              ReduceName(isLeft)
            ),
            List(functionResultType)
          ),
          List(Func2(leftParamName, rightParamName, body))
        ) =>
        Some((array, componentType, functionResultType.symbol, leftParamName, rightParamName, Reduce, isLeft, body, null))
      case _ =>
        None
    }
  }

}