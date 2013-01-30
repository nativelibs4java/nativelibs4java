package scalacl
package impl

import scala.collection.immutable.Stack
import scala.reflect.NameTransformer

sealed trait SymbolKind
object SymbolKind {
  case object Array extends SymbolKind
  case object Constant extends SymbolKind
  case object ConvertiblePredefined extends SymbolKind
  case object Other extends SymbolKind
}

sealed trait UsageKind {
  def merge(usage: UsageKind): UsageKind
}
object UsageKind {
  case object Input extends UsageKind {
    override def merge(usage: UsageKind) = 
      if (usage == Input) Input
      else InputOutput
  }
  case object Output extends UsageKind {
    override def merge(usage: UsageKind) = 
      if (usage == Output) Output
      else InputOutput
  }
  case object InputOutput extends UsageKind {
    override def merge(usage: UsageKind) = 
      this
  }
}

trait KernelSymbolsAnalysis 
extends ConversionNames
with MiscMatchers
{ 
  val global: reflect.api.Universe
  import global._
  import definitions._

  import collection._
  
  case class KernelSymbols(
    symbolKinds: mutable.HashMap[Symbol, SymbolKind] = new mutable.HashMap[Symbol, SymbolKind],
    symbolUsages: mutable.HashMap[Symbol, UsageKind] = new mutable.HashMap[Symbol, UsageKind],
    localSymbols: mutable.HashSet[Symbol] = new mutable.HashSet[Symbol]
  ) {
    def declareSymbolUsage(symbol: Symbol, usage: UsageKind) {
      if (symbol == NoSymbol) {
        // TODO error("Cannot declare usage of NoSymbol!")
      } else {
        val symbolKind = getKind(symbol)
        if (symbolKind == SymbolKind.Other)
          sys.error("Cannot handle usage of symbol " + symbol)
        
        symbolUsages.get(symbol) match {
          case Some(u) =>
            symbolUsages(symbol) = u.merge(usage)
          case None =>
            symbolUsages(symbol) = usage
        }
      }
    }
    def getKind(symbol: Symbol): SymbolKind = {
      // TODO
      SymbolKind.Array
    }
  }
  
  def getExternalSymbols(tree: Tree, knownSymbols: Set[Symbol] = Set()): KernelSymbols = {
    
    val symbols = new KernelSymbols
    
    new Traverser {
      override def traverse(tree: Tree) = tree match {
        case Ident(n) =>
          symbols.declareSymbolUsage(tree.symbol, UsageKind.Input)
        case Apply(Select(target, updateName()), List(index, value)) =>
          symbols.declareSymbolUsage(target.symbol, UsageKind.Output)
          super.traverse(index)
          super.traverse(value)
        case Apply(Select(target, applyName()), List(index)) =>
          symbols.declareSymbolUsage(target.symbol, UsageKind.Output)
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
