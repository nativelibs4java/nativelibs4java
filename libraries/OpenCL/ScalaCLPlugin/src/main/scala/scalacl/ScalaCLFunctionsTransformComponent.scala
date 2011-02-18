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
   with OpenCLConverter
   with WithOptions
   with CodeFlattening
   with TupleAnalysis
   with CodeAnalysis
{
  import global._
  import global.definitions._
  import gen._
  import scala.tools.nsc.symtab.Flags._
  import typer.typed
  import analyzer.{SearchResult, ImplicitSearch, UnTyper}

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
  
  def newTransformer(unit: CompilationUnit) = new TypingTransformer(unit) {
    var currentClassName: Name = null

    def createImplicitDataIO(context: analyzer.Context, tree: Tree, tpe: Type) = {
      val applicableViews: List[SearchResult] = 
        new ImplicitSearch(tree, tpe, isView = false, context.makeImplicit(reportAmbiguousErrors = false)).allImplicits
      for (view <- applicableViews) {

      }
      null: Tree
    }
    
    /**
     * Remove all symbols from the trees, because old symbols from before the unique renaming will preempt on renamed Ident names when printing the tree out to C code.
     * Hit Java verifier exceptions (Scala compiler bug) when letting this function definition where it's used, so lifted it up manually :-S
     */
    private def removeSymbolsExceptParamSymbolAsUnderscore(paramSymbol: Symbol, t: Tree) = { 
      val trans = new Transformer { 
        override def transform(tree: Tree) = tree match {
          case Ident(name) if tree.hasSymbol =>
            if (tree.symbol == paramSymbol)
              treeCopy.Ident(tree, N("_"))
            else {
              tree.setSymbol(NoSymbol) // to make renaming effective
              super.transform(tree)
            }
          case _ =>
            super.transform(tree)
        }
      }
      trans transform t
    }
  
    /*
    lazy val scalaCLContexts = {
      import com.nativelibs4java.opencl._
      JavaCL.listPlatforms.flatMap(_.listAllDevices(true).flatMap(device => {
        try {
          Some(new Context(JavaCL.createContext(null, device)))
        } catch {
          case ex =>
            ex.printStackTrace
            None
        }
      }))
    }.toArray
    */

    private lazy val duplicator = new Transformer {
      override val treeCopy = new StrictTreeCopier
    }

    val primitiveTypeSymbols = Set(IntClass, LongClass, ShortClass, ByteClass, BooleanClass, DoubleClass, FloatClass, CharClass)

    def conversionError(pos: Position, msg: String) = {
      unit.error(pos, msg)
      throw new UnsupportedOperationException("Conversion error : " + msg)
    }
    def convertFunctionToCLFunction(originalFunction: Tree): Tree = {
      
      val f = duplicator transform originalFunction
      val unknownSymbols = getUnknownSymbols(f, t => t.symbol != NoSymbol && {
        t.symbol match {
          case s: MethodSymbol =>
            !primitiveTypeSymbols.contains(s.owner) && s.owner != ScalaMathCommonClass
          case s: ModuleSymbol =>
            false
          case s: TermSymbol =>
            t.isInstanceOf[Ident]
          case _ =>
            false
        }
      })
      for (s <- unknownSymbols) s match {
        case _: MethodSymbol =>
          unit.error(s.pos, "Cannot capture externals methods yet")
        case _ =>
          unit.error(s.pos, "Cannot capture externals symbols yet")
      }

      if (!unknownSymbols.isEmpty)
        conversionError(originalFunction.pos, "Cannot convert functions that capture external symbols yet")
    
      assert(f.id != originalFunction.id)
      var Function(List(uniqueParam), body) = f
      val renamed = renameDefinedSymbolsUniquely(body, unit)
      val tupleAnalyzer = new TupleAnalyzer(renamed)
      val flattener = new TuplesAndBlockFlattener(tupleAnalyzer)
      val flattened = flattener.flattenTuplesAndBlocksWithInputSymbol(renamed, uniqueParam.symbol, uniqueParam.name, currentOwner)(unit)
      
      /*
      if (options.verbose)
        println("Flattened tuples and blocks : \n\t" + 
          flattened.outerDefinitions.mkString("\n").replaceAll("\n", "\n\t") + 
          "\n\t" + uniqueParam + " => {\n\t\t" +
          flattened.statements.mkString("\n").replaceAll("\n", "\n\t\t") + 
          "\n\t\t(\n\t\t\t" + 
            flattened.values.mkString("\n").replaceAll("\n", "\n\t\t\t") + 
          "\n\t\t)\n\t}"
        )
      */
      
      def convertCode(tree: Tree) = 
        convert(removeSymbolsExceptParamSymbolAsUnderscore(uniqueParam.symbol, tree))
      
      val symsMap = Map(uniqueParam.symbol -> "_")
      val convDefs: Seq[FlatCode[String]] = flattened.outerDefinitions map convertCode
      val convStats: Seq[FlatCode[String]] = flattened.statements map convertCode
      val convVals: Seq[FlatCode[String]] = flattened.values map convertCode
      
      val outerDefinitions = 
        Seq(convDefs, convStats, convVals).flatMap(_.flatMap(_.outerDefinitions)).distinct.toArray.sortBy(_.startsWith("#"))
      
      val statements = 
        Seq(convStats, convVals).flatMap(_.flatMap(_.statements))
      
      val values: Seq[String] = 
        convVals.flatMap(_.values)

      //println("Renamed defined symbols uniquely : " + renamed)
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
        analyzer.inferImplicit(originalFunction, dataIOTpe, false, false, context).tree
      })
      
      //val (statements, values) = convertExpr(Map(uniqueParam.symbol -> "_"), body)
      val uniqueSignature = Literal(Constant(
        (
          Array(
            originalFunction.symbol.outerSource, originalFunction.symbol.tag + "|" + originalFunction.pos,
            sourceTpe, mappedTpe
          ) ++
          outerDefinitions ++
          statements ++
          values
        ).map(_.toString).mkString("|")
      ))
      val uniqueId = uniqueSignature.hashCode // TODO !!!
      
      if (options.verbose) {
        def indent(t: Any) =
          "\t" + t.toString.replaceAll("\n", "\n\t")

        println(
          (
            Array(
              "[scalacl] Converted <<<",
              indent(body),
              ">>> to <<<"
            ) ++
            outerDefinitions.map(indent) ++
            statements.map(indent) ++
            Array(
              indent("(" + values.mkString(", ") + ")"),
              ">>>"
            )
          ).mkString("\n")
        )
      }
      
      /*
      if (System.getenv("SCALACL_VERIFY") != "0") {
        val errors = scalaCLContexts.flatMap(context => {
          try {
            val f = new impl.CLFunction[Int, Int](null, outerDefinitions, statements, values, Seq())
            f.compile(context)
            // TODO illegal access : f.release(context)
            None
          } catch { case ex => Some((context.context.getDevices()(0), ex)) }
        })
        
                                
        if (errors.length > 0)
          throw new RuntimeException("Failed to compile the OpenCL kernel with some of the available devices :\n" + (errors.map { case (device, ex) => "Device " + device + " : " + ex }).mkString("\n"))
        else if (options.verbose)
          println("Program successfully compiled on " + scalaCLContexts.size + " available device(s)") 
      }
      */
      
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
                originalFunction,
                newSeqApply(TypeTree(StringClass.tpe), outerDefinitions.map(d => Literal(Constant(d))):_*),
                newSeqApply(TypeTree(StringClass.tpe), statements.map(s => Literal(Constant(s))):_*),
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

      clFunction
    }
    override def transform(tree: Tree): Tree = {
      if (!shouldOptimize(tree))
        super.transform(tree)
      else {
        try {
          tree match {
            case TraversalOp(traversalOp) if traversalOp.op.f != null =>
              import traversalOp._
              val colTpe = collection.tpe.widen.dealias.deconst
              //if (colTpe <:< CLCollectionClass.tpe) {
              if (colTpe.toString.startsWith("scalacl.")) { // TODO
                op match {
                  case opType @ (TraversalOp.Map(_, _) | TraversalOp.Filter(_, false)) =>
                    msg(unit, tree.pos, "associated equivalent OpenCL source to " + colTpe + "." + op + "'s function argument.") {
                      val clFunction = convertFunctionToCLFunction(op.f)
                      replaceOccurrences(super.transform(tree), Map(), Map(), Map(op.f -> (() => clFunction)), unit)
                    }
                  case _ =>
                    super.transform(tree)
                }
              } else {
                super.transform(tree)
              }
            case
              Apply(
                Apply(
                  TypeApply(
                    Select(
                      tg,
                      Function2CLFunctionName()
                    ),
                    List(typeFrom, typeTo)
                  ),
                  List(f)
                ),
                List(ioFrom, ioTo)
              ) if tg.toString == "scalacl.package" =>
              msg(unit, tree.pos, "transformed Scala function to a CLFunction.") {
                val clFunction = convertFunctionToCLFunction(f)
                clFunction.tpe = tree.tpe
                clFunction
                //replaceOccurrences(super.transform(tree), Map(), Map(), Map(f -> (() => clFunction)), unit)
              }
            case _ =>
              super.transform(tree)
          }
        } catch {
          case ex =>
            super.transform(tree)
        }
      }
    }
  }
}
