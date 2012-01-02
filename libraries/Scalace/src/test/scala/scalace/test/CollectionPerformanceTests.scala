/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.scalace ; package test
import plugin._

import org.junit._
import Assert._

trait ChainedPerformanceTests {
  this: CollectionPerformanceTests =>
  def chain(du: (String, String)) = {
    val (definition, use) = du
    (definition, use + ".filter(v => (v % 2) == 0).map(_ * 2)")
  }
}

trait NoRightTests { //extends CollectionPerformanceTests {
  this: CollectionPerformanceTests => 
  override def simpleScanRight = {}
  override def simpleFoldRight = {}
  override def simpleReduceRight = {}
}
trait NoScalarReductionTests {//extends CollectionPerformanceTests {
  this: CollectionPerformanceTests => 
  override def simpleSum = {}
  override def simpleProduct = {}
  override def simpleMin = {}
  override def simpleMax = {}
}
class ListPerformanceTest extends CollectionPerformanceTests with NoRightTests {
  override def col = ("val col: List[Int] = (0 to n).toList", "col")//.filter(v => (v % 2) == 0).map(_ * 2)")
}
class ListChainedPerformanceTest extends ListPerformanceTest with ChainedPerformanceTests {
  override def col = chain(super.col)
}
class ArrayPerformanceTest extends CollectionPerformanceTests {
  override def col = ("val col = Array.tabulate(n)(i => i)", "col")
  @Test def simpleArrayTabulate =  if (!skip) ensureFasterCodeWithSameResult(null, "Array.tabulate(n)(i => i).toSeq")
}
class ArrayChainedPerformanceTest extends ArrayPerformanceTest with ChainedPerformanceTests {
  override def col = chain(super.col)
}
class RangePerformanceTest extends CollectionPerformanceTests with NoRightTests with NoScalarReductionTests {
  override def col = (null: String, "(0 until n)")
  override def simpleToArray = {}
  override def simpleToList = {}
  override def simpleTakeWhile = {}
  override def simpleDropWhile = {}
  override def simpleSum = {}
  override def simpleProduct = {}
  override def simpleMin = {}
  override def simpleMax = {} 
}
class RangeChainedPerformanceTest extends CollectionPerformanceTests with ChainedPerformanceTests with NoRightTests with NoScalarReductionTests {
  override def col = chain((null, "(0 until n)"))
}

trait CollectionPerformanceTests extends PerformanceTests {
  val skip = PerformanceTests.skip
  def col: (String, String)
  
  /**************************
   * Collection conversions *
   **************************/
  @Test def simpleToArray = if (!skip) testToArray(col)              
  @Test def simpleToList = if (!skip) testToList(col)              
  //@Test def simpleToVector = if (!skip) testToVector(col)              
  
  @Test def simpleFilter = testFilter(col)           
  @Test def simpleFilterNot = testFilterNot(col)     
  @Test def simpleCount = testCount(col)             
  @Test def simpleExists = testExists(col)           
  @Test def simpleForall = testForall(col)           
  @Test def simpleTakeWhile = testTakeWhile(col)     
  @Test def simpleDropWhile = testDropWhile(col)     
  @Test def simpleForeach = testForeach(col)         
  @Test def simpleMap = testMap(col)                 
  @Test def simpleSum = testSum(col)                 
  @Test def simpleProduct = testProduct(col)                 
  @Test def simpleMin = testMin(col)                 
  @Test def simpleMax = testMax(col)                 
  @Test def simpleScanLeft = testScanLeft(col)       
  @Test def simpleScanRight = testScanRight(col)     
  @Test def simpleFoldLeft = testFoldLeft(col)       
  @Test def simpleFoldRight = testFoldRight(col)     
  @Test def simpleReduceLeft = testReduceLeft(col)   
  @Test def simpleReduceRight = testReduceRight(col) 
  
}
