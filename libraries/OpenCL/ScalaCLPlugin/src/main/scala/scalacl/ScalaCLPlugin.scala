package scalacl

import scala.reflect.generic.Names
import scala.reflect.generic.Trees
import scala.tools.nsc.Global
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.symtab.Definitions

/** 
 * http://code.google.com/p/simple-build-tool/wiki/CompilerPlugins
 * mvn scala:run -DmainClass=scalacl.Compile "-DaddArgs=-d|out|src/examples/BasicExample.scala|-Xprint:scalaclfunctionstransform"
 * scala -cp target/scalacl-compiler-1.0-SNAPSHOT-shaded.jar scalacl.Main -d out src/examples/BasicExample.scala
 * javap -c -classpath out/ scalacl.examples.BasicExample
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
   with MiscMatchers
   with Printers
{
  import global._
  import global.definitions._
  import scala.tools.nsc.symtab.Flags._
  import typer.{typed, atOwner}    // methods to type trees

  override val runsAfter = List[String]("refchecks")
  override val phaseName = "scalaclfunctionstransform"

  class Ids(start: Long = 1) {
    private var nx = start
    def next = this.synchronized {
      val v = nx
      nx += 1
      v
    }
  }

  def nodeToStringNoComment(tree: Tree) =
    nodeToString(tree).replaceAll("\\s*//.*\n", "\n").replaceAll("\\s*\n\\s*", " ").replaceAll("\\(\\s+", "(").replaceAll("\\s+\\)", "")

  val eqName = N("$eq")
  def replace(varName: String, tree: Tree, by: Tree, unit: CompilationUnit) = new TypingTransformer(unit) {
    val n = N(varName)
    override def transform(tree: Tree): Tree = tree match {
      case Ident(n()) =>
        by
      
      case Select(This(n), id) =>
        Ident(id)
      case Apply(Select(This(n), varEq), List(v)) =>
        //Apply(Select(a, N(NameTransformer.encode(op))), List(b))
        val dec = varEq.decode
        println("varEq = '" + varEq + "', dec = '" + dec + "'")
        val transV = super.transform(v)
        if (varEq.toString.endsWith("_$eq")) {
          val n = varEq.toString.substring(0, varEq.length - "_$eq".length)
          Assign(Ident(N(n)), transV)
        } else
          Apply(Ident(varEq), List(transV))//super.transform(tree)
      
      case _ =>
        //println("Unknown expr: " + nodeToStringNoComment(tree))
        super.transform(tree)
    }
  }.transform(tree)
  
  def newTransformer(unit: CompilationUnit) = new TypingTransformer(unit) {
    var currentClassName: Name = null
    implicit def O(n: Name)(implicit symbol: Symbol) = atOwner(symbol) { n }
    implicit def O(t: Tree)(implicit symbol: Symbol) = atOwner(symbol) { t }

    val labelIds = new Ids
    val whileIds = new Ids

    def binOp(a: Tree, op: String, b: Tree) = Apply(Select(a, N(NameTransformer.encode(op))), List(b))
    def incrementIntVar(n: Name) = Assign(Ident(n), binOp(Ident(n), "+", Literal(Constant(1))))
    def intVar(n: Name, initValue: Tree) = ValDef(Modifiers(MUTABLE), n, TypeTree(IntClass.tpe), initValue)
    def intVal(n: Name, initValue: Tree) = ValDef(Modifiers(0), n, TypeTree(IntClass.tpe), initValue)
    def whileLoop(cond: Tree, body: Tree) = {
      val lab = "while$" + whileIds.next
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
              Apply(Ident(lab), Nil)
            ),
            Literal(Constant())
          )
        )
    }
    
    override def transform(tree0: Tree): Tree = {
      val tree = super.transform(tree0)
      
      //println("case " + printAny(tree).toString)
      if (false) {
        println("case " + nodeToStringNoComment(tree) + " => ")
        val lines = tree.toString
        if (lines.contains("\n"))
          println("\t/*\n\t\t" + tree.toString.replaceAll("\n", "\n\t\t") + "\n\t*/")
        else
          println("\t// " + lines)
      }


      val trans: Tree = tree match {
        case ClassDef(mods, name, tparams, template) =>
          currentClassName = name
          tree
        case IntRangeForeach(from, to, isUntil, Function(List(ValDef(paramMods, paramName, t1: TypeTree, rhs)), body)) =>
          println("from = " + from)
          println("to = " + to)
          println("FROMTO = " + toStr(tree))
          tree
          /*
          val id = labelIds.next
          val iVar = N("iVar$" + id)
          val nVal = N("nVal$" + id)
          
          localTyper.typed { atPos(tree.pos) { atOwner(tree.symbol) {
            Block(
              List(
                intVar(iVar, from),
                intVal(nVal, to)
              ),
              whileLoop(
                binOp(Ident(iVar), if (isUntil) "<" else "<=", Ident(nVal)),
                Block(
                  List(
                    /*{
                      val r = localTyper.typed { replace(paramName.toString, body, Ident(iVar), unit) }
                      println("REPLACED <<<\n" + body + "\n>>> by <<<\n" + r + "\n>>>")
                      r
                    }*/
                  ),
                  incrementIntVar(iVar)
                )
              )
            )
          }}}
          */
        case
          Apply(
            Apply(
              TypeApply(
                Select(collectionExpr, functionName @ (mapName() | filterName() | updateName())),
                funTypeArgs @ List(outputType @ TypeTree())
              ),
              List(functionExpr @ Function(List(ValDef(paramMods, paramName, inputType @ TypeTree(), rhs)), body))
            ),
            implicitArgs @ List(io1, io2)
          ) =>
          /*
           * if functionExpr is :
           *    ((x$1: Int) => (x$1: Int).*(2.0)),
           * functionOpenCLExprString will be :
           *    "_ * 2.0"
           */
          val uniqueSignature = Literal(Constant(tree.symbol.outerSource + "" + tree.symbol.tag + tree.symbol.pos)) // TODO
          val functionOpenCLExprString = convertExpr(paramName.toString, body).toString
          println("Converted <<< " + body + " >>> to <<< \"" + functionOpenCLExprString + "\" >>>")
          def seqExpr(typeExpr: Tree, values: Tree*) =
            Apply(
              TypeApply(
                Select(
                  Select(Select(Ident(N("scala")), N("collection")), N("Seq")),
                  N("apply")
                ),
                List(typeExpr)
              ),
              values.toList
            )

          val newArg =
            Apply(
              Apply(
                TypeApply(
                  Select(
                    Select(
                      Ident(N("scalacl")),
                      N("package")
                    ),
                    N("CLFullFun")
                  ),
                  List(inputType, outputType)
                ),
                List(
                  uniqueSignature,
                  functionExpr,
                  seqExpr(TypeTree(StringClass.tpe)), // statements TODO
                  seqExpr(TypeTree(StringClass.tpe), Literal(Constant(functionOpenCLExprString))) // expressions TODO
                )
              ),
              implicitArgs
            )
          //val newArgs = List(singleArg)
          localTyper.typed { atPos(tree.pos) { atOwner(tree.symbol) {
            Apply(
              Apply(
                TypeApply(
                  Select(collectionExpr, N("mapFun")),
                  funTypeArgs
                ),
                List(newArg)
              ),
              implicitArgs
            )
          } } }
        case _ =>
          tree
      }
      super.transform(trans)
    }
    
    def cast(expr: Tree, clType: String) = {
        b.append("((" + clType + ")")
        convertExpr(expr, b)
        b.append(")")
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
        case Apply(Select(expr, sizeTName()), Nil) => cast(expr, "size_t")
        case Apply(Select(expr, longName()), Nil) => cast(expr, "long")
        case Apply(Select(expr, intName()), Nil) => cast(expr, "int")
        case Apply(Select(expr, shortName()), Nil) => cast(expr, "short")
        case Apply(Select(expr, byteName()), Nil) => cast(expr, "char")
        case Apply(Select(expr, charName()), Nil) => cast(expr, "short")
        case Apply(Select(expr, doubleName()), Nil) => cast(expr, "double")
        case Apply(Select(expr, floatName()), Nil) => cast(expr, "float")
        case Apply(TypeApply(Select(Select(Select(Ident(scalaName())), mathName()), packageName()), funName), List(argType)), List(args)) =>
            b.append(funName).append("(")
            var first = false
            for (arg <- args) {
                if (first)
                    first = false
                else
                    b.append(", ")
                convertExpr(arg, b)
            }
            b.append(")")
        case Apply(Select(expr, fun), Nil) =>
            val fn = fun.toString
            if (fn.matches("_\\d+")) {
                convertExpr(expr, b)
                b.append(".").append(fn)
            }
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
