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
class NaiveOptimizerPlugin(val global: Global) extends Plugin {
  override val name = "Naive Optimizer Plugin"
  override val description =
    "This plugin transforms foreach loops on integer ranges into equivalent while loops, which execute much faster."

  val runsAfter = RangeForeach2WhileTransformComponent.runsAfter
  override val components = NaiveOptimizerPlugin.components(global)
}

object NaiveOptimizerPlugin {
  def components(global: Global) = List(new RangeForeach2WhileTransformComponent(global))
}

import scala.reflect.NameTransformer
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
   with Printers
{
  import global._
  import global.definitions._
  import scala.tools.nsc.symtab.Flags._
  import typer.{typed, atOwner}    // methods to type trees

  override val runsAfter = RangeForeach2WhileTransformComponent.runsAfter
  override val phaseName = RangeForeach2WhileTransformComponent.phaseName

  def replace(varName: String, tree: Tree, by: => Tree, unit: CompilationUnit) = new TypingTransformer(unit) {
    val n = N(varName)
    override def transform(tree: Tree): Tree = tree match {
      case Ident(n()) =>
        by
      case _ =>
        super.transform(tree)
    }
  }.transform(tree)

  def newTransformer(unit: CompilationUnit) = new TypingTransformer(unit) {
    var currentClassName: Name = null

    def binOp(a: Tree, op: Symbol, b: Tree) = typed {
      Apply(Select(a, op), List(b))
    }
    def intIdent(n: Name) = {
      val i = Ident(n)
      i.tpe = IntClass.tpe
      i
    }
    def intSymIdent(sym: Symbol, n: Name) = {
      val v = intIdent(n)
      v.symbol = sym
      v
    }

    def incrementIntVar(sym: Symbol, n: Name, value: Tree) =
      withType(UnitClass.tpe) {
        Assign(
          intSymIdent(sym, n),
          binOp(
            intSymIdent(sym, n),
            IntClass.tpe.member(nme.PLUS),
            value //Literal(Constant(1))
          )
        )
      }

    def intValOrVar(mod: Int, n: Name, initValue: Tree) = {
      val d = ValDef(Modifiers(mod), n, TypeTree(IntClass.tpe), initValue)
      d.symbol = currentOwner
      d.tpe = IntClass.tpe
      d
    }
    def withType[V <: Tree](tpe: Type)(t: V) = {
      t.tpe = tpe
      t
    }
    def withSymbol[V <: Tree](sym: Symbol)(t: V) = {
      t.symbol = sym
      if (t.tpe == null || t.tpe == NoType)
        t.tpe = sym.info
      t
    }
    def intVar(n: Name, initValue: Tree) = intValOrVar(MUTABLE, n, initValue)
    def intVal(n: Name, initValue: Tree) = intValOrVar(0, n, initValue)
    def whileLoop(sym: Symbol, cond: Tree, body: Tree) = {
      val lab = unit.fresh.newName(body.pos, "while")
      val labTyp = MethodType(Nil, UnitClass.tpe)
      val labSym = currentOwner.newLabel(sym.pos, N(lab)) setInfo labTyp

      withSymbol(labSym) {
        LabelDef(
          N(lab),
          Nil,
          withType(UnitClass.tpe) {
            If(
              cond,
              withType(UnitClass.tpe) {
                Block(
                  if (body == null)
                    Nil
                  else
                    List(body),
                  withType(UnitClass.tpe) {
                    Apply(
                      withSymbol(labSym) { Ident(lab) },
                      Nil
                    )
                  }
                )
              },
              typed { Literal(Constant()) }
            )
          }
        )
      }
    }
    override def transform(tree: Tree): Tree = 
      tree match {
      case IntRangeForeach(from, to, by, isUntil, f @ Function(List(ValDef(paramMods, paramName, t1: TypeTree, rhs)), body)) =>
        val iVar = unit.fresh.newName(body.pos, "i")
        val nVal = unit.fresh.newName(body.pos, "n")
        val iSym = currentOwner.newVariable(tree.pos, iVar) setInfo IntClass.tpe
        val nSym = currentOwner.newValue(tree.pos, nVal) setInfo IntClass.tpe

        val iDef = intVar(iVar, from)
        iDef.symbol = iSym
        val nDef = intVal(nVal, to)
        nDef.symbol = nSym

        val trans = super.transform(
          withType(UnitClass.tpe) {
            treeCopy.Block(
              tree,
              List(
                iDef,
                nDef
              ),
              whileLoop(
                currentOwner,
                binOp(
                  intSymIdent(iSym, iVar),
                  if (isUntil) IntClass.tpe.member(nme.LT) else IntClass.tpe.member(nme.LE),
                  intSymIdent(nSym, nVal)
                ),
                withType(UnitClass.tpe) {
                  Block(
                    List(
                      {
                        val r = replace(paramName.toString, body, intSymIdent(iSym, iVar), unit)
                        println("REPLACED <<<\n" + body + "\n>>> by <<<\n" + r + "\n>>>")
                        localTyper.typed { r }
                      }
                    ),
                    incrementIntVar(iSym, iVar, by)
                  )
                }
              )
            )
          }
        )
        trans
      case _ =>
        super.transform(tree)
    }
  }
}