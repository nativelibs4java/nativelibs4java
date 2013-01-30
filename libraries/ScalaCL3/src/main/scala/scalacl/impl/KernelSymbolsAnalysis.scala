package scalacl
package impl

import scala.collection.immutable.Stack
import scala.reflect.NameTransformer

trait KernelSymbolsAnalysis 
extends ConversionNames
with MiscMatchers
{ 
  val global: reflect.api.Universe
  import global._
  import definitions._

  import collection._
  
  case class KernelSymbols(
    symbolKinds: mutable.HashMap[Symbol, SymbolKind] = 
      new mutable.HashMap[Symbol, SymbolKind],
    symbolUsages: mutable.HashMap[Symbol, UsageKind] = 
      new mutable.HashMap[Symbol, UsageKind],
    symbolTypes: mutable.HashMap[Symbol, Type] = 
      new mutable.HashMap[Symbol, Type],
    localSymbols: mutable.HashSet[Symbol] = 
      new mutable.HashSet[Symbol]
  ) {
    lazy val symbols: Set[Symbol] = symbolKinds.keySet ++ symbolUsages.keySet ++ localSymbols
    lazy val capturedSymbols: Seq[Symbol] = (symbols -- localSymbols).toSeq
    
    def declareSymbolUsage(symbol: Symbol, tpe: Type, usage: UsageKind) {
      if (symbol == NoSymbol) {
        // TODO error("Cannot declare usage of NoSymbol!")
      } else {
        val actualTpe = try { symbol.typeSignature } catch { case _: Throwable => tpe }
        val symbolKind = getKind(symbol, actualTpe)
        if (symbolKind == SymbolKind.Other)
          sys.error("Cannot handle usage of symbol " + symbol)
        
        if (tpe.toString.endsWith(".type")) {
          println(s"""
          actualTpe: $actualTpe
          tpe: $tpe
          tpe.normalize: ${tpe.normalize}
          tpe.typeSymbol: ${tpe.typeSymbol}
          symbol: $symbol
          symbol.typeSignature: ${ try { symbol.typeSignature } catch { case ex => ex.toString } }
          """)
        }
        if ((tpe ne null) && actualTpe != NoType) {
          symbolTypes.get(symbol) match {
            case Some(t) =>
              assert(t == actualTpe)
            case None =>
              symbolTypes(symbol) = actualTpe
          }
        }
          
        symbolUsages.get(symbol) match {
          case Some(u) =>
            symbolUsages(symbol) = u.merge(usage)
          case None =>
            symbolUsages(symbol) = usage
        }
      }
    }
    lazy val primTypes = Set(IntTpe, LongTpe, ShortTpe, CharTpe, BooleanTpe, DoubleTpe, FloatTpe, ByteTpe)
    
    def getKind(symbol: Symbol, tpe: Type): SymbolKind = {
      // TODO
      //val tpe = symbolTypes.get(symbol).getOrElse(symbol.typeSignature)
      //val tpe = symbol.typeSignature
      if (tpe <:< typeOf[CLArray[_]] || tpe <:< typeOf[CLFilteredArray[_]])
        SymbolKind.ArrayLike
      else if (primTypes.find(tpe <:< _) != None)
        SymbolKind.Scalar
      else
        SymbolKind.Other
    }
  }
  
  def getExternalSymbols(tree: Tree, knownSymbols: Set[Symbol] = Set()): KernelSymbols = {
    
    val symbols = new KernelSymbols
    
    new Traverser {
      override def traverse(tree: Tree) = tree match {
        case Ident(n) =>
          if (!knownSymbols.contains(tree.symbol))
            symbols.declareSymbolUsage(tree.symbol, tree.tpe, UsageKind.Input)
        case Apply(Select(target, updateName()), List(index, value)) =>
          if (!knownSymbols.contains(target.symbol))
            symbols.declareSymbolUsage(target.symbol, target.tpe, UsageKind.Output)
          super.traverse(index)
          super.traverse(value)
        case Apply(Select(target, applyName()), List(index)) =>
          if (!knownSymbols.contains(target.symbol))
            symbols.declareSymbolUsage(target.symbol, target.tpe, UsageKind.Output) // TODO: switch to Input, there's a bug in scheduling.
          super.traverse(index)
        case ValDef(_, _, _, _) =>
          symbols.localSymbols += tree.symbol
          super.traverse(tree)
        case _ =>
          super.traverse(tree)
      }
    }.traverse(tree)
    
    symbols
  }
}
