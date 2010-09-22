/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package scalacl

import scala.reflect.generic.Constants
import scala.reflect.generic.Names
import scala.reflect.generic.Trees
import scala.reflect.generic.Types

trait Printers {
  val global: Trees with Constants with Types with Names
  import global._

  def toStr(a: AnyRef) = "(" + a.getClass.getName + ") " + a

  def printAny(arg: Any)(implicit bd: StringBuilder = new StringBuilder): Unit = arg match {
    case ImportSelector(a, b, c, d) =>
      pt("ImportSelector", a, b, c, d)
    case Modifiers(a, b, c, d) =>
      pt("Modifiers", a, b, c, d)
    case Constant(a) =>
      pt("Constant", a)
    case TypeApply(a, b) =>
      pt("TypeApply", a, b)
    case Apply(a, b) =>
      pt("Apply", a, b)
    case Select(a, b) =>
      pt("Select", a, b)
    case Super(a, b) =>
      pt("Super", a, b)
    case PackageDef(a, b) =>
      pt("PackageDef", a, b)
    case Star(a) =>
      pt("Star", a)
    case Function(a, b) =>
      pt("Function", a, b)
    case ValDef(a, b, c, d) =>
      pt("ValDef", a, b, c, d)
    case Typed(a, b) =>
      pt("Typed", a, b)
    case CompoundTypeTree(a) =>
      pt("CompoundTypeTree", a)
    case ExistentialTypeTree(a, b) =>
      pt("ExistentialTypeTree", a, b)
    case AppliedTypeTree(a, b) =>
      pt("AppliedTypeTree", a, b)
    case SelectFromTypeTree(a, b) =>
      pt("SelectFromTypeTree", a, b)
    case SingletonTypeTree(a) =>
      pt("SingletonTypeTree", a)
    case Block(a, b) =>
      pt("Block", a, b)
    case Assign(a, b) =>
      pt("Assign", a, b)
    case Annotated(a, b) =>
      pt("Annotated", a, b)
    case Alternative(a) =>
      pt("Alternative", a)
    case Ident(a) =>
      pt("Ident", a)
    case Return(a) =>
      pt("Return", a)
    case New(a) =>
      pt("New", a)
    case This(a) =>
      pt("This", a)
    case Throw(a) =>
      pt("Throw", a)
    case Literal(a) =>
      pt("Literal", a)
    case ModuleDef(a, b, c) =>
      pt("ModuleDef", a, b, c)
    case Template(a, b, c) =>
      pt("Template", a, b, c)
    case LabelDef(a, b, c) =>
      pt("LabelDef", a, b, c)
    case CaseDef(a, b, c) =>
      pt("CaseDef", a, b, c)
    case If(a, b, c) =>
      pt("If", a, b, c)
    case DefDef(a, b, c, d, e, f) =>
      pt("DefDef", a, b, c, d, e, f)
    case ClassDef(a, b, c, d) =>
      pt("ClassDef", a, b, c, d)
    case (a, b) =>
      printAny(a)
      bd.append(" -> ")
      printAny(b)
    case l: List[_] => pt("List", l:_*)
    case m: Map[_, _] => pt("Map", m.toSeq:_*)
      //case _: BoxedUnit => bd.append("()")
    case v: String => bd.append('"').append(v).append('"')
    case v: Int => bd.append(v)
    case v: Long => bd.append(v)
    case v: Short => bd.append(v)
    case v: Byte => bd.append(v)
    case v: Char => bd.append(v)
    case v: Boolean => bd.append(v)
    case v: Double => bd.append(v)
    case v: Float => bd.append(v)
    case n: Name => bd.append("newTermName(\"").append(n).append("\")")
      //case EmptyTree() =>
      //    pt("EmptyTree")
    case AnnotatedType(a, b, c) =>
      pt("AnnotatedType", a, b, c)
    case ClassInfoType(a, b, c) =>
      pt("ClassInfoType", a, b, c)
    case ConstantType(a) =>
      pt("ConstantType", a)
    case ExistentialType(a, b) =>
      pt("ExistentialType", a, b)
    case MethodType(a, b) =>
      pt("MethodType", a, b)
      /*case NoPrefix(a, b, c) =>
       pt("NoPrefix", a, b, c)
       case NoType(a, b) =>
       pt("NoType", a, b)*/
    case PolyType(a, b) =>
      pt("PolyType", a, b)
    case RefinedType(a, b) =>
      pt("RefinedType", a, b)
    case SingleType(a, b) =>
      pt("SingleType", a, b)
    case SuperType(a, b) =>
      pt("SuperType", a, b)
    case ThisType(a) =>
      pt("ThisType", a)
    case TypeBounds(a, b) =>
      pt("TypeBounds", a, b)
    case TypeRef(a, b, c) =>
      pt("TypeRef", a, b, c)
    case tt: TypeTree =>
      if (tt.equals(NoType))
        bd.append("NoType")
      else
        error("Unknown type tree: " + toStr(tt))
    case a: AnyRef =>
      if (a == EmptyTree)
        bd.append("EmptyTree")
      else
        error("Unknown type: " + toStr(a))
  }
  private def pt(n: String, args: Any*)(implicit bd: StringBuilder = new StringBuilder): Unit = {
    bd.append(n).append('(')
    val arr = args.toArray
    var i = 0
    while (i < arr.length) {
      if (i > 0)
        bd.append(", ")
      printAny(arr(i))
      i += 1
    }
    bd.append(')')
  }
}
