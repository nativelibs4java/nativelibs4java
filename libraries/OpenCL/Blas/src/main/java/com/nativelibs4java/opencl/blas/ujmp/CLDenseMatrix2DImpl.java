/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.blas.ujmp;

import org.bridj.Pointer;
import static org.bridj.Pointer.*;

import org.ujmp.core.Matrix;

import org.ujmp.core.calculation.Calculation.Ret;
import org.ujmp.core.exceptions.MatrixException;
import com.nativelibs4java.opencl.*;
import com.nativelibs4java.opencl.util.Fun1;
import com.nativelibs4java.opencl.util.Fun2;
import com.nativelibs4java.opencl.util.OpenCLType;
import com.nativelibs4java.opencl.util.Primitive;
import com.nativelibs4java.opencl.util.ReductionUtils;
import com.nativelibs4java.opencl.util.ReductionUtils.Reductor;
import org.ujmp.core.doublematrix.DoubleMatrix2D;

/**
 *
 * @author ochafik
 */
public class CLDenseMatrix2DImpl<V> {
    protected final CLMatrix2D<V> matrix;
    protected final long rows, columns, size[];
    public CLDenseMatrix2DImpl(CLMatrix2D<V> matrix) {
        this.matrix = matrix;
        this.rows = matrix.getRowCount();
        this.columns = matrix.getColumnCount();
        this.size = new long[] { rows, columns };
    }

    public CLMatrix2D<V> getMatrix() {
        return matrix;
    }
    
    protected long getStorageIndex(long row, long column) {
        return row * columns + column;
    }

    public V get(long row, long column) {
        final long offset = getStorageIndex(row, column);
        final Pointer<V> out = allocate(matrix.getPrimitiveClass()).order(matrix.getContext().getByteOrder());
        matrix.getEvents().performRead(new CLEvents.Action() {
            public CLEvent perform(CLEvent[] events) {
                return matrix.getBuffer().read(matrix.getQueue(), offset, 1, out, true, events);
            }
        });
        return out.get();
    }

    public void set(V value, long row, long column) {
        final long offset = getStorageIndex(row, column);
        final Pointer<V> in = allocate(matrix.getPrimitiveClass()).order(matrix.getContext().getByteOrder());
        in.set(value);
        matrix.getEvents().performWrite(new CLEvents.Action() {
            public CLEvent perform(CLEvent[] events) {
                return matrix.getBuffer().write(matrix.getQueue(), offset, 1, in, false, events);
            }
        });
    }

    /*
    public Matrix mtimes(Matrix matrix) throws MatrixException {
        if (matrix instanceof DoubleMatrix2D) {
            CLMatrix2D<V> 
                b = CLMatrixUtils.asInputMatrix((DoubleMatrix2D)matrix, queue, context),
                out = blankMatrix(getRowCount(), matrix.getColumnCount());

            CLMatrixUtils.mtimes(this, b, out, primitive, clUJMP);
            return (Matrix)out;
        } else {
            return super.mtimes(matrix);
        }
    }
     */


    public CLDenseMatrix2DImpl<V> copy() throws MatrixException {
        return new CLDenseMatrix2DImpl<V>(CLMatrixUtils.clone(matrix));
    }
    
    public CLDenseMatrix2DImpl<V> transpose(Ret returnType) throws MatrixException {
        CLMatrix2D<V> 
            in = matrix,
            out = returnType == Ret.ORIG ? matrix : matrix.blankMatrix(columns, rows);
        CLMatrixUtils.matrixTranspose(in, out);
        return new CLDenseMatrix2DImpl(out);
    }

    public long[] getSize() {
        return size;
    }

    public Pointer<V> read() {
        Pointer<V> b = (Pointer)Pointer.allocateArray(matrix.getPrimitiveClass(), rows * columns);
        matrix.read(b);
        return b;
    }
    
    public static <V> CLDenseMatrix2DImpl<V> op1(final CLMatrix2D<V> in, final Fun1 fun, Ret returnType) throws MatrixException {
        return new CLDenseMatrix2DImpl(CLMatrixUtils.op1(in, fun, returnType));
    }
    public CLDenseMatrix2DImpl<V> sin(Ret returnType) throws MatrixException {
        return op1(matrix, Fun1.sin, returnType);
    }

    
    public CLDenseMatrix2DImpl<V> cos(Ret returnType) throws MatrixException {
        return op1(matrix, Fun1.cos, returnType);
    }

    
    public CLDenseMatrix2DImpl<V> sinh(Ret returnType) throws MatrixException {
        return op1(matrix, Fun1.sinh, returnType);
    }

    
    public CLDenseMatrix2DImpl<V> cosh(Ret returnType) throws MatrixException {
        return op1(matrix, Fun1.cosh, returnType);
    }

    
    public CLDenseMatrix2DImpl<V> tan(Ret returnType) throws MatrixException {
        return op1(matrix, Fun1.tan, returnType);
    }

    
    public CLDenseMatrix2DImpl<V> tanh(Ret returnType) throws MatrixException {
        return op1(matrix, Fun1.tanh, returnType);
    }


    public CLDenseMatrix2DImpl<V> asin(Ret returnType) throws MatrixException {
        return op1(matrix, Fun1.asin, returnType);
    }


    public CLDenseMatrix2DImpl<V> acos(Ret returnType) throws MatrixException {
        return op1(matrix, Fun1.acos, returnType);
    }


    public CLDenseMatrix2DImpl<V> asinh(Ret returnType) throws MatrixException {
        return op1(matrix, Fun1.asinh, returnType);
    }


    public CLDenseMatrix2DImpl<V> acosh(Ret returnType) throws MatrixException {
        return op1(matrix, Fun1.acosh, returnType);
    }


    public CLDenseMatrix2DImpl<V> atan(Ret returnType) throws MatrixException {
        return op1(matrix, Fun1.atan, returnType);
    }


    public CLDenseMatrix2DImpl<V> atanh(Ret returnType) throws MatrixException {
        return op1(matrix, Fun1.atanh, returnType);
    }

    public static <V> CLDenseMatrix2DImpl<V> op2(CLMatrix2D<V> in1, Fun2 fun, CLMatrix2D<V> in2, Ret returnType) throws MatrixException {
        return new CLDenseMatrix2DImpl(CLMatrixUtils.op2(in1, fun, in2, returnType));
    }
    public static <V> CLDenseMatrix2DImpl<V> op2(CLMatrix2D<V> in1, Fun2 fun, V in2, Ret returnType) throws MatrixException {
        return new CLDenseMatrix2DImpl(CLMatrixUtils.op2(in1, fun, in2, returnType));
    }
    

    public CLDenseMatrix2DImpl<V> minus(Ret returnType, boolean ignoreNaN, CLDenseMatrix2DImpl<V> m) throws MatrixException {
        return op2(matrix, Fun2.substract, m.getMatrix(), returnType);
    }

    public CLDenseMatrix2DImpl<V> minus(Ret returnType, boolean ignoreNaN, V v) throws MatrixException {
        return op2(matrix, Fun2.substract, v, returnType);
    }

    public CLDenseMatrix2DImpl<V> plus(Ret returnType, boolean ignoreNaN, CLDenseMatrix2DImpl<V> m) throws MatrixException {
        return op2(matrix, Fun2.add, m.getMatrix(), returnType);
    }

    public CLDenseMatrix2DImpl<V> plus(Ret returnType, boolean ignoreNaN, V v) throws MatrixException {
        return op2(matrix, Fun2.add, v, returnType);
    }

    public CLDenseMatrix2DImpl<V> times(Ret returnType, boolean ignoreNaN, CLDenseMatrix2DImpl<V> factor) throws MatrixException {
        return op2(matrix, Fun2.multiply, factor.getMatrix(), returnType);
    }

    public CLDenseMatrix2DImpl<V> times(Ret returnType, boolean ignoreNaN, V factor) throws MatrixException {
        return op2(matrix, Fun2.multiply, factor, returnType);
    }

    public CLDenseMatrix2DImpl<V> divide(Ret returnType, boolean ignoreNaN, CLDenseMatrix2DImpl<V> factor) throws MatrixException {
        return op2(matrix, Fun2.divide, factor.getMatrix(), returnType);
    }

    public CLDenseMatrix2DImpl<V> divide(Ret returnType, boolean ignoreNaN, V factor) throws MatrixException {
        return op2(matrix, Fun2.divide, factor, returnType);
    }
    //protected abstract Matrix createMatrix(CLMatrixImpl<V> mi);


    static final int MAX_REDUCTION_SIZE = 32;
    volatile Reductor<V> minReductor;
    public CLDenseMatrix2DImpl<V> min() throws CLBuildException {
        synchronized (this) {
            if (minReductor == null)
                minReductor = ReductionUtils.createReductor(matrix.getContext(), ReductionUtils.Operation.Min, matrix.getPrimitive().oclType, 1);
        }
        CLMatrix2D<V> out = matrix.blankMatrix(1, 1);
        CLMatrixUtils.reduce(matrix, out, minReductor);
        return new CLDenseMatrix2DImpl<V>(out);
    }
    volatile Reductor<V> maxReductor;
    public CLDenseMatrix2DImpl<V> max() throws CLBuildException {
        synchronized (this) {
            if (maxReductor == null)
                maxReductor = ReductionUtils.createReductor(matrix.getContext(), ReductionUtils.Operation.Max, matrix.getPrimitive().oclType, 1);
        }
        CLMatrix2D<V> out = matrix.blankMatrix(1, 1);
        CLMatrixUtils.reduce(matrix, out, minReductor);
        return new CLDenseMatrix2DImpl<V>(out);
    }

    boolean containsValue(final V value) throws CLBuildException {
        final boolean ret[] = new boolean[1];
        matrix.getEvents().performRead(new CLEvents.Action() {
            public CLEvent perform(CLEvent[] events) {
                ret[0] = matrix.getCLUJMP().containsValue(matrix.getPrimitive(), matrix.getBuffer(), matrix.getBuffer().getElementCount(), value, events);
                return null;
            }
        });
        return ret[0];
    }

    void clear() throws CLBuildException {
        matrix.getEvents().performWrite(new CLEvents.Action() {
            public CLEvent perform(CLEvent[] events) {
                return matrix.getCLUJMP().clear(matrix.getPrimitive(), matrix.getBuffer(), matrix.getBuffer().getElementCount(), events);
            }
        });
    }

}
