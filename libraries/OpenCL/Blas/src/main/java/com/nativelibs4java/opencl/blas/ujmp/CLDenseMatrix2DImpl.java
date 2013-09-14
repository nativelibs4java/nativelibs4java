/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.blas.ujmp;

import com.nativelibs4java.opencl.CLBuildException;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.blas.CLEvents;
import com.nativelibs4java.opencl.blas.CLKernels;
import com.nativelibs4java.opencl.blas.CLMatrix2D;
import com.nativelibs4java.opencl.blas.CLMatrixUtils;
import com.nativelibs4java.opencl.util.Fun1;
import com.nativelibs4java.opencl.util.Fun2;
import com.nativelibs4java.opencl.util.ReductionUtils;
import com.nativelibs4java.opencl.util.ReductionUtils.Reductor;

import org.bridj.Pointer;
import org.ujmp.core.calculation.Calculation.Ret;
import org.ujmp.core.exceptions.MatrixException;
import org.ujmp.core.matrix.Matrix2D;

import static org.bridj.Pointer.allocate;

/**
 *
 * @author ochafik
 */
public class CLDenseMatrix2DImpl<V> {
    protected final CLMatrix2D<V> _matrix;
    protected final long rows, columns, stride, size[];
    protected Pointer<V> cache;
    protected int uncachedGetCount;
    protected static final int GET_COUNT_BEFORE_CACHING = 3;

    public CLDenseMatrix2DImpl(CLMatrix2D<V> _matrix) {
        this._matrix = _matrix;
        this.rows = _matrix.getRowCount();
        this.columns = _matrix.getColumnCount();
        this.stride = _matrix.getStride();
        this.size = new long[] { rows, columns };
        _matrix.getEvents().addListener(new CLEvents.Listener() {
            public void writing(CLEvents evts) {
                synchronized (CLDenseMatrix2DImpl.this) {
                    cache = null;
                    uncachedGetCount = 0;
                }
            }

            public void reading(CLEvents evts) {}
        });
    }

    protected CLMatrix2D<V> getMatrix() {
        return _matrix;
    }

    protected long getStorageIndex(long row, long column) {
        return stride * row + column;
    }

    protected synchronized void cache() {
        if (cache != null)
            return;

        cache = read();
        uncachedGetCount = 0;
    }
    public V get(long row, long column) {
        final long offset = getStorageIndex(row, column);
        synchronized (this) {
            if (uncachedGetCount >= GET_COUNT_BEFORE_CACHING)
                cache();

            if (cache != null)
                return cache.get(offset);
            else
                uncachedGetCount++;
        }
        final Pointer<V> out = allocate(getMatrix().getPrimitiveClass()).order(getMatrix().getContext().getByteOrder());
        getMatrix().getEvents().performRead(new CLEvents.Action() {
            public CLEvent perform(CLEvent[] events) {
                return getMatrix().getBuffer().read(getMatrix().getQueue(), offset, 1, out, true, events);
            }
        });
        return out.get();
    }
    public CLDenseMatrix2DImpl<V> clone() {
        return new CLDenseMatrix2DImpl<V>(CLMatrixUtils.clone(getMatrix()));
    }
    public void waitFor() {
        getMatrix().getEvents().waitFor();
    }
    public void write(Pointer<V> p) {
        getMatrix().write(p);
    }
    public void read(Pointer<V> p) {
        synchronized (this) {
            if (cache != null) {
                cache.copyTo(p);
                return;
            }
        }
        getMatrix().read(p);
    }
    public Pointer<V> read() {
        synchronized (this) {
            if (cache != null)
                return cache.clone();
        }
        Pointer<V> b = (Pointer)Pointer.allocateArray(getMatrix().getPrimitiveClass(), rows * stride);
        getMatrix().read(b);
        return b;
    }
    
    public void set(V value, long row, long column) {
        final long offset = getStorageIndex(row, column);
        final Pointer<V> in = allocate(getMatrix().getPrimitiveClass()).order(getMatrix().getContext().getByteOrder());
        in.set(value);
        getMatrix().getEvents().performWrite(new CLEvents.Action() {
            public CLEvent perform(CLEvent[] events) {
                return getMatrix().getBuffer().write(getMatrix().getQueue(), offset, 1, in, false, events);
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
        return new CLDenseMatrix2DImpl<V>(CLMatrixUtils.clone(getMatrix()));
    }
    
    public CLDenseMatrix2DImpl<V> multiply(Ret returnType, boolean ignoreNaN, Matrix2D matrix) throws MatrixException {
        CLKernels clUJMP = getMatrix().getKernels();
        CLMatrix2D<V> 
            in1 = getMatrix(),
            in2 = CLWrappedMatrix2D.wrap(matrix, clUJMP),
            out = returnType == Ret.ORIG ? in1 : in1.blankMatrix(in1.getRowCount(), in2.getColumnCount());

        CLMatrixUtils.matrixMultiply(in1, in2, out);
        return new CLDenseMatrix2DImpl(out);
    }
    public CLDenseMatrix2DImpl<V> transpose(Ret returnType) throws MatrixException {
        CLMatrix2D<V> 
            in = getMatrix(),
            out = returnType == Ret.ORIG ? in : in.blankMatrix(columns, rows);
        CLMatrixUtils.matrixTranspose(in, out);
        return new CLDenseMatrix2DImpl(out);
    }

    public long[] getSize() {
        return size;
    }

    public static <V> CLDenseMatrix2DImpl<V> op1(final CLMatrix2D<V> in, final Fun1 fun, Ret returnType) throws MatrixException {
        final CLMatrix2D<V> out = returnType == Ret.ORIG ? in : in.blankClone();
        return new CLDenseMatrix2DImpl(CLMatrixUtils.op1(in, fun, out));
    }
    public CLDenseMatrix2DImpl<V> sin(Ret returnType) throws MatrixException {
        return op1(getMatrix(), Fun1.sin, returnType);
    }

    
    public CLDenseMatrix2DImpl<V> cos(Ret returnType) throws MatrixException {
        return op1(getMatrix(), Fun1.cos, returnType);
    }

    
    public CLDenseMatrix2DImpl<V> sinh(Ret returnType) throws MatrixException {
        return op1(getMatrix(), Fun1.sinh, returnType);
    }

    
    public CLDenseMatrix2DImpl<V> cosh(Ret returnType) throws MatrixException {
        return op1(getMatrix(), Fun1.cosh, returnType);
    }

    
    public CLDenseMatrix2DImpl<V> tan(Ret returnType) throws MatrixException {
        return op1(getMatrix(), Fun1.tan, returnType);
    }

    
    public CLDenseMatrix2DImpl<V> tanh(Ret returnType) throws MatrixException {
        return op1(getMatrix(), Fun1.tanh, returnType);
    }


    public CLDenseMatrix2DImpl<V> asin(Ret returnType) throws MatrixException {
        return op1(getMatrix(), Fun1.asin, returnType);
    }


    public CLDenseMatrix2DImpl<V> acos(Ret returnType) throws MatrixException {
        return op1(getMatrix(), Fun1.acos, returnType);
    }


    public CLDenseMatrix2DImpl<V> asinh(Ret returnType) throws MatrixException {
        return op1(getMatrix(), Fun1.asinh, returnType);
    }


    public CLDenseMatrix2DImpl<V> acosh(Ret returnType) throws MatrixException {
        return op1(getMatrix(), Fun1.acosh, returnType);
    }


    public CLDenseMatrix2DImpl<V> atan(Ret returnType) throws MatrixException {
        return op1(getMatrix(), Fun1.atan, returnType);
    }


    public CLDenseMatrix2DImpl<V> atanh(Ret returnType) throws MatrixException {
        return op1(getMatrix(), Fun1.atanh, returnType);
    }

    public static <V> CLDenseMatrix2DImpl<V> op2(CLMatrix2D<V> in1, Fun2 fun, CLMatrix2D<V> in2, Ret returnType) throws MatrixException {
        final CLMatrix2D<V> out = returnType == Ret.ORIG ? in1 : in1.blankClone();
        return new CLDenseMatrix2DImpl(CLMatrixUtils.op2(in1, fun, in2, out));
    }
    public static <V> CLDenseMatrix2DImpl<V> op2(CLMatrix2D<V> in1, Fun2 fun, V in2, Ret returnType) throws MatrixException {
        final CLMatrix2D<V> out = returnType == Ret.ORIG ? in1 : in1.blankClone();
        return new CLDenseMatrix2DImpl(CLMatrixUtils.op2(in1, fun, in2, out));
    }
    

    public CLDenseMatrix2DImpl<V> minus(Ret returnType, boolean ignoreNaN, CLDenseMatrix2DImpl<V> m) throws MatrixException {
        return op2(getMatrix(), Fun2.substract, m.getMatrix(), returnType);
    }

    public CLDenseMatrix2DImpl<V> minus(Ret returnType, boolean ignoreNaN, V v) throws MatrixException {
        return op2(getMatrix(), Fun2.substract, v, returnType);
    }

    public CLDenseMatrix2DImpl<V> plus(Ret returnType, boolean ignoreNaN, CLDenseMatrix2DImpl<V> m) throws MatrixException {
        return op2(getMatrix(), Fun2.add, m.getMatrix(), returnType);
    }

    public CLDenseMatrix2DImpl<V> plus(Ret returnType, boolean ignoreNaN, V v) throws MatrixException {
        return op2(getMatrix(), Fun2.add, v, returnType);
    }

    public CLDenseMatrix2DImpl<V> times(Ret returnType, boolean ignoreNaN, CLDenseMatrix2DImpl<V> factor) throws MatrixException {
        return op2(getMatrix(), Fun2.multiply, factor.getMatrix(), returnType);
    }

    public CLDenseMatrix2DImpl<V> times(Ret returnType, boolean ignoreNaN, V factor) throws MatrixException {
        return op2(getMatrix(), Fun2.multiply, factor, returnType);
    }

    public CLDenseMatrix2DImpl<V> divide(Ret returnType, boolean ignoreNaN, CLDenseMatrix2DImpl<V> factor) throws MatrixException {
        return op2(getMatrix(), Fun2.divide, factor.getMatrix(), returnType);
    }

    public CLDenseMatrix2DImpl<V> divide(Ret returnType, boolean ignoreNaN, V factor) throws MatrixException {
        return op2(getMatrix(), Fun2.divide, factor, returnType);
    }
    //protected abstract Matrix createMatrix(CLMatrixImpl<V> mi);


    static final int MAX_REDUCTION_SIZE = 32;
    volatile Reductor<V> minReductor;
    public CLDenseMatrix2DImpl<V> min() throws CLBuildException {
        synchronized (this) {
            if (minReductor == null)
                minReductor = ReductionUtils.createReductor(getMatrix().getContext(), ReductionUtils.Operation.Min, getMatrix().getPrimitive().oclType, 1);
        }
        CLMatrix2D<V> out = getMatrix().blankMatrix(1, 1);
        CLMatrixUtils.reduce(getMatrix(), out, minReductor);
        return new CLDenseMatrix2DImpl<V>(out);
    }
    volatile Reductor<V> maxReductor;
    public CLDenseMatrix2DImpl<V> max() throws CLBuildException {
        synchronized (this) {
            if (maxReductor == null)
                maxReductor = ReductionUtils.createReductor(getMatrix().getContext(), ReductionUtils.Operation.Max, getMatrix().getPrimitive().oclType, 1);
        }
        CLMatrix2D<V> 
            in = getMatrix(),
            out = in.blankMatrix(1, 1);
        CLMatrixUtils.reduce(in, out, minReductor);
        return new CLDenseMatrix2DImpl<V>(out);
    }

    public boolean containsValue(final V value) throws CLBuildException {
        final boolean ret[] = new boolean[1];
        getMatrix().getEvents().performRead(new CLEvents.Action() {
            public CLEvent perform(CLEvent[] events) {
                ret[0] = getMatrix().getKernels().containsValue(getMatrix().getPrimitive(), getMatrix().getBuffer(), getMatrix().getBuffer().getElementCount(), value, events);
                return null;
            }
        });
        return ret[0];
    }

    public void clear() throws CLBuildException {
        getMatrix().getEvents().performWrite(new CLEvents.Action() {
            public CLEvent perform(CLEvent[] events) {
                return getMatrix().getKernels().clear(getMatrix().getPrimitive(), getMatrix().getBuffer(), getMatrix().getBuffer().getElementCount(), events);
            }
        });
    }

    public CLQueue getQueue() {
        return getMatrix().getQueue();
    }

}
