package scalacl

import scala.collection.immutable.Stack
import scala.reflect.generic.Names
import scala.reflect.generic.Trees
import scala.tools.nsc.Global
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.symtab.Definitions

/**
 * http://www.scala-lang.org/node/140
 * http://lamp.epfl.ch/~emir/bqbase/2005/06/02/nscTutorial.html
 * http://code.google.com/p/simple-build-tool/wiki/CompilerPlugins
 * mvn scala:run -DmainClass=scalacl.Compile "-DaddArgs=-d|out|src/main/examples/BasicExample.scala|-Xprint:scalaclfunctionstransform"
 * scala -cp target/scalacl-compiler-1.0-SNAPSHOT-shaded.jar scalacl.Main -d out src/examples/BasicExample.scala
 * javap -c -classpath out/ scalacl.examples.BasicExample
 */
class ScalaCLPlugin(val global: Global) extends Plugin {
  override val name = "ScalaCL Optimizer"
  override val description =
    "This plugin transforms some Scala functions into OpenCL kernels (for CLCol[T].map and filter's arguments), so they can run on a GPU.\n" +
  "It will also soon feature autovectorization of ScalaCL programs, detecting parallelizable loops and unnecessary collection creations."

  val runsAfter = List[String]("namer")//refchecks")
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
   with OpenCLConverter
{
  import global._
  import global.definitions._
  import scala.tools.nsc.symtab.Flags._
  import typer.{typed, atOwner}    // methods to type trees

  override val runsAfter = List[String]("namer")//refchecks")
  override val phaseName = "scalaclfunctionstransform"

  def nodeToStringNoComment(tree: Tree) =
    nodeToString(tree).replaceAll("\\s*//.*\n", "\n").replaceAll("\\s*\n\\s*", " ").replaceAll("\\(\\s+", "(").replaceAll("\\s+\\)", "")
  
  def newTransformer(unit: CompilationUnit) = new TypingTransformer(unit) {
    var currentClassName: Name = null

    override def transform(tree: Tree): Tree = tree match {
      // Transform inline functions into OpenCL mixed functions / expression code
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
        val functionOpenCLExprString = convertExpr(Map(paramName.toString -> "_"), body).toString
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
      case _ =>
        super.transform(tree)
    }

  }
}
