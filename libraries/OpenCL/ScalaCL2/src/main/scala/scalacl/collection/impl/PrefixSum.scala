package scalacl
package collection
package impl

import org.bridj.PointerIO
import org.bridj.SizeT

object PrefixSum {

  lazy val prefixSumCode = new CLSimpleCode("""
    __kernel void prefSum(int size, __global const char* in, __global int* out) {
      int i = get_global_id(0);
      int j;
      if (i >= size)
        return;

      int total = 0;
      for (j = 0; j <= i; j++)
        total += in[j];

      out[i] = total;
    }
  """)
  def prefixSum(bitmap: CLGuardedBuffer[Boolean], output: CLGuardedBuffer[Int])(implicit context: ScalaCLContext) = {
    val kernel = prefixSumCode.getKernel(context)
    kernel.synchronized {
      kernel.setArgs(bitmap.size.toInt.asInstanceOf[Object], bitmap.buffer, output.buffer)
      CLEventBound.syncBlock(Array(bitmap), Array(output), evts => {
        kernel.enqueueNDRange(context.queue, Array(bitmap.size.toInt), null, evts:_*)
      })
    }
  }
  lazy val copyPrefixedCode = new CLSimpleCode("""
    __kernel void copyPrefixed(
        int size,
        __global const int* presencePrefix,
        __global const char* in,
        int elementSize,
        __global char* out
    ) {
      int i = get_global_id(0);
      if (i >= size)
        return;

      int prefix = presencePrefix[i];
      if (!i && prefix > 0 || i && prefix > presencePrefix[i - 1]) {
        int j, inOffset = i * elementSize, outOffset = (prefix - 1) * elementSize;
        for (j = 0; j < elementSize; j++) {
          out[outOffset + j] = in[inOffset + j];
        }
      }
    }
  """)
  def copyPrefixed[T](size: Int, presencePrefix: CLGuardedBuffer[Int], in: CLGuardedBuffer[T], out: CLGuardedBuffer[T])(implicit t: ClassManifest[T], context: ScalaCLContext) = {
    val kernel = copyPrefixedCode.getKernel(context)
    val pio = PointerIO.getInstance(t.erasure)
    assert(pio != null)
    kernel.synchronized {
      kernel.setArgs(in.size.toInt.asInstanceOf[Object], presencePrefix.buffer, in.buffer, pio.getTargetSize.toInt.asInstanceOf[Object], out.buffer)
      CLEventBound.syncBlock(Array(in, presencePrefix), Array(out), evts => {
        kernel.enqueueNDRange(context.queue, Array(in.size.toInt), null, evts:_*)
      })
    }
  }
}
