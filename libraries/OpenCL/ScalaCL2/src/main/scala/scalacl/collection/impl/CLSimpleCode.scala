package scalacl
package collection
package impl

class CLSimpleCode(
  override val sources: Seq[String],
  override val macros: Map[String, String],
  override val compilerArguments: Seq[String]
) extends CLCode {
  def this(source: String) = this(Seq(source), Map(), Seq())
}

