package scalacl

import java.io.File
import org.junit._
import Assert._

class IntRangeForeach2WhileTest extends TestUtils {

  implicit val outDir = new File("target/intRangeTestOut")
  outDir.mkdirs
    
  @Test
  def simpleToLoop {
    ensurePluginCompilesSnippetsToSameByteCode(
      """ var t = 0
          for (i <- 0 to 100)
            t += 2 * i
      """,
      """ var t = 0
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
  def simpleUntilLoop {
    ensurePluginCompilesSnippetsToSameByteCode(
      """ var t = 0
          for (i <- 0 until 100)
            t += 2 * i
      """,
      """ var t = 0
          var i = 0
          val n = 100
          while (i < n)
          {
            t += 2 * i
            i += 1
          }
      """
    )
  }


  @Test
  def simpleToByLoop {
    ensurePluginCompilesSnippetsToSameByteCode(
      """ var t = 0
          for (j <- 50 to 200 by 3)
            t += j / 2
      """,
      """ var t = 0
          var j = 50
          val m = 200
          while (j <= m)
          {
            t += j / 2
            j += 3
          }
      """
    )
  }

  @Test
  def simpleUntilByLoop {
    ensurePluginCompilesSnippetsToSameByteCode(
      """ var t = 0
          for (j <- 50 until 200 by 3)
            t += j / 2
      """,
      """ var t = 0
          var j = 50
          val m = 200
          while (j < m)
          {
            t += j / 2
            j += 3
          }
      """
    )
  }
  @Test
  def testNestedLoop {
    ensurePluginCompilesSnippetsToSameByteCode(
      """ var t = 0
          for (i <- 0 to 100 by 5; j <- 0 until 1000)
            t += 2 * (i + j)
      """,
      """ var t = 0
          var i = 0
          val n = 100
          while (i <= n)
          {
              var j = 0
              val m = 1000
              while (j < m)
              {
                t += 2 * (i + j)
                j += 1
              }
              i += 5
          }
      """
    )
  }
  
  @Test
  def testNestedLoopWithExtRefs {
    ensurePluginCompilesSnippetsToSameByteCode(
      """ var t = 0
          def f(x: Int) = x + 1
          def g(x: Int) = x - 1
          for (i <- 0 to 100 by 5; j <- 0 until 1000)
            t += 2 * (f(i) + g(j))
      """,
      """ var t = 0
          def f(x: Int) = x + 1
          var i = 0
          val n = 100
          while (i <= n)
          {
              def g(x: Int) = x - 1
              var j = 0
              val m = 1000
              while (j < m)
              {
                t += 2 * (f(i) + g(j))
                j += 1
              }
              i += 5
          }
      """
    )
  }
}
