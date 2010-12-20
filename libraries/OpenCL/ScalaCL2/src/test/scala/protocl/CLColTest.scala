/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package protocl

import com.nativelibs4java.opencl._
import org.bridj.Pointer
import org.bridj.Pointer._

import org.junit._
import Assert._

object CLColTest {

  implicit var context: ScalaCLContext = _
  @BeforeClass
  def setUp: Unit = {
    context = new ScalaCLContext(JavaCL.createBestContext)
  }

  @AfterClass
  def tearDown: Unit = {
    context.context.release
  }
}

class CLColTest {

  import CLColTest._

  @Test
  def testTuples2 {

    val tup = new CLArray[(Float, Float)](3)
    var i = 0
    //val ff: ((Float, Float)) => (Float, Float) = { case (x, y) => i += 1; val f = i.toFloat; (f, f * f) }
    val mapTup = tup.map({ case (x, y) => i += 1; val f = i.toFloat; (f, f * f) })
    println("mapTup = " + mapTup.toSeq)
    assertEquals(Seq((1f,1f), (2f,4f), (3f,9f)), mapTup.toSeq)
    
    //val fl: ((Float, Float)) => Boolean = p => (p._1 % 2) == 1
    val filTup = mapTup.filter((x: (Float, Float)) => (x._1 % 2) == 1)
    println("filTup = " + filTup.toSeq)
    assertEquals(Seq((1f,1f), (3f,9f)), filTup.toSeq)

    val tup2Scal = mapTup.mapFun(CLFun[(Float, Float), Float](Seq("_._1 + _._2")))
    println("tup2Scal = " + tup2Scal.toSeq)
    equals(Seq(2f, 6f, 12f), tup2Scal)
  }
  @Test
  def testTuples3 {
    val tup = new CLArray[(Float, Float, Float)](3)
    var i = 0
    val ff: ((Float, Float, Float)) => (Float, Float, Float) = { case (x, y, z) => i += 1; val f = i.toFloat; (f, f * f, f * f * f) }
    val mapTup = tup.map(ff)
    //mapTup2.waitFor
    println("mapTup = " + mapTup.toSeq)
    assertEquals(Seq((1f,1f,1f), (2f,4f,8f), (3f,9f,27f)), mapTup.toSeq)
    
    val fl: ((Float, Float, Float)) => Boolean = p => (p._1 % 2) == 1
    val filTup = mapTup.filter(fl)
    println("filTup = " + filTup.toSeq)
    assertEquals(Seq((1f,1f,1f), (3f,9f,27f)), filTup.toSeq)

    val tup2Scal = mapTup.mapFun(CLFun[(Float, Float, Float), Float](Seq("_._1 + _._2 + _._3")))
    println("tup2Scal = " + tup2Scal.toSeq)
    equals(Seq(3f, 14f, 39f), tup2Scal)
  }
  @Test
  def testTuples12 {
    val tup = new CLArray[(Float, (Float, Float))](3)
    var i = 0
    val ff: ((Float, (Float, Float))) => (Float, (Float, Float)) = { case (x, (y, z)) => i += 1; val f = i.toFloat; (f, (f * f, f * f * f)) }
    val mapTup = tup.map(ff)
    //mapTup2.waitFor
    println("mapTup = " + mapTup.toSeq)
    assertEquals(Seq((1f,(1f,1f)), (2f,(4f,8f)), (3f,(9f,27f))), mapTup.toSeq)
    
    val fl: ((Float, (Float, Float))) => Boolean =  p => (p._1 % 2) == 1
    val filTup = mapTup.filter(fl)
    println("filTup = " + filTup.toSeq)
    assertEquals(Seq((1f,(1f,1f)), (3f,(9f,27f))), filTup.toSeq)


    val tup2Scal = mapTup.mapFun(CLFun[(Float, (Float, Float)), Float](Seq("_._1 + _._2._1 + _._2._2")))
    println("tup2Scal = " + tup2Scal.toSeq)
    equals(Seq(3f, 14f, 39f), tup2Scal)
  }
  @Test
  def filterMapAndPack {
    val a = Array(1, 2, 3)

    var input = new CLArray[Int](10).map((_: Int) + 10)

    val clMapped = input.mapFun(CLFunSeq[Int, Int](Seq("int v = _ + $i;"), Seq("v * (v - 1)")))

    //println("original = " + cla.toArray.toSeq)
    //println("mapped = " + mapped.toArray.toSeq)
    println("clMapped = " + clMapped.toSeq)
    val expectedMapped = Seq(90, 110, 132, 156, 182, 210, 240, 272, 306, 342)
    assertEquals(expectedMapped, clMapped.toSeq)
    
    val filtered = clMapped.filterFun(CLFun(Seq("(_ % 10) == 0")))
    filtered.waitFor
    println("filtered = " + filtered.buffers(0).toArray.toSeq)
    val arr: Array[Boolean] = filtered.presence.toArray.asInstanceOf[Array[Boolean]]
    val arrB: Array[Byte] = filtered.presence.buffer.as(classOf[Byte]).read(context.queue).getBytes(filtered.buffers(0).size.asInstanceOf[Int])
    val packed = filtered.toCLArray
    val pref = filtered.updatedPresencePrefixSum.toArray
    val filteredSize = filtered.size
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
