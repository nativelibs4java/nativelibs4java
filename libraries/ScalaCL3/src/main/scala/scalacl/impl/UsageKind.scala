package scalacl.impl

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
