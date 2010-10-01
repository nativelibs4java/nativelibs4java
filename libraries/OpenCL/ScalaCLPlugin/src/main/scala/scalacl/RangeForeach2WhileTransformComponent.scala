/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package scalacl

import scala.tools.nsc.Global

import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.{Transform, TypingTransformers}

object RangeForeach2WhileTransformComponent {
  val runsAfter = List[String]("namer")
  val phaseName = "rangeforeach2whiletransform"
}
class RangeForeach2WhileTransformComponent(val global: Global)
extends PluginComponent
   with Transform
   with TypingTransformers
   with MiscMatchers
   with TreeBuilders
   with Printers
{
  import global._
  import global.definitions._
  import scala.tools.nsc.symtab.Flags._
  import typer.{typed, atOwner}    // methods to type trees

  override val runsAfter = RangeForeach2WhileTransformComponent.runsAfter
  override val phaseName = RangeForeach2WhileTransformComponent.phaseName

  def newTransformer(unit: CompilationUnit) = new TypingTransformer(unit) {
    var currentClassName: Name = null

    override def transform(tree: Tree): Tree =
      tree match {
      case IntRangeForeach(from, to, by, isUntil, f @ Function(List(ValDef(paramMods, paramName, t1: TypeTree, rhs)), body)) =>
        val (iIdentGen, iDef) = newIntVariable(unit, "i", currentOwner, tree.pos, true, from)
        val (nIdentGen, nDef) = newIntVariable(unit, "n", currentOwner, tree.pos, false, to)
        typed {
          super.transform(
            treeCopy.Block(
              tree,
              List(
                iDef,
                nDef
              ),
              whileLoop(
                currentOwner,
                unit,
                tree,
                binOp(
                  iIdentGen(),
                  if (isUntil) IntClass.tpe.member(nme.LT) else IntClass.tpe.member(nme.LE),
                  nIdentGen()
                ),
                Block(
                  List(
                    {
                      val r = replace(paramName.toString, body, iIdentGen(), unit)
                      unit.comment(tree.pos, "ScalaCL plugin transformed int range foreach loop into equivalent while loop.")
                      println(tree.pos + ": transformed int range foreach loop into equivalent while loop.")
                      //println("REPLACED <<<\n" + body + "\n>>> by <<<\n" + r + "\n>>>")
                      typed { r }
                    }
                  ),
                  incrementIntVar(iIdentGen, by)
                )
              )
            )
          )
        }
      case _ =>
        super.transform(tree)
    }
  }
}