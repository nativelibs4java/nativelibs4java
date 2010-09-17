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
  def testTuples2 {

    val tup = clArray[(Float, Float)](3)
    var i = 0
    val ff: ((Float, Float)) => (Float, Float) = { case (x, y) => i += 1; val f = i.toFloat; (f, f * f) }
    val mapTup = tup.map(ff)
    println("mapTup = " + mapTup.toSeq)
    assertEquals(Seq((1.0,1.0), (2.0,4.0), (3.0,9.0)), mapTup.toSeq)
    
    val fl: ((Float, Float)) => Boolean = p => (p._1 % 2) == 1
    val filTup = mapTup.filter(fl)
    println("filTup = " + filTup.toSeq)
    assertEquals(Seq((1.0,1.0), (3.0,9.0)), filTup.toSeq)
  }
  @Test
  def testTuples3 {
    val tup = clArray[(Float, Float, Float)](3)
    var i = 0
    val ff: ((Float, Float, Float)) => (Float, Float, Float) = { case (x, y, z) => i += 1; val f = i.toFloat; (f, f * f, f * f * f) }
    val mapTup = tup.map(ff)
    //mapTup2.waitFor
    println("mapTup = " + mapTup.toSeq)
    assertEquals(Seq((1.0,1.0,1.0), (2.0,4.0,8.0), (3.0,9.0,27.0)), mapTup.toSeq)
    
    val fl: ((Float, Float, Float)) => Boolean = p => (p._1 % 2) == 1
    val filTup = mapTup.filter(fl)
    println("filTup = " + filTup.toSeq)
    assertEquals(Seq((1.0,1.0,1.0), (3.0,9.0,27.0)), filTup.toSeq)
  }
  @Test
  def testTuples12 {
    val tup = clArray[(Float, (Float, Float))](3)
    var i = 0
    val ff: ((Float, (Float, Float))) => (Float, (Float, Float)) = { case (x, (y, z)) => i += 1; val f = i.toFloat; (f, (f * f, f * f * f)) }
    val mapTup = tup.map(ff)
    //mapTup2.waitFor
    println("mapTup = " + mapTup.toSeq)
    assertEquals(Seq((1.0,(1.0,1.0)), (2.0,(4.0,8.0)), (3.0,(9.0,27.0))), mapTup.toSeq)
    
    val fl: ((Float, (Float, Float))) => Boolean =  p => (p._1 % 2) == 1
    val filTup = mapTup.filter(fl)
    println("filTup = " + filTup.toSeq)
    assertEquals(Seq((1.0,(1.0,1.0)), (3.0,(9.0,27.0))), filTup.toSeq)

  }
  @Test
  def filterMapAndPack {
    val a = Array(1, 2, 3)

    var input = clArray[Int](10).map(_ + 10)

    val clMapped = input.map[Int]((Seq("int v = _ + $i;"), "v * (v - 1)"))

    //println("original = " + cla.toArray.toSeq)
    //println("mapped = " + mapped.toArray.toSeq)
    println("clMapped = " + clMapped.toSeq)
    val expectedMapped = Seq(90, 110, 132, 156, 182, 210, 240, 272, 306, 342)
    assertEquals(expectedMapped, clMapped.toSeq)
    
    val filtered = clMapped.filter("(_ % 10) == 0")
    filtered.waitFor
    println("filtered = " + filtered.buffers(0).toArray.toSeq)
    val arr: Array[Boolean] = filtered.presence.toArray.asInstanceOf[Array[Boolean]]
    val arrB: Array[Byte] = filtered.presence.buffer.as(classOf[Byte]).read(context.queue).getBytes(filtered.buffers(0).size.asInstanceOf[Int])
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
