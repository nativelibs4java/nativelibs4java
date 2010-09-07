/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.blas.ujmp;

import org.bridj.Pointer;
import static org.bridj.Pointer.*;
import org.ujmp.core.doublematrix.stub.AbstractDenseDoubleMatrix2D;

/**
 *
 * @author ochafik
 */
public abstract class AbstractNIODenseDoubleMatrix2D extends AbstractDenseDoubleMatrix2D {
	private static final long serialVersionUID = -5550605402491407083L;
	final long rows, columns;

    public AbstractNIODenseDoubleMatrix2D(long rows, long columns) {
        this.rows = rows;
        this.columns = columns;
    }

    public abstract Pointer<Double> getReadableData();
    public abstract Pointer<Double> getWritableData();

    @Override
    public long[] getSize() {
        return new long[] { rows, columns };
    }

    protected long getStorageIndex(long row, long column) {
        return row * columns + column;
    }

    @Override
    public double getDouble(long row, long column) {
        return getReadableData().get((int)getStorageIndex(row, column));
    }

    @Override
    public void setDouble(double value, long row, long column) {
        getWritableData().get((int)getStorageIndex(row, column));
    }

    @Override
    public double getDouble(int row, int column) {
        return getDouble(row, (long)column);
    }

    @Override
    public void setDouble(double value, int row, int column) {
        setDouble(value, row, (long)column);
    }

}
