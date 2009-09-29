package com.nativelibs4java.opencl;

import com.nativelibs4java.opencl.*;
import java.io.File;
import java.util.*;
import java.nio.*;
import static com.nativelibs4java.opencl.OpenCL4Java.*;
import static com.nativelibs4java.opencl.CLTestUtils.*;
import static com.nativelibs4java.nio.NIOUtils.*;
import static com.nativelibs4java.test.BenchmarkUtils.*;

//import com.nativelibs4java.scalacl.*;

/// @see http://ati.amd.com/technology/streamcomputing/intro_opencl.html#simple
public class OpenCL4JavaBenchmarkTest {

    static final boolean warmup = true;

    
    static ExecResult<FloatBuffer> testJava_float_aSinB(int loops, int dataSize) throws CLBuildException {

        FloatBuffer aBuffer = FloatBuffer.allocate(dataSize);
        FloatBuffer bBuffer = FloatBuffer.allocate(dataSize);
        FloatBuffer outputBuffer = FloatBuffer.allocate(dataSize);
        float[] a = aBuffer.array(), b = bBuffer.array(), output = outputBuffer.array();

        if (warmup) {
            System.out.print("Warming up Java operations...");
            for (int i = 0; i < 3000; i++)
                java_aSinB(a, b, output, 100);
            System.out.println();
        }

        fillBuffersWithSomeData(aBuffer, bBuffer);
        gc();

        long start = System.nanoTime();
        for (int i = 0; i < loops; i++) {
            java_aSinB(a, b, output, dataSize);
        }
        long time = System.nanoTime() - start;
        System.out.println("Java operations : " + time + "ns");
        return new ExecResult(outputBuffer, time / (loops * (double)dataSize));
    }
    static ExecResult<DoubleBuffer> testJava_double_aSinB(int loops, int dataSize) throws CLBuildException {

        DoubleBuffer aBuffer = DoubleBuffer.allocate(dataSize);
        DoubleBuffer bBuffer = DoubleBuffer.allocate(dataSize);
        DoubleBuffer outputBuffer = DoubleBuffer.allocate(dataSize);
        double[] a = aBuffer.array(), b = bBuffer.array(), output = outputBuffer.array();

        if (warmup) {
            System.out.print("Warming up Java operations...");
            for (int i = 0; i < 3000; i++)
                java_aSinB(a, b, output, 100);
            System.out.println();
        }

        fillBuffersWithSomeData(aBuffer, bBuffer);
        gc();

        long start = System.nanoTime();
        for (int i = 0; i < loops; i++) {
            java_aSinB(a, b, output, dataSize);
        }
        long time = System.nanoTime() - start;
        System.out.println("Java operations : " + time + "ns");
        return new ExecResult(outputBuffer, time / (loops * (double)dataSize));
    }
    static ExecResult<FloatBuffer> testOpenCL_float_aSinB(Target target, int loops, int dataSize, boolean hostInOpenCL) throws CLBuildException {

        ExecResult<ByteBuffer> er = testOpenCL_aSinB(target, Prim.Float, loops, dataSize, hostInOpenCL, new Action2<ByteBuffer, ByteBuffer>() {
            public void call(ByteBuffer a, ByteBuffer b) {
                fillBuffersWithSomeData(a.asFloatBuffer(), b.asFloatBuffer());
            }
        });
        return new ExecResult(er.buffer.asFloatBuffer(), er.unitTimeNano);
    }
    static ExecResult<DoubleBuffer> testOpenCL_double_aSinB(Target target, int loops, int dataSize, boolean hostInOpenCL) throws CLBuildException {

        ExecResult<ByteBuffer> er = testOpenCL_aSinB(target, Prim.Double, loops, dataSize, hostInOpenCL, new Action2<ByteBuffer, ByteBuffer>() {
            public void call(ByteBuffer a, ByteBuffer b) {
                fillBuffersWithSomeData(a.asDoubleBuffer(), b.asDoubleBuffer());
            }
        });
        return new ExecResult(er.buffer.asDoubleBuffer(), er.unitTimeNano);
    }

    
    static ExecResult<ByteBuffer> testOpenCL_aSinB(Target target, Prim nativePrim, int loops, int dataSize, boolean hostInOpenCL, Action2<ByteBuffer, ByteBuffer> fillBuffersWithSomeData) throws CLBuildException {

        CLKernel kernel = setupASinB(nativePrim, target);
        CLQueue queue = kernel.program.context.createDefaultQueue();

        ByteBuffer input1 = null, input2 = null, output = null;
        CLMem memIn1, memIn2, memOut;
        if (hostInOpenCL) {
            memIn1 = kernel.program.context.createInput(dataSize * nativePrim.sizeof());
            memIn2 = kernel.program.context.createInput(dataSize * nativePrim.sizeof());
            memOut = kernel.program.context.createOutput(dataSize * nativePrim.sizeof());
        } else {
            input1 = directBytes(dataSize * nativePrim.sizeof());
            input2 = directBytes(dataSize * nativePrim.sizeof());
            output = directBytes(dataSize * nativePrim.sizeof());

            memIn1 = kernel.program.context.createInput(input1, false);
            memIn2 = kernel.program.context.createInput(input2, false);
            memOut = kernel.program.context.createOutput(output);
        }
        kernel.setArgs(memIn1, memIn2, memOut);

        if (warmup) {
            for (int i = 0; i < 3000; i++)
                kernel.enqueueNDRange(queue, new int[]{10}, new int[]{1});
            queue.finish();
        }

        if (hostInOpenCL) {
            input1 = memIn1.blockingMapWrite(queue);
            input2 = memIn2.blockingMapWrite(queue);
        }
        fillBuffersWithSomeData.call(input1, input2);
        if (hostInOpenCL) {
            memIn1.unmap(queue, input1);
            memIn2.unmap(queue, input2);
        }
        queue.finish();
        gc();

        long start = System.nanoTime();
        for (int i = 0; i < loops; i++) {
            kernel.enqueueNDRange(queue, new int[]{dataSize}, new int[]{1});
        }
        queue.finish();
        long time = System.nanoTime() - start;

        //System.out.println("OpenCL operations(" + target + ") : " + time + "ns");
        if (hostInOpenCL) {
            // Copy the OpenCL-hosted array back to RAM
            output = memOut.blockingMapRead(queue);
            //queue.finish();
            ByteBuffer b = directBytes(dataSize * nativePrim.sizeof());
            b.put(output);
            output.rewind();
            b.rewind();
            memOut.unmap(queue, output);
            output = b;
        }
        return new ExecResult(output, time / (loops * (double)dataSize));
    }

    static CLKernel setupASinB(Prim nativeType, Target target) throws CLBuildException {
        String src = "\n" +
                "__kernel aSinB(                                                  \n" +
                "   __global const " + nativeType + "* a,                                       \n" +
                "   __global const " + nativeType + "* b,                                       \n" +
                "   __global " + nativeType + "* output)                                        \n" +
                "{                                                                 \n" +
                "   int i = get_global_id(0);                                      \n" +
                "   output[i] = a[i] * sin(b[i]);                                       \n" +
                "}                                                                 \n";

        CLDevice[] devices = getDevices(target);
        for (CLDevice device : devices)
            System.out.println("OpenCL device: " + device);
        CLProgram program = CLContext.createContext(devices).createProgram(src).build();
        CLKernel kernel = program.createKernel("aSinB");

        return kernel;
    }
    
    private static void openCL_aSinB(/*FloatBuffer input1, FloatBuffer input2, FloatBuffer output, */int dataSize, CLKernel kernel, CLQueue queue) throws CLBuildException {
        /*kernel.setArgs(
            context.createInput(input1, false),
            context.createInput(input2, false),
            context.createOutput(output)
        );
*/
        kernel.enqueueNDRange(queue, new int[]{dataSize}, new int[]{1});
    }
    public static void java_aSinB(float[] a, float[] b, float[] output, int dataSize) throws CLBuildException {
        for (int i = 0; i < dataSize; i++) {
            output[i] = a[i] * (float)Math.sin(b[i]);
        }
    }
    public static void java_aSinB(double[] a, double[] b, double[] output, int dataSize) throws CLBuildException {
        for (int i = 0; i < dataSize; i++) {
            output[i] = a[i] * Math.sin(b[i]);
        }
    }

    enum Prim {
        Float(4), Double(8), Int(4), Short(2), Half(2);

        final int sizeof;
        Prim(int sizeof) {
            this.sizeof = sizeof;
        }
        public int sizeof() {
            return sizeof;
        }
        public String toString() {
            return name().toLowerCase();
        }
    }
    public static void main(String[] args) {
        File f = new File("C:\\Program Files\\ATI Stream\\bin\\x86\\OpenCL.dll");
        if (f.exists())
            System.setProperty("OpenCL.library", f.toString());
        try {
            System.out.println("Found platforms : " + Arrays.asList(OpenCL4Java.listPlatforms()));
            int loops = 10;
            int dataSize = 1000000;
            CLPlatform platform = OpenCL4Java.listPlatforms()[0];

            Target target = platform.listGPUDevices(false).length == 0 ? Target.CPU : Target.GPU;
            if (true) {
                System.out.println("[Double Operations]");
                ExecResult<DoubleBuffer> nsByJavaOp = testJava_double_aSinB(loops, dataSize);
                ExecResult<DoubleBuffer> nsByCLHostedOp = testOpenCL_double_aSinB(target, loops, dataSize, true);
                ExecResult<DoubleBuffer> nsByNativeHostedCLOp = testOpenCL_double_aSinB(target, loops, dataSize, false);
                double errCLHosted = avgError(nsByJavaOp.buffer, nsByCLHostedOp.buffer, dataSize);
                double errNativeHosted = avgError(nsByJavaOp.buffer, nsByNativeHostedCLOp.buffer, dataSize);

                System.out.println(" Avg relative error (hosted in CL) = " + errCLHosted);
                System.out.println("Avg relative error (hosted in RAM) = " + errNativeHosted);
                System.out.println();

                System.out.println("                  java op\t= " + nsByJavaOp.unitTimeNano + " ns");
                System.out.println();
                System.out.println(" opencl (hosted in CL) op\t= " + nsByCLHostedOp.unitTimeNano + " ns");
                System.out.println("    times slower than Java = " + (nsByCLHostedOp.unitTimeNano / nsByJavaOp.unitTimeNano));
                System.out.println("    times faster than Java = " + (nsByJavaOp.unitTimeNano / nsByCLHostedOp.unitTimeNano));
                System.out.println();
                System.out.println("opencl (hosted in RAM) op\t= " + nsByNativeHostedCLOp.unitTimeNano + " ns");
                System.out.println("    times slower than Java = " + (nsByNativeHostedCLOp.unitTimeNano / nsByJavaOp.unitTimeNano));
                System.out.println("    times faster than Java = " + (nsByJavaOp.unitTimeNano / nsByNativeHostedCLOp.unitTimeNano));
            }


            if (true) {
                System.out.println("[Float Operations]");
                ExecResult<FloatBuffer> nsByJavaOp = testJava_float_aSinB(loops, dataSize);
                ExecResult<FloatBuffer> nsByCLHostedOp = testOpenCL_float_aSinB(target, loops, dataSize, true);
                ExecResult<FloatBuffer> nsByNativeHostedCLOp = testOpenCL_float_aSinB(target, loops, dataSize, false);
                double errCLHosted = avgError(nsByJavaOp.buffer, nsByCLHostedOp.buffer, dataSize);
                double errNativeHosted = avgError(nsByJavaOp.buffer, nsByNativeHostedCLOp.buffer, dataSize);

                /*for (int i = 0; i < 10; i++) {
                    System.out.print("i\t = " + i + ",\t");
                    System.out.print("buf \t=" + nsByJavaOp.buffer.get(i) +",\t");
                    System.out.print("nat \t=" + nsByNativeHostedCLOp.buffer.get(i) +",\t");
                    System.out.print("ocl \t=" + nsByCLHostedOp.buffer.get(i) +",\t");
                    System.out.println();
                }*/
                System.out.println(" Avg relative error (hosted in CL) = " + errCLHosted);
                System.out.println("Avg relative error (hosted in RAM) = " + errNativeHosted);
                System.out.println();

                System.out.println("                  java op\t= " + nsByJavaOp.unitTimeNano + " ns");
                System.out.println();
                System.out.println(" opencl (hosted in CL) op\t= " + nsByCLHostedOp.unitTimeNano + " ns");
                System.out.println("    times slower than Java = " + (nsByCLHostedOp.unitTimeNano / nsByJavaOp.unitTimeNano));
                System.out.println("    times faster than Java = " + (nsByJavaOp.unitTimeNano / nsByCLHostedOp.unitTimeNano));
                System.out.println();
                System.out.println("opencl (hosted in RAM) op\t= " + nsByNativeHostedCLOp.unitTimeNano + " ns");
                System.out.println("    times slower than Java = " + (nsByNativeHostedCLOp.unitTimeNano / nsByJavaOp.unitTimeNano));
                System.out.println("    times faster than Java = " + (nsByJavaOp.unitTimeNano / nsByNativeHostedCLOp.unitTimeNano));
            }

            
        } catch (CLBuildException e) {
            e.printStackTrace();
        }
    }
}
