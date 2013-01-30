package scalacl.impl

sealed trait ParamKind {
  def isArray: Boolean = false
}
object ParamKind {
  case object ImplicitArrayElement extends ParamKind
  case object RangeIndex extends ParamKind
  case object Normal extends ParamKind
}
