/*
 * ScalaCL - putting Scala on the GPU with JavaCL / OpenCL
 * http://scalacl.googlecode.com/
 *
 * Copyright (c) 2009-2010, Olivier Chafik (http://ochafik.free.fr/)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Olivier Chafik nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY OLIVIER CHAFIK AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package scalacl

import scala.collection.immutable.Stack
import scala.collection.mutable.ArrayBuffer
import scala.reflect.NameTransformer
import scala.reflect.generic.{Names, Trees, Types, Constants, Universe}
import scala.tools.nsc.Global
import tools.nsc.plugins.PluginComponent

trait CodeFlattening
extends MiscMatchers
   with TreeBuilders
   with TupleAnalysis
{
  this: PluginComponent with WithOptions =>

  import global._
  import global.definitions._
  import scala.tools.nsc.symtab.Flags._
  import typer.{typed}    // methods to type trees
  import CODE._
  
  import gen._
  import scala.tools.nsc.symtab.Flags._
  import analyzer.{SearchResult, ImplicitSearch}
  
  
  case class FlatCode[T](
    /// External functions that are referenced by statements and / or values 
    outerDefinitions: Seq[T], 
    /// List of variable definitions and other instructions (if statements, do / while loops...)
    statements: Seq[T], 
    /// Final values of the code in a "flattened tuple" style
    values: Seq[T]
  ) {
    def mapEachValue(f: T => Seq[T]): FlatCode[T] =
      copy(values = values.flatMap(f))
    
    def mapValues(f: Seq[T] => Seq[T]): FlatCode[T] =
      copy(values = f(values))
    
    def ++(fc: FlatCode[T]) =
      FlatCode(outerDefinitions ++ fc.outerDefinitions, statements ++ fc.statements, values ++ fc.values)

    def >>(fc: FlatCode[T]) =
          FlatCode(outerDefinitions ++ fc.outerDefinitions, statements ++ fc.statements ++ values, fc.values)

    def noValues =
          FlatCode(outerDefinitions, statements ++ values, Seq())

    def addOuters(outerDefs: Seq[T]) =
      copy(outerDefinitions = outerDefinitions ++ outerDefs)
      
    def addStatements(stats: Seq[T]) = 
      copy(statements = statements ++ stats)

    def printDebug(name: String = "") = {
      def pt(seq: Seq[T]) = println("\t" + seq.map(_.toString.replaceAll("\n", "\n\t")).mkString("\n\t"))
      println("FlatCode(" + name + "):")
      pt(outerDefinitions)
      println("\t--")
      pt(statements)
      println("\t--")
      pt(values)
    }
  }
  
  def merge[T](fcs: FlatCode[T]*)(f: Seq[T] => Seq[T]) =
    fcs.reduceLeft(_ ++ _).mapValues(f)
    
  def EmptyFlatCode[T] = FlatCode[T](Seq(), Seq(), Seq())
  
  /**
   * Phases :
   * - unique renaming
   * - tuple cartography (map symbols and treeId to TupleSlices : x._2 will be checked against x ; if is x's symbol is mapped, the resulting slice will be composed and flattened
   * - tuple + block flattening (gives (Seq[Tree], Seq[Tree]) result)
   */
   // separate pass should return symbolsDefined, symbolsUsed
   // DefTree vs. RefTree
   
  def getDefAndRefTrees(tree: Tree) = {
    val defTrees = new ArrayBuffer[DefTree]()
    val refTrees = new ArrayBuffer[RefTree]()
    new Traverser {
      override def traverse(tree: Tree): Unit = {
        if (tree.hasSymbol)
          tree match {
            case dt: DefTree => defTrees += dt
            case rt: RefTree => refTrees += rt
            case _ =>
          }
        super.traverse(tree)
      }
    }.traverse(tree)
    (defTrees.toArray, refTrees.toArray)
  }
  def renameDefinedSymbolsUniquely(tree: Tree, unit: CompilationUnit) = {
    import scala.collection.mutable.ArrayBuffer
    
    val (defTrees, refTrees) = getDefAndRefTrees(tree)
    val definedSymbols   = defTrees.collect { case d if d.name != null => d.symbol -> d.name } toMap
    val usedIdentSymbols = refTrees.collect { case ident @ Ident(name) => ident.symbol -> name} toMap
    
    val outerSymbols = usedIdentSymbols.keys.toSet.diff(definedSymbols.keys.toSet)
    val nameCollisions = (definedSymbols ++ usedIdentSymbols).groupBy(_._2).filter(_._2.size > 1)
    val renamings = nameCollisions.flatMap(_._2) map { case (sym, name) =>
      val newName: Name = N(unit.fresh.newName(tree.pos, name.toString))
      (sym, newName)
    } toMap
    
    if (renamings.isEmpty)
      tree
    else
      new Transformer {
        // TODO rename the symbols themselves ??
        override def transform(tree: Tree): Tree = {
          def setAttrs(newTree: Tree) =
            //newTree
            newTree.setSymbol(tree.symbol).setType(tree.tpe)
            
          renamings.get(tree.symbol).map(newName => {
            tree match {
              case ValDef(mods, name, tpt, rhs) =>
                setAttrs(treeCopy.ValDef(tree, mods, newName, super.transform(tpt), super.transform(rhs)))
              case DefDef(mods, name, tparams, vparams, tpt, rhs) =>
                setAttrs(treeCopy.DefDef(tree, mods, newName, tparams, vparams, super.transform(tpt), super.transform(rhs)))
              case Ident(name) =>
                setAttrs(treeCopy.Ident(tree, newName)) 
              case _ =>
                super.transform(tree)
            }
          }).getOrElse(super.transform(tree))
        }
      }.transform(tree)
  }

  class TuplesAndBlockFlattener(val tupleAnalyzer: TupleAnalyzer) 
  {
    import tupleAnalyzer._
    
    def isOpenCLIntrinsicTuple(components: List[Type]) = {
      components.size match {
        case 2 | 4 | 8 | 16 =>
          components.distinct.size == 1
        case _ =>
          false
      }
    }

    var sliceReplacements = new scala.collection.mutable.HashMap[TupleSlice, TreeGen]()

    object NumberConversion {
      def unapply(tree: Tree): Option[(Tree, String)] = tree match {
        case Select(expr, n) =>
          Option(n) collect {
            case toSizeTName() => (expr, "size_t")
            case toLongName() => (expr, "long")
            case toIntName() => (expr, "int")
            case toShortName() => (expr, "short")
            case toByteName() => (expr, "char")
            case toCharName() => (expr, "short")
            case toDoubleName() => (expr, "double")
            case toFloatName() => (expr, "float")
          }
        case _ =>
          None
      }
    }
    def makeValuesSideEffectFree(code: FlatCode[Tree], unit: CompilationUnit, symbolOwner: Symbol) = 
    {
      //code /*
      var hasNewStatements = false
      val vals = for (value <- code.values) yield {
        value match {
          case Select(ScalaMathFunction(_, _, _), toFloatName()) =>
            // special case for non-double math :
            // exp(20: Float).toFloat
            (Seq(), value)
          case ScalaMathFunction(_, _, _) =>//Apply(f @ Select(left, name), args) if left.toString == "scala.math.package" =>
            // TODO this is not fair : ScalaMathFunction should have worked here !!!
            (Seq(), value)
          /*case Apply(s @ Select(left, name), args) if NameTransformer.decode(name.toString) match {
              case op @ ("+" | "-" | "*" | "/" | "%" | "^" | "^^" | "&" | "&&" | "|" | "||" | "<<" | ">>" | "==" | "<" | ">" | "<=" | ">=" | "!=") =>
                true
              case n if left.toString == "scala.math.package" =>
                true
              case _ =>
                false
          } =>
            (Seq(), value)*/
          case Ident(_) | Select(_, _) | ValDef(_, _, _, _) | Literal(_) | NumberConversion(_, _) | Typed(_, _) | Apply(_, List(_)) =>
            // already side-effect-free (?)
            (Seq(), value)
          case _ if value.tpe == null || value.tpe == NoType || value.tpe == UnitClass.tpe =>
            (Seq(), value)
          case _ =>
            assert(value.tpe != null, value + " (" + value.getClass.getName + " = " + nodeToString(value) + ")")
            val tempVar = newVariable(unit, "tmp", symbolOwner, value.pos, false, value)
            if (options.verbose)
              println("Creating temp variable " + tempVar.symbol + " for " + value)
            hasNewStatements = true
            for (slice <- getTreeSlice(value))
              setSlice(tempVar.definition, slice)
              
            (Seq(tempVar.definition), tempVar())
        }
      }
      if (!hasNewStatements)
        code
      else
        FlatCode[Tree](
          code.outerDefinitions,
          code.statements ++ vals.flatMap(_._1),
          vals.map(_._2)
        )
    }
    def flattenTuplesAndBlocksWithInputSymbol(tree: Tree, inputSymbol: Symbol, inputName: Name, symbolOwner: Symbol)(implicit unit: CompilationUnit): FlatCode[Tree] = {
      val flattenedTypes = flattenTypes(inputSymbol.tpe)
      
      //setSlice(inputSymbol, TupleSlice(inputSymbol, 0, flattenedTypes.size))
        
      val fiberVars = if (flattenedTypes.size == 1) {
        sliceReplacements ++= Seq(TupleSlice(inputSymbol, 0, 1) -> (() => ident(inputSymbol, inputName)))
        Seq()
      } else {
        val fiberPaths = flattenFiberPaths(inputSymbol.tpe)
        //println("flattenedTypes = " + flattenedTypes)
        //println("fiberPaths = " + fiberPaths)
        fiberPaths.zip(flattenedTypes).zipWithIndex map { case ((path, tpe), i) =>
          val fiberExpr = applyFiberPath(() => {
            val res = ident(inputSymbol, inputName)
            setSlice(res, TupleSlice(inputSymbol, i, 1))
            res
          }, path)
          fiberExpr.setType(tpe)
          val fiberVar = newVariable(unit, inputName + "$" + path.map(_ + 1).mkString("$"), symbolOwner, tree.pos, false, fiberExpr)
          sliceReplacements ++= Seq(TupleSlice(inputSymbol, i, 1) -> fiberVar.identGen)
          fiberVar
        }
      }
      val top @ FlatCode(outerDefinitions, statements, values) = flattenTuplesAndBlocks(tree, sideEffectFree = true, symbolOwner)
      //top.printDebug("top")
      //top
      //*
      def lastMinuteReplace = {
        val trans = new Transformer {
          override def transform(tree: Tree) =
            replace(super.transform(tree))
        }
        trans transform _
      }
      val replaced = FlatCode[Tree](
        outerDefinitions map lastMinuteReplace,
        (fiberVars.map(_.definition) ++ statements) map lastMinuteReplace,
        values map lastMinuteReplace
      )
      //replaced.printDebug("replaced")
      replaced
      //*/
      /*FlatCode[Tree](
        outerDefinitions,
        fiberVars.map(_.definition) ++ statements,
        values
      )*/
    }
    def replace(tree: Tree): Tree =
      replaceValues(tree) match {
        case Seq(unique) => unique
        case _ => tree
      }

    def replaceValues(tree: Tree): Seq[Tree] = if (tree.isInstanceOf[ValDef]) Seq(tree) else try {
      getTreeSlice(tree, recursive = true) match {
        case Some(slice) if slice.baseSymbol != tree.symbol =>
          sliceReplacements.get(slice) match {
            case Some(identGen) =>
              val ident = identGen()
              //setSlice(ident, slice)
              //println("Replacing " + tree + " by " + ident + " (slice = " + slice + ")")
              Seq(ident)
            case None =>
              for (i <- 0 until slice.sliceLength) yield {
                val subSlice = slice.subSlice(i, 1)
                val ident = subSlice.toTreeGen(tupleAnalyzer)()
                //println("Replacing " + tree + " by slice ident " + ident + " (slice = " + slice + ")")
                //println("Tree " + tree + " has associated slice " + slice + ", extracting subSlice " + subSlice + " = " + ident)
                ident
              }
          }
        case _ =>
          //println("Tree " + tree + " has no associated slice")
          Seq(tree)
      }
    } catch { case ex =>
      ex.printStackTrace
      Seq(tree)
    }
    def flattenTuplesAndBlocks(initialTree: Tree, sideEffectFree: Boolean, symbolOwner: Symbol)(implicit unit: CompilationUnit): FlatCode[Tree] = {
      val tree = initialTree//replace(initialTree)
      // If the tree is mapped to a slice and that slice is mapped to a replacement, then replace the tree by an ident to the corresponding name+symbol
      val res: FlatCode[Tree] = tree match {
        case i: Ident =>
          FlatCode[Tree](Seq(), Seq(), Seq(i))
        case Block(statements, value) =>
          // Flatten blocks up
          val FlatCode(defs, stats, flattenedValues) = flattenTuplesAndBlocks(value, sideEffectFree = true, symbolOwner)
          val sub = statements.map(flattenTuplesAndBlocks(_, sideEffectFree = true, symbolOwner))
          sub.foreach(_.printDebug("sub"))
          FlatCode[Tree](
            defs ++ sub.flatMap(_.outerDefinitions),
            sub.flatMap(_.statements) ++
            stats ++
            sub.flatMap(_.values),
            flattenedValues
          )
        case TupleCreation(components) =>
          val sub = components.map(flattenTuplesAndBlocks(_, sideEffectFree = true, symbolOwner))
          FlatCode[Tree](
            sub.flatMap(_.outerDefinitions),
            sub.flatMap(_.statements),
            sub.flatMap(_.values)
          )
        case TupleComponent(target, i) =>//if getTreeSlice(target).collect(sliceReplacements) != None =>
          getTreeSlice(target, recursive = true) match {
            case Some(slice) =>
              //println("Found slice " + slice + " for tuple component " + target + " i = " + i)
              sliceReplacements.get(slice) match {
                case Some(rep) =>
                  FlatCode[Tree](Seq(), Seq(), Seq(rep()))
                case None =>
                  FlatCode[Tree](Seq(), Seq(), Seq(tree))
              }
            case _ =>
              FlatCode[Tree](Seq(), Seq(), Seq(tree))
          }
        case Ident(_) | Literal(_) =>
          // this ident has no known replacement !
          FlatCode[Tree](Seq(), Seq(), Seq(tree))
        case Select(target, name) =>
          val FlatCode(defs, stats, vals) = flattenTuplesAndBlocks(target, sideEffectFree = target.tpe != null, symbolOwner)
          FlatCode[Tree](
            defs,
            stats,
            vals.map(v => Select(v, name))
          )
        case Apply(ident @ Ident(functionName), args) =>
          val f = args.map(flattenTuplesAndBlocks(_, sideEffectFree = true, symbolOwner))
          // TODO assign vals to new vars before the calls, to ensure a correct evaluation order !
          FlatCode[Tree](
            f.flatMap(_.outerDefinitions),
            f.flatMap(_.statements),
            Seq(Apply(Ident(functionName).setType(ident.tpe).setSymbol(ident.symbol), f.flatMap(_.values)).setSymbol(tree.symbol).setType(tree.tpe))
          )
        case Apply(target, List(arg)) =>
          /*
          val convArgs = args.map(flattenTuplesAndBlocks(_, symbolOwner))
          target match { 
            case Select(leftOperand, name) if args.length == 1 =>
              NameTransformer.decode(name.toString) match {
                case op @ ("+" | "-" | "*" | "/" | "%" | "^" | "&" | "|" | "==" ) =>
                  Some(out("(", left, " ", op, " ", args(0), ")")
          */
          val FlatCode(defs1, stats1, vals1) = flattenTuplesAndBlocks(target, sideEffectFree = target.tpe != null, symbolOwner)
          val FlatCode(defs2, stats2, vals2) = flattenTuplesAndBlocks(arg, sideEffectFree = true, symbolOwner)
          val tpes = flattenTypes(tree.tpe)
          // TODO assign vals to new vars before the calls, to ensure a correct evaluation order !
          FlatCode[Tree](
            defs1 ++ defs1,
            stats1 ++ stats2,
            vals1.zip(vals2).zip(tpes).map { case ((v1, v2), tpe) => Apply(v1, List(v2)).setType(tpe) }
          )
        case f: DefDef =>
          FlatCode[Tree](Seq(f), Seq(), Seq())
        case Assign(lhs, rhs) =>
          merge(Seq(lhs, rhs).map(flattenTuplesAndBlocks(_, true, symbolOwner)):_*) { case Seq(l, r) => Seq(Assign(l, r)) }
        case WhileLoop(condition, content) =>
          // TODO clean this up !!!
          val flatCondition = flattenTuplesAndBlocks(condition, sideEffectFree = true, symbolOwner)
          val flatContent = content.map(flattenTuplesAndBlocks(_, sideEffectFree = true, symbolOwner)).reduceLeft(_ >> _)
          val Seq(flatConditionValue) = flatCondition.values
          FlatCode[Tree](
            flatCondition.outerDefinitions ++ flatContent.outerDefinitions, 
            flatCondition.statements,
            Seq(
              whileLoop(
                symbolOwner, 
                unit, 
                tree, 
                flatConditionValue,
                Block(
                  (flatContent.statements ++ flatContent.values).toList,
                  newUnit
                )
              )
            )
          )
        case If(condition, then, otherwise) =>
          // val (a, b) = if ({ val d = 0 ; d != 0 }) (1, d) else (2, 0)
          // ->
          // val d = 0
          // val condition = d != 0
          // val a = if (condition) 1 else 2
          // val b = if (condition) d else 0
          val FlatCode(dc, sc, Seq(vc)) = flattenTuplesAndBlocks(condition, sideEffectFree = true, symbolOwner)
          assert(vc.tpe != null, vc)
          val conditionVar = newVariable(unit, "condition", symbolOwner, tree.pos, false, vc)

          val fct @ FlatCode(Seq(), st, vt) = flattenTuplesAndBlocks(then, sideEffectFree = true, symbolOwner)
          val fco @ FlatCode(Seq(), so, vo) = flattenTuplesAndBlocks(otherwise, sideEffectFree = true, symbolOwner)

          FlatCode[Tree](
            dc,
            sc ++ Seq(conditionVar.definition),
            (st, so) match {
              case (Seq(), Seq()) =>
                vt.zip(vo).map { case (t, o) => If(conditionVar(), t, o) } // pure (cond ? then : otherwise) form, possibly with tuple values
              case _ =>
                Seq(
                  If(conditionVar(), Block(vt.toList, newUnit), Block(vo.toList, newUnit))
                )
            }
          )
        case Typed(expr, tpt) =>
          flattenTuplesAndBlocks(expr, true, symbolOwner).mapValues(_.map(Typed(_, tpt)))
        case ValDef(paramMods, paramName, tpt, rhs) =>
          val isVal = !paramMods.hasFlag(MUTABLE)
          // val p = {
          //   val x = 10
          //   (x, x + 2)
          // }
          // ->
          // val x = 10
          // val p_1 = x
          // val p_2 = x + 2
          val FlatCode(defs, stats, values) = flattenTuplesAndBlocks(replace(rhs), sideEffectFree = true, tree.symbol)
          //val flatValues = values.flatMap(replaceValues)
          val flattenedTypes = if (tree.tpe != NoType) {
            val types = flattenTypes(tree.tpe)
            assert(values.size == types.size, "values = " + values + ", flattenedTypes = " + types + ", tree = " + tree)
            types
          } else {
            values.map(_.tpe)
          }
          //for ((value, i) <- values.zipWithIndex; slice <- getTreeSlice(value)) {
          //  setSlice(tree, slice.subSlice(i, 1))
          //}
          if (flattenedTypes.size == 1) {
            //assert(values.size == 1, "values = " + values + ", flattenedTypes = " + flattenedTypes + ", tree = " + tree)
            val List(value) = values
            val vd = ValDef(paramMods, paramName, tpt, value).setSymbol(tree.symbol).setType(tree.tpe)
            //println("single vd = " + vd)
            //replace(value) // TODO REMOVE THIS DEBUG LINE
            FlatCode[Tree](
              defs,
              stats,
              Seq(vd)
            )
          } else {
            val splitSyms = //: Map[TupleSlice, (String, Symbol)] =
              flattenedTypes.zipWithIndex.map({ case (tpe, i) =>
                val name = unit.fresh.newName(tree.pos, paramName + "_" + (i + 1))
                val sym = symbolOwner.newValue(tree.pos, name)
                sym.setInfo(tpe).setFlag(SYNTHETIC | LOCAL)
                (TupleSlice(tree.symbol, i, 1), (name, sym))//() => ident(sym, name))
              })

            //println("splitSyms = " + splitSyms)
            if (splitSyms.size != 1 && isVal)
              sliceReplacements ++= splitSyms.map { case (slice, (name, sym)) =>
                val rep = ident(sym, name)//slice.toTreeGen(replace(_, false), tupleAnalyzer)()
                //println("New replacement for " + slice + " = " + rep + " (but name = " + name + ", sym = " + sym + ")")
                (slice, () => rep)
              }

            val FlatCode(defs, stats, flattenedValues) = flattenTuplesAndBlocks(rhs, sideEffectFree = true, tree.symbol)
            
            if (isVal)
              for (((splitSlice, (splitName, splitSym)), value) <- splitSyms.zip(flattenedValues)) {
                for (slice <- getTreeSlice(value)) {
                  //println("Forwarded split slice " + slice + " from value " + value + " to split symbol " + splitSym)
                  setSlice(splitSym, slice)
                }
              }
              
            FlatCode[Tree](
              defs,
              stats ++ splitSyms.zip(flattenedValues).zip(flattenedTypes).map({ case (((slice, (name, sym)), value), tpe) =>
                val vv = value
                val vd = ValDef(Modifiers(0), name, TypeTree(tpe), replace(value)).setSymbol(sym).setType(tpe): Tree
                //println("vd = " + vd)
                vd
              }),
              Seq()//splitSyms.map({ case (slice, (name, sym)) => ident(sym, name) })
            )
          }
        case Match(selector, List(CaseDef(pat, guard, body))) =>
          def extract(tree: Tree): Tree = tree match {
            case Typed(expr, tpt) => extract(expr)
            case Annotated(annot, arg) => extract(arg)
            case _ => tree
          }
          getTreeSlice(selector, recursive = true).orElse(getTreeSlice(extract(selector), recursive = true)) match {
            case Some(slice) =>
              val subMatcher = new BoundTuple(slice)
              val subMatcher(m) = pat
              
              //val info = getTupleInfo(slice.baseSymbol.tpe)
              //if (false)
              for ((sym, subSlice) <- m) {
                val info = getTupleInfo(sym.tpe)
                val boundSlice = TupleSlice(sym, 0, info.flattenTypes.size)
                val rep = subSlice.toTreeGen(tupleAnalyzer)
                //setSlice(rep, subSlice)
                //println("Not Adding replacement for slice " + boundSlice + " to " + rep())
                //println("Adding replacement for slice " + boundSlice + " to " + subSlice)
                setSlice(sym, subSlice)
                                                //sliceReplacements ++= Seq(boundSlice -> rep)
                if (subSlice.sliceLength == 1)
                  sliceReplacements ++= Seq(boundSlice -> subSlice.toTreeGen(tupleAnalyzer))
                  //(() => applyFiberPath(subSlice.toTreeGen(replace), path)))
              }
              /*for ((path, i) <- info.flattenPaths.zipWithIndex) {
                val subSlice = TupleSlice(slice.baseSymbol, i, 1)
                sliceReplacements ++=
                  Seq(subSlice -> subSlice.toTreeGen(replace(_, false), tupleAnalyzer))
                  //(() => applyFiberPath(subSlice.toTreeGen(replace), path)))
              }*/
              
              flattenTuplesAndBlocks(body, sideEffectFree = true, symbolOwner)
            case _ =>
              if (options.verbose) {
                println("selector: " + selector.getClass.getName + " = " + selector + " = " + nodeToString(selector))
                println("selector.symbol = " + selector.symbol)
                println("extract(selector): " + extract(selector).getClass.getName + " = " + selector + " = " + nodeToString(extract(selector)))
                println("extract(selector).symbol = " + extract(selector).symbol)
                println("sliceReplacements = \n\t" + sliceReplacements.mkString("\n\t"))
              }
              throw new RuntimeException("Unable to connect the matched pattern with its corresponding single case")
              //FlatCode[Tree](Seq(), Seq(), Seq())
          }
        case _ =>
          assert(false, "Case not handled in tuples and blocks flattening : " + tree + " (" + tree.getClass.getName + ") :\n\t" + nodeToString(tree))
          FlatCode[Tree](Seq(), Seq(), Seq())
      }
      //res.printDebug("res")
      val ret = if (sideEffectFree)
        makeValuesSideEffectFree(
          res,
          unit,
          symbolOwner
        )
      else
        res

      //ret.printDebug("ret")
      //println("tree = \n\t" + tree.toString.replaceAll("\n", "\n\t"))
      //println("res = \n\t" + res.toString.replaceAll("\n", "\n\t"))
      //println("ret = \n\t" + ret.toString.replaceAll("\n", "\n\t"))
      
      val out = ret.mapValues(s => s.flatMap(replaceValues))
      //out.printDebug("out")
      out
    }
  }
}
