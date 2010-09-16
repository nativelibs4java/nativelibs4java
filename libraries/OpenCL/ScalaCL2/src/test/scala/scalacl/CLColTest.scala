/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package scalacl

import com.nativelibs4java.opencl._
import org.bridj.Pointer
import org.bridj.Pointer._

import org.junit._
import Assert._

class CLColTest {

  implicit var context: ScalaCLContext = _
  @Before
  def setUp: Unit = {
    context = new ScalaCLContext(JavaCL.createBestContext)
  }

  @After
  def tearDown: Unit = {
    context.context.release
  }

  @Test
  def filterMapAndPack = {
    val a = Array(1, 2, 3)

    var input = new CLArray[Int](10).map(_ + 10)

    val clMapped = input.map[Int]((Seq("int v = _ + $i;"), "v * (v - 1)"))

    //println("original = " + cla.toArray.toSeq)
    //println("mapped = " + mapped.toArray.toSeq)
    println("clMapped = " + clMapped.toSeq)
    val expectedMapped = Seq(90, 110, 132, 156, 182, 210, 240, 272, 306, 342)
    assertEquals(expectedMapped, clMapped.toSeq)
    
    val filtered = clMapped.filter("(_ % 10) == 0")
    filtered.waitFor
    println("filtered = " + filtered.values.toArray.toSeq)
    val arr: Array[Boolean] = filtered.presence.toArray.asInstanceOf[Array[Boolean]]
    val arrB: Array[Byte] = filtered.presence.buffer.as(classOf[Byte]).read(context.queue).getBytes(filtered.values.size.asInstanceOf[Int])
    val packed = filtered.toCLArray
    val pref = filtered.updatedPresencePrefixSum.toArray
    val filteredSize = filtered.size.get
    //println("filtered presence = " + arr.toSeq)
    //println("filtered presence B = " + arrB.toSeq)
    //println("Size of filtered = " + filteredSize)
    //println("presence prefix sum = " + pref.toSeq)
    println("Packed = " + packed.toSeq)

    assertEquals(Seq(90, 110, 210, 240), packed.toSeq)
    assertEquals(4, filteredSize)
    //cla = a.toCL
  }

}
