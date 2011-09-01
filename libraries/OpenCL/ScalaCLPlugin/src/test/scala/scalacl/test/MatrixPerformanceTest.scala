/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package scalacl ; package test
import plugin._

import org.junit._
import Assert._

class MatrixPerformanceTest extends TestUtils {
  import PerformanceTests.{ skip, stream }
  
  @Test def simpleMatrixTest = if (!skip) ensureFasterCodeWithSameResult(
    """
      val a = Array.tabulate[Double](n, n)(_ + _)
      val b = Array.tabulate[Double](n, n)(_ + _)
    """,
    if (!stream)
      """
        var bigTot = 0.0
        val o = Array.tabulate(n, n)((i, j) => {
          var tot = 0.0
          for (k <- 0 until n)
            tot += a(i)(k) * b(k)(j)
    
          bigTot += tot
          tot
        })
        bigTot + o.size
      """ 
    else 
      """
        val out = Array.tabulate[Double](n, n)((i, j) => {
          (0 until n).map(k => a(i)(k) * b(k)(j)).sum
        })
        
        out.map(_.toSeq).toSeq // to make it equals-comparable
      """, 
    Seq(100), 
    minFaster = 20.0
  )

}

