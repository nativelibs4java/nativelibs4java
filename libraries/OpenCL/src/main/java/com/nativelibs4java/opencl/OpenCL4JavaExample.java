package com.nativelibs4java.opencl;

import java.nio.*;
import java.util.*;
import static com.nativelibs4java.opencl.OpenCL4Java.*;

public class OpenCL4JavaExample {

    public static void main(String[] args) {
        try {
            CLPlatform[] platforms = CLPlatform.listPlatforms();
            CLPlatform platform = platforms[0];
            System.out.println("Using platform " + platform);
            CLDevice[] devices = platform.listAllDevices();
            System.out.println("Using devices " + Arrays.asList(devices));
            //CLDevice[] devices = CLDevice.listCPUDevices();
            //CLDevice[] devices = CLDevice.listGPUDevices();
            CLContext context = CLContext.createContext(devices);

            int dataSize = 10000;
//			String src = //"#include <math.h>\n" + 
//					"__kernel function2(__global const double* in1, __global double* out2, __global const double* in3) {\n" + 
//					"	int dim1 = get_global_id(0);\n" + 
//					"	out2[dim1] = in1[dim1] * sin(in3[dim1]) + 1;\n" + 
//					"}\n" + 
//					"";
            String src = "\n" +
                    "__kernel aSinB(                                                   \n" +
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
            CLMem memIn1 = context.createInput(dataSize * 4);
            CLMem memIn2 = context.createInput(dataSize * 4);
            CLMem memOut = context.createOutput(dataSize * 4);

            // Bind these memory objects to the arguments of the kernel
            kernel.setArgs(memIn1, memIn2, memOut);

            /// Map input buffers to populate them with some data
            FloatBuffer a = memIn1.mapWrite(queue).asFloatBuffer();
            FloatBuffer b = memIn2.mapWrite(queue).asFloatBuffer();

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
            FloatBuffer output = OpenCL4Java.directFloats(dataSize);
            memOut.read(output, queue, true);

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

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
