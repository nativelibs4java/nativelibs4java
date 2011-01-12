package scalacl

import impl._

import org.junit._
import Assert._

import scala.math._
import com.nativelibs4java.opencl._

class FilterTest {
  
  @Test
  def testFilterPreferablyOnCPU = 
      testFilter(ScalaCLContext(CLPlatform.DeviceFeature.CPU))
  
  @Test
  def testFilterPreferablyOnGPU = 
      testFilter(ScalaCLContext(CLPlatform.DeviceFeature.GPU))
  
  def testFilter(implicit context: ScalaCLContext) = {
    val filt = (
      (x: Int) => (x % 2) == 0, 
      Seq("(_ % 2) == 0")
    ): CLFunction[Int, Boolean]
    
    for (dim <- 1 until 1000) {
      for (offset <- 0 to 1) {
        val opencl = (0 until dim).toCLArray.filter(filt).toArray.toSeq
        val scala = (0 until dim).toArray.filter(filt).toArray.toSeq
        if (opencl != scala) {
          //println("\tExpected : " + scala)
          //println("\t   Found : " + opencl)
          assertEquals("Different result for dim = " + dim + ", offset = " + offset + " !", scala, opencl)
        }
      }
    }
  }
}
