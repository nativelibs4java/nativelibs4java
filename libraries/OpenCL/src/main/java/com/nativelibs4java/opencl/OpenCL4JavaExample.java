package com.nativelibs4java.opencl;

import java.nio.*;
import static com.nativelibs4java.opencl.OpenCL4Java.*;

public class OpenCL4JavaExample {

    public static void main(String[] args) {
        try {
            int dataSize = 10000;
            String src = "\n" +
                    "__kernel aSinB(                                                   \n" +
                    "   __global const float* a,                                       \n" +
                    "   __global const float* b,                                       \n" +
                    "   __global float* output)                                        \n" +
                    "{                                                                 \n" +
                    "   int i = get_global_id(0);                                      \n" +
                    "   output[i] = a[i] * sin(b[i]);                                  \n" +
                    "}                                                                 \n";

            CLDevice[] devices = CLDevice.listAllDevices();
            //CLDevice[] devices = CLDevice.listCPUDevices();
            //CLDevice[] devices = CLDevice.listGPUDevices();
            CLContext context = CLContext.createContext(devices);
            CLProgram program = context.createProgram(src).build();
            CLKernel kernel = program.createKernel("aSinB");
            CLQueue queue = context.createDefaultQueue();

            CLMem memIn1 = kernel.program.context.createInput(dataSize * 4);
            CLMem memIn2 = kernel.program.context.createInput(dataSize * 4);
            CLMem memOut = kernel.program.context.createOutput(dataSize * 4);

            kernel.setArgs(memIn1, memIn2, memOut);

            /// Map input buffers to populate them with some data
            if (true)
            {
                FloatBuffer a = memIn1.mapWrite(queue).asFloatBuffer();
                FloatBuffer b = memIn2.mapWrite(queue).asFloatBuffer();

                for (int i = 0; i < dataSize; i++) {
                    a.put(i, i);
                    b.put(i, i);
                }

                /// Unmap input buffers
                memIn1.unmap(queue, a);
                memIn2.unmap(queue, b);
            }

            kernel.enqueueNDRange(queue, new int[]{dataSize}, new int[]{1});

            // Copy the OpenCL-hosted array back to RAM
            FloatBuffer output = OpenCL4Java.directFloats(dataSize);
            memOut.read(output, queue, true);

            // Compute absolute and relative average errors
            double totalAbsoluteError = 0, totalRelativeError = 0;
            for (int i = 0; i < dataSize; i++) {
                float expected = i * (float)Math.sin(i);
                float result = output.get(i);

                double d = result - expected;
                if (expected != 0)
                    totalRelativeError += d / expected;
                
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
