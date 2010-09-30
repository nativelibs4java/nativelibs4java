package scalacl

import scalacl._
import scala.math._

/** An example demonstrating the fancy features of the new
 *  compiler plugin.
 */
class CompilerExample {
  
  def test {
      var t = 0
      var iVar$1 = 0
      val nVal$1 = 100
      while (iVar$1 <= nVal$1)
      {
        t += 2 * iVar$1
        iVar$1 += 1
      }
  }
  def test2 {
      var t = 0
      for (i <- 0 to 100)
        t += 2 * i
  }
}
