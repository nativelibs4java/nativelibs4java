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
package scalacl ; package plugin
import com.nativelibs4java.scalaxy._
import common._
import pluginBase._
import components._

import scala.tools.nsc.Global

import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.{Transform, TypingTransformers}
import scala.tools.nsc.typechecker.Analyzer
import scala.tools.nsc.typechecker.Contexts
import scala.Predef._

//import scala.tools.nsc.typechecker.Contexts._

object ScalaCLFunctionsTransformComponent {
  val runsAfter = List[String](
    "namer",
    LoopsTransformComponent.phaseName,
    StreamTransformComponent.phaseName
  )
  val runsBefore = List[String](
    "refchecks"
  )
  val phaseName = "scalacl-functionstransform"
}

class ScalaCLFunctionsTransformComponent(val global: Global, val options: PluginOptions)
extends PluginComponent
   with Transform
   with TypingTransformers
   with MiscMatchers
   with TreeBuilders
   with TraversalOps
   with OpenCLConverter
   with WithOptions
   with CodeFlattening
   with TupleAnalysis
   with CodeAnalysis
   with WorkaroundsForOtherPhases
{
  import global._
  import global.definitions._
  import gen._
  import CODE._
  import scala.tools.nsc.symtab.Flags._
  import typer.typed
  import analyzer.{SearchResult, ImplicitSearch, UnTyper}

  override val runsAfter = ScalaCLFunctionsTransformComponent.runsAfter
  override val runsBefore = ScalaCLFunctionsTransformComponent.runsBefore
  override val phaseName = ScalaCLFunctionsTransformComponent.phaseName

  import impl._
  
  val ScalaCLPackage       = getModule("scalacl")
  val ScalaCLPackageClass  = ScalaCLPackage.tpe.typeSymbol
  val CLDataIOClass = definitions.getClass("scalacl.impl.CLDataIO")
  val CLArrayClass = definitions.getClass("scalacl.CLArray")
  val CLFunctionClass = definitions.getClass("scalacl.impl.CLFunction")
  val CLRangeClass = definitions.getClass("scalacl.CLRange")
  val CLCollectionClass = definitions.getClass("scalacl.CLCollection")
  val CLFilteredArrayClass = definitions.getClass("scalacl.CLFilteredArray")
  val scalacl_ = N("scalacl")
  val getCachedFunctionName = N("getCachedFunction")
  val Function2CLFunctionName = N("Function2CLFunction")
  val withCaptureName = N("withCapture")
  
  
  def getDataIOByTupleInfo(ti: TupleInfo): CLDataIO[Any] = {
    if (ti.components.size == 1)
      getDataIO(ti.tpe)
    else
      new CLTupleDataIO[Any](ios = ti.components.toArray.map(getDataIOByTupleInfo _), null, null) // TODO
  }
  
  def getDataIO(tpe: Type): CLDataIO[Any] = {
    val dataIO = tpe.typeSymbol match {
	    case IntClass => CLIntDataIO
	    case ShortClass => CLShortDataIO
	    case ByteClass => CLByteDataIO
	    case CharClass => CLCharDataIO
	    case LongClass => CLLongDataIO
	    case BooleanClass => CLBooleanDataIO
	    case FloatClass => CLFloatDataIO
	    case DoubleClass => CLDoubleDataIO
	    case _ => {
	        try {
			    val tupleInfo = getTupleInfo(tpe)
			    val tupleDataIO = getDataIOByTupleInfo(tupleInfo)
			    //println("ScalaCLFunctionsTransform: dataIO=" + tupleDataIO.toString + "  tupleInfo=" + tupleInfo.toString)
			    tupleDataIO
			} catch { case e => 
			    println("ScalaCLFunctionsTransform: Exception getting CLDataIO for tuple.  e=" + e.getStackTraceString)
			    throw e
			}
	    }
    }
    dataIO.asInstanceOf[CLDataIO[Any]]
  }
  
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
    private def removeSymbolsExceptParamSymbolAsUnderscore(symbolReplacements: Map[Symbol, String], t: Tree) = {
      val trans = new Transformer { 
        override def transform(tree: Tree) = tree match {
          case Ident(name) if tree.hasSymbol =>
            //paramSymbol: Symbol
            symbolReplacements.get(tree.symbol) match {
              case Some(rep) =>
                treeCopy.Ident(tree, N(rep))
              case None =>
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

    def canCaptureSymbol(s: Symbol) = {
      //(System.getenv("SCALACL_CAPTURE_VARIABLES") != null) &&
      s.isValue && s.isStable && !s.isMutable &&
      s.isInstanceOf[TermSymbol] && s.isEffectivelyFinal && // this rules out captured fields !
      {
        val tpe = normalize(s.tpe) match {
          case NullaryMethodType(result) =>
            result
          case tpe =>
            tpe
        }
        
        tpe == IntClass.tpe ||
        tpe == ShortClass.tpe ||
        tpe == LongClass.tpe ||
        tpe == ByteClass.tpe ||
        tpe == DoubleClass.tpe ||
        tpe == FloatClass.tpe ||
        tpe == CharClass.tpe ||
        (tpe match { // tpe == CLArrayClass.tpe
          case TypeRef(_, CLArrayClass, List(_)) =>
            true
          case _ =>
            false
        })
      }
    }
    
    def getDataIOImplicit(tpe: Type, context: analyzer.Context, enclosingTree: Tree): (Tree, CLDataIO[Any]) = {
      val dataIOTpe = appliedType(CLDataIOClass.tpe, List(tpe))
      var ioTree = analyzer.inferImplicit(enclosingTree, dataIOTpe, false, false, context).tree
      if (ioTree == null)
        null
      else {
        val dataIO = getDataIO(tpe)
        (ioTree, dataIO)
      }
    }
    def conversionError(pos: Position, msg: String) = {
      unit.error(pos, msg)
      throw new UnsupportedOperationException("Conversion error : " + msg)
    }
    
    case class Capture(symbol: Symbol, io: (Tree, CLDataIO[Any]), isArray: Boolean, arg: Tree)
      
    val anyCLDataIOTpe = TypeRef(NoPrefix, CLDataIOClass, List(AnyClass.tpe))
    val anyCLArrayTpe = TypeRef(NoPrefix, CLArrayClass, List(AnyClass.tpe))
      
    def getCaptures(f: Tree, context: analyzer.Context, enclosingTree: Tree): (Seq[Capture], Boolean) = {
      val externalSymbolInfo = 
        getUnknownSymbolInfo(f, t => t.symbol != NoSymbol && {
          //println("Examining t  = " + t )
          t.symbol match {
            case s: MethodSymbol =>
              !(s.owner == CLArrayClass && s.toString == "method apply") &&
              !primitiveTypeSymbols.contains(s.owner) && 
              s.owner != ScalaMathCommonClass &&
              !s.toString.matches("value _\\d+") &&
              !s.toString.matches("method to(Double|Int|Float|Long|Short|Byte|Boolean|Char)") &&
              !(s.toString == "method apply" && s.owner != null && s.owner.toString.matches("object Tuple\\d+")) // TODO cleanup hack
            case s: ModuleSymbol =>
              false
            case s: TermSymbol =>
              t.isInstanceOf[Ident]
            case _ =>
              false
          }
        })
      
      val externalRefsBySymbol = externalSymbolInfo.unknownReferences.groupBy(_.symbol)
      val externalSymbols = externalRefsBySymbol.keys.toSet
      
      val (capturableSymbols, nonCapturableSymbols) =
        externalSymbols.partition(canCaptureSymbol)
        
      for (s <- nonCapturableSymbols) yield {
        val details = 
          if (options.verbose)
            "\nDetails about symbol '" + s + "' (" + s.getClass.getName + ") :\n\t" +
            Map(
              "tpe" -> {
                val t = normalize(s.tpe)
                t + " (" + t.getClass.getName + ")"
              },
              "isMutable" -> s.isMutable,
              "isFinal" -> s.isFinal,
              "isEffectivelyFinal" -> s.isEffectivelyFinal,
              "isStable" -> s.isStable,
              "isConstant" -> s.isConstant,
              "isValue" -> s.isValue,
              "isVariable" -> s.isVariable,
              "isModuleVar" -> s.isModuleVar,
              "isLocal" -> s.isLocal,
              "isValueParameter" -> s.isValueParameter,
              "isCapturedVariable" -> s.isCapturedVariable,
              "isLocalDummy" -> s.isLocalDummy,
              "isErroneous" -> s.isErroneous,
              "isOuterField" -> s.isOuterField,
              "isOuterAccessor" -> s.isOuterAccessor
            ).map({ case (n, v) => n + ":\t" + v }).mkString("\n\t")
          else
            ""
             
        val refs = externalRefsBySymbol(s)
          for (ref <- refs) {
            unit.error(ref.pos, "Cannot capture this external symbol (can only capture final AnyVal or CLArray values)." + details)
          }
      }
      
      val captures = for (s <- capturableSymbols) yield {
        val refs = externalRefsBySymbol(s)
        val (tpe: Type, isArray: Boolean) = s.tpe match {
          case TypeRef(_, CLArrayClass, List(tpe)) =>
            (tpe, true)
          case _ =>
            (s.tpe, false)
        }
        
        var io = getDataIOImplicit(tpe, context, enclosingTree)
        if (io == null) {
          for (ref <- refs)
            unit.error(ref.pos, "Cannot infer CLDataIO instance for type " + tpe + " of captured " + (if (isArray) "array" else "") + " variable !")
        }
        val arg = ident(s, N(refs.first.toString))
        Capture(symbol = s, io = io, isArray = isArray, arg = arg)
      }
      
      (captures.toSeq, nonCapturableSymbols.isEmpty)
    }
    
    def convertFunctionToCLFunction(originalFunction: Tree): Tree = {
      val f = duplicator transform originalFunction

      //println("originalFunction = " + originalFunction)

      val context = localTyper.context1
      
      val (captures, succeeded) = getCaptures(f, context, originalFunction)
      //println("captures = " + captures)
      
      if (!succeeded) {
        // Failure !
        originalFunction
      } else {
        val extraInputBufferArgsIOs = Seq[(Tree, CLDataIO[Any])]() // TODO put here the ios for arrays that are used in read-only mode
        val extraOutputBufferArgsIOs = captures.filter(_.isArray).map(_.io)
        val extraScalarArgsIOs = captures.filter(!_.isArray).map(_.io)
        
        val extraInputBufferArgs = Seq[Tree]() // TODO put here the ios for arrays that are used in read-only mode
        val extraOutputBufferArgs = captures.filter(_.isArray).map(_.arg.AS(anyCLArrayTpe))
        val extraScalarArgs = captures.filter(!_.isArray).map(_.arg)
        
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
  
        // Symbols replacement map : replace function param by "_" and captured symbols by "_1", "_2"...
        val symsMap =
          Map(uniqueParam.symbol -> "_") ++
          (captures.zipWithIndex.map { case (c, i) => c.symbol -> ("_" + (i + 1)) })
        
        //println("symsMap = " + symsMap)
  
        def convertCode(tree: Tree) =
          convert(removeSymbolsExceptParamSymbolAsUnderscore(symsMap/*uniqueParam.symbol*/, tree))
  
        val Array(convDefs, convStats, convVals) = Array(flattened.outerDefinitions, flattened.statements, flattened.values).map(_ map convertCode)
        
        val outerDefinitions = 
          Seq(convDefs, convStats, convVals).flatMap(_.flatMap(_.outerDefinitions)).distinct.toArray.sortBy(_.startsWith("#"))
        
        val statements = 
          Seq(convStats, convVals).flatMap(_.flatMap(_.statements))
        
        val values: Seq[String] = 
          convVals.flatMap(_.values)
  
        //println("Renamed defined symbols uniquely : " + renamed)
        val sourceTpe = uniqueParam.symbol.tpe
        val mappedTpe = body.tpe
        val Array(
          sourceDataIO, 
          mappedDataIO
        ) = Array(
          sourceTpe, 
          mappedTpe
        ).map(tpe => getDataIOImplicit(tpe, context, originalFunction))
        
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
        
        val sourceData = {
          val aIO = sourceDataIO._2
          val bIO = mappedDataIO._2
          println("aIO = " + aIO + ", bIO = " + bIO)
          CLFunctionCode.buildSourceData[Any, Any](
            outerDeclarations = outerDefinitions,
            declarations = statements.toArray,
            expressions = values.toArray,
            includedSources = Array(),
            extraArgsIOs = CapturedIOs(
              extraInputBufferArgsIOs.toArray.map(_._2),
              extraOutputBufferArgsIOs.toArray.map(_._2),
              extraScalarArgsIOs.toArray.map(_._2)
            )
          )(aIO, bIO)
        }
        
        println("sourceData = " + sourceData)
        
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
                  newArrayApply(TypeTree(StringClass.tpe), outerDefinitions.map(d => Literal(Constant(d))):_*),
                  newArrayApply(TypeTree(StringClass.tpe), statements.map(s => Literal(Constant(s))):_*),
                  newArrayApply(TypeTree(StringClass.tpe), values.map(value => Literal(Constant(value))):_*),
                  newArrayApply(TypeTree(anyCLDataIOTpe), extraInputBufferArgsIOs.map(_._1):_*),
                  newArrayApply(TypeTree(anyCLDataIOTpe), extraOutputBufferArgsIOs.map(_._1):_*),
                  newArrayApply(TypeTree(anyCLDataIOTpe), extraScalarArgsIOs.map(_._1):_*)
                )
              ).setSymbol(getCachedFunctionSym),
              List(
                sourceDataIO._1,
                mappedDataIO._1
              )
            ).setSymbol(getCachedFunctionSym)
          }
  
        if (captures.isEmpty)
          clFunction
        else
          typed {
            val withCaptureSym = CLFunctionClass.tpe member withCaptureName
            Apply(
              Select(
                clFunction,
                withCaptureName
              ).setSymbol(withCaptureSym),
              List(
                newArrayApply(TypeTree(anyCLArrayTpe), extraInputBufferArgs:_*),
                newArrayApply(TypeTree(anyCLArrayTpe), extraOutputBufferArgs:_*),
                newArrayApply(TypeTree(AnyClass.tpe), extraScalarArgs:_*)
              )
            ).setSymbol(withCaptureSym)
          }
      }
    }
    override def transform(tree: Tree): Tree = {
      if (!shouldOptimize(tree))
        super.transform(tree)
      else {
        try {
          tree match {
            case TraversalOp(traversalOp) if traversalOp.op.f != null =>
              import traversalOp._
              val colTpe = normalize(collection.tpe)
              //if (colTpe <:< CLCollectionClass.tpe) {
              if (colTpe.toString.startsWith("scalacl.")) { // TODO
                op match {
                  case opType @ (TraversalOps.MapOp(_, _, _) | TraversalOps.FilterOp(_, _, false)) =>
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
              ) if isPackageReference(tg, "scalacl") =>
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
