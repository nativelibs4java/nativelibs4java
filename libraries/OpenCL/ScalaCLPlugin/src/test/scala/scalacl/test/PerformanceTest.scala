/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package scalacl ; package test
import plugin._

import org.junit._
import Assert._

object PerformanceTest {
  lazy val skip = {
    val test = "1" == System.getenv("SCALACL_TEST_PERF")
    if (!test)
      println("You can run " + getClass.getName + " by setting the environment variable SCALACL_TEST_PERF=1")
    !test
  } 
  val stream = new ScalaCLPlugin.PluginOptions(null).stream
  val deprecated = new ScalaCLPlugin.PluginOptions(null).deprecated
}
class MatrixPerformanceTest extends TestUtils {
  import PerformanceTest.{ skip, stream }
  
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

trait ChainedPerformanceTest {
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
class ListChainedPerformanceTest extends ListPerformanceTest with ChainedPerformanceTest {
  override def col = chain(super.col)
}
class ArrayPerformanceTest extends CollectionPerformanceTests {
  override def col = ("val col = Array.tabulate(n)(i => i)", "col")
  @Test def simpleArrayTabulate =  if (!skip) ensureFasterCodeWithSameResult(null, "Array.tabulate(n)(i => i).toSeq")
}
class ArrayChainedPerformanceTest extends ArrayPerformanceTest with ChainedPerformanceTest {
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
class RangeChainedPerformanceTest extends CollectionPerformanceTests with ChainedPerformanceTest with NoRightTests with NoScalarReductionTests {
  override def col = chain((null, "(0 until n)"))
}



trait CollectionPerformanceTests extends PerformanceTests {
  import PerformanceTest.{ deprecated, stream }
  val skip = PerformanceTest.skip
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

/*
class PerformanceTest extends PerformanceTests {
  import PerformanceTest.{ deprecated, stream }
  val skip = PerformanceTest.skip
  
  def arr: (String, String) = ("val col = Array.tabulate(n)(i => i)", "col") 
  def lis: (String, String) = ("val col = (0 to n).toList", "col.filter(v => (v % 2) == 0).map(_ * 2)")
  def rng: (String, String) = (null, "(0 until n)")

  import options.{ experimental } // SCALACL_EXPERIMENTAL
  
  val testLists = {
    deprecated
  }
  
  @Test def simpleRangeToArray = if (experimental) testToArray(rng)              
  //@Test def simpleRangeToList = testToList(rng) 
  @Test def simpleListToArray = if (testLists) testToArray(lis)              
  @Test def simpleArrayToList = if (experimental) testToList(arr) 
  
  
  @Test def simpleListFilter = if (testLists) testFilter(lis)              
  @Test def simpleListFilterNot = if (testLists) testFilterNot(lis) 
  // TODO: not working well, no speedup :
  @Test def simpleListCount = if (testLists) testCount(lis)                
  @Test def simpleListExists = if (testLists) testExists(lis)              
  @Test def simpleListForall = if (testLists) testForall(lis)              
  // TODO: Must be implemented differently : @Test def simpleListTakeWhile = testTakeWhile(lis)        
  // TODO: Must be implemented differently : @Test def simpleListDropWhile = testDropWhile(lis)        
  @Test def simpleListForeach = if (testLists) testForeach(lis)            
  @Test def simpleListMap = if (testLists) testMap(lis)                    
  @Test def simpleListSum = if (testLists) testSum(lis)                    
  @Test def simpleListMin = if (testLists) testMin(lis)                    
  @Test def simpleListMax = if (testLists) testMax(lis)                    
  @Test def simpleListScanLeft = if (testLists) testScanLeft(lis)          
  @Test def simpleListFoldLeft = if (testLists) testFoldLeft(lis)          
  @Test def simpleListReduceLeft = if (testLists) testReduceLeft(lis)      

  @Test def simpleRangeFilter = testFilter(rng)           
  @Test def simpleRangeFilterNot = testFilterNot(rng)     
  @Test def simpleRangeCount = testCount(rng)             
  @Test def simpleRangeExists = testExists(rng)           
  @Test def simpleRangeForall = testForall(rng)           
  // TODO: Must be implemented differently : @Test def simpleRangeTakeWhile = testTakeWhile(rng)     
  // TODO: Must be implemented differently : @Test def simpleRangeDropWhile = testDropWhile(rng)     
  @Test def simpleRangeForeach = testForeach(rng)         
  @Test def simpleRangeMap = testMap(rng)                 
  @Test def simpleRangeSum = testSum(rng)                 
  // TODO: Must be implemented differently : @Test def simpleRangeMin = testMin(rng)                 
  // TODO: Must be implemented differently : @Test def simpleRangeMax = testMax(rng)                 
  @Test def simpleRangeScanLeft = testScanLeft(rng)       
  @Test def simpleRangeFoldLeft = testFoldLeft(rng)       
  // TODO: Must be implemented differently : @Test def simpleRangeReduceLeft = testReduceLeft(rng)   
  
  @Test def simpleArrayTabulate =  if (!skip) ensureFasterCodeWithSameResult(null, "Array.tabulate(n)(i => i).toSeq")
  @Test def simpleArrayFilter = testFilter(arr)           
  @Test def simpleArrayFilterNot = testFilterNot(arr)     
  @Test def simpleArrayCount = testCount(arr)             
  @Test def simpleArrayExists = testExists(arr)           
  @Test def simpleArrayForall = testForall(arr)           
  @Test def simpleArrayTakeWhile = testTakeWhile(arr)     
  @Test def simpleArrayDropWhile = testDropWhile(arr)     
  @Test def simpleArrayForeach = testForeach(arr)         
  @Test def simpleArrayMap = testMap(arr)                 
  @Test def simpleArraySum = testSum(arr)                 
  @Test def simpleArrayMin = testMin(arr)                 
  @Test def simpleArrayMax = testMax(arr)                 
  @Test def simpleArrayScanLeft = testScanLeft(arr)       
  //@Test def simpleArrayScanRight = testScanRight(arr)     
  @Test def simpleArrayFoldLeft = testFoldLeft(arr)       
  @Test def simpleArrayFoldRight = testFoldRight(arr)     
  @Test def simpleArrayReduceLeft = testReduceLeft(arr)   
  @Test def simpleArrayReduceRight = testReduceRight(arr) 
  
}*/
trait PerformanceTests extends TestUtils {
  val skip: Boolean
  
  val oddPred = "x => (x % 2) != 0"
  val firstHalfPred = "x => x < n / 2"
  val midPred = "x => x == n / 2"
  
  def testToList(cc: (String, String)) = if (!skip)
    ensureFasterCodeWithSameResult(cc._1, cc._2 + ".toList")

  def testToArray(cc: (String, String)) = if (!skip)
    ensureFasterCodeWithSameResult(cc._1, cc._2 + ".toArray.toSeq")

  def testFilter(cc: (String, String)) = if (!skip)
    ensureFasterCodeWithSameResult(cc._1, cc._2 + ".filter(" + oddPred + ").toSeq")

  def testFilterNot(cc: (String, String)) = if (!skip) 
    ensureFasterCodeWithSameResult(cc._1, cc._2 + ".filterNot(" + oddPred + ").toSeq")

  def testCount(cc: (String, String)) = if (!skip)
    ensureFasterCodeWithSameResult(cc._1, cc._2 + ".count(" + oddPred + ")")

  def testForeach(cc: (String, String)) = if (!skip)
    ensureFasterCodeWithSameResult(cc._1, "var tot = 0L; for (v <- " + cc._2 + ") { tot += v }; tot")

  def testMap(cc: (String, String)) = if (!skip)
    ensureFasterCodeWithSameResult(cc._1, cc._2 + ".map(_ + 1).toSeq")

  def testTakeWhile(cc: (String, String)) = if (!skip) 
    ensureFasterCodeWithSameResult(cc._1, cc._2 + ".takeWhile(" + firstHalfPred + ").toSeq")

  def testDropWhile(cc: (String, String)) = if (!skip)
    ensureFasterCodeWithSameResult(cc._1, cc._2 + ".dropWhile(" + firstHalfPred + ").toSeq")

  def testExists(cc: (String, String)) = if (!skip)
    ensureFasterCodeWithSameResult(cc._1, cc._2 + ".exists(" + midPred + ")")

  def testForall(cc: (String, String)) = if (!skip)
    ensureFasterCodeWithSameResult(cc._1, cc._2 + ".forall(" + firstHalfPred + ")")

  def testSum(cc: (String, String)) = if (!skip)
    ensureFasterCodeWithSameResult(cc._1, cc._2 + ".sum")

  def testProduct(cc: (String, String)) = if (!skip)
    ensureFasterCodeWithSameResult(cc._1, cc._2 + ".product")

  def testMin(cc: (String, String)) = if (!skip)
    ensureFasterCodeWithSameResult(cc._1, cc._2 + ".min")

  def testMax(cc: (String, String)) = if (!skip)
    ensureFasterCodeWithSameResult(cc._1, cc._2 + ".max")

  def testReduceLeft(cc: (String, String)) = if (!skip)
    ensureFasterCodeWithSameResult(cc._1, cc._2 + ".reduceLeft(_ + _)")

  def testReduceRight(cc: (String, String)) = if (!skip)
    ensureFasterCodeWithSameResult(cc._1, cc._2 + ".reduceRight(_ + _)")

  def testFoldLeft(cc: (String, String)) = if (!skip)
    ensureFasterCodeWithSameResult(cc._1, cc._2 + ".foldLeft(0)(_ + _)")

  def testFoldRight(cc: (String, String)) = if (!skip)
    ensureFasterCodeWithSameResult(cc._1, cc._2 + ".foldRight(0)(_ + _)")

  def testScanLeft(cc: (String, String)) = if (!skip)
    ensureFasterCodeWithSameResult(cc._1, cc._2 + ".scanLeft(0)(_ + _).toSeq")

  def testScanRight(cc: (String, String)) = if (!skip)
    ensureFasterCodeWithSameResult(cc._1, cc._2 + ".scanRight(0)(_ + _).toSeq")

}
