/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package scalacl

import java.io.File
import org.junit._
import Assert._

class ArrayForeach2WhileTest extends TestUtils {

  implicit val outDir = new File("target/arrayForeachTestOut")
  outDir.mkdirs

  @Test
  def simpleArrayForeach {
    for (t <- Array("Double", "Float", "Long", "Int", "Long", "Short", "Byte", "Char", "Boolean"))
        ensurePluginCompilesSnippetsToSameByteCode(
          """ val a = new Array[""" + t + """](10)
              a.foreach(println(_))
          """,
          """ val a = new Array[""" + t + """](10)
              val aa = a
              var i = 0
              val n = aa.length
              while (i < n)
              {
                println(a(i))
                i += 1
              }
          """
        )
  }
}
