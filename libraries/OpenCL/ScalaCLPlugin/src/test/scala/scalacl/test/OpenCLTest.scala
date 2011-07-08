/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package scalacl ; package test
import plugin._

import java.io.File
import org.junit._
import Assert._
import scala.math._

class OpenCLTest extends TestUtils {

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

}
