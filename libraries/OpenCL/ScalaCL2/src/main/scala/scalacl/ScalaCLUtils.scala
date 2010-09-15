/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package scalacl

import org.bridj.SizeT

object ScalaCLUtils {

  lazy val prefixSumCode = new CLCode("""
    __kernel void prefSum(size_t size, __global const char* in, __global long* out) {
      size_t i = get_global_id(0);
      size_t j;
      if (i >= size)
        return;

      long total = 0;
      for (j = 0; j <= i; j++)
        total += in[j];

      out[i] = total;
    }
  """)
  def prefixSum(bitmap: CLGuardedBuffer[Boolean], output: CLGuardedBuffer[Long])(implicit context: ScalaCLContext) = {
    val kernel = prefixSumCode.getKernel(context, bitmap, output)
    val globalSizes = Array(bitmap.size.asInstanceOf[Int])
    kernel.synchronized {
      kernel.setArgs(new SizeT(bitmap.size), bitmap.buffer, output.buffer)
      bitmap.read(readEvts => {
          output.write(writeEvts => {
              kernel.enqueueNDRange(context.queue, globalSizes, localSizes, (readEvts ++ writeEvts):_*)
          })
      })
    }
  }
  val localSizes = Array(1)
}
