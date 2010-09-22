package scalacl

import scala.reflect.generic.{Names, Trees, Types}

trait MiscMatchers {
  val global: Trees with Names with Types
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
  val untilName = N("until")
  val foreachName = N("foreach")
  val mapName = N("map")
  val filterName = N("filter")
  val updateName = N("update")
  val sizeTName = N("toSizeT")
  val longName = N("toLong")
  val intName = N("toInt") 
  val shortName = N("toShort")
  val byteName = N("toByte")
  val charName = N("toChar")
  val doubleName = N("toDouble")
  val floatName = N("toFloat")
  val mathName = N("math")
  val packageName = N("package")
    

  object IntRangeForeach {
    def apply(from: Tree, to: Tree, isUntil: Boolean, functionReturnType: Tree, function: Tree) =
      Apply(TypeApply(Select(Apply(Select(Apply(Select(Select(This(scalaName), PredefName), intWrapperName), List(from)), if (isUntil) untilName else toName), List(to)), foreachName), List(functionReturnType)), List(function))

	//char	*ecvt(double, int, int *__restrict, int *__restrict); /* LEGACY */
    def unapply(tree: Tree): Option[(Tree, Tree, Boolean, Tree)] = tree match {
      //case Apply(TypeApply(Select(Apply(Select(Apply(Select(Select(This(scalaName()), PredefName), intWrapperName()), List(from)), funToName), List(to)), foreachName()), List(fRetType)), List(function)) =>
      case Apply(TypeApply(Select(Apply(Select(Apply(Select(predef, intWrapperName()), List(from)), funToName), List(to)), foreachName()), List(fRetType)), List(function)) =>
        funToName match {
          case toName() =>
            Some((from, to, false, function))
          case untilName() =>
            Some((from, to, true, function))
          case _ =>
            None
        }
      case _ =>
        None
    }
  }
}