package scalacl.impl

sealed trait SymbolKind
object SymbolKind {
  case object ArrayLike extends SymbolKind
  case object Scalar extends SymbolKind
  //case object ConvertiblePredefined extends SymbolKind
  case object Other extends SymbolKind
}
