/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package scalacl ; package test
import plugin._

import org.junit._
import Assert._

/*
class PerformanceTest extends PerformanceTests {
  import PerformanceTests.{ deprecated, stream }
  val skip = PerformanceTests.skip
  
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
