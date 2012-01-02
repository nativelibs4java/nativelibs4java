/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package scalacl

import impl._

import com.nativelibs4java.opencl._
import org.bridj.Pointer
import org.bridj.Pointer._

import org.junit._
import Assert._

import scala.math._

object CLCollectionTest {

  implicit var context: Context = Context.best(DoubleSupport)
  
  def rng = (0 until n).cl
  def clrng = (0 until n).cl
  
  def cla = (0 until n).toCLArray
  def a = (0 until n).toArray
  
  val rand = new java.util.Random(System.nanoTime)
  def aRand = (0 until n).map(_ => rand.nextFloat).toArray
  def claRand = aRand.toCLArray
  
  val extVal = 10
  
  var f: Int => Boolean = _
  var fScalarCapture: Int => Int = _
  var fArrayCapture: Int => Int = _
  var m: Int => Int = _
  var m2: Int => (Int, Int) = _
  var m2join: ((Int, Int)) => Int = _
  
  val n = 10
  val samples = n

  def same[V](b: Traversable[V], a: CLIndexedSeq[V])(implicit v: ClassManifest[V]) {
    val aa = a.toArray.take(samples).toSeq
    val bb = b.toArray.take(samples).toSeq
    
    if (!(aa == bb)) {
      println("aa = " + aa)
      //println("a = " + a)
      a match { 
        case fa: CLFilteredArray[V] =>
          println("aa.presence = " + fa.presence.toArray.take(samples).toSeq)
          println("aa.updatedPresencePrefixSum = " + fa.updatedPresencePrefixSum.toArray.take(samples).toSeq)
        case _ =>
      }
      println("bb = " + bb)
      assert(false)
    }
    
    assertEquals(bb, aa)
  }
  
  @BeforeClass
  def setUp: Unit = {
    //context = new Context
    
    //cla = (0 until n).toCLArray
    //a = (0 until n).toArray
    
    f = (
      (x: Int) => (exp(x).toInt % 2) == 0, 
      Array("(((int)exp((float)_)) % 2) == 0")
    ): CLFunction[Int, Boolean]
    
    fScalarCapture = 
      (
        (
          (x: Int) => x * extVal - x - extVal, 
          Array("_ * _1 - _ - _1"),
          impl.CapturedIOs(
            Array(),
            Array(),
            Array(IntCLDataIO.asInstanceOf[CLDataIO[Any]])
          )
        ): CLFunction[Int, Int]
      )
    
    m = (
      (x: Int) => (x * 2 * exp(x)).toInt, 
      Array("(int)(_ * 2 * exp((float)_))")
    ): CLFunction[Int, Int]
    
    m2 = (
      (x: Int) => (x, x * 2), 
      Array("_", "_ * 2")
    ): CLFunction[Int, (Int, Int)]
 
    m2join = (
      (p: (Int, Int)) => p._1 + 2 * p._2, 
      Array("_._1 + 2 * _._2")
    ): CLFunction[(Int, Int), Int]
 
  }

  @AfterClass
  def tearDown: Unit = {
    //context.release
  }
}

class CLCollectionTest {

  import CLCollectionTest._
  
  @Test
  def testMapScalarCapture {
    val fCapt = fScalarCapture.withCapture(
      Array(),
      Array(),
      Array(extVal)
    )
    same(a.map(fCapt), cla.map(fCapt))
  }
  @Test
  def testMapArrayCapture {
    val rg = (0 until n).map(_ * 10).toCLArray
    val f: CLFunction[Int, Int] =
      (
        (x: Int) => rg(x) - x, 
        Array("_1[_] - _"),
        impl.CapturedIOs(
          Array(IntCLDataIO.asInstanceOf[CLDataIO[Any]]),
          Array(),
          Array()
        )
      )
      
    val fCapt = f.withCapture(
      Array(rg.asInstanceOf[CLArray[Any]]),
      Array(),
      Array()
    )
    same(a.map(fCapt), cla.map(fCapt))
  }
  
  
  @Test
  def testSimpleFilter {
    same(a.filter(f), cla.filter(f).toCLArray)
    //context.queue.finish
  }
  @Test
  def testSimpleMap {
    same(a.map(m), cla.map(m))
    //context.queue.finish
  }
  
  @Test
  def testTupleMap {
    same(a.map(m2), cla.map(m2))
    //context.queue.finish
  }
  
  @Test
  def testFilterMap {
    same(a.filter(f).map(m), cla.filter(f).map(m))
    //context.queue.finish
  }
  @Test
  def testFilterMapTuple2Result {
    same(a.filter(f).map(m2), cla.filter(f).map(m2))
    //context.queue.finish
  }
  
  @Test
  def testFilterMapTuple2Arg {
    same(a.filter(f).map(m2).map(m2join), cla.filter(f).map(m2).map(m2join))
    //context.queue.finish
  }
  
  @Test
  def testToString {
    assertEquals("CLArray()", CLArray[Int]().toString)
    assertEquals("CLArray(1, 2)", CLArray(1, 2).toString)
    assertEquals("CLFilteredArray()", CLArray(1, 2).filter(_ => false).toString)
    assertEquals("CLFilteredArray(1, 2)", CLArray(1, 2).filter(_ => true).toString)
  }
  
  @Test
  def testRangeMap {
    same(rng.map(m), clrng.map(m))
    //context.queue.finish
  }
  
  /*
  Code from old protocl.Example :
  
  
    val a = Array(1, 2, 3)

    implicit val context = new Context(JavaCL.createBestContext)

    val tup2 = new CLArray[(Float, Float)](3)
    var i = 0
    val ff2: ((Float, Float)) => (Float, Float) = { case (x, y) => i += 1; val f = i.toFloat; (f, f * f) }
    val mapTup2 = tup2.map(ff2)
    println("mapTup2 = " + mapTup2.toSeq)
    equals(Seq((1.0,1.0), (2.0,4.0), (3.0,9.0)), mapTup2.toSeq)
    val mapTup22 = mapTup2.mapFun(CLFun[(Float, Float), Float](Seq("_._1 + _._2")))
    println("mapTup22 = " + mapTup22.toSeq)
    equals(Seq(2f, 6f, 12f), mapTup22)


    val tup3 = new CLArray[(Float, Float, Float)](3)
    i = 0
    val ff3: ((Float, Float, Float)) => (Float, Float, Float) = { case (x, y, z) => i += 1; val f = i.toFloat; (f, f * f, f * f * f) }
    val mapTup3 = tup3.map(ff3)
    println("mapTup3 = " + mapTup3.toSeq)
    equals(Seq((1.0,1.0,1.0), (2.0,4.0,8.0), (3.0,9.0,27.0)), mapTup3.toSeq)


    var cla = new CLArray[Int](10)
    val mapped = cla.map((x: Int) => x + 10)

    val clMapped = mapped.mapFun(CLFunSeq[Int, Int](Seq("int v = _ + $i;"), Seq("v * (v - 1)")))

    println("original = " + cla.toArray.toSeq)
    println("mapped = " + mapped.toArray.toSeq)
    println("clMapped = " + clMapped.toArray.toSeq)

    val filtered = clMapped.filterFun(CLFun[Int, Boolean](Seq("(_ % 10) == 0")))
    filtered.waitFor
    println("filtered = " + filtered.buffers(0).toArray.toSeq)
    val arr: Array[Boolean] = filtered.presence.toArray.asInstanceOf[Array[Boolean]]
    val arrB: Array[Byte] = filtered.presence.buffer.as(classOf[Byte]).read(context.queue).getBytes(filtered.buffersSize.toInt)
    val packed = filtered.toCLArray
    val pref = filtered.updatedPresencePrefixSum.toArray
    val filteredSize = filtered.size
  */
}
