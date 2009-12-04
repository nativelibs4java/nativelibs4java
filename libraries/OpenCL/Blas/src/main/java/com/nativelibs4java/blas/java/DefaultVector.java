/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.blas.java;

import com.nativelibs4java.blas.Vector;
import com.nativelibs4java.util.NIOUtils;
import java.nio.DoubleBuffer;

/**
 *
 * @author Olivier
 */
public class DefaultVector extends Vector {

    protected DoubleBuffer buffer;

    public DefaultVector(int size) {
        super(size);
        buffer = NIOUtils.directDoubles(size);
    }

    @Override
    public double get(int i) {
        return buffer.get(i);
    }

    @Override
    public void set(int i, double value) {
        buffer.put(i, value);
    }

    @Override
    public void attach(Usage usage) {}

    @Override
    public void detach() {}

}
