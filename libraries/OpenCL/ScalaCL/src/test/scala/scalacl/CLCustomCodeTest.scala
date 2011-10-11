package scalacl

import impl._

import com.nativelibs4java.opencl._
import org.bridj.Pointer
import org.bridj.Pointer._

import org.junit._
import Assert._

import scala.math._

class CLCustomCodeTest {
  
  @Test
  def testSinCos = {
    import scalacl._
    implicit val context = Context.best(CPU)
    val n = 100
    val f = 0.5f
    val sinCosOutputs: CLArray[Float] = new CLArray[Float](2 * n)
    val sinCosCode = customCode("""
      __kernel void sinCos(__global float2* outputs, float f) {
       int i = get_global_id(0);
       float c, s = sincos(i * f, &c);
       outputs[i] = (float2)(s, c);
      }
    """)
    sinCosCode.execute(
      args = Array(sinCosOutputs, f),
      writes = Array(sinCosOutputs),
      globalSizes = Array(n)
    )
    val resCL = sinCosOutputs.toArray
    
    val resJava = (0 until n).flatMap(i => {
      val x = i * f
      Array(sin(x).toFloat, cos(x).toFloat)
    }).toArray
    
    assertArrayEquals(resJava, resCL, 0.00001f)
  }
}
