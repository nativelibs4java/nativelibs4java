/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.blas.ujmp;

import java.nio.DoubleBuffer;
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

    public abstract DoubleBuffer getReadableData();
    public abstract DoubleBuffer getWritableData();

    public long[] getSize() {
        return new long[] { rows, columns };
    }

    protected long getStorageIndex(long row, long column) {
        return row * columns + column;
    }

    public double getDouble(long row, long column) {
        return getReadableData().get((int)getStorageIndex(row, column));
    }

    public void setDouble(double value, long row, long column) {
        getWritableData().get((int)getStorageIndex(row, column));
    }

    public double getDouble(int row, int column) {
        return getDouble(row, (long)column);
    }

    public void setDouble(double value, int row, int column) {
        setDouble(value, row, (long)column);
    }

}
