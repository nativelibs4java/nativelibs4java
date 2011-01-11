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

import scala.tools.nsc.Global

import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.{Transform, TypingTransformers}
import scala.tools.nsc.typechecker.Analyzer
import scala.tools.nsc.typechecker.Contexts
//import scala.tools.nsc.typechecker.Contexts._

object ScalaCLFunctionsTransformComponent {
  val runsAfter = List[String](
    "namer",
    LoopsTransformComponent.phaseName
  )
  val runsBefore = List[String](
    "refchecks"
  )
  val phaseName = "scalacl-functionstransform"
}

class ScalaCLFunctionsTransformComponent(val global: Global, val options: ScalaCLPlugin.PluginOptions)
extends PluginComponent
   with Transform
   with TypingTransformers
   with MiscMatchers
   with TreeBuilders
  //with Analyzer
   with OpenCLConverter
   with WithOptions
{
  import global._
  import global.definitions._
  import gen._
  import scala.tools.nsc.symtab.Flags._
  import typer.{typed}    // methods to type trees
  import analyzer.{SearchResult, ImplicitSearch}

  override val runsAfter = ScalaCLFunctionsTransformComponent.runsAfter
  override val runsBefore = ScalaCLFunctionsTransformComponent.runsBefore
  override val phaseName = ScalaCLFunctionsTransformComponent.phaseName

  val ScalaCLPackage       = getModule("scalacl")
  val ScalaCLPackageClass  = ScalaCLPackage.tpe.typeSymbol
  val CLDataIOClass = definitions.getClass("scalacl.impl.CLDataIO")
  val CLArrayClass = definitions.getClass("scalacl.CLArray")
  val CLRangeClass = definitions.getClass("scalacl.CLRange")
  val CLCollectionClass = definitions.getClass("scalacl.CLCollection")
  val CLFilteredArrayClass = definitions.getClass("scalacl.CLFilteredArray")
  val scalacl_ = N("scalacl")
  val getCachedFunctionName = N("getCachedFunction")
  
  def nodeToStringNoComment(tree: Tree) =
    nodeToString(tree).replaceAll("\\s*//.*\n", "\n").replaceAll("\\s*\n\\s*", " ").replaceAll("\\(\\s+", "(").replaceAll("\\s+\\)", "")

  def newTransformer(unit: CompilationUnit) = new TypingTransformer(unit) {
    var currentClassName: Name = null

    def createImplicitDataIO(context: analyzer.Context, tree: Tree, tpe: Type) = {
      val applicableViews: List[SearchResult] = 
        new ImplicitSearch(tree, tpe, isView = false, context.makeImplicit(reportAmbiguousErrors = false)).allImplicits
      for (view <- applicableViews) {

      }
      null: Tree
    }

    def flattenTypes(tpe: Type): Seq[Type] = {
      error("not implemented")
      null
    }
    /**
     * Phases :
     * - unique renaming
     * - tuple cartography (map symbols and treeId to TupleSlices : x._2 will be checked against x ; if is x's symbol is mapped, the resulting slice will be composed and flattened
     * - tuple + block flattening (gives (Seq[Tree], Seq[Tree]) result)
     */

    def renameDefinedSymbolsUniquely(tree: Tree) = {

      var definedSymbols = new scala.collection.mutable.ArrayBuffer[(Symbol, Name)]()
      new Traverser {
        override def traverse(tree: Tree): Unit = {
          tree match {
            // TODO handle defs
            case Bind(name, rhs) =>
              error("weird, shouldn't be here at this phase !")
            case ValDef(_, name, _, _) =>
              definedSymbols += tree.symbol -> name
            case DefDef(mods, name, tparams, vparams, tpt, rhs) =>
              definedSymbols += tree.symbol -> name
            case _ =>
          }
          super.traverse(tree)
        }
      }.traverse(tree)

      val collisions = definedSymbols.groupBy(_._2).filter(_._2.size > 1)
      val renames = collisions.flatMap(_._2).map({ case (sym, name) =>
          val newName: Name = N(unit.fresh.newName(tree.pos, name.toString))
          (sym, newName)
      }).toMap

      new Transformer {
        override def transform(tree: Tree): Tree = tree match {
          case ValDef(mods, name, tpt, rhs) =>
            renames.get(tree.symbol).map(newName => {
              println("Renaming " + name + " to " + newName)
              ValDef(mods, name, super.transform(tpt), super.transform(rhs))
            }).getOrElse(super.transform(tree))
          case DefDef(mods, name, tparams, vparams, tpt, rhs) =>
            renames.get(tree.symbol).map(newName => {
              println("Renaming " + name + " to " + newName)
              DefDef(mods, newName, tparams, vparams, super.transform(tpt), super.transform(rhs))
            }).getOrElse(super.transform(tree))
          case Ident(name) =>
            renames.get(tree.symbol).map(newName => {
              println("Renaming " + name + " to " + newName)
              ident(tree.symbol, newName)
            }).getOrElse(super.transform(tree))
          case _ =>
            super.transform(tree)
        }
      }.transform(tree)
    }
    class TupleAnalysis(tree: Tree) {
      case class TupleSlice(baseSymbol: Symbol, sliceOffset: Int, sliceLength: Int) {
        def subSlice(offset: Int, length: Int) =
          TupleSlice(baseSymbol, sliceOffset + offset, length)
      }

      private var treeTupleSlices = new scala.collection.mutable.HashMap[Int, TupleSlice]()
      private var symbolTupleSlices = new scala.collection.mutable.HashMap[Symbol, TupleSlice]()
      
      def getSlice(tree: Tree) =
        symbolTupleSlices.get(tree.symbol).orElse(treeTupleSlices.get(tree.id))

      private def setSlice(tree: Tree, slice: TupleSlice) = {
        treeTupleSlices(tree.id) = slice
        tree match {
          case vd: ValDef =>
            symbolTupleSlices(tree.symbol) = slice
          case _ =>
        }
      }

      new Traverser {
        override def traverse(tree: Tree): Unit = {
          super.traverse(tree)
          tree match {
            case TupleComponent(target, i) =>
              getSlice(target).map(slice => {
                val length = flattenTypes(tree.tpe).size
                setSlice(tree, slice.subSlice(i, length))
              })
            case _ =>
          }
        }
      }.traverse(tree)

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
      var splitMap = Map[Symbol, Symbol]()
      
      def isOpenCLIntrinsicTuple(components: List[Type]) = {
        components.size match {
          case 2 | 4 | 8 | 16 =>
            components.distinct.size == 1
          case _ =>
            false
        }
      }

      var sliceReplacements = new scala.collection.mutable.HashMap[TupleSlice, (String, Symbol)]()

      def flattenTuplesAndBlocks(tree: Tree)(implicit symbolOwner: Symbol): (Seq[Tree], Seq[Tree]) = {
        // If the tree is mapped to a slice and that slice is mapped to a replacement, then replace the tree by an ident to the corresponding name+symbol
        getSlice(tree).flatMap(sliceReplacements.get).map { case (name, symbol) => (Seq(), Seq(ident(symbol, name))) } getOrElse
        tree match {
          case Block(statements, value) =>
            // Flatten blocks up
            val (stats, flattenedValues) = flattenTuplesAndBlocks(value)
            (
              statements.flatMap(s => {
                val (stats, flattenedValues) = flattenTuplesAndBlocks(s)
                stats ++ flattenedValues
              }) ++
              stats,
              flattenedValues
            )
          case If(condition, then, otherwise) =>
            // val (a, b) = if ({ val d = 0 ; d != 0 }) (1, d) else (2, 0)
            // ->
            // val d = 0
            // val condition = d != 0
            // val a = if (condition) 1 else 2
            // val b = if (condition) d else 0
            val (sc, Seq(vc)) = flattenTuplesAndBlocks(condition)
            val conditionVar = newVariable(unit, "condition", currentOwner, tree.pos, false, vc)
            (
              sc ++ Seq(conditionVar.definition),
              (flattenTuplesAndBlocks(then), flattenTuplesAndBlocks(otherwise)) match {
                case ((Seq(), vt), (Seq(), vo)) =>
                  vt.zip(vo).map { case (t, o) => If(conditionVar(), t, o) } // pure (cond ? then : otherwise) form, possibly with tuple values
                case ((st, vt), (so, vo)) =>
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
            val splitSyms: Map[TupleSlice, (String, Symbol)] = flattenedTypes.zipWithIndex.map({ case (tpe, i) =>
              val name = unit.fresh.newName(tree.pos, paramName + "_" + (i + 1))
              val sym = symbolOwner.newVariable(tree.pos, name)
              (TupleSlice(tree.symbol, i, 1), (name, sym))
            }).toMap

            sliceReplacements ++= splitSyms

            val (stats, flattenedValues) = flattenTuplesAndBlocks(rhs)
            (
              stats,
              splitSyms.zip(flattenedValues).zip(flattenedTypes).map({ case (((slice, (name, sym)), value), tpe) =>
                ValDef(Modifiers(MUTABLE), name, TypeTree(tpe), value).setSymbol(sym).setType(tpe): Tree
              }).toSeq
            )
        }
      }
    }
    override def transform(tree: Tree): Tree = {
      //println(".")
      if (!shouldOptimize(tree))
        super.transform(tree)
      else {
        tree match {
          case TraversalOp(traversalOp) if traversalOp.op.f != null =>
            import traversalOp._
            try {
              val colTpe = collection.tpe.widen.dealias.deconst
              //if (colTpe.matches(CLCollectionClass.tpe)) {
              if (colTpe.toString.startsWith("scalacl.")) { // TODO
                op match {
                  case opType @ (TraversalOp.Map(_, _) | TraversalOp.Filter(_, false)) =>
                    msg(unit, tree.pos, "associated equivalent OpenCL source to " + colTpe + "." + op + "'s function argument.") {
                      val Func(List(uniqueParam), body) = op.f
                      val context = localTyper.context1
                      val sourceTpe = uniqueParam.symbol.tpe
                      val mappedTpe = body.tpe
                      val Array(
                        sourceDataIO, 
                        mappedDataIO
                      ) = Array(
                        sourceTpe, 
                        mappedTpe
                      ).map(tpe => {
                        val dataIOTpe = appliedType(CLDataIOClass.tpe, List(tpe))
                        analyzer.inferImplicit(tree, dataIOTpe, false, false, context).tree
                      })
                      
                      val (statements, values) = convertExpr(Map(uniqueParam.symbol -> "_"), body)
                      val uniqueSignature = Literal(Constant(
                        (
                          Array(
                            tree.symbol.outerSource, tree.symbol.tag + "|" + tree.symbol.pos,
                            sourceTpe, mappedTpe, statements
                          ) ++ values
                        ).map(_.toString).mkString("|")
                      ))
                      val uniqueId = uniqueSignature.hashCode // TODO !!!
                      
                      if (options.verbose)
                        println("[scalacl] Converted <<< " + body + " >>> to <<< \"" + statements + "\n(" + values.mkString(", ") + ")\" >>>")
                      val getCachedFunctionSym = ScalaCLPackage.tpe member getCachedFunctionName
                      val clFunction = 
                        typed {
                          Apply(
                            Apply(
                              TypeApply(
                                Select(
                                  Ident(scalacl_) setSymbol ScalaCLPackage,
                                  getCachedFunctionName
                                ),
                                List(TypeTree(sourceTpe), TypeTree(mappedTpe))
                              ).setSymbol(getCachedFunctionSym),
                              List(
                                newInt(uniqueId),
                                op.f,
                                newSeqApply(TypeTree(StringClass.tpe), Literal(Constant(statements))),
                                newSeqApply(TypeTree(StringClass.tpe), values.map(value => Literal(Constant(value))):_*),
                                newSeqApply(TypeTree(AnyClass.tpe)) // args TODO
                              )
                            ).setSymbol(getCachedFunctionSym),
                            List(
                              sourceDataIO,
                              mappedDataIO
                            )
                          ).setSymbol(getCachedFunctionSym)
                        }
  
                      val rep = replaceOccurrences(super.transform(tree), Map(), Map(), Map(op.f -> (() => clFunction)), unit)
                      //println("REP = " + nodeToString(rep))
                      rep
                    }
                  case _ =>
                    super.transform(tree)
                }
              } else {
                super.transform(tree)
              }
            } catch { case ex => 
              ex.printStackTrace
              super.transform(tree)
            }
          case _ =>
            super.transform(tree)
        }
      }
    }
  }
}
