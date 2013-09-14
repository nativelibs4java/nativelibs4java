/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nativelibs4java.opencl.blas.ujmp;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLMem.Usage;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.blas.CLDefaultMatrix2D;
import com.nativelibs4java.opencl.blas.CLEvents;
import com.nativelibs4java.opencl.blas.CLKernels;
import com.nativelibs4java.opencl.blas.CLMatrix2D;
import com.nativelibs4java.opencl.blas.CLMatrixUtils;
import com.nativelibs4java.opencl.util.Primitive;

import org.bridj.Pointer;
import org.ujmp.core.doublematrix.DoubleMatrix2D;
import org.ujmp.core.floatmatrix.FloatMatrix2D;
import org.ujmp.core.matrix.Matrix2D;

import static org.bridj.Pointer.allocateArray;

/**
 *
 * @author ochafik
 */
public class CLWrappedMatrix2D<T> implements CLMatrix2D<T> {
    public static <T> CLMatrix2D<T> wrap(final Matrix2D matrix, final CLKernels clUJMP) {
        if (matrix instanceof CLMatrix2D)
            return (CLMatrix2D<T>)matrix;
        
        return new CLWrappedMatrix2D<T>(matrix, clUJMP, CLDefaultMatrix2D.DEFAULT_BLOCK_SIZE);
    }
    
    final Matrix2D matrix;
    final CLKernels kernels;
    final Primitive primitive;
    final Class<T> elementType;
    final int blockSize;
    final long stride;
    
    CLWrappedMatrix2D(Matrix2D matrix, CLKernels kernels, int blockSize) {
        this.matrix = matrix;
        this.kernels = kernels;
        this.blockSize = blockSize;
        this.stride = CLMatrixUtils.roundUp(matrix.getColumnCount(), blockSize);
        
        if (matrix instanceof DoubleMatrix2D)
            this.primitive = Primitive.Double;
        else if (matrix instanceof FloatMatrix2D)
            this.primitive = Primitive.Float;
        else
            throw new UnsupportedOperationException();
        
        elementType = (Class)primitive.primitiveType;
    }
    
    final CLEvents events = new CLEvents();
    public CLEvents getEvents() {
        return events;
    }

    public CLKernels getKernels() {
        return kernels;
    }


    public Primitive getPrimitive() {
        return primitive;
    }

    volatile CLBuffer<T> buffer;
    volatile Pointer data;
    public synchronized CLBuffer<T> getBuffer() {
        long length = stride * CLMatrixUtils.roundUp(matrix.getColumnCount(), blockSize);

        // Read data
        if (data == null) {
            data = allocateArray(elementType, length).order(getContext().getByteOrder());
        }
        MatrixUtils.read(matrix, data, stride);

        // Write data to CLBuffer
        if (buffer == null) {
            buffer = kernels.getContext().createBuffer(Usage.Input, elementType, length);
        }

        events.performWrite(new CLEvents.Action() {
            public CLEvent perform(CLEvent[] events) {
                return buffer.write(getQueue(), data, false, events);
            }
        });

        return buffer;
    }

    public long getRowCount() {
        return matrix.getRowCount();
    }

    public long getColumnCount() {
        return matrix.getColumnCount();
    }

    public int getBlockSize() {
        return blockSize;
    }

    public long getStride() {
        return stride;
    }

    
    public CLContext getContext() {
        return kernels.getContext();
    }
    public CLQueue getQueue() {
        return kernels.getQueue();
    }

    public CLMatrix2D<T> blankClone() {
        return CLMatrixUtils.createMatrix(getRowCount(), getColumnCount(), elementType, getKernels());
    }

    public Class<T> getPrimitiveClass() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public CLMatrix2D<T> blankMatrix(long rows, long columns) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void write(Pointer<T> b) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void read(Pointer<T> b) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Pointer<T> read() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
