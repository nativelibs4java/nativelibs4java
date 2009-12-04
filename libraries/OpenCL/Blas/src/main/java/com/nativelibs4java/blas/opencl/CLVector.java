/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.blas.opencl;

import com.nativelibs4java.blas.Matrix;
import com.nativelibs4java.blas.Vector;
import com.nativelibs4java.opencl.CLDoubleBuffer;
import com.nativelibs4java.opencl.CLMem;
import com.nativelibs4java.opencl.CLMem.MapFlags;
import com.nativelibs4java.opencl.CLMem.Usage;
import java.nio.DoubleBuffer;

/**
 *
 * @author Olivier
 */
public class CLVector extends Vector {

    CLDoubleBuffer buffer;
    DoubleBuffer mappedBuffer;
    CLLinearAlgebra al;

    public CLVector(CLLinearAlgebra al, int size) {
        super(size);
        this.al = al;
        buffer = al.context.createDoubleBuffer(CLMem.Usage.InputOutput, size);
    }

    @Override
    public double get(int index) {
        attach(Usage.Read);
        return mappedBuffer.get(index);
    }

    @Override
    public void set(int index, double value) {
        attach(Usage.Write);
        mappedBuffer.put(index, value);
    }

    @Override
    public void attach(Usage usage) {
        mappedBuffer = buffer.map(al.queue, CLMatrix.getMapFlags(usage));
    }

    @Override
    public void detach() {
        buffer.unmap(al.queue, mappedBuffer).waitFor();
    }

}
