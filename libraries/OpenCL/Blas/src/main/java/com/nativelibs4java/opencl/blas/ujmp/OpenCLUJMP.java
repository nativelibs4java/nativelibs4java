/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.blas.ujmp;

import java.io.IOException;
import org.bridj.Pointer;
import static org.bridj.Pointer.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ujmp.core.Matrix;
import org.ujmp.core.calculation.Calculation.Ret;
import org.ujmp.core.doublematrix.DoubleMatrix2D;
import org.ujmp.core.exceptions.MatrixException;

import com.nativelibs4java.opencl.CLBuildException;
import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLMem.MapFlags;
import com.nativelibs4java.opencl.CLMem.Usage;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.JavaCL;
import com.nativelibs4java.opencl.util.Fun1;
import com.nativelibs4java.opencl.util.Fun2;
import com.nativelibs4java.opencl.util.LinearAlgebraUtils;
import com.nativelibs4java.opencl.util.ParallelMath;
import com.nativelibs4java.opencl.util.Primitive;
import com.nativelibs4java.util.Pair;
import java.util.HashMap;
import java.util.Map;
import org.bridj.SizeT;

/**
 *
 * @author ochafik
 */
public class OpenCLUJMP {
	protected final LinearAlgebraUtils kernels;
    protected final ParallelMath math;
    protected final CLContext context;
    protected final CLQueue queue;

    public OpenCLUJMP() throws IOException, CLBuildException {
        this(JavaCL.createBestContext().createDefaultQueue());
    }
    public OpenCLUJMP(CLQueue queue) throws IOException, CLBuildException {
        kernels = new LinearAlgebraUtils(queue);
        math = new ParallelMath(queue);
        context = queue.getContext();
        this.queue = queue;
    }

    public <T> Pair<CLBuffer<T>, CLEvent> op1(Primitive prim, Fun1 fun, CLBuffer<T> a, long rows, long columns, CLBuffer<T> out, CLEvent... eventsToWaitFor) throws CLBuildException {
        long length = rows * columns;
        if (out != null)
            out = (CLBuffer<T>)context.createBuffer(Usage.Output, prim.primitiveType, length);

        CLKernel kernel = math.getKernel(fun, prim);
        synchronized (kernel) {
            kernel.setArgs(a, out, length);
            CLEvent evt = kernel.enqueueNDRange(queue, new int [] { (int)length }, new int[] { 1 }, eventsToWaitFor);
            return new Pair<CLBuffer<T>, CLEvent>(out, evt);
        }
    }

    public <T> Pair<CLBuffer<T>, CLEvent> op2(Primitive prim, Fun2 fun, CLBuffer<T> a, CLBuffer<T> b, long rows, long columns, CLBuffer<T> out, CLEvent... eventsToWaitFor) throws CLBuildException {
        long length = rows * columns;
        if (out != null)
            out = (CLBuffer<T>)context.createBuffer(Usage.Output, prim.primitiveType, length);

        CLKernel kernel = math.getKernel(fun, prim, false);
        synchronized (kernel) {
            kernel.setArgs(a, b, out, length);
            CLEvent evt = kernel.enqueueNDRange(queue, new int [] { (int)length }, new int[] { 1 }, eventsToWaitFor);
            return new Pair<CLBuffer<T>, CLEvent>(out, evt);
        }
    }

    public <T> Pair<CLBuffer<T>, CLEvent> op2(Primitive prim, Fun2 fun, CLBuffer<T> a, T b, long rows, long columns, CLBuffer<T> out, CLEvent... eventsToWaitFor) throws CLBuildException {
        long length = rows * columns;
        if (out != null)
            out = (CLBuffer<T>)context.createBuffer(Usage.Output, prim.primitiveType, length);

        CLKernel kernel = math.getKernel(fun, prim, true);
        synchronized (kernel) {
            kernel.setArgs(a, b, out, length);
            CLEvent evt = kernel.enqueueNDRange(queue, new int [] { (int)length }, new int[] { 1 }, eventsToWaitFor);
            return new Pair<CLBuffer<T>, CLEvent>(out, evt);
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
                    "	size_t length,              \n" +
                    "	double value,               \n" +
                    "	__global int* pOut          \n" +
                    ") {                            \n" +
                    "	size_t i = get_global_id(0);\n" +
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
        		CLBuffer<Integer> pOut = context.createBuffer(Usage.Output, Integer.class, 1);
            kernel.setArgs(buffer, new SizeT(length), value, pOut);
            kernel.enqueueNDRange(queue, new int[] { (int)length }, new int[] { 1 }, eventsToWaitFor).waitFor();
            return pOut.read(queue).getInt() != 0;
        }
    }

    Map<Primitive, CLKernel> clearKernels = new HashMap<Primitive, CLKernel>();
    public <V> CLEvent clear(Primitive primitive, CLBuffer<V> buffer, long length, CLEvent... eventsToWaitFor) throws CLBuildException {
        CLKernel kernel;
        synchronized (clearKernels) {
            kernel = clearKernels.get(primitive);
            if (kernel == null) {
                kernel = context.createProgram((
                    "__kernel void clear(   \n" +
                    "	__global const double* a,   \n" +
                    "	size_t length               \n" +
                    ") {                            \n" +
                    "	size_t i = get_global_id(0);\n" +
                    //"	if (i >= length)            \n" +
                    //"		return;                 \n" +
                    "		                        \n" +
                    "	a[i] == (double)0;          \n" +
                    "}                              \n"
                ).replaceAll("double", primitive.clTypeName())).createKernel("clear");
                clearKernels.put(primitive, kernel);
            }
        }
        synchronized(kernel) {
            kernel.setArgs(buffer, new SizeT(length));
            return kernel.enqueueNDRange(queue, new int[] { (int)length }, new int[] { 1 }, eventsToWaitFor);
        }
    }

    Map<Primitive, CLKernel> matrixMultiplyKernels = new HashMap<Primitive, CLKernel>();
    public <T> Pair<CLBuffer<T>, CLEvent> matrixMultiply(Primitive prim, CLBuffer<T> a, long aRows, long aColumns, CLBuffer<T> b, long bRows, long bColumns, CLBuffer<T> out, CLEvent... eventsToWaitFor) throws CLBuildException {
        if (out != null)
            out = (CLBuffer<T>)context.createBuffer(Usage.Output, prim.primitiveType, aRows * bColumns);

        CLKernel kernel;
        synchronized (matrixMultiplyKernels) {
            kernel = matrixMultiplyKernels.get(prim);
            if (kernel == null) {
                String src =
                    "__kernel void mulMat_double(                                  " +
                    "   __global const double* a, size_t aRows, size_t aColumns,   " +
                    "   __global const double* b, size_t bColumns,                 " +
                    "   __global double* c                                         " +
                    ") {                                                           " +
                    "    size_t i = get_global_id(0);                              " +
                    "    size_t j = get_global_id(1);                              " +
                    "                                                              " +
                    "    if (i >= aRows || j >= bColumns) return;                  " +
                    "    double total = 0;                                         " +
                    "    long iOff = i * aColumns;                                 " +
                    "    for (long k = 0; k < aColumns; k++) {                     " +
                    "        total += a[iOff + k] * b[k * bColumns + j];           " +
                    "    }                                                         " +
                    "    c[i * bColumns + j] = total;                              " +
                    "}                                                             "
                ;
                String clTypeName = prim.clTypeName();
                src = src.replaceAll("double", clTypeName);
                kernel = context.createProgram(src).createKernel("mulMat_" + clTypeName);
                matrixMultiplyKernels.put(prim, kernel);
            }
        }
        synchronized (kernel) {
            kernel.setArgs(a, aRows, aColumns, b, bColumns, out);
            CLEvent evt = kernel.enqueueNDRange(queue, new int [] { (int)aRows, (int)bColumns }, new int[] { 1 }, eventsToWaitFor);
            return new Pair<CLBuffer<T>, CLEvent>(out, evt);
        }
    }

    Map<Primitive, CLKernel> matrixTransposeKernels = new HashMap<Primitive, CLKernel>();
    public <T> Pair<CLBuffer<T>, CLEvent> matrixTranspose(Primitive prim, CLBuffer<T> a, long aRows, long aColumns, CLBuffer<T> out, CLEvent... eventsToWaitFor) throws CLBuildException {
        if (out != null)
            out = (CLBuffer<T>)context.createBuffer(Usage.Output, prim.primitiveType, aRows * aColumns);

        CLKernel kernel;
        synchronized (matrixTransposeKernels) {
            kernel = matrixTransposeKernels.get(prim);
            if (kernel == null) {
                String src =
                    "__kernel void transposeMat_double(                             \n" +
                    "   __global const double* a, size_t aRows, size_t aColumns,    \n" +
                    "   __global const double* out                                  \n" +
                    ") {                                                            \n" +
                    "    size_t i = get_global_id(0);                               \n" +
                    "    size_t j = get_global_id(1);                               \n" +
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
                kernel = context.createProgram(src).createKernel("mulMat_" + clTypeName);
                matrixTransposeKernels.put(prim, kernel);
            }
        }
        synchronized (kernel) {
            kernel.setArgs(a, new SizeT(aRows), new SizeT(aColumns), out);
            CLEvent evt = kernel.enqueueNDRange(queue, new int [] { (int)aRows, (int)aColumns }, new int[] { 1 }, eventsToWaitFor);
            return new Pair<CLBuffer<T>, CLEvent>(out, evt);
        }
    }

    public CLContext getContext() {
        return context;
    }

    public CLQueue getQueue() {
        return queue;
    }
    
}
