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
import org.ujmp.core.doublematrix.DoubleMatrix2D;
import org.ujmp.core.exceptions.MatrixException;

import com.nativelibs4java.opencl.CLBuildException;
import com.nativelibs4java.opencl.CLDoubleBuffer;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLMem.MapFlags;
import com.nativelibs4java.opencl.CLMem.Usage;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.util.LinearAlgebraUtils;
import com.nativelibs4java.util.NIOUtils;
import java.nio.Buffer;
import org.ujmp.core.doublematrix.stub.AbstractDenseDoubleMatrix2D;

/**
 *
 * @author ochafik
 */
public class CLDenseDoubleMatrix2D extends AbstractDenseDoubleMatrix2D implements CLMatrix2D<Double> {
	private static final long serialVersionUID = -36941159548127670L;
	protected final LinearAlgebraUtils kernels;
    protected final CLBuffer<Double> buffer;
    protected CLQueue queue;
    protected final CLContext context;
    protected final long rows, columns;
    //protected DoubleBuffer data;

    CLEvents _events = new CLEvents();

    public CLEvents getEvents() {
        return _events;
    }

    @Override
    public long getRowCount() {
        return rows;
    }

    @Override
    public long getColumnCount() {
        return columns;
    }

    
    
    private final static CLEvent[] EMPTY_EVENTS = new CLEvent[0];

	public CLDenseDoubleMatrix2D(CLBuffer<Double> buffer, long rows, long columns, LinearAlgebraUtils kernels) {
        this.rows = rows;
        this.columns = columns;
        this.buffer = buffer;
        this.kernels = kernels;
        queue = kernels.getQueue();
        context = queue.getContext();
    }

    public CLDenseDoubleMatrix2D(long rows, long columns, LinearAlgebraUtils kernels) {
        this(kernels.getContext().createDoubleBuffer(Usage.InputOutput, rows * columns), rows, columns, kernels);
    }

    public CLDenseDoubleMatrix2D(long rows, long columns) {
        this(rows, columns, CLDenseDoubleMatrix2DFactory.getLinearAlgebraKernels());
    }

    public CLDenseDoubleMatrix2D(long[] size) {
        this(size[0], size[1]);
        if (size.length != 2)
            throw new IllegalArgumentException("Size is not 2D !");
    }

    protected long getStorageIndex(long row, long column) {
        return row * columns + column;
    }

    @Override
    public double getDouble(long row, long column) {
        final long offset = getStorageIndex(row, column);
        final DoubleBuffer out = NIOUtils.directDoubles(1, context.getByteOrder());
        getEvents().performRead(new CLEvents.Action() {
            public CLEvent perform(CLEvent[] events) {
                return buffer.read(queue, offset, 1, out, true, events);
            }
        });
        return out.get();
    }

    @Override
    public void setDouble(double value, long row, long column) {
        final long offset = getStorageIndex(row, column);
        final DoubleBuffer in = NIOUtils.directDoubles(1, context.getByteOrder());
        in.put(0, value);
        getEvents().performWrite(new CLEvents.Action() {
            public CLEvent perform(CLEvent[] events) {
                return buffer.write(queue, offset, 1, in, false, events);
            }
        });
        
    }

    @Override
    public Matrix mtimes(Matrix matrix) throws MatrixException {
        if (matrix instanceof DoubleMatrix2D) {
            CLMatrix2D<Double> 
                b = CLMatrixUtils.asInputMatrix((DoubleMatrix2D)matrix, queue, context),
                out = new CLDenseDoubleMatrix2D(getRowCount(), matrix.getColumnCount());

            CLMatrixUtils.mtimes(this, b, out, kernels);
            return (Matrix)out;
        } else {
            return super.mtimes(matrix);
        }
    }



    @Override
    public synchronized Matrix copy() throws MatrixException {
        return (Matrix)CLMatrixUtils.clone(this, queue);
    }
    
    @Override
    public synchronized Matrix transpose() throws MatrixException {
        CLDenseDoubleMatrix2D out = new CLDenseDoubleMatrix2D(columns, rows, kernels);
        CLMatrixUtils.transpose(this, out, kernels);
        return out;
    }

    public void write(final DoubleBuffer b) {
        getEvents().performWrite(new CLEvents.Action() {

            public CLEvent perform(CLEvent[] events) {
                return buffer.write(queue, b, false, events);
            }
        });
    }

    public void read(final DoubleBuffer b) {
        getEvents().performRead(new CLEvents.Action() {

            public CLEvent perform(CLEvent[] events) {
                return buffer.read(queue, b, true, events);
            }
        });
    }

    public CLBuffer<Double> getBuffer() {
        return buffer;
    }

    public CLContext getContext() {
        return context;
    }

    public synchronized CLQueue getQueue() {
        return queue;
    }

    public synchronized void setQueue(CLQueue queue) {
        if (this.queue != null && queue != null) {
            if (this.queue.equals(queue))
                return;
        }
        getEvents().waitFor();
        this.queue = queue;
    }

    public CLDenseDoubleMatrix2D blankClone() {
        return new CLDenseDoubleMatrix2D(getRowCount(), getColumnCount(), kernels);
    }

    public long[] getSize() {
        return new long[] { getRowCount(), getColumnCount() };
    }

    public double getDouble(int row, int column) {
        return getDouble((long)row, (long)column);
    }

    public void setDouble(double value, int row, int column) {
        setDouble(value, (long)row, (long)column);
    }


}
