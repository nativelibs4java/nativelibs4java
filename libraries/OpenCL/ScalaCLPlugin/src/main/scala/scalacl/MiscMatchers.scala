package scalacl

import scala.reflect.AppliedType
import scala.reflect.generic.{Names, Trees, Types, Constants, Symbols, StandardDefinitions, Universe}
import scala.tools.nsc.Global
import scala.tools.nsc.symtab.Definitions

trait MiscMatchers {
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
  val untilName = N("until")
  val foreachName = N("foreach")
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

  object IntRangeForeach {
    def apply(from: Tree, to: Tree, by: Tree, isUntil: Boolean, functionReturnType: Tree, function: Tree) =
      Apply(TypeApply(Select(Apply(Select(Apply(Select(Select(This(scalaName), PredefName), intWrapperName), List(from)), if (isUntil) untilName else toName), List(to)), foreachName), List(functionReturnType)), List(function))

	def unapply(tree: Tree): Option[(Tree, Tree, Tree, Boolean, Tree)] = tree match {
      case Apply(TypeApply(Select(Apply(Select(Apply(Select(Apply(Select(predef, intWrapperName()), List(from)), funToName), List(to)), byName()), List(by)), foreachName()), List(fRetType)), List(function)) =>
        funToName match {
          case toName() =>
            Some((from, to, by, false, function))
          case untilName() =>
            Some((from, to, by, true, function))
          case _ =>
            None
        }
      case Apply(TypeApply(Select(Apply(Select(Apply(Select(predef, intWrapperName()), List(from)), funToName), List(to)), foreachName()), List(fRetType)), List(function)) =>
        funToName match {
          case toName() =>
            Some((from, to, Literal(Constant(1)), false, function))
          case untilName() =>
            Some((from, to, Literal(Constant(1)), true, function))
          case _ =>
            None
        }
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
  object ArrayForeach {
    def apply(array: Tree, componentType: Symbol, paramName: Name, body: Tree) = error("not implemented")
    def unapply(tree: Tree): Option[(Tree, Symbol, Name, Tree)] = tree match {
      case Apply(
          TypeApply(
            Select(
              Apply(
                Select(
                  Predef(),
                  n @ (
                    doubleArrayOpsName() |
                    floatArrayOpsName() |
                    intArrayOpsName() |
                    shortArrayOpsName() |
                    longArrayOpsName() |
                    byteArrayOpsName() |
                    charArrayOpsName() |
                    booleanArrayOpsName()/* |
                    refArrayOpsName()*/
                  )
                ),
                List(array)
              ),
              foreachName()
            ),
            List(functionReturnType)
          ),
          List(
            Function(
              List(
                ValDef(
                  paramMods,
                  paramName,
                  t1: TypeTree,
                  rhs
                )
              ),
              body
            )
          )
        ) =>
        val tpe = array.tpe
        val sym = tpe.typeSymbol
        val componentType = //functionReturnType.symbol/*
          n match {
            case doubleArrayOpsName() => DoubleClass
            case floatArrayOpsName() => FloatClass
            case intArrayOpsName() => IntClass
            case shortArrayOpsName() => ShortClass
            case longArrayOpsName() => LongClass
            case byteArrayOpsName() => ByteClass
            case charArrayOpsName() => CharClass
            case booleanArrayOpsName() => BooleanClass
            //case refArrayOpsName() => functionReturnType.symbol
          }
        
        val symStr = sym.toString
        if (symStr == "class Array")// || symStr == "class ArrayOps")
          Some((array, componentType, paramName, body))
        else
          None
      case
        Apply(
          TypeApply(
            Select(
              Apply(
                TypeApply(
                  Select(
                    Predef(),
                    refArrayOpsName()
                  ),
                  List(functionReturnType)
                ),
                List(array)
              ),
              foreachName()
            ),
            List(functionReturnType2)
          ),
          List(
            Function(
              List(
                ValDef(
                  paramMods,
                  paramName,
                  t1: TypeTree,
                  rhs
                )
              ),
              body
            )
          )
        ) =>
        Some((array, functionReturnType.symbol, paramName, body))
      case _ =>
        None
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
                  Select(
                    Predef(),
                    n @ (
                      doubleArrayOpsName() |
                      floatArrayOpsName() |
                      intArrayOpsName() |
                      shortArrayOpsName() |
                      longArrayOpsName() |
                      byteArrayOpsName() |
                      charArrayOpsName() |
                      booleanArrayOpsName()/* |
                      refArrayOpsName()*/
                    )
                  ),
                  List(array)
                ),
                mapName()
              ),
              List(functionArgType, mappedArrayType)//AppliedTypeTree(arrayType, List(functionReturnType)))
            ),
            List(
              Function(
                List(
                  ValDef(
                    paramMods,
                    paramName,
                    t1: TypeTree,
                    rhs
                  )
                ),
                body
              )
            )
          ),
          List(
            Apply(
              TypeApply(
                Select(
                  xxx,
                  canBuildFromName()
                ),
                yyy
              ),
              zzz
            )
          )
        ) =>
        val tpe = array.tpe
        val sym = tpe.typeSymbol
        val componentType = //functionReturnType.symbol/*
          n match {
            case doubleArrayOpsName() => DoubleClass
            case floatArrayOpsName() => FloatClass
            case intArrayOpsName() => IntClass
            case shortArrayOpsName() => ShortClass
            case longArrayOpsName() => LongClass
            case byteArrayOpsName() => ByteClass
            case charArrayOpsName() => CharClass
            case booleanArrayOpsName() => BooleanClass
            //case refArrayOpsName() => functionReturnType.symbol
          }

        val symStr = sym.toString

        if (symStr == "class Array") {
          mappedArrayType.tpe match {
            case TypeRef(_, _, List(TypeRef(_, sym, args))) =>
              Some((array, componentType, sym, paramName, body))
            case _ =>
              None
          }
        }
        else
          None
      case
        Apply(
          Apply(
            TypeApply(
              Select(
                Apply(
                  TypeApply(
                    Select(
                      Predef(),
                      refArrayOpsName()),
                    List(
                      TypeTree()
                    )
                  ),
                  List(
                    array
                  )
                ),
                mapName()
              ),
              List(componentType, mappedArrayType)
            ),
            List(
              Function(
                List(
                  ValDef(
                    paramMods,
                    paramName,
                    t1: TypeTree,
                    rhs
                  )
                ),
                body
              )
            )
          ),
          List(
            Apply(
              TypeApply(
                Select(
                  Select(
                    This(scalaName()),
                    ArrayName()
                  ),
                  canBuildFromName()
                ),
                List(TypeTree())
              ),
              List(manif)
            )
          )
        ) =>
        mappedArrayType.tpe match {
          case TypeRef(_, _, List(TypeRef(_, sym, args))) =>
            Some((array, componentType.symbol, sym, paramName, body))
          case _ =>
            None
        }
      case _ =>
        None
    }
  }
}