/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package scalacl

import java.io.File
import org.junit._
import Assert._

class ArrayMap2WhileTest extends TestUtils {

  implicit val outDir = new File("target/arrayMapTestOut")
  outDir.mkdirs

  @Test
  def simplePrimitiveArrayMap = 
    for (p <- Seq("Double", "Float", "Int", "Short", "Long", "Byte", "Char", "Boolean"))
      simpleArrayMap(p)

  @Test
  def simpleStringArrayMap =
    simpleArrayMap("String")

  def simpleArrayMap(typeStr: String) {
    ensurePluginCompilesSnippetsToSameByteCode(
      """ val a = new Array[""" + typeStr + """](10)
          val m = a.map(_ + "...")
      """,
      """ val a = new Array[""" + typeStr + """](10)
          val m = {
            val aa = a
            var i = 0
            val n = aa.length
            val mm = new Array[String](n)
            while (i < n)
            {
              mm(i) = aa(i) + "..."
              i += 1
            }
            mm
          }
      """
    )
  }
}
