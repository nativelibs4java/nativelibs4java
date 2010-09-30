package scalacl

import java.io.File
import org.junit._
import Assert._

class IntRangeForeach2WhileTest extends TestUtils {

  implicit val outDir = new File("target/intRangeTestOut")
  outDir.mkdirs
    
  @Test
  def testSimpleLoop {
    ensureSourceIsTransformedToExpectedByteCode(
      """
        var t = 0
        for (i <- 0 to 100)
          t += 2 * i
      """,
      """
        var t = 0
        var i = 0
        val n = 100
        while (i <= n)
        {
          t += 2 * i
          i += 1
        }
      """
    )
  }
  @Test
  def testNestedLoop {
    ensureSourceIsTransformedToExpectedByteCode(
      """
        var t = 0
        for (i <- 0 to 100; j <- 0 to 1000)
          t += 2 * (i + j)
      """,
      """
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
      """
    )
  }
}
