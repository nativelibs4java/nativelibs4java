package scalacl

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.Plugin

/** A class describing the compiler plugin
 *
 *  @todo Adapt the name of this class to the plugin being
 *  implemented
 */
class ScalaCLPlugin(val global: Global) extends Plugin {
  override val name = "ScalaCL Optimizer"
  override val description =
    "This plugin transforms some Scala functions into OpenCL kernels (for CLCol[T].map and filter's arguments), so they can run on a GPU.\n" +
    "It will also soon feature autovectorization of ScalaCL programs, detecting parallelizable loops and unnecessary collection creations."

  val runsAfter = List[String]("refchecks")
  override val components = ScalaCLPlugin.components(global)
}

object ScalaCLPlugin {
  def components(global: Global) = List(new ScalaCLFunctionsTransformComponent(global))
}

import scala.reflect.NameTransformer
import scala.tools.nsc.Global
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.{Transform, TypingTransformers}

class ScalaCLFunctionsTransformComponent(val global: Global)
extends PluginComponent
   with Transform
   with TypingTransformers
{
  import global._
  import global.definitions._

  override val runsAfter = List[String]("refchecks")
  override val phaseName = "scalaclfunctionstransform"

  def newTransformer(unit: CompilationUnit) = new TypingTransformer(unit) {
    override def transform(tree: Tree): Tree = {
      val trans = tree match {
        case Apply(a1 @ TypeApply(s, typeArgs), List(a2 @ Apply(a3 @ Apply(a4 @ TypeApply(convertFunction, typeArgs2), List(singleArg)), impArgs @ List(io1, io2)))) =>
          try {
            if (s.symbol.toString == "method map" && convertFunction.toString == "scalacl.package.Expression2CLFunction") {
              singleArg match {
                case Function(List(ValDef(paramMods, paramName, t1: TypeTree, rhs)), body) =>
                  val conv = convertExpr(paramName.toString, body).toString
                  println("Converted <<< " + body + " >>> to <<< \"" + conv + "\" >>>")
                  val newArgs = List(singleArg, Literal(Constant(conv)))
                  //val newArgs = List(singleArg)
                  treeCopy.Apply(tree, treeCopy.TypeApply(a1, s, typeArgs), List(treeCopy.Apply(a2, treeCopy.Apply(a3, treeCopy.TypeApply(a4, convertFunction, typeArgs2), newArgs), impArgs)))
                case _ =>
                  // Already converted, maybe ?
                  tree
              }
            } else
              tree
          } catch {
            case ex =>
              ex.printStackTrace
              tree
          }
        case _ =>
          //println("TREE " + tree.getClass.getName + " : " + tree)
          tree
      }
      super.transform(trans)
    }

    def convertExpr(argName: String, body: Tree, b: StringBuilder = new StringBuilder): StringBuilder = {
      body match {
        case Literal(Constant(value)) =>
          b.append(value)
        case Ident(name) =>
          if (name.toString.equals(argName))
            b.append("_")
          else
            error("Unknown identifier : '" + name + "' (expected '" + argName + "')")
        case Apply(s @ Select(left, name), args) =>
          NameTransformer.decode(name.toString) match {
            case op @ ("+" | "-" | "*" | "/" | "%" | "^" | "^^" | "&" | "&&" | "|" | "||" | "<<" | ">>") =>
              convertExpr(argName, left, b)
              b.append(' ').append(op).append(' ')
              convertExpr(argName, args(0), b)
            case n => error("Unhandled method name !")
          }
        //case Apply(TypeApply(fun, args1), args2) =>
          /*NameTransformer.decode(name.toString) match {
            case "foreach" =>
              b.append("FOREACH")
            case _ =>
              println("traversing application of "+ name)
          }*/
        case Typed(expr, tpe) =>
          convertExpr(argName, expr, b)
        case _ =>
          println("Failed to convert " + body.getClass.getName + ": " + body)
      }
      b
    }
    def convertTpt(tpt: TypeTree) = tpt.toString match {
      case "Int" => "int"
      case "Long" => "long"
      case "org.bridj.SizeT" => "size_t"
      case "Float" => "float"
      case "Double" => "double"
      case _ => error("Cannot convert unknown type " + tpt + " to OpenCL")
    }
  }
}
