package com.nativelibs4java.opencl;

import java.nio.*;
import static com.nativelibs4java.opencl.OpenCL4Java.*;

//import com.nativelibs4java.scalacl.*;

/// @see http://ati.amd.com/technology/streamcomputing/intro_opencl.html#simple
public class OpenCL4JavaExample {

    public static void fillData(FloatBuffer a, FloatBuffer b) {
        int s = a.capacity();
        for (int i = 0; i < s; i++) {
            a.put(i, i);
            b.put(i, i);
        }
    }
    static final boolean warmup = true;

    private static double avgError(ExecResult<FloatBuffer> a, ExecResult<FloatBuffer> b, int dataSize) {
        double tot = 0;
        for (int i = 0; i < dataSize; i++) {
            float va = a.buffer.get(i), vb = b.buffer.get(i);
            float d = va - vb;
            if (d < 0)
                d = -d;
            float m = (va + vb) / 2;
            if (m == 0)
                continue;
            double r = d / (double)m;
            tot += r;
        }
        return tot / dataSize;
    }
    static class ExecResult<B extends Buffer> {
        public B buffer;
        public double unitTimeNano;
        public ExecResult(B buffer, double unitTimeNano) {
            this.buffer = buffer;
            this.unitTimeNano = unitTimeNano;
        }
    }
    static ExecResult<FloatBuffer> testBuffers(int loops, int dataSize) throws CLBuildException {

        FloatBuffer input1 = FloatBuffer.allocate(dataSize);
        FloatBuffer input2 = FloatBuffer.allocate(dataSize);
        FloatBuffer output = FloatBuffer.allocate(dataSize);

        if (warmup) {
            for (int i = 0; i < 3000; i++)
                mulBuffers(input1, input2, output, 10);
        }

        fillData(input1, input2);
        gc();
        
        long start = System.nanoTime();
        for (int i = 0; i < loops; i++) {
            mulBuffers(input1, input2, output, dataSize);
        }
        long time = System.nanoTime() - start;
        System.out.println("Java operations : " + time + "ns");
        return new ExecResult(output, time / (loops * (double)dataSize));
    }
    static ExecResult<FloatBuffer> testOpenCL(int loops, int dataSize, boolean hostInOpenCL) throws CLBuildException {

        CLKernel kernel = setupMulNative();
        CLQueue queue = kernel.program.context.createDefaultQueue();
        
        FloatBuffer input1 = null, input2 = null, output = null;
        CLMem memIn1, memIn2, memOut;
        if (!hostInOpenCL) {
            input1 = OpenCL4Java.directFloats(dataSize);
            input2 = OpenCL4Java.directFloats(dataSize);
            output = OpenCL4Java.directFloats(dataSize);

            memIn1 = kernel.program.context.createInput(input1, false);
            memIn2 = kernel.program.context.createInput(input2, false);
            memOut = kernel.program.context.createOutput(output);
            kernel.setArgs(memIn1, memIn2, memOut);
        } else {
            memIn1 = kernel.program.context.createInput(dataSize * 4);
            memIn2 = kernel.program.context.createInput(dataSize * 4);
            memOut = kernel.program.context.createOutput(dataSize * 4);
            kernel.setArgs(memIn1, memIn2, memOut);
        }

        if (warmup) {
            for (int i = 0; i < 3000; i++)
                mulNative(/*input1, input2, output, */10, kernel, queue);
            queue.finish();
        }


        if (hostInOpenCL) {
            input1 = memIn1.mapWrite(queue).asFloatBuffer();
            input2 = memIn2.mapWrite(queue).asFloatBuffer();
        }
        fillData(input1, input2);
        if (hostInOpenCL) {
            memIn1.unmap(queue, input1);
            memIn2.unmap(queue, input2);
            //memOut.unmap(queue, output);
        }
        queue.finish();
        gc();
        long start = System.nanoTime();
        for (int i = 0; i < loops; i++) {
            mulNative(/*input1, input2, output, */dataSize, kernel, queue);
        }
        queue.finish();

        long time = System.nanoTime() - start;
        System.out.println("OpenCL operations : " + time + "ns");
        if (hostInOpenCL) {
            output = memOut.mapRead(queue).asFloatBuffer();
            //queue.finish();
            FloatBuffer b = OpenCL4Java.directFloats(dataSize);
            b.put(output);
            output.rewind();
            b.rewind();
            memOut.unmap(queue, output);
            output = b;
        }
        return new ExecResult(output, time / (loops * (double)dataSize));
    }

    static void gc() {
        try {
            System.gc();
            Thread.sleep(200);
            System.gc();
            Thread.sleep(200);
        } catch (InterruptedException ex) {}
    }
    static CLKernel setupMulNative() throws CLBuildException {
        String src = "\n" +
                "__kernel square(                                                       \n" +
                "   __global const float* input1,                                       \n" +
                "   __global const float* input2,                                       \n" +
                "   __global float* output)                                             \n" +
                "{                                                                      \n" +
                "   int i = get_global_id(0);                                           \n" +
                "   float v = input1[i] * input2[i];                                    \n" +
                "   output[i] = v * sin(v);                                                  \n" +
                "}                                                                      \n";

        CLContext context = CLContext.createContext(CLDevice.listAllDevices());
        CLProgram program = context.createProgram(src).build();
        CLKernel kernel = program.createKernel("square");

        return kernel;
    }
    
    public static void mulNative(/*FloatBuffer input1, FloatBuffer input2, FloatBuffer output, */int dataSize, CLKernel kernel, CLQueue queue) throws CLBuildException {
        /*kernel.setArgs(
            context.createInput(input1, false),
            context.createInput(input2, false),
            context.createOutput(output)
        );
*/
        kernel.enqueueNDRange(queue, new int[]{dataSize}, new int[]{1});
    }
    
    public static void mulBuffers(FloatBuffer input1, FloatBuffer input2, FloatBuffer output, int dataSize) throws CLBuildException {
        float[] in1 = input1.array(), in2 = input2.array(), out = output.array();
        if (in1 != null && in2 != null && out != null) {
            for (int i = 0; i < dataSize; i++) {
                float v = in1[i] * in2[i];
                out[i] = v * (float)Math.sin(v);
            }
        } else {
            for (int i = 0; i < dataSize; i++) {
                float v = input1.get(i) * input2.get(i);
                output.put(i, v * (float)Math.sin(v));
            }
        }
    }
    
    public static void main(String[] args) {
        try {
            int loops = 10;
            int dataSize = 1000000;

            
            ExecResult<FloatBuffer> nsByJavaOp = testBuffers(loops, dataSize);
            ExecResult<FloatBuffer> nsByCLHostedOp = testOpenCL(loops, dataSize, true);
            ExecResult<FloatBuffer> nsByNativeHostedCLOp = testOpenCL(loops, dataSize, false);

            for (int i = 0; i < 10; i++) {
                System.out.print("i\t = " + i + ",\t");
                System.out.print("buf \t=" + nsByJavaOp.buffer.get(i) +",\t");
                System.out.print("nat \t=" + nsByNativeHostedCLOp.buffer.get(i) +",\t");
                System.out.print("ocl \t=" + nsByCLHostedOp.buffer.get(i) +",\t");
                System.out.println();
            }
            double errCLHosted = avgError(nsByJavaOp, nsByCLHostedOp, dataSize);
            double errNativeHosted = avgError(nsByJavaOp, nsByNativeHostedCLOp, dataSize);
            System.out.println("     errCLHosted = " + errCLHosted);
            System.out.println(" errNativeHosted = " + errNativeHosted);
            System.out.println();

            System.out.println("                  java op\t= " + nsByJavaOp.unitTimeNano);
            System.out.println();
            System.out.println(" opencl (hosted in CL) op\t= " + nsByCLHostedOp.unitTimeNano);
            System.out.println("    times slower than Java = " + (nsByCLHostedOp.unitTimeNano / nsByJavaOp.unitTimeNano));
            System.out.println("    times faster than Java = " + (nsByJavaOp.unitTimeNano / nsByCLHostedOp.unitTimeNano));
            System.out.println();
            System.out.println("opencl (hosted in RAM) op\t= " + nsByNativeHostedCLOp.unitTimeNano);
            System.out.println("    times slower than Java = " + (nsByNativeHostedCLOp.unitTimeNano / nsByJavaOp.unitTimeNano));
            System.out.println("    times faster than Java = " + (nsByJavaOp.unitTimeNano / nsByNativeHostedCLOp.unitTimeNano));

            
            if (false)
                example();
        } catch (CLBuildException e) {
            e.printStackTrace();
        }
    }
    static void example() throws CLBuildException {
        //ScalaCLTest.
        int dataSize = 100000;

        FloatBuffer data = directFloats(dataSize);
        FloatBuffer resultsf = directFloats(dataSize);
        IntBuffer resultsi = directInts(dataSize);

        for (int i = 0; i < dataSize; i++) {
            data.put(i, i);
        }

        CLDevice[] devices = CLDevice.listAllDevices();
        for (CLDevice device : devices) {
            System.out.println("[OpenCL] Found device \"" + device.getName() + "\"");
            System.out.println(device.getExecutionCapabilities());
        }
        CLContext context = CLContext.createContext(devices);

        String src = "\n" +
                "__kernel square(                                                       \n" +
                "   __global const float* input,                                              \n" +
                "   __global float* outputf,                                             \n" +
                "   __global int* outputi,                                             \n" +
                "   const unsigned int count)                                           \n" +
                "{                                                                      \n" +
                "   int i = get_global_id(0);                                           \n" +
                "   if(i < count)                                                       \n" +
                "       outputf[i] = input[i] * input[i];                                \n" +
                "       outputi[i] = i;// + input[i] * input[i];                                \n" +
                "}                                                                      \n" +
                "\n";

        CLProgram program = context.createProgram(src).build();
        CLKernel kernel = program.createKernel(
                "square",
                context.createInput(data, false),
                context.createOutput(resultsf),
                context.createOutput(resultsi),
                dataSize);

        CLQueue queue = context.createDefaultQueue();
        kernel.enqueueNDRange(queue, new int[]{dataSize}, new int[]{1});
        queue.finish();

        for (int iRes = 0; iRes < dataSize; iRes++) {
            float d = data.get(iRes), rf = resultsf.get(iRes);
            int ri = resultsi.get(iRes);
            System.out.println(d + "\t->\tfloat: " + rf + ", int: " + ri);
        }
    }
}
