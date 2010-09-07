/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.blas.ujmp;

import org.ujmp.core.interfaces.Wrapper;

import org.bridj.Pointer;
import static org.bridj.Pointer.*;

/**
 *
 * @author ochafik
 */
public class DirectNIODenseDoubleMatrix2D extends AbstractNIODenseDoubleMatrix2D implements Wrapper<Pointer<Double>> {

    /**
	 * 
	 */
	private static final long serialVersionUID = 8936390922363132043L;
	Pointer<Double> data;

    public DirectNIODenseDoubleMatrix2D(Pointer<Double> data, long rows, long columns) {
        super(rows, columns);
        this.data = data;
    }

    public DirectNIODenseDoubleMatrix2D(long rows, long columns) {
        this(allocateDoubles(rows * columns).order(CLDenseDoubleMatrix2DFactory.LINEAR_ALGEBRA_KERNELS.getContext().getKernelsDefaultByteOrder()), rows, columns);
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

    @Override
    public Pointer<Double> getWrappedObject() {
        return data;
    }

    @Override
    public void setWrappedObject(Pointer<Double> object) {
        this.data = object;
    }

}
