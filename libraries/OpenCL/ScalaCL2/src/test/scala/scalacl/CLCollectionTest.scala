/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package scalacl

import impl._

import com.nativelibs4java.opencl._
import org.bridj.Pointer
import org.bridj.Pointer._

import org.junit._
import Assert._

import scala.math._
import com.nativelibs4java.opencl._

object CLCollectionTest {

  implicit var context: ScalaCLContext = _
  
  def cla = (0 until n).toCLArray
  def a = (0 until n).toArray
  
  var f: Int => Boolean = _
  var m: Int => Int = _
  var m2: Int => (Int, Int) = _
  var m2join: ((Int, Int)) => Int = _
  
  val n = 10
  val samples = n

  def same[V](b: Traversable[V], a: CLIndexedSeq[V])(implicit v: ClassManifest[V]) {
    val aa = a.toArray.take(samples).toSeq
    val bb = b.toArray.take(samples).toSeq
    assertEquals(aa, bb)
  }
  
  @BeforeClass
  def setUp: Unit = {
    //CLEvent.setNoEvents(true)
    
    if ("0" == System.getenv("JAVACL_CACHE_BINARIES"))
      JavaCL.setCacheBinaries(false)
    
    context = new ScalaCLContext
    
    //cla = (0 until n).toCLArray
    //a = (0 until n).toArray
    
    f = (
      (x: Int) => (exp(x).toInt % 2) == 0, 
      Seq("(((int)exp((float)_)) % 2) == 0")
    ): CLFunction[Int, Boolean]
    
    m = (
      (x: Int) => (x * 2 * exp(x)).toInt, 
      Seq("(int)(_ * 2 * exp((float)_))")
    ): CLFunction[Int, Int]
    
    m2 = (
      (x: Int) => (x, x * 2), 
      Seq("_", "_ * 2")
    ): CLFunction[Int, (Int, Int)]
 
    m2join = (
      (p: (Int, Int)) => p._1 + 2 * p._2, 
      Seq("_._1 + 2 * _._2")
    ): CLFunction[(Int, Int), Int]
 
  }

  @AfterClass
  def tearDown: Unit = {
    context.release
  }
}

class CLCollectionTest {

  import CLCollectionTest._
  
  @Test
  def testSimpleFilter {
    same(a.filter(f), cla.filter(f).toCLArray)
    context.queue.finish
  }
  @Test
  def testSimpleMap {
    same(a.map(m), cla.map(m))
    context.queue.finish
  }
  
  @Test
  def testTupleMap {
    same(a.map(m2), cla.map(m2))
    context.queue.finish
  }
  
  @Test
  def testFilterMap {
    same(a.filter(f).map(m), cla.filter(f).map(m))
    context.queue.finish
  }
  @Test
  def testFilterMapTuple2Result {
    same(a.filter(f).map(m2), cla.filter(f).map(m2))
    context.queue.finish
  }
  
  @Test
  def testFilterMapTuple2Arg {
    same(a.filter(f).map(m2).map(m2join), cla.filter(f).map(m2).map(m2join))
    context.queue.finish
  }
  
  @Test
  def testToString {
    assertEquals("CLArray()", CLArray[Int]().toString)
    assertEquals("CLArray(1, 2)", CLArray(1, 2).toString)
    assertEquals("CLFilteredArray()", CLArray(1, 2).filter(_ => false).toString)
    assertEquals("CLFilteredArray(1, 2)", CLArray(1, 2).filter(_ => true).toString)
  }
}
