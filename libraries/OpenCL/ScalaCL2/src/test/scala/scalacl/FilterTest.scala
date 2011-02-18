package scalacl

import impl._

import org.junit._
import Assert._

import scala.math._
import com.nativelibs4java.opencl._

class FilterTest {
  
  @Test
  def testFilterPreferablyOnCPU = 
      testFilter(Context.best(CPU))
  
  @Test
  def testFilterPreferablyOnGPU = 
      testFilter(Context.best(GPU))
  
  def testFilter(implicit context: Context) = {
    val filt = (
      (x: Int) => (x % 2) == 0, 
      Seq("(_ % 2) == 0")
    ): CLFunction[Int, Boolean]
    
    for (dim <- 1 until 1000) {
      for (offset <- 0 to 1) {
        val openclArray = (0 until dim).toCLArray.filter(filt).toArray.toSeq
        val openclRangePre: CLIndexedSeq[Int] = (0 until dim).cl.filter(filt)
        val openclRange = openclRangePre.toArray.toSeq
        val scala = (0 until dim).toArray.filter(filt).toArray.toSeq
        if (openclArray != scala) {
          //println("\tExpected : " + scala)
          //println("\t   Found : " + opencl)
          assertEquals("Different result for dim = " + dim + ", offset = " + offset + " !", scala, openclArray)
        }
        assertEquals("Different result between range and array filters for dim = " + dim + ", offset = " + offset + " !", openclArray, openclRange)
      }
    }
  }
}
