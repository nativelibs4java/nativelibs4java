
package com.nativelibs4java.opencl;

import com.nativelibs4java.opencl.*;

import java.lang.reflect.*;
import java.util.EnumSet;
import java.util.logging.*;
import org.junit.*;
import static org.junit.Assert.*;
import static com.nativelibs4java.test.MiscTestUtils.*;
import java.nio.*;
import java.util.*;
import static com.nativelibs4java.opencl.JavaCL.*;
import static com.nativelibs4java.util.NIOUtils.*;

public class OpenCL4JavaBasicTest {

    public static final double ABSOLUTE_FLOAT_ERROR_TOLERANCE = 1e-4;
    public static final double RELATIVE_FLOAT_ERROR_TOLERANCE = 1e-8;

    @Test
    public void simpleTest() {
        try {
			CLContext context = createBestContext();
			CLPlatform platform = context.getPlatform();
            CLDevice[] devices = platform.listAllDevices(true);
            for (CLDevice device : context.getDevices()) {
				System.out.println("Using device " + device + "\n\tmaxWorkItemSizes = " + Arrays.toString(device.getMaxWorkItemSizes()));
			}

			System.out.println("Supported images 2d: " + Arrays.asList(context.getSupportedImageFormats(CLMem.Flags.ReadOnly, CLMem.ObjectType.Image2D)));
            System.out.println("Supported images 3d: " + Arrays.asList(context.getSupportedImageFormats(CLMem.Flags.ReadOnly, CLMem.ObjectType.Image3D)));

            int dataSize = 10000;
//			String src = //"#include <math.h>\n" + 
//					"__kernel void function2(__global const double* in1, __global double* out2, __global const double* in3) {\n" +
//					"	int dim1 = get_global_id(0);\n" + 
//					"	out2[dim1] = in1[dim1] * sin(in3[dim1]) + 1;\n" + 
//					"}\n" + 
//					"";
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
            FloatBuffer output = directFloats(dataSize);
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
