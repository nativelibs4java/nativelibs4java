package scalacl

package impl

class CLSimpleCode(
  override val sources: Array[String],
  override val macros: Map[String, String],
  override val compilerArguments: Array[String]
) extends CLCode {
  def this(source: String) = this(Array(source), Map(), Array())
}

