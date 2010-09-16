/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package scalacl

import com.nativelibs4java.opencl.JavaCL

object Example {

  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {

    val a = Array(1, 2, 3)

    implicit val context = new ScalaCLContext(JavaCL.createBestContext)
    var cla = new CLArray[Int](10)
    val mapped = cla.map(_ + 10)

    val clMapped = mapped.map[Int]((Seq("int v = _ + $i;"), "v * (v - 1)"))

    println("original = " + cla.toArray.toSeq)
    println("mapped = " + mapped.toArray.toSeq)
    println("clMapped = " + clMapped.toArray.toSeq)

    val filtered = clMapped.filter("(_ % 10) == 0")
    filtered.waitFor
    println("filtered = " + filtered.values.toArray.toSeq)
    val arr: Array[Boolean] = filtered.presence.toArray.asInstanceOf[Array[Boolean]]
    val arrB: Array[Byte] = filtered.presence.buffer.as(classOf[Byte]).read(context.queue).getBytes(filtered.values.size.asInstanceOf[Int])
    val packed = filtered.toCLArray
    val pref = filtered.updatedPresencePrefixSum.toArray
    val filteredSize = filtered.size.get
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
