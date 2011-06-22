
package scalacl

import impl._

import com.nativelibs4java.opencl._
import org.bridj.Pointer
import org.bridj.Pointer._

import org.junit._
import Assert._

import scala.math._

class ReductionTest {

  import CLCollectionTest._
  
  @Test
  def testMinMaxSumProduct {
    val aR = aRand
    val claR = aR.cl
    //val claR = claRand
    
    //val aR = (0 to 5).map(i => pow(10, i).toFloat)
    //val claR = aR.cl
    
    assertEquals("sum", aR.sum, claR.sum, 0)
    assertEquals("product", aR.product, claR.product, 0)
    
    assertEquals("min", aR.min, claR.min, 0)
    assertEquals("max", aR.max, claR.max, 0)
    
  }
}
