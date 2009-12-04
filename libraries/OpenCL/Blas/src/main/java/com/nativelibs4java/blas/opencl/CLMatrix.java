/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.blas.opencl;

import com.nativelibs4java.blas.Matrix;
import com.nativelibs4java.opencl.CLDoubleBuffer;
import com.nativelibs4java.opencl.CLMem;
import com.nativelibs4java.opencl.CLMem.MapFlags;
import com.nativelibs4java.opencl.CLMem.Usage;
import java.nio.DoubleBuffer;

/**
 *
 * @author Olivier
 */
public class CLMatrix extends Matrix {

    CLDoubleBuffer buffer;
    DoubleBuffer mappedBuffer;
    CLLinearAlgebra al;

    public CLMatrix(CLLinearAlgebra al, int rows, int columns) {
        super(rows, columns);
        this.al = al;
        buffer = al.context.createDoubleBuffer(CLMem.Usage.InputOutput, rows * columns);
    }

    @Override
    public double get(int row, int column) {
        attach(Usage.Read);
        //if (mappedBuffer == null)
        //    throw new IllegalThreadStateException("Matrix buffer was not attached !");

        return mappedBuffer.get(getIndex(row, column));
    }

    @Override
    public void set(int row, int column, double value) {
        attach(Usage.Write);
        mappedBuffer.put(getIndex(row, column), value);
    }

    static MapFlags getMapFlags(Usage usage) {
        MapFlags flags;
        switch (usage) {
            case Read:
                flags = MapFlags.Read;
                break;
            case Write:
                flags = MapFlags.Write;
                break;
            case ReadWrite:
                flags = MapFlags.ReadWrite;
                break;
            default:
                throw new UnsupportedOperationException();
        }
        return flags;
    }
    @Override
    public void attach(Usage usage) {
        mappedBuffer = buffer.map(al.queue, getMapFlags(usage));
    }

    @Override
    public void detach() {
        buffer.unmap(al.queue, mappedBuffer);
    }

}
