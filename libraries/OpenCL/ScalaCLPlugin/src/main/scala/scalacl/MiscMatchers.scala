package scalacl

import scala.reflect.generic.{Names, Trees, Types, Constants}

trait MiscMatchers {
  val global: Trees with Names with Types with Constants
  import global._

  class N(val s: String) {
    def unapply(n: Name): Boolean = n.toString == s
  }
  object N {
    def apply(s: String) = new N(s)
  }
  implicit def N2Name(n: N) = newTermName(n.s)
  
  val scalaName = N("scala")
  val PredefName = N("Predef")
  val intWrapperName = N("intWrapper")
  val toName = N("to")
  val byName = N("by")
  val untilName = N("until")
  val foreachName = N("foreach")
  val mapName = N("map")
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

	//char	*ecvt(double, int, int *__restrict, int *__restrict); /* LEGACY */
    def unapply(tree: Tree): Option[(Tree, Tree, Tree, Boolean, Tree)] = tree match {
      //case Apply(TypeApply(Select(Apply(Select(Apply(Select(Select(This(scalaName()), PredefName), intWrapperName()), List(from)), funToName), List(to)), foreachName()), List(fRetType)), List(function)) =>
      /*case Apply(TypeApply(Select(Apply(Select(Apply(Select(Apply(Select(predef, intWrapperName()), List(from)), funToName), List(to)), byName()), List(by)), foreachName()), List(fRetType)), List(function)) =>
        funToName match {
          case toName() =>
            Some((from, to, by, false, function))
          case untilName() =>
            Some((from, to, by, true, function))
          case _ =>
            None
        }*/
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
}