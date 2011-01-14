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
  )
  
  case class TupleInfo(tpe: Type, components: Seq[TupleInfo]) {
    def flattenTypes: Seq[Type] = {
      components match {
        case Seq() =>
          Seq(tpe)
        case _ =>
          components.flatMap(_.flattenTypes)
      }
    }
    lazy val componentSize: Int = {
      components match {
        case Seq() =>
          1
        case _ =>
          components.map(_.componentSize).sum
      }
    }
  }
  val tupleInfos = new scala.collection.mutable.HashMap[Type, TupleInfo]
  def getTupleInfo(tpe: Type): TupleInfo = {
    val actualTpe = tpe.dealias.widen.deconst
    tupleInfos.getOrElseUpdate(actualTpe, {
      actualTpe match {
        case t: TypeRef =>
          if (isTupleSymbol(t.sym))
            TupleInfo(t, t.args.map(getTupleInfo))
          else
            TupleInfo(t, Seq())
        case NoType => 
          TupleInfo(NoType, Seq())
        case _ =>
          error("Unhandled type : " + tpe + " (" + actualTpe + ": " + actualTpe.getClass.getName + ")")
          System.exit(0)
          null
      }
    })
  }
  def flattenTypes(tpe: Type): Seq[Type] = 
    getTupleInfo(tpe).flattenTypes
  
  def getComponentOffsetAndSizeOfIthMember(tpe: Type, i: Int) = {
    val TupleInfo(_, components) = getTupleInfo(tpe)
    (
      components.take(i).map(_.componentSize).sum,
      components(i).componentSize
    )
  }
  
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
    val renamings = nameCollisions.flatMap(_._2).map({ case (sym, name) =>
      val newName: Name = N(unit.fresh.newName(tree.pos, name.toString))
      (sym, newName)
    }).toMap
    
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

  class TupleAnalysis(tree: Tree) {
    case class TupleSlice(baseSymbol: Symbol, sliceOffset: Int, sliceLength: Int) {
      def subSlice(offset: Int, length: Int) =
        TupleSlice(baseSymbol, sliceOffset + offset, length)
    }

    var treeTupleSlices = new scala.collection.mutable.HashMap[(Int, Tree), TupleSlice]()
    //private var symbolTupleSlices = new scala.collection.mutable.HashMap[Symbol, TupleSlice]()
    var symbolTupleSlices = new scala.collection.mutable.HashMap[Symbol, TupleSlice]()

    def getSymbolSlice(sym: Symbol, recursive: Boolean = false): Option[TupleSlice] = {
      val direct = symbolTupleSlices.get(sym)
      direct match {
        case Some(directSlice) if recursive && directSlice.sliceLength > 1 =>
          getSymbolSlice(directSlice.baseSymbol, recursive)
        case _ =>
          direct
      }
    }
    def getTreeSlice(tree: Tree, recursive: Boolean = false): Option[TupleSlice] = {
      val direct = symbolTupleSlices.get(tree.symbol).orElse(treeTupleSlices.get((tree.id, tree)))
      if (recursive && direct != None)
        getSymbolSlice(direct.get.baseSymbol, recursive)
      else
        direct
    }
    
    class BoundTuple(rootSlice: TupleSlice) {
      def unapply(tree: Tree): Option[Seq[(Symbol, TupleSlice)]] = tree match {
        case Bind(name, Ident(_)) =>
          Some(Seq(tree.symbol -> rootSlice))
        case TupleCreation(components) =>
          var currentOffset = 0
          val ret = new scala.collection.mutable.ArrayBuffer[(Symbol, TupleSlice)]
          for ((component, i) <- components.zipWithIndex) {
            val compTpes = flattenTypes(component.tpe)
            val compSize = compTpes.size
            val subMatcher = new BoundTuple(rootSlice.subSlice(currentOffset, compSize))
            currentOffset += compSize
            component match {
              case subMatcher(m) =>
                ret ++= m
              case _ =>
                return None // strict binding
            }
          }
          Some(ret)
        case _ =>
          None
      }
    }

    def setSlice(sym: Symbol, slice: TupleSlice) =
      symbolTupleSlices(sym) = slice

    def setSlice(tree: Tree, slice: TupleSlice) = {
      println("Setting slice " + slice + " for tree " + tree)
      treeTupleSlices((tree.id, tree)) = slice
      tree match {
        case vd: ValDef =>
          symbolTupleSlices(tree.symbol) = slice
        case _ =>
      }
    }

    // Identify trees and symbols that represent tuple slices
    new Traverser {
      override def traverse(tree: Tree): Unit = {
        tree match {
          case ValDef(mods, name, tpt, rhs) =>
            super.traverse(tree)
            println("Got valdef " + name)
            val tupleInfo = getTupleInfo(rhs.tpe)
            if (tupleInfo == null) {
              error("No tuple info !")
              System.exit(0)
            }
            setSlice(tree.symbol, TupleSlice(tree.symbol, 0, tupleInfo.componentSize))
            for (slice <- getTreeSlice(rhs)) {
              println("\tvaldef has slice " + slice)
              setSlice(tree.symbol, slice)
            }
          case Match(selector, cases) =>
            println("Found match") 
            for (slice <- getTreeSlice(selector)) {
              println("\tMatch has slice " + slice)
              val subMatcher = new BoundTuple(slice)
              for (CaseDef(pat, _, _) <- cases) {
                pat match {
                  case subMatcher(m) =>
                    for ((sym, subSlice) <- m)
                      setSlice(sym, subSlice)
                  case _ =>
                    error("Case matching only supports tuples for now (TODO: add (CL)Array(...) case).")
                }
              }
            }
            super.traverse(tree)
          case TupleComponent(target, i) if target != null =>
            super.traverse(tree)
            val (componentsOffset, componentCount) = getComponentOffsetAndSizeOfIthMember(target.tpe, i)
            
            println("Identified tuple component " + i + " of " + target)
            getTreeSlice(target) match {
              case Some(slice) =>
                println("\ttarget got slice " + slice)
                setSlice(tree, TupleSlice(slice.baseSymbol, componentsOffset, componentCount))
              case _ =>
                println("No tuple slice symbol info for tuple component i = " + i + " : " + target + "\n\t-> " + nodeToStringNoComment(target))
                println("\ttree : " + nodeToStringNoComment(tree))
            }
          case _ =>
            super.traverse(tree)
        }
      }
    }.traverse(tree)

    //println("treeTupleSlices = \n\t" + treeTupleSlices.mkString("\n\t"))
    //println("symbolTupleSlices = \n\t" + symbolTupleSlices.mkString("\n\t"))
    
    // 1) Create unique names for unique symbols !
    // 2) Detect external references, lift them up in arguments.
    // 3) Annotate code with usage :
    //    - symbol to Array and CLArray val : read, written, both ?
    //    - extern vars : forbidden
    //    -
    // 4) Flatten tuples and blocks, function definitions arg lists, function calls args
    //
    // Symbol => TupleSlice
    // Tree => TupleSlice
    // e.g. x: ((Double, Float), Int) ; x._1._2 => TupleSlice(x, 1, 1)
    //
    // Tuples flattening :
    // - list tuple definitions
    // - explode each definition unless it's an OpenCL intrinsic :
    //    -> create a new symbol for each split component,
    //    -> map resulting TupleSlice => componentSymbol
    //    -> splitSymbolsTable = Map[Symbol, Seq[(TupleSlice, componentSymbol, componentName)]]
    // - given a Tree, we get an exploded Seq[Tree] + pre-definitions
    //    e.g.:
    //      val a: (Int, Int) = (1, 3)
    //        -> val a1 = 1
    //           val a2 = 3
    //      val a: (Int, Int) = f(x) // special case for int2 : no change
    // We need to propagate slices that are of length > 1 :
    // - arr1.zip(arr2).zipWithIndex.map { case r @ (p @ (a1, a2), i) => p } map { p => p._1 }
    //    -> p => TupleSlice(mapArg, 0, 2)
    // - val (a, b) = p // p is mapped
    //    -> val a = p1 // using splitSymbolsTable
    //       val b = p2
    // Jump over blocks :
    // val p = {
    //  val x = 10
    //  (x, x + 2)
    // }
    // ->
    // val x = 10
    // val p1 = x
    // val p2 = x + 2
    //
    // Each Tree gives a list of statements + a list of split value components :
    // convertTree(tree: Tree): (Seq[Tree], Seq[Tree])
    //
    //
  }
  class TuplesAndBlockFlattener(tupleAnalysis: TupleAnalysis) 
  {
    import tupleAnalysis._
    
    def isOpenCLIntrinsicTuple(components: List[Type]) = {
      components.size match {
        case 2 | 4 | 8 | 16 =>
          components.distinct.size == 1
        case _ =>
          false
      }
    }

    private var sliceReplacements = new scala.collection.mutable.HashMap[TupleSlice, (String, Symbol)]()

    def flattenTuplesAndBlocks(tree: Tree)(implicit symbolOwner: Symbol, unit: CompilationUnit): FlatCode[Tree] = {
      // If the tree is mapped to a slice and that slice is mapped to a replacement, then replace the tree by an ident to the corresponding name+symbol
      getTreeSlice(tree, true).flatMap(sliceReplacements.get).map { case (name, symbol) => 
        FlatCode[Tree](Seq(), Seq(), Seq(ident(symbol, name))) 
      } getOrElse {
        tree match {
          case Block(statements, value) =>
            // Flatten blocks up
            val FlatCode(defs, stats, flattenedValues) = flattenTuplesAndBlocks(value)
            val sub = statements.map(flattenTuplesAndBlocks(_))
            FlatCode(
              sub.flatMap(_.outerDefinitions),
              sub.flatMap(_.statements) ++
              sub.flatMap(_.values) ++ 
              stats,
              flattenedValues
            )
          case TupleCreation(components) =>
            val sub = components.map(flattenTuplesAndBlocks(_))
            // TODO reorder BUG HERE WITH ORDER OF EVALUATION IF THERE ARE SIDE EFFECTS !
            val varDefs = sub.zipWithIndex map { case (v, iVar) =>
              v.values.zipWithIndex map { case (value, iValue) =>
                newVariable(unit, "comp_" + iVar + "_" + iValue, symbolOwner, tree.pos, false, value)
              }
            }
            FlatCode(
              sub.flatMap(_.outerDefinitions),
              sub.zip(varDefs).flatMap { case (v, vdefs) =>
                v.statements ++ vdefs.map(_.definition)
              },
              varDefs.flatMap(vdefs => vdefs.map(_()))
            )
            /*
            FlatCode(
              Seq(),
              sub.flatMap(_.statements),
              sub.flatMap(_.values)
            )
            */
          case Ident(_) | Literal(_) =>
            // this ident has no known replacement !
            FlatCode(Seq(), Seq(), Seq(tree))
          case Select(target, name) =>
            val FlatCode(defs, stats, vals) = flattenTuplesAndBlocks(target)
            FlatCode(
              defs,
              stats,
              vals.map(v => Select(v, name))
            )
          case Apply(target, List(arg)) =>
            val FlatCode(defs1, stats1, vals1) = flattenTuplesAndBlocks(target)
            val FlatCode(defs2, stats2, vals2) = flattenTuplesAndBlocks(arg)
            FlatCode(
              defs1 ++ defs1,
              stats1 ++ stats2,
              vals1.zip(vals2).map { case (v1, v2) => Apply(v1, List(v2)) }
            )
          case f: Function =>
            FlatCode(Seq(f), Seq(), Seq())
          case If(condition, then, otherwise) =>
            // val (a, b) = if ({ val d = 0 ; d != 0 }) (1, d) else (2, 0)
            // ->
            // val d = 0
            // val condition = d != 0
            // val a = if (condition) 1 else 2
            // val b = if (condition) d else 0
            val FlatCode(dc, sc, Seq(vc)) = flattenTuplesAndBlocks(condition)
            val conditionVar = newVariable(unit, "condition", symbolOwner, tree.pos, false, vc)
            FlatCode(
              dc,
              sc ++ Seq(conditionVar.definition),
              (flattenTuplesAndBlocks(then), flattenTuplesAndBlocks(otherwise)) match {
                case (FlatCode(Seq(), Seq(), vt), FlatCode(Seq(), Seq(), vo)) =>
                  vt.zip(vo).map { case (t, o) => If(conditionVar(), t, o) } // pure (cond ? then : otherwise) form, possibly with tuple values
                case (FlatCode(Seq(), st, vt), FlatCode(Seq(), so, vo)) =>
                  Seq(
                    If(conditionVar(), Block(vt.toList, newUnit), Block(vo.toList, newUnit))
                  )
              }
            )
          case ValDef(paramMods, paramName, tpt, rhs) =>
            // val p = {
            //   val x = 10
            //   (x, x + 2)
            // }
            // ->
            // val x = 10
            // val p_1 = x
            // val p_2 = x + 2
            val flattenedTypes = flattenTypes(tree.tpe)
            if (flattenedTypes.size == 1) {
              val FlatCode(defs, stats, Seq(value)) = flattenTuplesAndBlocks(rhs)
              FlatCode(
                defs,
                stats,
                Seq(ValDef(paramMods, paramName, tpt, value).setSymbol(tree.symbol).setType(tree.tpe))
              )
            } else {
              val splitSyms: Map[TupleSlice, (String, Symbol)] = 
                flattenedTypes.zipWithIndex.map({ case (tpe, i) =>
                  val name = unit.fresh.newName(tree.pos, paramName + "_" + (i + 1))
                  val sym = symbolOwner.newVariable(tree.pos, name)
                  (TupleSlice(tree.symbol, i, 1), (name, sym))
                }).toMap
    
              if (splitSyms.size != 1)
                sliceReplacements ++= splitSyms
  
              val FlatCode(defs, stats, flattenedValues) = flattenTuplesAndBlocks(rhs)
              FlatCode(
                defs,
                stats,
                splitSyms.zip(flattenedValues).zip(flattenedTypes).map({ case (((slice, (name, sym)), value), tpe) =>
                  ValDef(Modifiers(0), name, TypeTree(tpe), value).setSymbol(sym).setType(tpe): Tree
                }).toSeq
              )
            }
          case _ =>
            error("Case not handled in tuples and blocks flattening : " + tree + " (" + tree.getClass.getName + ")")
            System.exit(1)
            FlatCode[Tree](Seq(), Seq(), Seq())
        }
      }
    }
  }
}
