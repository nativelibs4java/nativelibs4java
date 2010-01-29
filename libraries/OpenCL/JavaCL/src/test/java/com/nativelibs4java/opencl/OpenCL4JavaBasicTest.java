
package com.nativelibs4java.opencl;

import static com.nativelibs4java.opencl.JavaCL.createBestContext;
import static com.nativelibs4java.util.NIOUtils.directFloats;
import static org.junit.Assert.assertEquals;

import java.nio.FloatBuffer;

import org.junit.BeforeClass;
import org.junit.Test;

import com.nativelibs4java.test.MiscTestUtils;

public class OpenCL4JavaBasicTest {

    public static final double ABSOLUTE_FLOAT_ERROR_TOLERANCE = 1e-4;
    public static final double RELATIVE_FLOAT_ERROR_TOLERANCE = 1e-8;

    @BeforeClass
    public static void setup() {
        MiscTestUtils.protectJNI();
    }

    @Test
    public void simpleTest() {
        try {
			CLContext context = createBestContext();
			
            int dataSize = 10000;
			String src = "\n" +
                    "__kernel void aSinB(                                                   \n" +
                    "   __global const float* a,                                       \n" +
                    "   __global const float* b,                                       \n" +
                    "   __global float* output)                                        \n" +
                    "{                                                                 \n" +
                    "   int i = get_global_id(0);                                      \n" +
                    "   output[i] = a[i] * sin(b[i]) + 1;                              \n" +
                    "}                                                                 \n";

            CLProgram program = context.createProgram(src).build();
			CLKernel kernel = program.createKernel("aSinB");
            CLQueue queue = context.createDefaultQueue();

            // Allocate OpenCL-hosted memory for inputs and output
            CLFloatBuffer memIn1 = context.createFloatBuffer(CLMem.Usage.Input, dataSize);
            CLFloatBuffer memIn2 = context.createFloatBuffer(CLMem.Usage.Input, dataSize);
            CLFloatBuffer memOut = context.createFloatBuffer(CLMem.Usage.Output, dataSize);

            // Bind these memory objects to the arguments of the kernel
            kernel.setArgs(memIn1, memIn2, memOut);

            /// Map input buffers to populate them with some data
            FloatBuffer a = memIn1.map(queue, CLMem.MapFlags.Write);
            FloatBuffer b = memIn2.map(queue, CLMem.MapFlags.Write);

            // Fill the mapped input buffers with data
            for (int i = 0; i < dataSize; i++) {
                a.put(i, i);
                b.put(i, i);
            }

            /// Unmap input buffers
            memIn1.unmap(queue, a);
            memIn2.unmap(queue, b);

            // Ask for execution of the kernel with global size = dataSize
            //   and workgroup size = 1
            kernel.enqueueNDRange(queue, new int[]{dataSize}, new int[]{1});

            // Wait for all operations to be performed
            queue.finish();

            // Copy the OpenCL-hosted array back to RAM
            FloatBuffer output = directFloats(dataSize, context.getByteOrder());
            memOut.read(queue, output, true);

            // Compute absolute and relative average errors wrt Java implem
            double totalAbsoluteError = 0, totalRelativeError = 0;
            for (int i = 0; i < dataSize; i++) {
                float expected = i * (float) Math.sin(i) + 1;
                float result = output.get(i);

                double d = result - expected;
                if (expected != 0) {
                    totalRelativeError += d / expected;
                }

                totalAbsoluteError += d < 0 ? -d : d;
            }
            double avgAbsoluteError = totalAbsoluteError / dataSize;
            double avgRelativeError = totalRelativeError / dataSize;
            System.out.println("Average absolute error = " + avgAbsoluteError);
            System.out.println("Average relative error = " + avgRelativeError);

            assertEquals("Bad relative error", 0, avgRelativeError, RELATIVE_FLOAT_ERROR_TOLERANCE);
            assertEquals("Bad absolute error", 0, avgAbsoluteError, ABSOLUTE_FLOAT_ERROR_TOLERANCE);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
