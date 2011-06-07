/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.blas;

import java.io.IOException;

import com.nativelibs4java.opencl.CLBuildException;
import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLMem.Usage;
import com.nativelibs4java.opencl.CLPlatform.DeviceFeature;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.JavaCL;
import com.nativelibs4java.opencl.util.Fun1;
import com.nativelibs4java.opencl.util.Fun2;
import com.nativelibs4java.opencl.util.LinearAlgebraUtils;
import com.nativelibs4java.opencl.util.ParallelMath;
import com.nativelibs4java.opencl.util.Primitive;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author ochafik
 */
public class CLKernels {
	protected final LinearAlgebraUtils kernels;
    protected final ParallelMath math;
    protected final CLContext context;
    protected final CLQueue queue;

    private static volatile CLKernels instance;

    public static synchronized CLKernels getInstance() {
        if (instance == null) {
            try {
                instance = new CLKernels();
            } catch (Throwable ex) {
                ex.printStackTrace();
                throw new RuntimeException(ex);
            }
        }
        return instance;
    }
    
    public CLKernels() throws IOException, CLBuildException {
        this(
            JavaCL.createBestContext(
                DeviceFeature.DoubleSupport, 
                DeviceFeature.MaxComputeUnits
            ).createDefaultQueue()
        );
    }
    public CLKernels(CLQueue queue) throws IOException, CLBuildException {
        kernels = new LinearAlgebraUtils(queue);
        math = new ParallelMath(queue);
        context = queue.getContext();
        this.queue = queue;
    }

    public <T> CLEvent op1(Primitive prim, Fun1 fun, CLBuffer<T> a, long rows, long columns, CLBuffer<T> out, CLEvent... eventsToWaitFor) throws CLBuildException {
        long length = rows * columns;
        if (out == null || out.getElementCount() != length)
            throw new IllegalArgumentException("Expected buffer of length " + length + ", got " + out);
        //if (out != null)
        //    out = (CLBuffer<T>)context.createBuffer(Usage.Output, prim.primitiveType, length);

        CLKernel kernel = math.getKernel(fun, prim);
        synchronized (kernel) {
            kernel.setArgs(a, out, length);
            CLEvent evt = kernel.enqueueNDRange(queue, new int [] { (int)length }, eventsToWaitFor);
            return evt;//new Pair<CLBuffer<T>, CLEvent>(out, evt);
        }
    }

    public <T> CLEvent op2(Primitive prim, Fun2 fun, CLBuffer<T> a, CLBuffer<T> b, long rows, long columns, CLBuffer<T> out, CLEvent... eventsToWaitFor) throws CLBuildException {
        long length = rows * columns;
        if (out == null || out.getElementCount() != length)
            throw new IllegalArgumentException("Expected buffer of length " + length + ", got " + out);
        //if (out != null)
        //    out = (CLBuffer<T>)context.createBuffer(Usage.Output, prim.primitiveType, length);

        CLKernel kernel = math.getKernel(fun, prim, false);
        synchronized (kernel) {
            kernel.setArgs(a, b, out, length);
            CLEvent evt = kernel.enqueueNDRange(queue, new int [] { (int)length }, eventsToWaitFor);
            return evt;//new Pair<CLBuffer<T>, CLEvent>(out, evt);
        }
    }

    public <T> CLEvent op2(Primitive prim, Fun2 fun, CLBuffer<T> a, T b, long rows, long columns, CLBuffer<T> out, CLEvent... eventsToWaitFor) throws CLBuildException {
        long length = rows * columns;
        if (out == null || out.getElementCount() != length)
            throw new IllegalArgumentException("Expected buffer of length " + length + ", got " + out);
        //if (out != null)
        //    out = (CLBuffer<T>)context.createBuffer(Usage.Output, prim.primitiveType, length);

        CLKernel kernel = math.getKernel(fun, prim, true);
        synchronized (kernel) {
            kernel.setArgs(a, b, out, length);
            CLEvent evt = kernel.enqueueNDRange(queue, new int [] { (int)length }, eventsToWaitFor);
            return evt;//new Pair<CLBuffer<T>, CLEvent>(out, evt);
        }
    }

    Map<Primitive, CLKernel> containsValueKernels = new HashMap<Primitive, CLKernel>();
    public <V> boolean containsValue(Primitive primitive, CLBuffer<V> buffer, long length, V value, CLEvent... eventsToWaitFor) throws CLBuildException {
        CLKernel kernel;
        synchronized (containsValueKernels) {
            kernel = containsValueKernels.get(primitive);
            if (kernel == null) {
                kernel = context.createProgram((
                    "__kernel void containsValue(   \n" +
                    "	__global const double* a,   \n" +
                    "	int length,              \n" +
                    "	double value,               \n" +
                    "	__global char* pOut          \n" +
                    ") {                            \n" +
                    "	int i = get_global_id(0);\n" +
                    "	if (i >= length)            \n" +
                    "		return;                 \n" +
                    "		                        \n" +
                    "	if (a[i] == value)          \n" +
                    "		*pOut = 1;              \n" +
                    "}                              \n"
                ).replaceAll("double", primitive.clTypeName())).createKernel("containsValue");
                containsValueKernels.put(primitive, kernel);
            }
        }
        synchronized(kernel) {
            CLBuffer<Byte> pOut = context.createBuffer(Usage.Output, Byte.class, 1);
            kernel.setArgs(buffer, (int)length, value, pOut);
            kernel.enqueueNDRange(queue, new int[] { (int)length }, eventsToWaitFor).waitFor();
            return pOut.read(queue).getBoolean();
        }
    }

    Map<Primitive, CLKernel> clearKernels = new HashMap<Primitive, CLKernel>();
    public <V> CLEvent clear(Primitive primitive, CLBuffer<V> buffer, long length, CLEvent... eventsToWaitFor) throws CLBuildException {
        CLKernel kernel;
        synchronized (clearKernels) {
            kernel = clearKernels.get(primitive);
            if (kernel == null) {
                kernel = context.createProgram((
                    "__kernel void clear_buffer(    \n" +
                    "	__global double* a,         \n" +
                    "	int length                  \n" +
                    ") {                            \n" +
                    "	int i = get_global_id(0);   \n" +
                    "	if (i >= length)            \n" +
                    "		return;                 \n" +
                    "		                        \n" +
                    "	a[i] = (double)0;           \n" +
                    "}                              \n"
                ).replaceAll("double", primitive.clTypeName())).createKernel("clear_buffer");
                clearKernels.put(primitive, kernel);
            }
        }
        synchronized(kernel) {
            kernel.setArgs(buffer, (int)length);
            CLEvent evt = kernel.enqueueNDRange(queue, new int[] { (int)length }, eventsToWaitFor);
            //Object array = buffer.read(queue, evt).getArray();
            return evt;
        }
    }

    Map<Primitive, CLKernel> matrixMultiplyKernels = new HashMap<Primitive, CLKernel>();
    public <T> CLEvent matrixMultiply(Primitive prim, CLBuffer<T> a, long aRows, long aColumns, CLBuffer<T> b, long bRows, long bColumns, CLBuffer<T> out, CLEvent... eventsToWaitFor) throws CLBuildException {
        if (out == null)
            throw new IllegalArgumentException("Null output matrix !");
        //if (out != null)
        //    out = (CLBuffer<T>)context.createBuffer(Usage.Output, prim.primitiveType, aRows * bColumns);

        CLKernel kernel;
        synchronized (matrixMultiplyKernels) {
            kernel = matrixMultiplyKernels.get(prim);
            if (kernel == null) {
                String src =
                    "__kernel void mulMat(                                  " +
                    "   __global const double* a, int aRows, int aColumns,   " +
                    "   __global const double* b, int bColumns,                 " +
                    "   __global double* c                                         " +
                    ") {                                                           " +
                    "    int i = get_global_id(0);                              " +
                    "    int j = get_global_id(1);                              " +
                    "                                                              " +
                    "    if (i >= aRows || j >= bColumns) return;                  " +
                    "    double total = 0;                                         " +
                    "    size_t iOff = i * (size_t)aColumns;                                 " +
                    "    for (long k = 0; k < aColumns; k++) {                     " +
                    "        total += a[iOff + k] * b[k * (size_t)bColumns + j];           " +
                    "    }                                                         " +
                    "    c[i * (size_t)bColumns + j] = total;                              " +
                    "}                                                             "
                ;
                String clTypeName = prim.clTypeName();
                src = src.replaceAll("double", clTypeName);
                kernel = context.createProgram(src).createKernel("mulMat");
                matrixMultiplyKernels.put(prim, kernel);
            }
        }
        synchronized (kernel) {
            kernel.setArgs(a, aRows, aColumns, b, bColumns, out);
            CLEvent evt = kernel.enqueueNDRange(queue, new int [] { (int)aRows, (int)bColumns }, eventsToWaitFor);
            return evt;//new Pair<CLBuffer<T>, CLEvent>(out, evt);
        }
    }

    Map<Primitive, CLKernel> matrixTransposeKernels = new HashMap<Primitive, CLKernel>();
    public <T> CLEvent matrixTranspose(Primitive prim, CLBuffer<T> a, long aRows, long aColumns, CLBuffer<T> out, CLEvent... eventsToWaitFor) throws CLBuildException {
        if (out == null)
            throw new IllegalArgumentException("Null output matrix !");
        //if (out != null)
        //    out = (CLBuffer<T>)context.createBuffer(Usage.Output, prim.primitiveType, aRows * aColumns);

        CLKernel kernel;
        synchronized (matrixTransposeKernels) {
            kernel = matrixTransposeKernels.get(prim);
            if (kernel == null) {
                String src =
                    "__kernel void transposeMat(                             \n" +
                    "   __global const double* a, int aRows, int aColumns,    \n" +
                    "   __global double* out                                  \n" +
                    ") {                                                            \n" +
                    "    int i = get_global_id(0);                               \n" +
                    "    int j = get_global_id(1);                               \n" +
                    "                                                               \n" +
                    "    if (i >= aRows || j >= aColumns) return;                   \n" +
                    "                                                               \n" +
                    "    size_t aIndex = i * aColumns + j;                          \n" +
                    "    size_t outIndex = j * aRows + i;                           \n" +
                    "    if (a == out) {                                            \n" +
                    "    	double temp = out[outIndex];                            \n" +
                    "    	out[outIndex] = a[aIndex];                              \n" +
                    "    	a[aIndex] = temp;                                       \n" +
                    "	} else {                                                    \n" +
                    "		out[outIndex] = a[aIndex];                              \n" +
                    "	}                                                           \n" +
                    "}                                                              \n"
                ;
                String clTypeName = prim.clTypeName();
                src = src.replaceAll("double", clTypeName);
                kernel = context.createProgram(src).createKernel("transposeMat");
                matrixTransposeKernels.put(prim, kernel);
            }
        }
        synchronized (kernel) {
            kernel.setArgs(a, (int)aRows, (int)aColumns, out);
            CLEvent evt = kernel.enqueueNDRange(queue, new int [] { (int)aRows, (int)aColumns }, eventsToWaitFor);
            return evt;//new Pair<CLBuffer<T>, CLEvent>(out, evt);
        }
    }

    public CLContext getContext() {
        return context;
    }

    public CLQueue getQueue() {
        return queue;
    }
    
}
