package com.nativelibs4java.opencl;

import static com.nativelibs4java.opencl.CLTestUtils.avgError;
import static com.nativelibs4java.opencl.CLTestUtils.fillBuffersWithSomeDatad;
import static com.nativelibs4java.opencl.CLTestUtils.fillBuffersWithSomeDataf;
import static com.nativelibs4java.test.BenchmarkUtils.gc;
import static com.nativelibs4java.util.NIOUtils.directBytes;

import org.bridj.Pointer;
import static org.bridj.Pointer.*;
import java.util.Arrays;

import java.nio.FloatBuffer;
import java.nio.DoubleBuffer;

import org.junit.BeforeClass;
import org.junit.Test;

import com.nativelibs4java.opencl.CLTestUtils.Action2;
import com.nativelibs4java.opencl.CLTestUtils.ExecResult;
import com.nativelibs4java.test.MiscTestUtils;

//import com.nativelibs4java.scalacl.*;
/// @see http://ati.amd.com/technology/streamcomputing/intro_opencl.html#simple
public class OpenCL4JavaBenchmarkTest {

    @BeforeClass
    public static void setup() {
        MiscTestUtils.protectJNI();
    }

    static final boolean warmup = true;

    static ExecResult<FloatBuffer> testJava_float_aSinB(int loops, int dataSize) throws CLBuildException {

        FloatBuffer aBuffer = FloatBuffer.allocate(dataSize);
        FloatBuffer bBuffer = FloatBuffer.allocate(dataSize);
        FloatBuffer outputBuffer = FloatBuffer.allocate(dataSize);
        float[] a = aBuffer.array(), b = bBuffer.array(), output = outputBuffer.array();

        if (warmup) {
            System.out.print("Warming up Java operations...");
            for (int i = 0; i < 3000; i++) {
                java_aSinB(a, b, output, 100);
            }
            System.out.println();
        }

        fillBuffersWithSomeDataf(aBuffer, bBuffer);
        gc();

        long start = System.nanoTime();
        for (int i = 0; i < loops; i++) {
            java_aSinB(a, b, output, dataSize);
        }
        long time = System.nanoTime() - start;
        System.out.println("Java operations : " + time + "ns");
        return new ExecResult<FloatBuffer>(outputBuffer, time / (loops * (double) dataSize));
    }

    static ExecResult<DoubleBuffer> testJava_double_aSinB(int loops, int dataSize) throws CLBuildException {

        DoubleBuffer aBuffer = DoubleBuffer.allocate(dataSize);
        DoubleBuffer bBuffer = DoubleBuffer.allocate(dataSize);
        DoubleBuffer outputBuffer = DoubleBuffer.allocate(dataSize);
        double[] a = aBuffer.array(), b = bBuffer.array(), output = outputBuffer.array();

        if (warmup) {
            System.out.print("Warming up Java operations...");
            for (int i = 0; i < 3000; i++) {
                java_aSinB(a, b, output, 100);
            }
            System.out.println();
        }

        fillBuffersWithSomeDatad(aBuffer, bBuffer);
        gc();

        long start = System.nanoTime();
        for (int i = 0; i < loops; i++) {
            java_aSinB(a, b, output, dataSize);
        }
        long time = System.nanoTime() - start;
        System.out.println("Java operations : " + time + "ns");
        return new ExecResult<DoubleBuffer>(outputBuffer, time / (loops * (double) dataSize));
    }

    static ExecResult<Pointer<Float>> testOpenCL_float_aSinB(CLContext context, int loops, int dataSize, boolean hostInOpenCL) throws CLBuildException {

        ExecResult<Pointer<Byte>> er = testOpenCL_aSinB(context, Prim.Float, loops, dataSize, hostInOpenCL, new Action2<Pointer<Byte>, Pointer<Byte>>() {

            public void call(Pointer<Byte> a, Pointer<Byte> b) {
                fillBuffersWithSomeDataf(a.as(Float.class), b.as(Float.class));
            }
        });
        return new ExecResult<Pointer<Float>>(er.buffer.as(Float.class), er.unitTimeNano);
    }

    static ExecResult<Pointer<Double>> testOpenCL_double_aSinB(CLContext context, int loops, int dataSize, boolean hostInOpenCL) throws CLBuildException {

        ExecResult<Pointer<Byte>> er = testOpenCL_aSinB(context, Prim.Double, loops, dataSize, hostInOpenCL, new Action2<Pointer<Byte>, Pointer<Byte>>() {

            public void call(Pointer<Byte> a, Pointer<Byte> b) {
                fillBuffersWithSomeDatad(a.as(Double.class), b.as(Double.class));
            }
        });
        return new ExecResult<Pointer<Double>>(er.buffer.as(Double.class), er.unitTimeNano);
    }

    static ExecResult<Pointer<Byte>> testOpenCL_aSinB(CLContext context, Prim nativePrim, int loops, int dataSize, boolean hostInOpenCL, Action2<Pointer<Byte>, Pointer<Byte>> fillBuffersWithSomeData) throws CLBuildException {

        CLKernel kernel = setupASinB(nativePrim, context);
        CLQueue queue = context.createDefaultQueue();

        Pointer<Byte> input1 = null, input2 = null, output = null;
        CLBuffer<Byte> memIn1, memIn2, memOut;
        if (hostInOpenCL) {
            memIn1 = kernel.program.context.createBuffer(CLMem.Usage.Input, Byte.class, dataSize * nativePrim.sizeof());
            memIn2 = kernel.program.context.createBuffer(CLMem.Usage.Input, Byte.class, dataSize * nativePrim.sizeof());
            memOut = kernel.program.context.createBuffer(CLMem.Usage.Output, Byte.class, dataSize * nativePrim.sizeof());
        } else {
            input1 = allocateBytes(dataSize * nativePrim.sizeof()).order(context.getByteOrder());
            input2 = allocateBytes(dataSize * nativePrim.sizeof()).order(context.getByteOrder());
            output = allocateBytes(dataSize * nativePrim.sizeof()).order(context.getByteOrder());

            memIn1 = kernel.program.context.createBuffer(CLMem.Usage.Input, input1, false);
            memIn2 = kernel.program.context.createBuffer(CLMem.Usage.Input, input2, false);
            memOut = kernel.program.context.createBuffer(CLMem.Usage.Output, output, false);
        }
        kernel.setArgs(memIn1, memIn2, memOut);

        long[] maxWorkItemSizes = queue.getDevice().getMaxWorkItemSizes();
        int workItemSize = (int) maxWorkItemSizes[0];
        if (workItemSize > 32) {
            workItemSize = 32;
        }

        if (warmup) {
            for (int i = 0; i < 3000; i++) {
                kernel.enqueueNDRange(queue, new int[]{workItemSize}, new int[]{workItemSize});
            }
            queue.finish();
        }

        if (hostInOpenCL) {
            input1 = memIn1.map(queue, CLMem.MapFlags.Write);
            input2 = memIn2.map(queue, CLMem.MapFlags.Write);
        }
        fillBuffersWithSomeData.call(input1, input2);
        if (hostInOpenCL) {
            memIn1.unmap(queue, input1);
            memIn2.unmap(queue, input2);
        }
        queue.finish();
        gc();

        if (dataSize < workItemSize) {
            System.err.println("dataSize = " + dataSize + " is lower than max workItemSize for first dim = " + workItemSize + " !!!");
            workItemSize = 1;
        }

        long start = System.nanoTime();
        for (int i = 0; i < loops; i++) {
            kernel.enqueueNDRange(queue, new int[]{dataSize}, new int[]{workItemSize});
        }
        queue.finish();
        long time = System.nanoTime() - start;

        //System.out.println("OpenCL operations(" + target + ") : " + time + "ns");
        if (hostInOpenCL) {
            // Copy the OpenCL-hosted array back to RAM
            output = memOut.map(queue, CLMem.MapFlags.Read);
            System.out.println("memOut.map = " + Long.toHexString(output.getPeer()));
            //queue.finish();
            Pointer<Byte> b = allocateBytes(dataSize * nativePrim.sizeof()).order(context.getByteOrder());
            output.copyTo(b, dataSize);
            memOut.unmap(queue, output);
            output = b;
        }
        return new ExecResult<Pointer<Byte>>(output, time / (loops * (double) dataSize));
    }

    static CLKernel setupASinB(Prim nativeType, CLContext context) throws CLBuildException {
        String src = "\n"
                + "#pragma OPENCL EXTENSION cl_khr_fp16 : enable\n"
                + "#pragma OPENCL EXTENSION cl_khr_byte_addressable_store : enable\n"
                + (nativeType == Prim.Double ? "#pragma OPENCL EXTENSION cl_khr_fp64 : enable\n" : "")
                + "__kernel void aSinB(                                                  \n"
                + "   __global const " + nativeType + "* a,                                       \n"
                + "   __global const " + nativeType + "* b,                                       \n"
                + "   __global " + nativeType + "* output)                                        \n"
                + "{                                                                 \n"
                + "   int i = get_global_id(0);                                      \n"
                + "   float ai = a[i], bi = b[i];                                    \n"
                + "   output[i] = ai * sin(bi);// + atan2(ai, bi);                     \n"
                + "}                                                                 \n";

        CLProgram program = context.createProgram(src).build();
        CLKernel kernel = program.createKernel("aSinB");

        return kernel;
    }

    public static void java_aSinB(float[] a, float[] b, float[] output, int dataSize) throws CLBuildException {
        for (int i = 0; i < dataSize; i++) {
            float ai = a[i], bi = b[i];
            output[i] = ai * (float) Math.sin(bi);// + (float)Math.atan2(ai, bi);
        }
    }

    public static void java_aSinB(double[] a, double[] b, double[] output, int dataSize) throws CLBuildException {
        for (int i = 0; i < dataSize; i++) {
            double ai = a[i], bi = b[i];
            output[i] = ai * Math.sin(bi);// + Math.atan2(ai, bi);
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

    @Test
    public void testBenchmark() {
        /*for (String s : new String[] {
        "C:\\Program Files (x86)\\ATI Stream\\bin\\x86_64\\OpenCL.dll",
        "C:\\Program Files (x86)\\ATI Stream\\bin\\x86\\OpenCL.dll",
        "C:\\Program Files\\ATI Stream\\bin\\x86\\OpenCL.dll"
        })
        if ((f = new File(s)).exists())
        break;

        if (f.exists())
        System.setProperty("OpenCL.library", f.toString());
        //sss */
        try {
            System.out.println("#\n# " + OpenCL4JavaBenchmarkTest.class.getName() + "\n#");
            System.out.println("java.vm.name = " + System.getProperty("java.vm.name"));
            System.out.println("java.vm.version = " + System.getProperty("java.runtime.version"));
            System.out.println("Found platforms : " + Arrays.asList(JavaCL.listPlatforms()));
            CLPlatform platform = JavaCL.listPlatforms()[0];
            String v = platform.getVendor();
	    System.out.println("Platform Vendor: " + v);
            boolean isAMD = v.equals("Advanced Micro Devices, Inc.");
            int loops = 10;
            int dataSize = isAMD ? 1024 : 1024 * 1024;
            
            CLContext context = JavaCL.createBestContext();
            boolean hasDoubleSupport = context.isDoubleSupported();

            if (!hasDoubleSupport) {
                System.out.println("OpenCL context does not support double precision computations : skipping second part of the	test");
            } else {
                System.out.println("#\n# [Double Operations]\n#");
                ExecResult<DoubleBuffer> nsByJavaOp = testJava_double_aSinB(loops, dataSize);
                ExecResult<Pointer<Double>> nsByCLHostedOp = testOpenCL_double_aSinB(context, loops, dataSize, true);
                ExecResult<Pointer<Double>> nsByNativeHostedCLOp = testOpenCL_double_aSinB(context, loops, dataSize, false);
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
                System.out.println("#\n# [Float Operations]\n#");
                ExecResult<FloatBuffer> nsByJavaOp = testJava_float_aSinB(loops, dataSize);
                ExecResult<Pointer<Float>> nsByCLHostedOp = testOpenCL_float_aSinB(context, loops, dataSize, true);
                ExecResult<Pointer<Float>> nsByNativeHostedCLOp = testOpenCL_float_aSinB(context, loops, dataSize, false);
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
