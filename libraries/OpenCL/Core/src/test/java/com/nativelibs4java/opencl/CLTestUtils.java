package com.nativelibs4java.opencl;
import static com.nativelibs4java.opencl.JavaCL.listPlatforms;

import java.nio.Buffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;


import org.bridj.*;
import static org.bridj.Pointer.*;
/**
 * @author ochafik
 */
public class CLTestUtils {
    public enum Target {
        CPU, GPU, CPU_GPU;
        
    }
    public static interface Action1<X> {
        void call(X x);
    }
    public static interface Action2<X, Y> {
        void call(X x, Y y);
    }
    public static interface Action3<X, Y, Z> {
        void call(X x, Y y, Z z);
    }
    /// Calculate the average relative error of operations between two result buffers
    public static double avgError(FloatBuffer a, Pointer<Float> b, int dataSize) {
        double tot = 0;
        for (int i = 0; i < dataSize; i++) {
            float va = a.get(i), vb = b.get(i);
            float d = va - vb;
            if (Float.isNaN(d))
                d = d + 0;
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
    public static double avgError(DoubleBuffer a, Pointer<Double> b, int dataSize) {
        double tot = 0;
        for (int i = 0; i < dataSize; i++) {
            double va = a.get(i), vb = b.get(i);
            double d = va - vb;
            if (Double.isNaN(d))
                d = d + 0;
            if (d < 0)
                d = -d;
            double m = (va + vb) / 2;
            if (m == 0)
                continue;
            double r = d / (double)m;
            tot += r;
        }
        return tot / dataSize;
    }

    public static void fillBuffersWithSomeDataf(FloatBuffer a, FloatBuffer b) {
        fillBuffersWithSomeDataf((Pointer<Float>)pointerToBuffer(a), (Pointer<Float>)pointerToBuffer(b));
    }
    public static void fillBuffersWithSomeDatad(DoubleBuffer a, DoubleBuffer b) {
        fillBuffersWithSomeDatad((Pointer<Double>)pointerToBuffer(a), (Pointer<Double>)pointerToBuffer(b));
    }
    public static void fillBuffersWithSomeDataf(Pointer<Float> a, Pointer<Float> b) {
        int s = (int)a.getValidElements();
        for (int i = 0; i < s; i++) {
            float v = i;
            a.set(i, v);
            b.set(i, v);
        }
    }
    public static void fillBuffersWithSomeDatad(Pointer<Double> a, Pointer<Double> b) {
        int s = (int)a.getValidElements();
        for (int i = 0; i < s; i++) {
            double v = i;
            a.set(i, v);
            b.set(i, v);
        }
    }
    
    public static CLDevice[] getDevices(Target target) {
        CLPlatform platform = listPlatforms()[0];
        switch (target) {
            case CPU:
                return platform.listCPUDevices(true);
            case GPU:
                return platform.listGPUDevices(true);
            case CPU_GPU:
                return platform.listAllDevices(true);
            default:
                throw new IllegalArgumentException("Unknown target : " + target);
        }
    }
    public static class ExecResult<B> {
        public B buffer;
        public double unitTimeNano;
        public ExecResult(B buffer, double unitTimeNano) {
            this.buffer = buffer;
            this.unitTimeNano = unitTimeNano;
        }
    }
}
