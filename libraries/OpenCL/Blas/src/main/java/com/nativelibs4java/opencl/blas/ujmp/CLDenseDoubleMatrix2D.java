/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.blas.ujmp;

import com.nativelibs4java.opencl.CLBuildException;
import com.nativelibs4java.opencl.util.Primitive;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bridj.Pointer;

import org.ujmp.core.Matrix;
import org.ujmp.core.calculation.Calculation.Ret;
import org.ujmp.core.exceptions.MatrixException;

import org.ujmp.core.doublematrix.stub.AbstractDenseDoubleMatrix2D;

/**
 *
 * @author ochafik
 */
public class CLDenseDoubleMatrix2D extends AbstractDenseDoubleMatrix2D implements CLDenseMatrix2D<Double> {
	private static final long serialVersionUID = -36941159548127670L;
	protected final CLDenseMatrix2DImpl<Double> impl;

    public CLDenseDoubleMatrix2D(OpenCLUJMP clUJMP, long rows, long columns) {
        this(new CLDenseMatrix2DImpl<Double>(Primitive.Double, null, rows, columns, clUJMP));
    }

    public CLDenseDoubleMatrix2D(long... size) {
        this(CLDenseDoubleMatrix2DFactory.getOpenCLUJMP(), size[0], size[1]);
    }

    public CLDenseDoubleMatrix2D(long size) {
        this(size, size);
    }

    protected CLDenseDoubleMatrix2D(CLDenseMatrix2DImpl<Double> impl) {
        this.impl = impl;
    }

    @Override
    public long[] getSize() {
        return new long[] { impl.rows, impl.columns };
    }

    public void write(Pointer<Double> b) {
        impl.write(b);
    }

    public void read(Pointer<Double> b) {
        impl.read(b);
    }

    public Pointer<Double> read() {
        return impl.read();
    }

    @Override
    public double getDouble(int row, int column) {
        return getDouble((long)row, (long)column);
    }

    @Override
    public void setDouble(double value, int row, int column) {
        setDouble(value, (long)row, (long)column);
    }

    @Override
    public CLDenseMatrix2DImpl<Double> getImpl() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Iterable<Object> allValues() {
        return (Pointer)impl.read();
    }


    @Override
    public Matrix min(Ret returnType, int dimension) throws MatrixException {
        switch (dimension) {
            case ROW:
            case COLUMN:
                // TODO
                return super.min(returnType, dimension);
            case ALL:
                try {
                    return inst(impl.min());
                } catch (CLBuildException ex) {
                    throw new MatrixException("Failed to compute min", ex);
                }
            default:
                throw new IllegalArgumentException("Invalid dimension : " + dimension);
        }
    }

    @Override
    public Matrix max(Ret returnType, int dimension) throws MatrixException {
        switch (dimension) {
            case ROW:
            case COLUMN:
                // TODO
                return super.max(returnType, dimension);
            case ALL:
                try {
                    return inst(impl.max());
                } catch (CLBuildException ex) {
                    throw new MatrixException("Failed to compute max", ex);
                }
            default:
                throw new IllegalArgumentException("Invalid dimension : " + dimension);
        }
    }

    @Override
    public Matrix mean(Ret returnType, int dimension, boolean ignoreNaN) throws MatrixException {
        // TODO
        return super.mean(returnType, dimension, ignoreNaN);
    }

    @Override
    public Matrix center(Ret returnType, int dimension, boolean ignoreNaN) throws MatrixException {
        switch (dimension) {
            case ROW:
            case COLUMN:
                // TODO
                return super.center(returnType, dimension, ignoreNaN);
            case ALL:
                return minus(returnType, ignoreNaN, mean(Ret.NEW, dimension, ignoreNaN).getAsDouble(0, 0));
            default:
                throw new IllegalArgumentException("Invalid dimension : " + dimension);
        }
    }

    @Override
    public double getDouble(long row, long column) {
        return impl.get(row, column);
    }

    @Override
    public void setDouble(double value, long row, long column) {
        impl.set(value, row, column);
    }

    static CLDenseDoubleMatrix2D inst(CLDenseMatrix2DImpl<Double> impl) {
        return new CLDenseDoubleMatrix2D(impl);
    }

    @Override
    public Matrix mtimes(Ret returnType, boolean ignoreNaN, Matrix matrix) throws MatrixException {
        return inst(impl.multiplyMatrix(((CLDenseMatrix2D<Double>)matrix).getImpl()));
    }

    @Override
    public synchronized Matrix copy() throws MatrixException {
        return inst(impl.copy());
    }

    @Override
    public Matrix transpose() throws MatrixException {
        return inst(impl.transpose(Ret.NEW));
    }

    @Override
    public Matrix transpose(Ret returnType, int dimension1, int dimension2) throws MatrixException {
        return inst(impl.transpose(returnType));
    }

    @Override
    public Matrix sin(Ret returnType) throws MatrixException {
        return inst(impl.sin(returnType));
    }

    @Override
    public Matrix cos(Ret returnType) throws MatrixException {
        return inst(impl.cos(returnType));
    }

    @Override
    public Matrix sinh(Ret returnType) throws MatrixException {
        return inst(impl.sinh(returnType));
    }

    @Override
    public Matrix cosh(Ret returnType) throws MatrixException {
        return inst(impl.cosh(returnType));
    }

    @Override
    public Matrix tan(Ret returnType) throws MatrixException {
        return inst(impl.tan(returnType));
    }

    @Override
    public Matrix tanh(Ret returnType) throws MatrixException {
        return inst(impl.tanh(returnType));
    }

    //@Override
    public Matrix asin(Ret returnType) throws MatrixException {
        return inst(impl.asin(returnType));
    }

    //@Override
    public Matrix acos(Ret returnType) throws MatrixException {
        return inst(impl.acos(returnType));
    }

    //@Override
    public Matrix asinh(Ret returnType) throws MatrixException {
        return inst(impl.asinh(returnType));
    }

    //@Override
    public Matrix acosh(Ret returnType) throws MatrixException {
        return inst(impl.acosh(returnType));
    }

    //@Override
    public Matrix atan(Ret returnType) throws MatrixException {
        return inst(impl.atan(returnType));
    }

    //@Override
    public Matrix atanh(Ret returnType) throws MatrixException {
        return inst(impl.atanh(returnType));
    }

    @Override
    public Matrix minus(Ret returnType, boolean ignoreNaN, Matrix m) throws MatrixException {
        return inst(impl.minus(returnType, ignoreNaN, ((CLDenseMatrix2D)m).getImpl()));
    }
    @Override
    public Matrix minus(Ret returnType, boolean ignoreNaN, double v) throws MatrixException {
        return inst(impl.minus(returnType, ignoreNaN, v));
    }

    @Override
    public Matrix plus(Ret returnType, boolean ignoreNaN, Matrix m) throws MatrixException {
        return inst(impl.plus(returnType, ignoreNaN, ((CLDenseMatrix2D)m).getImpl()));
    }

    @Override
    public Matrix plus(Ret returnType, boolean ignoreNaN, double v) throws MatrixException {
        return inst(impl.plus(returnType, ignoreNaN, v));
    }

    @Override
    public Matrix times(Ret returnType, boolean ignoreNaN, Matrix factor) throws MatrixException {
        return inst(impl.times(returnType, ignoreNaN, ((CLDenseMatrix2D)factor).getImpl()));
    }

    @Override
    public Matrix times(Ret returnType, boolean ignoreNaN, double factor) throws MatrixException {
        return inst(impl.times(returnType, ignoreNaN, factor));
    }

    @Override
    public Matrix divide(Ret returnType, boolean ignoreNaN, Matrix factor) throws MatrixException {
        return inst(impl.divide(returnType, ignoreNaN, ((CLDenseMatrix2D)factor).getImpl()));
    }

    @Override
    public Matrix divide(Ret returnType, boolean ignoreNaN, double factor) throws MatrixException {
        return inst(impl.divide(returnType, ignoreNaN, factor));
    }

    @Override
    public double[][] toDoubleArray() throws MatrixException {
        Pointer<Double> b = impl.read();
        double[][] ret = new double[(int)impl.rows][];
        for (int i = 0; i < impl.rows; i++) {
            ret[i] = b.getDoubles(i * impl.columns, (int)impl.columns);
        }
        return ret;
    }

    @Override
    public boolean containsDouble(double v) {
        try {
            return impl.containsValue(v);
        } catch (CLBuildException ex) {
            throw new RuntimeException("Failed to test value presence", ex);
        }
    }

    @Override
    public void clear() {
        try {
            impl.clear();
        } catch (CLBuildException ex) {
            throw new RuntimeException("Failed to clear matrix", ex);
        }
    }

    public void waitFor() {
        impl.waitFor();
    }



}

