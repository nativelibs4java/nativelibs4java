package com.nativelibs4java.opencl;
import com.nativelibs4java.opencl.*;
import java.nio.*;

import static com.nativelibs4java.opencl.JavaCL.*;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;
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
    public static double avgError(FloatBuffer a, FloatBuffer b, int dataSize) {
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
    public static double avgError(DoubleBuffer a, DoubleBuffer b, int dataSize) {
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

    public static void fillBuffersWithSomeData(FloatBuffer a, FloatBuffer b) {
        int s = a.capacity();
        for (int i = 0; i < s; i++) {
            a.put(i, i);
            b.put(i, i);
        }
    }
    public static void fillBuffersWithSomeData(DoubleBuffer a, DoubleBuffer b) {
        int s = a.capacity();
        for (int i = 0; i < s; i++) {
            a.put(i, i);
            b.put(i, i);
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
    public static class ExecResult<B extends Buffer> {
        public B buffer;
        public double unitTimeNano;
        public ExecResult(B buffer, double unitTimeNano) {
            this.buffer = buffer;
            this.unitTimeNano = unitTimeNano;
        }
    }
}
