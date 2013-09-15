/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nativelibs4java.opencl.blas;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.util.Primitive;
import org.bridj.Pointer;

/**
 *
 * @author ochafik
 */
public interface CLMatrix2D<T> {

    Primitive getPrimitive();
    Class<T> getPrimitiveClass();
    CLEvents getEvents();
    CLBuffer<T> getBuffer();
    CLContext getContext();
    CLQueue getQueue();
    long getRowCount();
    long getColumnCount();
    long getStride();
    int getBlockSize();
    CLMatrix2D<T> blankClone();
    CLMatrix2D<T> blankMatrix(long rows, long columns);
    CLKernels getKernels();
    
    void write(Pointer<T> b);
    void read(Pointer<T> b);
    Pointer<T> read();
}
