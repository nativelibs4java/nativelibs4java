/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package scalacl

import org.junit._
import Assert._

class PerformanceTest extends TestUtils {

  val arr = ("Array", "Array.tabulate(n)(i => i)")
  val lis = ("List", "(0 to n).toList")
  val rng = ("Range", "(0 to n)")

  @Test def simpleListFilter = testFilter(lis)              ()
  @Test def simpleListFilterNot = testFilterNot(lis)        ()
  @Test def simpleListCount = testCount(lis)                ()
  @Test def simpleListExists = testExists(lis)              ()
  @Test def simpleListForall = testForall(lis)              ()
  @Test def simpleListTakeWhile = testTakeWhile(lis)        ()
  @Test def simpleListDropWhile = testDropWhile(lis)        ()
  @Test def simpleListForeach = testForeach(lis)            ()
  @Test def simpleListMap = testMap(lis)                    ()
  @Test def simpleListSum = testSum(lis)                    ()
  @Test def simpleListMin = testMin(lis)                    ()
  @Test def simpleListMax = testMax(lis)                    ()
  @Test def simpleListScanLeft = testScanLeft(lis)          ()
  @Test def simpleListFoldLeft = testFoldLeft(lis)          ()
  @Test def simpleListReduceLeft = testReduceLeft(lis)      ()

  @Test def simpleRangeFilter = testFilter(rng)             (3)
  @Test def simpleRangeFilterNot = testFilterNot(rng)       (3)
  @Test def simpleRangeCount = testCount(rng)               (3)
  @Test def simpleRangeExists = testExists(rng)             (3)
  @Test def simpleRangeForall = testForall(rng)             (3)
  @Test def simpleRangeTakeWhile = testTakeWhile(rng)       (3)
  @Test def simpleRangeDropWhile = testDropWhile(rng)       (3)
  @Test def simpleRangeForeach = testForeach(rng)           (5)
  @Test def simpleRangeMap = testMap(rng)                   (1.5f)
  //@Test def simpleRangeSum = testSum(rng)                 ()
  //@Test def simpleRangeMin = testMin(rng)                 ()
  //@Test def simpleRangeMax = testMax(rng)                 ()
  @Test def simpleRangeScanLeft = testScanLeft(rng)         (1.5f)
  @Test def simpleRangeFoldLeft = testFoldLeft(rng)         (15)
  //@Test def simpleRangeReduceLeft = testReduceLeft(rng)   ()
  
  @Test def simpleArrayFilter = testFilter(arr)           (3)
  @Test def simpleArrayFilterNot = testFilterNot(arr)     (3)
  @Test def simpleArrayCount = testCount(arr)             (3)
  @Test def simpleArrayExists = testExists(arr)           (3)
  @Test def simpleArrayForall = testForall(arr)           (3)
  @Test def simpleArrayTakeWhile = testTakeWhile(arr)     (3)
  @Test def simpleArrayDropWhile = testDropWhile(arr)     (3)
  @Test def simpleArrayForeach = testForeach(arr)         (10)
  @Test def simpleArrayMap = testMap(arr)                 (10)
  @Test def simpleArraySum = testSum(arr)                 (10)
  @Test def simpleArrayMin = testMin(arr)                 (10)
  @Test def simpleArrayMax = testMax(arr)                 (10)
  @Test def simpleArrayScanLeft = testScanLeft(arr)       (10)
  @Test def simpleArrayScanRight = testScanRight(arr)     (10)
  @Test def simpleArrayFoldLeft = testFoldLeft(arr)       (10)
  @Test def simpleArrayFoldRight = testFoldRight(arr)     (10)
  @Test def simpleArrayReduceLeft = testReduceLeft(arr)   (10)
  @Test def simpleArrayReduceRight = testReduceRight(arr) (8)

  @Test def simpleMatrixTest = ensureFasterCodeWithSameResult("""
    val a = Array.tabulate[Double](n, n)(_ + _)
    val b = Array.tabulate[Double](n, n)(_ + _)
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
  """, 2f, Seq(100))

  val oddPred = "x => (x % 2) != 0"
  val firstHalfPred = "x => x < n / 2"
  val midPred = "x => x == n / 2"
  
  def testFilter(colNameAndExpr: (String, String))(f: Float = 1.1f) =
    ensureFasterCodeWithSameResult(colNameAndExpr._2 + ".filter(" + oddPred + ").size", f)

  def testFilterNot(colNameAndExpr: (String, String))(f: Float = 1.1f) =
    ensureFasterCodeWithSameResult(colNameAndExpr._2 + ".filterNot(" + oddPred + ").size", f)

  def testCount(colNameAndExpr: (String, String))(f: Float = 1.1f) =
    ensureFasterCodeWithSameResult(colNameAndExpr._2 + ".count(" + oddPred + ")", f)

  def testForeach(colNameAndExpr: (String, String))(f: Float = 1.1f) =
    ensureFasterCodeWithSameResult("""var tot = 0L; for (v <- """ + colNameAndExpr._2 + ") { tot += v }; tot", f)

  def testMap(colNameAndExpr: (String, String))(f: Float = 1.1f) =
    ensureFasterCodeWithSameResult(colNameAndExpr._2 + ".map(_ + 1).toSeq", f)

  def testTakeWhile(colNameAndExpr: (String, String))(f: Float = 1.1f) =
    ensureFasterCodeWithSameResult(colNameAndExpr._2 + ".takeWhile(" + firstHalfPred + ").toSeq", f)

  def testDropWhile(colNameAndExpr: (String, String))(f: Float = 1.1f) =
    ensureFasterCodeWithSameResult(colNameAndExpr._2 + ".dropWhile(" + firstHalfPred + ").toSeq", f)

  def testExists(colNameAndExpr: (String, String))(f: Float = 1.1f) =
    ensureFasterCodeWithSameResult(colNameAndExpr._2 + ".exists(" + midPred + ")", f)

  def testForall(colNameAndExpr: (String, String))(f: Float = 1.1f) =
    ensureFasterCodeWithSameResult(colNameAndExpr._2 + ".forall(" + firstHalfPred + ")", f)

  def testSum(colNameAndExpr: (String, String))(f: Float = 1.1f) =
    ensureFasterCodeWithSameResult(colNameAndExpr._2 + ".sum", f)

  def testMin(colNameAndExpr: (String, String))(f: Float = 1.1f) =
    ensureFasterCodeWithSameResult(colNameAndExpr._2 + ".min", f)

  def testMax(colNameAndExpr: (String, String))(f: Float = 1.1f) =
    ensureFasterCodeWithSameResult(colNameAndExpr._2 + ".max", f)

  def testReduceLeft(colNameAndExpr: (String, String))(f: Float = 1.1f) =
    ensureFasterCodeWithSameResult(colNameAndExpr._2 + ".reduceLeft(_ + _)", f)

  def testReduceRight(colNameAndExpr: (String, String))(f: Float = 1.1f) =
    ensureFasterCodeWithSameResult(colNameAndExpr._2 + ".reduceRight(_ + _)", f)

  def testFoldLeft(colNameAndExpr: (String, String))(f: Float = 1.1f) =
    ensureFasterCodeWithSameResult(colNameAndExpr._2 + ".foldLeft(0)(_ + _)", f)

  def testFoldRight(colNameAndExpr: (String, String))(f: Float = 1.1f) =
    ensureFasterCodeWithSameResult(colNameAndExpr._2 + ".foldRight(0)(_ + _)", f)

  def testScanLeft(colNameAndExpr: (String, String))(f: Float = 1.1f) =
    ensureFasterCodeWithSameResult(colNameAndExpr._2 + ".scanLeft(0)(_ + _).toSeq", f)

  def testScanRight(colNameAndExpr: (String, String))(f: Float = 1.1f) =
    ensureFasterCodeWithSameResult(colNameAndExpr._2 + ".scanRight(0)(_ + _).toSeq", f)

}
