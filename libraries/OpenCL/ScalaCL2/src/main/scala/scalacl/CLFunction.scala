/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package scalacl

import scala.collection.mutable.HashMap

object CLFunction {
  val compositions = new HashMap[(Long, Long), CLFunction[_, _]]
  val ands = new HashMap[(Long, Long), CLFunction[_, _]]
  private var nextuid = 1
  protected def newuid = this.synchronized {
    val uid = nextuid
    nextuid += 1
    uid
  }
}

class CLFunction[A, B](
  val function: A => B,
  declarations: Seq[String],
  val expression: String,
  includedSources: Seq[String],
  inVar: String = "_",
  indexVar: String = "$i",
  sizeVar: String = "$size"
)(
  implicit
  val aIO: CLDataIO[A],
  val bIO: CLDataIO[B]
)
extends CLCode
{
  lazy val uid = CLFunction.newuid
  lazy val functionName = "f" + uid

  private def toRxb(s: String) = {
    val rx = //"(^|\\b)" +
      java.util.regex.Matcher.quoteReplacement(s)// + "($|\\b)"
    rx
  }
  def replaceAllButIn(s: String) = if (s == null) null else {
    var r = s.replaceAll(toRxb(indexVar), "i")
    r = r.replaceAll(toRxb(sizeVar), "size")
    r
  }
  val ta = clType[A]
  val tb = clType[B]
  val inParam = "__global const " + ta + "* in"
  val outParam = "__global " + tb + "* out"
  def replaceForFunction(s: String) = if (s == null) null else {
    var r = replaceAllButIn(s)
    r = r.replaceAll(toRxb(inVar), "(*in)")
    r
  }

  val indexHeader = """
      size_t i = get_global_id(0);
  """
  val sizeHeader =
      indexHeader + """
      if (i >= size)
          return;
  """
  val funDecls = declarations.map(replaceForFunction).reduceLeftOption(_ + "\n" + _).getOrElse("")
  val functionSource = if (expression == null) null else """
      inline void """ + functionName + """(
          """ + inParam + """,
          """ + outParam + """
      ) {
          """ + indexHeader + """
          """ + funDecls + """
          *out = """ + replaceForFunction(expression) + """;
      }
  """

  def replaceForKernel(s: String) = if (s == null) null else replaceAllButIn(s).replaceAll(toRxb(inVar), "in[i]")
  val assignt = "out[i] = " + replaceForKernel(expression) + ";"

  val presenceParam = "__global const char* presence"
  val kernDecls = declarations.map(replaceForKernel).reduceLeftOption(_ + "\n" + _).getOrElse("")
  val kernelsSource = if (expression == null) null else """
      __kernel void array(
          size_t size,
          """ + inParam + """,
          """ + outParam + """
      ) {
          """ + sizeHeader + """
          """ + kernDecls + """
          """ + assignt + """
      }
      __kernel void filteredArray(
          size_t size,
          """ + inParam + """,
          """ + presenceParam + """,
          """ + outParam + """
      ) {
          """ + sizeHeader + """
          if (!presence[i])
              return;
          """ + kernDecls + """
          """ + assignt + """
      }
  """

  val sourcesToInclude = if (expression == null) null else includedSources ++ Seq(functionSource)
  override val sources = if (expression == null) null else sourcesToInclude ++ Seq(kernelsSource)
  override val macros = Map[String, String]()
  override val compilerArguments = Seq[String]()
  
  import CLFunction._
  def compose[C](f: CLFunction[C, A])(implicit cIO: CLDataIO[C]): CLFunction[C, B] = {
    compositions.synchronized {
      compositions.getOrElseUpdate((uid, f.uid), {
        new CLFunction[C, B](function.compose(f.function), Seq(), functionName + "(" + f.functionName + "(_))", sourcesToInclude ++ f.sourcesToInclude).asInstanceOf[CLFunction[_, _]]
      }).asInstanceOf[CLFunction[C, B]]
    }
  }

  def and(f: CLFunction[A, B])(implicit el: B =:= Boolean): CLFunction[A, B] = {
    ands.synchronized {
      ands.getOrElseUpdate((uid, f.uid), {
        new CLFunction[A, B](a => (function(a) && f.function(a)).asInstanceOf[B], Seq(), "(" + functionName + "(_) && " + f.functionName + "(_))", sourcesToInclude ++ f.sourcesToInclude).asInstanceOf[CLFunction[_, _]]
      }).asInstanceOf[CLFunction[A, B]]
    }
  }
}
