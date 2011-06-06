/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.blas.ujmp;

import com.nativelibs4java.opencl.blas.ujmp.CLEvents.Action;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ujmp.core.Matrix;
import org.ujmp.core.calculation.Calculation.Ret;
import org.ujmp.core.doublematrix.DoubleMatrix2D;
import org.ujmp.core.exceptions.MatrixException;

import com.nativelibs4java.opencl.CLBuildException;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLMem.MapFlags;
import com.nativelibs4java.opencl.CLMem.Usage;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.util.LinearAlgebraUtils;
import com.nativelibs4java.opencl.util.Primitive;
import com.nativelibs4java.util.NIOUtils;
import java.nio.Buffer;
import org.bridj.Pointer;
import org.ujmp.core.doublematrix.stub.AbstractDenseDoubleMatrix2D;

/**
 *
 * @author ochafik
 */
public class CLDenseDoubleMatrix2D extends AbstractDenseDoubleMatrix2D {
	
    protected final CLDenseMatrix2DImpl<Double> impl;

    public CLDenseMatrix2DImpl getImpl() {
        return impl;
    }
    CLDenseDoubleMatrix2D(CLDenseMatrix2DImpl impl) {
        this.impl = impl;
    }
    CLDenseDoubleMatrix2D(CLMatrix2D<Double> matrix) {
        this(new CLDenseMatrix2DImpl<Double>(matrix));
    }
    public CLDenseDoubleMatrix2D(long rows, long columns, OpenCLUJMP clUJMP) {
        this(new CLDefaultMatrix2D(Primitive.Double, null, rows, columns, clUJMP));
    }
    public CLDenseDoubleMatrix2D(long... size) {
        this(size[0], size[1], OpenCLUJMP.getInstance());
    }
    
    public void write(Pointer<Double> p) {
        getImpl().getMatrix().write(p);
    }

    public void read(Pointer<Double> p) {
        getImpl().getMatrix().read(p);
    }
    
    public Pointer<Double> read() {
        return getImpl().getMatrix().read();
    }

    static CLDenseDoubleMatrix2D inst(CLMatrix2D<Double> matrix) {
        return new CLDenseDoubleMatrix2D(matrix);
    }
    
    static CLDenseDoubleMatrix2D inst(CLDenseMatrix2DImpl matrix) {
        return new CLDenseDoubleMatrix2D(matrix);
    }
    
    @Override
    public Matrix mtimes(Ret returnType, boolean ignoreNaN, Matrix matrix) throws MatrixException {
        if (matrix instanceof DoubleMatrix2D) {
            OpenCLUJMP clUJMP = getImpl().getMatrix().getCLUJMP();
            CLMatrix2D<Double> 
                in1 = getImpl().getMatrix(),
                in2 = CLWrappedMatrix2D.wrap((DoubleMatrix2D)matrix, clUJMP),
                out = returnType == Ret.ORIG ? in1 : CLMatrixUtils.createMatrix(in1.getRowCount(), in2.getColumnCount(), Double.class, clUJMP);

            CLMatrixUtils.matrixMultiply(in1, in2, out);
            return inst(out);
        } else {
            return super.mtimes(matrix);
        }
    }
    
    @Override
    public Matrix mtimes(Matrix matrix) throws MatrixException {
        return mtimes(Ret.NEW, true, matrix);
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
    public synchronized Matrix copy() throws MatrixException {
        return inst(CLMatrixUtils.clone(impl.getMatrix()));
    }

    @Override
    public Matrix transpose(Ret returnType) throws MatrixException {
        return inst(impl.transpose(returnType));
    }
    
    @Override
    public synchronized Matrix transpose() throws MatrixException {
        return transpose(Ret.NEW);
    }

    public long[] getSize() {
        return impl.getSize();
    }

    public double getDouble(long row, long column) {
        return impl.get(row, column);
    }
    public void setDouble(double value, long row, long column) {
        impl.set(value, row, column);
    }
    
    public double getDouble(int row, int column) {
        return getDouble((long)row, (long)column);
    }

    public void setDouble(double value, int row, int column) {
        setDouble(value, (long)row, (long)column);
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
        int rows = (int)impl.rows, columns = (int)impl.columns;
        double[][] ret = new double[rows][];
        for (int i = 0; i < rows; i++) {
            ret[i] = b.getDoublesAtOffset(i * columns, columns);
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
        impl.getMatrix().getEvents().waitFor();
    }
}
