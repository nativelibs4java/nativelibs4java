/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.blas;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLBuildException;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLMem.Usage;
import com.nativelibs4java.opencl.CLPlatform.DeviceFeature;
import com.nativelibs4java.opencl.CLProgram;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.JavaCL;
import com.nativelibs4java.opencl.LocalSize;
import com.nativelibs4java.opencl.util.Fun1;
import com.nativelibs4java.opencl.util.Fun2;
import com.nativelibs4java.opencl.util.LinearAlgebraUtils;
import com.nativelibs4java.opencl.util.ParallelMath;
import com.nativelibs4java.opencl.util.Primitive;

import static com.nativelibs4java.opencl.blas.CLMatrixUtils.roundUp;
import static org.bridj.Pointer.pointerToInt;

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

    public static synchronized void setInstance(CLKernels kernels) {
        instance = kernels;
    }
    public static synchronized CLKernels getInstance() {
        if (instance == null) {
            try {
                instance = new CLKernels();
            } catch (Throwable ex) {
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

    public <T> CLEvent op1(Primitive prim, Fun1 fun, CLBuffer<T> a, long rows, long columns, long stride, CLBuffer<T> out, CLEvent... eventsToWaitFor) throws CLBuildException {
        long length = rows * stride;
        if (out == null || out.getElementCount() < length)
            throw new IllegalArgumentException("Expected buffer of length >= " + length + ", got " + out);
        //if (out != null)
        //    out = (CLBuffer<T>)context.createBuffer(Usage.Output, prim.primitiveType, length);

        CLKernel kernel = math.getKernel(fun, prim);
        synchronized (kernel) {
            kernel.setArgs(a, out, length);
            CLEvent evt = kernel.enqueueNDRange(queue, new int [] { (int)length }, eventsToWaitFor);
            return evt;
        }
    }

    public <T> CLEvent op2(Primitive prim, Fun2 fun, CLBuffer<T> a, CLBuffer<T> b, long rows, long columns, long stride, CLBuffer<T> out, CLEvent... eventsToWaitFor) throws CLBuildException {
        long length = rows * stride;
        if (out == null || out.getElementCount() < length)
            throw new IllegalArgumentException("Expected buffer of length >= " + length + ", got " + out.getElementCount());
        //if (out != null)
        //    out = (CLBuffer<T>)context.createBuffer(Usage.Output, prim.primitiveType, length);

        CLKernel kernel = math.getKernel(fun, prim, false);
        synchronized (kernel) {
            kernel.setArgs(a, b, out, length);
            CLEvent evt = kernel.enqueueNDRange(queue, new int [] { (int)length }, eventsToWaitFor);
            return evt;
        }
    }

    public <T> CLEvent op2(Primitive prim, Fun2 fun, CLBuffer<T> a, T b, long rows, long columns, long stride, CLBuffer<T> out, CLEvent... eventsToWaitFor) throws CLBuildException {
        long length = rows * stride;
        if (out == null || out.getElementCount() < length)
            throw new IllegalArgumentException("Expected buffer of length >= " + length + ", got " + out.getElementCount());
        //if (out != null)
        //    out = (CLBuffer<T>)context.createBuffer(Usage.Output, prim.primitiveType, length);

        CLKernel kernel = math.getKernel(fun, prim, true);
        synchronized (kernel) {
            kernel.setArgs(a, b, out, length);
            CLEvent evt = kernel.enqueueNDRange(queue, new int [] { (int)length }, eventsToWaitFor);
            return evt;
        }
    }

    Map<Primitive, CLKernel> containsValueKernels = new HashMap<Primitive, CLKernel>();
    public <V> boolean containsValue(Primitive primitive, CLBuffer<V> buffer, long length, V value, CLEvent... eventsToWaitFor) throws CLBuildException {
        CLKernel kernel;
        synchronized (containsValueKernels) {
            kernel = containsValueKernels.get(primitive);
            if (kernel == null) {
                kernel = context.createProgram((
                	primitive.getRequiredPragmas() +
                    "__kernel void containsValue(   \n" +
                    "	__global const double* a,   \n" +
                    "	int length,              \n" +
                    "	double value,               \n" +
                    "	__global int* pOut          \n" +
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
            CLBuffer<Integer> pOut = context.createBuffer(Usage.Output, pointerToInt(0));
            kernel.setArgs(buffer, (int)length, value, pOut);
            kernel.enqueueNDRange(queue, new int[] { (int)length }, eventsToWaitFor).waitFor();
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
                	primitive.getRequiredPragmas() +
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

    Map<String, CLKernel> matrixMultiplyKernels = new HashMap<String, CLKernel>();
    public <T> CLEvent matrixMultiply(Primitive prim,
        CLBuffer<T> a, long aRows, long aColumns, int aBlockSize,
        CLBuffer<T> b, long bRows, long bColumns, int bBlockSize,
        CLBuffer<T> out, CLEvent... eventsToWaitFor) throws CLBuildException {
      boolean useBlocks = false;
      int blockSize = aBlockSize;
      if (blockSize > 1 && blockSize == bBlockSize) {
        long[] maxWorkItemSizes = queue.getDevice().getMaxWorkItemSizes();
        useBlocks = maxWorkItemSizes.length >= 2 &&
            maxWorkItemSizes[0] >= blockSize &&
            maxWorkItemSizes[1] >= blockSize;
      }
      if (useBlocks) {
        return blockMatrixMultiply(
            blockSize, prim,
            a, roundUp(aRows, blockSize), roundUp(aColumns, blockSize),
            b, roundUp(bRows, blockSize), roundUp(bColumns, blockSize),
            out, eventsToWaitFor);
      } else {
        return naiveMatrixMultiply(prim, a, aRows, aColumns, b, bRows, bColumns, out, eventsToWaitFor);
      }
    }
    public <T> CLEvent blockMatrixMultiply(int blockSize, Primitive prim, CLBuffer<T> a, long aRows, long aColumns, CLBuffer<T> b, long bRows, long bColumns, CLBuffer<T> out, CLEvent... eventsToWaitFor) throws CLBuildException {
        if (out == null)
            throw new IllegalArgumentException("Null output matrix !");
        //if (out != null)
        //    out = (CLBuffer<T>)context.createBuffer(Usage.Output, prim.primitiveType, aRows * bColumns);

        CLKernel kernel;
        String key = "block_" + blockSize + "_" + prim;
        synchronized (matrixMultiplyKernels) {
            kernel = matrixMultiplyKernels.get(key);
            if (kernel == null) {
                String src = prim.getRequiredPragmas() +
                    "#define BLOCK_SIZE " + blockSize + "\n" +
                    "#define AS(i, j) As[j + i * BLOCK_SIZE]\n" +
                    "#define BS(i, j) Bs[j + i * BLOCK_SIZE]\n" +
                    "\n" +
                    "__kernel void mulMat(                                  " +
                    "   __global const double* A, int aColumns,   " +
                    "   __global const double* B, int bColumns,                 " +
                    "   __global double* C,                                         " +
                    "   __local double* As,                                         " +
                    "   __local double* Bs                                         " +
                    ") {                                                           " +
                    "    // Block index\n" +
                    "    int bx = get_group_id(0);\n" +
                    "    int by = get_group_id(1);\n" +
                    "\n" +
                    "    // Thread index\n" +
                    "    int tx = get_local_id(0);\n" +
                    "    int ty = get_local_id(1);\n" +
                    "\n" +
                    "    // Index of the first sub-matrix of A processed by the block\n" +
                    "    int aBegin = aColumns * BLOCK_SIZE * by + aColumns * ty + tx;\n" +
                    "\n" +
                    "    // Index of the last sub-matrix of A processed by the block\n" +
                    "    int aEnd   = aBegin + aColumns;\n" +
                    "\n" +
                    "    // Step size used to iterate through the sub-matrices of A\n" +
                    "    int aStep  = BLOCK_SIZE;\n" +
                    "\n" +
                    "    // Index of the first sub-matrix of B processed by the block\n" +
                    "    int bBegin = BLOCK_SIZE * bx + bColumns * ty + tx;\n" +
                    "\n" +
                    "    // Step size used to iterate through the sub-matrices of B\n" +
                    "    int bStep  = BLOCK_SIZE * bColumns;\n" +
                    "\n" +
                    "    // total is used to store the element of the block sub-matrix\n" +
                    "    // that is computed by the thread\n" +
                    "    float total = 0.0f;\n" +
                    "\n" +
                    "    // Loop over all the sub-matrices of A and B\n" +
                    "    // required to compute the block sub-matrix\n" +
                    "    for (int a = aBegin, b = bBegin;\n" +
                    "             a < aEnd;\n" +
                    "             a += aStep, b += bStep) {\n" +
                    "\n" +
                    "        // Load the matrices from device memory\n" +
                    "        // to shared memory; each thread loads\n" +
                    "        // one element of each matrix\n" +
                    "        AS(ty, tx) = A[a];\n" +
                    "        BS(ty, tx) = B[b];\n" +
                    "\t\n" +
                    "        // Synchronize to make sure the matrices are loaded\n" +
                    "        barrier(CLK_LOCAL_MEM_FENCE);\n" +
                    "\n" +
                    "        // Multiply the two matrices together;\n" +
                    "        // each thread computes one element\n" +
                    "        // of the block sub-matrix        \n" +
                    "        #pragma unroll\n" +
                    "        for (int k = 0; k < BLOCK_SIZE; ++k)\n" +
                    "            total += AS(ty, k) * BS(k, tx);\n" +
                    "\n" +
                    "        // Synchronize to make sure that the preceding\n" +
                    "        // computation is done before loading two new\n" +
                    "        // sub-matrices of A and B in the next iteration\n" +
                    "        barrier(CLK_LOCAL_MEM_FENCE);\n" +
                    "    }\n" +
                    "\n" +
                    "    C[get_global_id(1) * get_global_size(0) + get_global_id(0)] = total;\n" +
                    "}                                                             "
                ;
                String clTypeName = prim.clTypeName();
                src = src.replaceAll("double", clTypeName);
                kernel = context.createProgram(src).createKernel("mulMat");
                matrixMultiplyKernels.put(key, kernel);
            }
        }
        synchronized (kernel) {
            kernel.setArgs(a, (int) aColumns, b, (int) bColumns, out,
                    LocalSize.ofFloatArray(blockSize * blockSize),
                    LocalSize.ofFloatArray(blockSize * blockSize));
            CLEvent evt = kernel.enqueueNDRange(queue,
                    new int[]{(int) aRows, (int) bColumns},
                    new int[]{blockSize, blockSize},
                    eventsToWaitFor);
            return evt;
        }
    }

    public <T> CLEvent naiveMatrixMultiply(Primitive prim, CLBuffer<T> a, long aRows, long aColumns, CLBuffer<T> b, long bRows, long bColumns, CLBuffer<T> out, CLEvent... eventsToWaitFor) throws CLBuildException {
        if (out == null)
            throw new IllegalArgumentException("Null output matrix !");
        //if (out != null)
        //    out = (CLBuffer<T>)context.createBuffer(Usage.Output, prim.primitiveType, aRows * bColumns);

        CLKernel kernel;
        String key = "naive_" + prim;
        synchronized (matrixMultiplyKernels) {
            kernel = matrixMultiplyKernels.get(key);
            if (kernel == null) {
                String src = prim.getRequiredPragmas() +
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
                matrixMultiplyKernels.put(key, kernel);
            }
        }
        synchronized (kernel) {
            kernel.setArgs(a, (int)aRows, (int)aColumns, b, (int)bColumns, out);
            CLEvent evt = kernel.enqueueNDRange(queue, new int [] { (int)aRows, (int)bColumns }, eventsToWaitFor);
            return evt;
        }
    }

    Map<Primitive, CLKernel[]> matrixTransposeKernels = new HashMap<Primitive, CLKernel[]>();
    public <T> CLEvent matrixTranspose(Primitive prim, CLBuffer<T> a, long aRows, long aColumns, long aStride, CLBuffer<T> out, CLEvent... eventsToWaitFor) throws CLBuildException {
        if (out == null)
            throw new IllegalArgumentException("Null output matrix !");
        //if (out != null)
        //    out = (CLBuffer<T>)context.createBuffer(Usage.Output, prim.primitiveType, aRows * aColumns);

        CLKernel[] kernels;
        synchronized (matrixTransposeKernels) {
            kernels = matrixTransposeKernels.get(prim);
            if (kernels == null) {
                String src =
                	prim.getRequiredPragmas() +
                    "__kernel void transposeSelf(                                   \n" +
                    "   __global double* a, int aRows, int aColumns, int aStride    \n" +
                    ") {                                                            \n" +
                    "    int i = get_global_id(0);                                  \n" +
                    "    int j = get_global_id(1);                                  \n" +
                    "                                                               \n" +
                    "    if (i >= aRows || j >= aColumns || j >= i) return;         \n" +
                    "                                                               \n" +
                    "    size_t aIndex = i * aStride + j;                           \n" +
                    "    size_t outIndex = j * aRows + i;                           \n" +
                    "    double temp = a[outIndex];                                 \n" +
                    "    a[outIndex] = a[aIndex];                                   \n" +
                    "    a[aIndex] = temp;                                          \n" +
                    "}                                                              \n" +
                    "__kernel void transposeOther(                                  \n" +
                    "   __global const double* a, int aRows, int aColumns, int aStride, \n" +
                    "   __global double* out                                        \n" +
                    ") {                                                            \n" +
                    "    int i = get_global_id(0);                                  \n" +
                    "    int j = get_global_id(1);                                  \n" +
                    "                                                               \n" +
                    "    if (i >= aRows || j >= aColumns) return;                   \n" +
                    "                                                               \n" +
                    "    size_t aIndex = i * aStride + j;                           \n" +
                    "    size_t outIndex = j * aRows + i;                           \n" +
                    "    out[outIndex] = a[aIndex];                                 \n" +
                    "}                                                              \n"
                ;
                String clTypeName = prim.clTypeName();
                src = src.replaceAll("double", clTypeName);
                CLProgram program = context.createProgram(src);
                kernels = new CLKernel[] { program.createKernel("transposeSelf"), program.createKernel("transposeOther") };
                matrixTransposeKernels.put(prim, kernels);
            }
        }
        boolean self = a.equals(out);
        CLKernel kernel = kernels[self ? 0 : 1];
        synchronized (kernel) {
            if (self)
                kernel.setArgs(a, (int)aRows, (int)aColumns, (int)aStride);
            else
                kernel.setArgs(a, (int)aRows, (int)aColumns, (int)aStride, out);
            
            CLEvent evt = kernel.enqueueNDRange(queue, new int [] { (int)aRows, (int)aColumns }, eventsToWaitFor);
            return evt;
        }
    }

    public CLContext getContext() {
        return context;
    }

    public CLQueue getQueue() {
        return queue;
    }
    
}
