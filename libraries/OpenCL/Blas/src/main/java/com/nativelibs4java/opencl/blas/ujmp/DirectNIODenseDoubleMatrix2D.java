/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.blas.ujmp;

import org.ujmp.core.doublematrix.stub.AbstractDenseDoubleMatrix2D;
import org.ujmp.core.interfaces.Wrapper;

import org.bridj.Pointer;
import static org.bridj.Pointer.*;

/**
 *
 * @author ochafik
 */
public class DirectNIODenseDoubleMatrix2D extends AbstractDenseDoubleMatrix2D {

    /**
	 * 
	 */
	private static final long serialVersionUID = 8936390922363132043L;
	final Pointer<Double> data;
    final long rows, columns;

    public DirectNIODenseDoubleMatrix2D(Pointer<Double> data, long rows, long columns) {
        this.rows = rows;
        this.columns = columns;
        this.data = data;
    }

    public DirectNIODenseDoubleMatrix2D(long rows, long columns) {
        this(allocateDoubles(rows * columns).order(OpenCLUJMP.getInstance().getContext().getKernelsDefaultByteOrder()), rows, columns);
    }

    public DirectNIODenseDoubleMatrix2D(long[] size) {
        this(size[0], size[1]);
        if (size.length != 2)
            throw new IllegalArgumentException("Size is not 2D !");
    }


    public Pointer<Double> getData() {
        return data;
    }

    public Pointer<Double> getReadableData() {
        return getData();
    }


    public Pointer<Double> getWritableData() {
        return getData();
    }

    public long[] getSize() {
        return new long[] { rows, columns };
    }

    public double getDouble(long row, long column) {
        return data.getDoubleAtOffset((row * columns + column) << 3);
    }

    @Override
    public void setDouble(double value, long row, long column) {
        data.setDoubleAtOffset((row * columns + column) << 3, value);
    }

    @Override
    public double getDouble(int row, int column) {
        return getDouble((long)row, column);
    }

    @Override
    public void setDouble(double value, int row, int column) {
        setDouble(value, (long)row, column);
    }
}
