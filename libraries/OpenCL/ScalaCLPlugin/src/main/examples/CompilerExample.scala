package scalacl

import scalacl._
import scala.math._

/** An example demonstrating the fancy features of the new
 *  compiler plugin.
 */
class CompilerExample {
  
  def simple_while {
      var t = 0
      var i = 0
      val n = 100
      while (i <= n)
      {
        t += 2 * i
        i += 1
      }
  }
  def simple_foreach {
      var t = 0
      for (i <- 0 to 100)
        t += 2 * i
  }
  
  def nested_while {
      var t = 0
      var i = 0
      val n = 100
      while (i <= n)
      {
          var j = 0
          val m = 1000
          while (j <= m)
          {
            t += 2 * (i + j)
            j += 1
          }
          i += 1
      }
  }
  def nested_foreach {
      var t = 0
      for (i <- 0 to 100; j <- 0 to 1000)
        t += 2 * (i + j)
  }
}
