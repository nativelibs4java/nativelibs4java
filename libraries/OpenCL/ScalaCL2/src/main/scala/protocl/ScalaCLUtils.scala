/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package protocl

import org.bridj.PointerIO
import org.bridj.SizeT

object ScalaCLUtils {

  lazy val prefixSumCode = new CLSimpleCode("""
    __kernel void prefSum(size_t size, __global const char* in, __global int* out) {
      size_t i = get_global_id(0);
      size_t j;
      if (i >= size)
        return;

      int total = 0;
      for (j = 0; j <= i; j++)
        total += in[j];

      out[i] = total;
    }
  """)
  def prefixSum(bitmap: CLGuardedBuffer[Boolean], output: CLGuardedBuffer[Int])(implicit context: ScalaCLContext) = {
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
  lazy val copyPrefixedCode = new CLSimpleCode("""
    __kernel void copyPrefixed(
        size_t size,
        __global const int* presencePrefix,
        __global const char* in,
        size_t elementSize,
        __global char* out
    ) {
      size_t i = get_global_id(0);
      if (i >= size)
        return;

      int prefix = presencePrefix[i];
      if (!i && prefix > 0 || i && prefix > presencePrefix[i - 1]) {
        size_t j, inOffset = i * elementSize, outOffset = (prefix - 1) * elementSize;
        for (j = 0; j < elementSize; j++) {
          out[outOffset + j] = in[inOffset + j];
        }
      }
    }
  """)
  def copyPrefixed[T](size: Int, presencePrefix: CLGuardedBuffer[Int], in: CLGuardedBuffer[T], out: CLGuardedBuffer[T])(implicit t: ClassManifest[T], context: ScalaCLContext) = {
    val kernel = copyPrefixedCode.getKernel(context, in, out)
    val globalSizes = Array(in.size.asInstanceOf[Int])
    val pio = PointerIO.getInstance(t.erasure)
    assert(pio != null)
    kernel.synchronized {
      kernel.setArgs(new SizeT(in.size), presencePrefix.buffer, in.buffer, new SizeT(pio.getTargetSize), out.buffer)
      in.read(inEvts => presencePrefix.read(presEvts => {
          out.write(writeEvts => {
              kernel.enqueueNDRange(context.queue, globalSizes, localSizes, (inEvts ++ presEvts ++ writeEvts):_*)
          })
      }))
    }
  }
  val localSizes = Array(1)
}
