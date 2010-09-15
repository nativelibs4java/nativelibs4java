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

    lazy val f = new CLFunction[Int, Int]("""
      __kernel void map(size_t size, __global const int* in, __global int* out) {
        size_t i = get_global_id(0);
        if (i >= size)
          return;
        int v = in[i] + i;
        out[i] = v * (v - 1);
      }
    """)
    val clMapped = mapped.map(f)

    println("original = " + cla.toArray.toSeq)
    println("mapped = " + mapped.toArray.toSeq)
    println("clMapped = " + clMapped.toArray.toSeq)

    lazy val filter = new CLFunction[Int, Boolean]("""
      __kernel void filter(size_t size, __global const int* in, __global char* out) {
        size_t i = get_global_id(0);
        if (i >= size)
          return;
        int v = in[i];
        out[i] = (v % 10) == 0;// | ((i % 1) << 2);
      }
    """)
    val filtered = clMapped.filter(filter)
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
