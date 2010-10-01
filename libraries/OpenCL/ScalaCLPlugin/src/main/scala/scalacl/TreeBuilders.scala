package scalacl

import scala.reflect.generic.{Constants, Names, Trees, Types, Symbols}

//import scala.tools.nsc.CompilationUnits
import scala.tools.nsc.CompilationUnits
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.symtab.Definitions
import scala.tools.nsc.transform.TypingTransformers

trait TreeBuilders
extends MiscMatchers
   with TypingTransformers
{
  this: PluginComponent =>
  
  //val global: Trees with Names with Types with Constants with Definitions with Symbols
  import global._

  import global._
  import global.definitions._
  import scala.tools.nsc.symtab.Flags._
  import typer.{typed, atOwner}    // methods to type trees


  def replace(varName: String, tree: Tree, by: => Tree, unit: CompilationUnit) = new TypingTransformer(unit) {
    val n = N(varName)
    override def transform(tree: Tree): Tree = tree match {
      case Ident(n()) =>
        by
      case _ =>
        super.transform(tree)
    }
  }.transform(tree)

  def binOp(a: Tree, op: Symbol, b: Tree) = typed {
    Apply(Select(a, op), List(b))
  }
  def ident(sym: Symbol, n: Name) = {
    val v = Ident(n)
    v.symbol = sym
    v.tpe = sym.tpe
    v
  }

  def incrementIntVar(sym: Symbol, n: Name, value: Tree): Assign =
    incrementIntVar(() => ident(sym, n), value)

  def incrementIntVar(identGen: () => Ident, value: Tree): Assign =
    Assign(
      identGen(),
      binOp(
        identGen(),
        IntClass.tpe.member(nme.PLUS),
        value //Literal(Constant(1))
      )
    ).setType(UnitClass.tpe)

  def intValOrVar(owner: Symbol, mod: Int, n: Name, initValue: Tree) = {
    val d = ValDef(Modifiers(mod), n, TypeTree(IntClass.tpe), initValue)
    d.symbol = owner
    d.tpe = IntClass.tpe
    d
  }

  def intVar(owner: Symbol, n: Name, initValue: Tree) = intValOrVar(owner, MUTABLE, n, initValue)
  def intVal(owner: Symbol, n: Name, initValue: Tree) = intValOrVar(owner, 0, n, initValue)
  def whileLoop(owner: Symbol, unit: CompilationUnit, tree: Tree, cond: Tree, body: Tree) = {
    val lab = unit.fresh.newName(body.pos, "while")
    val labTyp = MethodType(Nil, UnitClass.tpe)
    val labSym = owner.newLabel(tree.pos, N(lab)) setInfo labTyp

    typed {
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
            Apply(
              ident(labSym, lab),
              Nil
            )
          ),
          Literal(Constant())
        )
      ).setSymbol(labSym)
    }
  }
  def newIntVariable(unit: CompilationUnit, prefix: String, symbolOwner: Symbol, pos: Position, mutable: Boolean, initialValue: Tree = Literal(Constant(0))) = {
    val name = unit.fresh.newName(pos, prefix)
    val sym = symbolOwner.newVariable(pos, name) setInfo IntClass.tpe
    val definition = (
      if (mutable)
        intVar(symbolOwner, name, initialValue)
      else
        intVal(symbolOwner, name, initialValue)
    ).setSymbol(sym)
    //(name, sym, definition)
    (() => ident(sym, name), definition)
  }
}

