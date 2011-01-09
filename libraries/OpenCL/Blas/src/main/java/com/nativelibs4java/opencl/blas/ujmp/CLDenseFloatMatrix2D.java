/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.blas.ujmp;

import com.nativelibs4java.opencl.CLBuildException;
import com.nativelibs4java.opencl.util.Primitive;
import org.bridj.Pointer;

import org.ujmp.core.Matrix;
import org.ujmp.core.calculation.Calculation.Ret;
import org.ujmp.core.exceptions.MatrixException;

import org.ujmp.core.floatmatrix.stub.AbstractDenseFloatMatrix2D;

/**
 *
 * @author ochafik
 */
public class CLDenseFloatMatrix2D extends AbstractDenseFloatMatrix2D implements CLDenseMatrix2D<Float> {
	private static final long serialVersionUID = -36941159548127670L;
	protected final CLDenseMatrix2DImpl<Float> impl;

    public CLDenseFloatMatrix2D(OpenCLUJMP clUJMP, long rows, long columns) {
        this(new CLDenseMatrix2DImpl<Float>(Primitive.Float, null, rows, columns, clUJMP));
    }

    public CLDenseFloatMatrix2D(long... size) {
        this(CLDenseFloatMatrix2DFactory.getOpenCLUJMP(), size[0], size[1]);
    }

    public CLDenseFloatMatrix2D(long size) {
        this(size, size);
    }

    protected CLDenseFloatMatrix2D(CLDenseMatrix2DImpl<Float> impl) {
        this.impl = impl;
    }

    @Override
    public long[] getSize() {
        return new long[] { impl.rows, impl.columns };
    }

    public void write(Pointer<Float> b) {
        impl.write(b);
    }

    public void read(Pointer<Float> b) {
        impl.read(b);
    }

    public Pointer<Float> read() {
        return impl.read();
    }

    @Override
    public float getFloat(int row, int column) {
        return getFloat((long)row, (long)column);
    }

    @Override
    public void setFloat(float value, int row, int column) {
        setFloat(value, (long)row, (long)column);
    }

    @Override
    public CLDenseMatrix2DImpl<Float> getImpl() {
        throw new UnsupportedOperationException("Not supported yet.");
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
                return minus(returnType, ignoreNaN, mean(Ret.NEW, dimension, ignoreNaN).getAsFloat(0, 0));
            default:
                throw new IllegalArgumentException("Invalid dimension : " + dimension);
        }
    }

    @Override
    public float getFloat(long row, long column) {
        return impl.get(row, column);
    }

    @Override
    public void setFloat(float value, long row, long column) {
        impl.set(value, row, column);
    }

    static CLDenseFloatMatrix2D inst(CLDenseMatrix2DImpl<Float> impl) {
        return new CLDenseFloatMatrix2D(impl);
    }

    @Override
    public Matrix mtimes(Ret returnType, boolean ignoreNaN, Matrix matrix) throws MatrixException {
        return inst(impl.multiplyMatrix(((CLDenseMatrix2D<Float>)matrix).getImpl()));
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

    @Override
    public Matrix minus(Ret returnType, boolean ignoreNaN, Matrix m) throws MatrixException {
        return inst(impl.minus(returnType, ignoreNaN, ((CLDenseMatrix2D)m).getImpl()));
    }
    @Override
    public Matrix minus(Ret returnType, boolean ignoreNaN, double v) throws MatrixException {
        return inst(impl.minus(returnType, ignoreNaN, (float)v));
    }

    @Override
    public Matrix plus(Ret returnType, boolean ignoreNaN, Matrix m) throws MatrixException {
        return inst(impl.plus(returnType, ignoreNaN, ((CLDenseMatrix2D)m).getImpl()));
    }

    @Override
    public Matrix plus(Ret returnType, boolean ignoreNaN, double v) throws MatrixException {
        return inst(impl.plus(returnType, ignoreNaN, (float)v));
    }

    @Override
    public Matrix times(Ret returnType, boolean ignoreNaN, Matrix factor) throws MatrixException {
        return inst(impl.times(returnType, ignoreNaN, ((CLDenseMatrix2D)factor).getImpl()));
    }

    @Override
    public Matrix times(Ret returnType, boolean ignoreNaN, double factor) throws MatrixException {
        return inst(impl.times(returnType, ignoreNaN, (float)factor));
    }

    @Override
    public Matrix divide(Ret returnType, boolean ignoreNaN, Matrix factor) throws MatrixException {
        return inst(impl.divide(returnType, ignoreNaN, ((CLDenseMatrix2D)factor).getImpl()));
    }

    @Override
    public Matrix divide(Ret returnType, boolean ignoreNaN, double factor) throws MatrixException {
        return inst(impl.divide(returnType, ignoreNaN, (float)factor));
    }

    @Override
    public float[][] toFloatArray() throws MatrixException {
        Pointer<Float> b = impl.read();
        float[][] ret = new float[(int)impl.rows][];
        for (int i = 0; i < impl.rows; i++) {
            ret[i] = b.getFloatsAtOffset(i * impl.columns, (int)impl.columns);
        }
        return ret;
    }

    @Override
    public double[][] toDoubleArray() throws MatrixException {
        Pointer<Float> b = impl.read();
        double[][] ret = new double[(int)impl.rows][(int)impl.columns];
        for (int i = 0; i < impl.rows; i++) {
            float[] floats = b.getFloatsAtOffset(i * impl.columns, (int) impl.columns);
            double[] doubles = ret[i];
            for (int j = 0; j < floats.length; j++)
                doubles[j] = floats[j];
        }
        return ret;
    }

}

