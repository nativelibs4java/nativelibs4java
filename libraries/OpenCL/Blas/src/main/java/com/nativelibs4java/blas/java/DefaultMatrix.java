/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.blas.java;

import com.nativelibs4java.blas.Matrix;
import com.nativelibs4java.util.NIOUtils;
import java.nio.DoubleBuffer;

/**
 *
 * @author Olivier
 */
public class DefaultMatrix extends Matrix {

    @Override
    public double get(int row, int column) {
        return data.get(getIndex(row, column));
    }

    @Override
    public void set(int row, int column, double value) {
        data.put(getIndex(row, column), value);
    }

    public DefaultMatrix(int rows, int columns) {
        super(rows, columns);
        data = NIOUtils.directDoubles(rows * columns);
    }
    protected DoubleBuffer data;

    @Override
    public void attach(Usage usage) {}

    @Override
    public void detach() {}
}
