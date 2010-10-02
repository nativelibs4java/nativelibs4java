/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package scalacl

import scala.tools.nsc.Global

import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.{Transform, TypingTransformers}

object ArrayLoopsTransformComponent {
  val runsAfter = List[String]("namer")
  val phaseName = "arrayloopstransform"
}
class ArrayLoopsTransformComponent(val global: Global)
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

  override val runsAfter = ArrayLoopsTransformComponent.runsAfter
  override val phaseName = ArrayLoopsTransformComponent.phaseName

  def newTransformer(unit: CompilationUnit) = new TypingTransformer(unit) {
    var currentClassName: Name = null

    override def transform(tree: Tree): Tree = tree match {
      case Apply(TypeApply(Select(Apply(Select(predef, doubleArrayOpsName()), List(array)), foreachName()), List(functionReturnType)), List(Function(List(ValDef(paramMods, paramName, t1: TypeTree, rhs)), body))) =>
        //typed { array }
        val tpe = array.tpe
        val sym = tpe.typeSymbol
        array.tpe = appliedType(ArrayClass.tpe, List(DoubleClass.tpe))
        //array.tpe = sym.tpe
        val symDir = tpe.typeSymbolDirect
        val args = tpe.typeParams
        println(tree)
        println(nodeToString(tree))
        if (sym.toString == "class Array") {
          val (aIdentGen, aSym, aDef) = newVariable(unit, "array$", currentOwner, tree.pos, false, array)
          val (iIdentGen, iSym, iDef) = newVariable(unit, "i$", currentOwner, tree.pos, true, Literal(Constant(0)).setType(IntClass.tpe))
          val (nIdentGen, nSym, nDef) = newVariable(unit, "n$", currentOwner, tree.pos, false, Select(aIdentGen(), "length").setSymbol(getMember(aSym, "length")).setType(IntClass.tpe))
          typed {
            super.transform(
              treeCopy.Block(
                tree,
                List(
                  aDef,
                  iDef,
                  nDef
                ),
                whileLoop(
                  currentOwner,
                  unit,
                  tree,
                  binOp(
                    iIdentGen(),
                    IntClass.tpe.member(nme.LT),
                    nIdentGen()
                  ),
                  Block(
                    List(
                      {
                        val componentSymbol = DoubleClass
                        val r = replace(
                          paramName.toString,
                          body,
                          typed {
                            Apply(
                              Select(
                                aIdentGen(),
                                N("apply")
                              ).setSymbol(getMember(array.symbol, "apply")),
                              List(iIdentGen())
                            )
                          },
                          unit
                        )
                        unit.comment(tree.pos, "ScalaCL plugin transformed array foreach into equivalent while loop.")
                        println(tree.pos + ": transformed array foreach into equivalent while loop.")
                        //println("REPLACED <<<\n" + body + "\n>>> by <<<\n" + r + "\n>>>")
                        typed { r }
                      }
                    ),
                    incrementIntVar(iIdentGen, typed { Literal(Constant(1)) })
                  )
                )
              )
            )
          }
        } else
          tree
      case _ =>
        super.transform(tree)
    }
  }
}
