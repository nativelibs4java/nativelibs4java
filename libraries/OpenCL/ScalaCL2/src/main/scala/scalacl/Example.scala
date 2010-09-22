/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package scalacl

import com.nativelibs4java.opencl.JavaCL

import scalacl._

object Example {

  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {

    val a = Array(1, 2, 3)

    implicit val context = new ScalaCLContext(JavaCL.createBestContext)

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
    //mapTup2.waitFor
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
    println("filtered presence = " + arr.toSeq)
    println("filtered presence B = " + arrB.toSeq)

    println("Size of filtered = " + filteredSize)

    println("presence prefix sum = " + pref.toSeq)
    println("Packed = " + packed.toSeq)
    //cla = a.toCL

    val v = cla.size //(v is not an int, it's a CLFuture[Int]
    //val mapped = cla(x => x + 1)
    //-> f.map(currpackage.CLFunctions.funMap1)

  }
}
