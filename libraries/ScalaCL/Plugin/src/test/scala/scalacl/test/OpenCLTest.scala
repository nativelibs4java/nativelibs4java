/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package scalacl ; package test
import plugin._
import com.nativelibs4java.scalaxy._
import plugin._
import pluginBase._
import components._

import java.io.File
import org.junit._
import Assert._
import scala.math._

class OpenCLTest extends ScalaCLTestUtils {

  val stdImportsAndContext = """
    import scalacl._
    import scala.math._

    implicit val context = Context.best(DoubleSupport)
  """
  
  @Test
  def testMath {
    val cla = compileCodeWithPlugin(
      stdImportsAndContext,
      """
      (0 to 10).cl.map(i => cos(i).toFloat).toArray
      """
    ).newInstance()().asInstanceOf[Array[Float]]
    val a = 
         (0 to 10).map(i => cos(i).toFloat).toArray
    
    assertArrayEquals(a, cla, 0.00001f)
  }

  
  @Test
  def testScalarCapture {
    val cla = compileCodeWithPlugin(
      stdImportsAndContext,
      """
      val n = (0 to 10).sum
      (0 to 10).cl.map(i => i * n + n * n * (i - 1)).toArray
      """
    ).newInstance()().asInstanceOf[Array[Int]]
    
    val n = (0 to 10).sum
    val a = 
         (0 to 10).map(i => i * n + n * n * (i - 1)).toArray
    
    assertArrayEquals(a, cla)
  }

  @Test
  def testArrayCapture {
    val cla = compileCodeWithPlugin(
      stdImportsAndContext,
      """
      val arr = (0 to 10).toCLArray
      (0 to 10).cl.map(i => arr(i / 2) * 2).toArray
      """
    ).newInstance()().asInstanceOf[Array[Int]]
    
    val arr = (0 to 10).toArray
    val a = 
         (0 to 10).map(i => arr(i / 2) * 2).toArray
      
    assertArrayEquals(a, cla)
  }
  
  @Ignore
  @Test
  def testMatrixMult {
    val cla = compileCodeWithPlugin(
      stdImportsAndContext ++ """
        type Matrix =
          CLArray[Double]
          
        def newMatrix(rows: Int, columns: Int)(implicit context: Context) =
          new Matrix(rows * columns)
          
        def fill(m: Matrix, rows: Int, columns: Int) = {
          for (idx <- (0 until (rows * columns)).cl) {
            val i = idx / columns
            val j = idx - i * columns
            m(idx) = i - j
          }
        }
        
        def multiply(a: Matrix, aRows: Int, aCols: Int, b: Matrix, bRows: Int, bCols: Int)(implicit context: Context): Matrix = {
          assert(aCols == bRows)
          
          val outRows = aRows
          val outCols = bCols
          val out = newMatrix(outRows, outCols)
          for (idx <- (0 until (outRows * outCols)).cl) {
            val i = idx / outCols
            val j = idx - i * outCols
            
            out(idx) = 
              (0 until aCols).map(
                k => a(i * aCols + k) * b(k * bCols + j)
              ).sum
          }
          out
        }
      """,
      """
      
      val m = 3
      val n = 4
      val o = 5
      
      val a = newMatrix(m, n)
      fill(a, m, n)
      val b = newMatrix(n, o)
      fill(b, n, o)
      
      val out = Array.tabulate[Double](n, n)((i, j) => {
        (0 until n).map(k => a(i)(k) * b(k)(j)).sum
      })
        
      (a.toSeq, b.toSeq, out.toSeq)
      """
    ).newInstance()().asInstanceOf[Array[Int]]
    
    val arr = (0 to 10).toArray
    val a = 
         (0 to 10).map(i => arr(i / 2) * 2).toArray
      
    assertArrayEquals(a, cla)
  }

}
