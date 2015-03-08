package com.nativelibs4java.opencl;

import com.nativelibs4java.opencl.CLMem.Usage;
import java.lang.reflect.Array;
import static org.junit.Assert.*;
import org.bridj.*;
import static org.bridj.Pointer.*;

import org.junit.*;

public class KernelTest {

    CLContext context;
    CLQueue queue;
    
    @Before
    public void setup() {
      context = JavaCL.createBestContext(CLPlatform.DeviceFeature.CPU);
      queue = context.createDefaultQueue();
    }
    
    public <T> Pointer<T> testArg(String type, Object value, Class<T> targetType) {
        long size = BridJ.sizeOf(targetType);
        CLBuffer<Byte> out = context.createByteBuffer(Usage.Output, size) ;
        CLKernel k = context.createProgram(
//            "#if __OPENCL_VERSION__ <= CL_VERSION_1_1\n" +
            "  #pragma OPENCL EXTENSION cl_khr_fp64 : enable\n" +
//            "#endif\n" +
            "kernel void f(" + type + " arg, global " + type + "* out, long size) {\n" +
                "char* in = (char*) &arg;\n" +
                "for (long i = 0; i < size; i++) {\n" +
                "out[i] = in[i];\n" +
                "}\n" +
            "}"
        ).createKernel("f", value, out, size);
        CLEvent e = k.enqueueTask(queue);
        return out.as(targetType).read(queue, e);
    }
    
    public <T> Object testArrayArg(String type, Object array, Class<T> targetType) {
        long size = BridJ.sizeOf(targetType);
        long length = Array.getLength(array);
        CLBuffer<Byte> out = context.createByteBuffer(Usage.Output, size * length);
        StringBuilder b = new StringBuilder(
            "#pragma OPENCL EXTENSION cl_khr_fp64 : enable\n" +
            "kernel void f(" + type + length + " arg, global " + type + "* out, long length) {\n");
        for (long i = 0; i < length; i++) {
            b.append("out[" + i + "] = arg.s" + (i < 10 ? i + "" : ((char)((int)'a' + (i - 10))) + "") + ";\n");
        }
        b.append("}\n");
        System.out.println(b);
        CLKernel k = context.createProgram(b.toString()).createKernel("f", array, out, length);
        CLEvent e = k.enqueueTask(queue);
        return out.as(targetType).read(queue, e).getArray();
    }

    @Test
    public void nullArg() {
        assertArrayEquals(new Pointer[] { null }, testArg("int", CLKernel.NULL_POINTER_KERNEL_ARGUMENT, Pointer.class).getPointers());
    }

    byte[] byteTup(int n) {
        byte[] a = new byte[n];
        for (int i = 0; i < n; i++) a[i] = (byte)(i + 1);
        return a;
    }
    @Test
    public void byteArg() {
        assertArrayEquals(new byte[] { 2 }, testArg("char", (byte) 2, byte.class).getBytes());
        for (byte[] tup : new byte[][] { byteTup(2), byteTup(3), byteTup(4), byteTup(8), byteTup(16) }) {
          assertArrayEquals(tup, (byte[]) testArrayArg("char", tup, byte.class));
        }
    }

    short[] shortTup(int n) {
        short[] a = new short[n];
        for (int i = 0; i < n; i++) a[i] = (short)(i + 1);
        return a;
    }
    @Test
    public void shortArg() {
        assertArrayEquals(new short[] { 2 }, testArg("short", (short) 2, short.class).getShorts());
        for (short[] tup : new short[][] { shortTup(2), shortTup(3), shortTup(4), shortTup(8), shortTup(16) }) {
          assertArrayEquals(tup, (short[]) testArrayArg("short", tup, short.class));
        }
    }

    int[] intTup(int n) {
        int[] a = new int[n];
        for (int i = 0; i < n; i++) a[i] = i + 1;
        return a;
    }
    @Test
    public void intArg() {
        assertArrayEquals(new int[] { 2 }, testArg("int", (int) 2, int.class).getInts());
        for (int[] tup : new int[][] { intTup(2), intTup(3), intTup(4), intTup(8), intTup(16) }) {
          assertArrayEquals(tup, (int[]) testArrayArg("int", tup, int.class));
        }
    }

    long[] longTup(int n) {
        long[] a = new long[n];
        for (int i = 0; i < n; i++) a[i] = i + 1;
        return a;
    }
    @Test
    public void longArg() {
        assertArrayEquals(new long[] { 2 }, testArg("long", (long) 2, long.class).getLongs());
        for (long[] tup : new long[][] { longTup(2), longTup(3), longTup(4), longTup(8), longTup(16) }) {
          assertArrayEquals(tup, (long[]) testArrayArg("long", tup, long.class));
        }
    }

    float[] floatTup(int n) {
        float[] a = new float[n];
        for (int i = 0; i < n; i++) a[i] = i + 1;
        return a;
    }
    @Ignore
    @Test
    public void floatArg() {
        assertArrayEquals(new float[] { 2f }, testArg("float", (float) 4, float.class).getFloats(), 0);
        for (float[] tup : new float[][] { floatTup(2), floatTup(3), floatTup(4), floatTup(8), floatTup(16) }) {
          assertArrayEquals(tup, (float[]) testArrayArg("float", tup, float.class), 0);
        }
    }

    double[] doubleTup(int n) {
        double[] a = new double[n];
        for (int i = 0; i < n; i++) a[i] = i + 1;
        return a;
    }
    @Ignore
    @Test
    public void doubleArg() {
        assertArrayEquals(new double[] { 2d }, testArg("double", (double) 8, double.class).getDoubles(), 0);
        for (double[] tup : new double[][] { doubleTup(2), doubleTup(3), doubleTup(4), doubleTup(8), doubleTup(16) }) {
          assertArrayEquals(tup, (double[]) testArrayArg("double", tup, double.class), 0);
        }
    }
//    void assertArrayEquals(Object exp, Object act) {
//        assertEquals(Arrays)
//    }
}
