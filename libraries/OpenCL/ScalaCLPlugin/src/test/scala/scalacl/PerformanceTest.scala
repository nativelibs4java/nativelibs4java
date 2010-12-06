/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package scalacl

import org.junit._
import Assert._

object PerformanceTest {
  lazy val skip = {
    val test = "1" == System.getenv("SCALACL_TEST_PERF")
    if (!test)
      println("You can run " + getClass.getName + " by setting the environment variable SCALACL_TEST_PERF=1")
    !test
  } 
}
class PerformanceTest extends TestUtils {
  import PerformanceTest.skip
  
  val arr = ("val col = Array.tabulate(n)(i => i)", "col") 
  val lis = ("val col = (0 to n).toList", "col")
  val rng = (null, "(0 until n)")

  import ScalaCLPlugin.experimental // SCALACL_EXPERIMENTAL
  
  /********
   * List *
   ********/
  @Test def simpleListFilter = testFilter(lis)              
  @Test def simpleListFilterNot = testFilterNot(lis) 
  // TODO: not working well, no speedup :
  @Test def simpleListCount = testCount(lis)                
  @Test def simpleListExists = testExists(lis)              
  @Test def simpleListForall = testForall(lis)              
  // TODO: Must be implemented differently : @Test def simpleListTakeWhile = testTakeWhile(lis)        
  // TODO: Must be implemented differently : @Test def simpleListDropWhile = testDropWhile(lis)        
  @Test def simpleListForeach = testForeach(lis)            
  @Test def simpleListMap = testMap(lis)                    
  @Test def simpleListSum = testSum(lis)                    
  @Test def simpleListMin = testMin(lis)                    
  @Test def simpleListMax = testMax(lis)                    
  @Test def simpleListScanLeft = testScanLeft(lis)          
  @Test def simpleListFoldLeft = testFoldLeft(lis)          
  @Test def simpleListReduceLeft = testReduceLeft(lis)      

  /*********
   * Range *
   *********/
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
  
  /*********
   * Array *
   *********/
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
  @Test def simpleArrayScanRight = testScanRight(arr)     
  @Test def simpleArrayFoldLeft = testFoldLeft(arr)       
  @Test def simpleArrayFoldRight = testFoldRight(arr)     
  @Test def simpleArrayReduceLeft = testReduceLeft(arr)   
  @Test def simpleArrayReduceRight = testReduceRight(arr) 
  
  @Test def simpleMatrixTest = if (!skip) ensureFasterCodeWithSameResult(
  """
    val a = Array.tabulate[Double](n, n)(_ + _)
    val b = Array.tabulate[Double](n, n)(_ + _)
  """,
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
    //(0 until n).map(k => a(i)(k) * b(k)(j)).sum
  """, Seq(100))

  val oddPred = "x => (x % 2) != 0"
  val firstHalfPred = "x => x < n / 2"
  val midPred = "x => x == n / 2"
  
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
