/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.blas.ujmp;

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
import com.nativelibs4java.opencl.blas.LinearAlgebraUtils;
import com.nativelibs4java.util.NIOUtils;

/**
 *
 * @author ochafik
 */
public class CLDenseDoubleMatrix2D extends AbstractNIODenseDoubleMatrix2D {
	private static final long serialVersionUID = -36941159548127670L;
	protected final LinearAlgebraUtils kernels;
    protected CLDoubleBuffer buffer;
    protected DoubleBuffer data;


    private final static CLEvent[] EMPTY_EVENTS = new CLEvent[0];

	List<CLEvent> writeEvents, readEvents;

    public CLDenseDoubleMatrix2D(CLDoubleBuffer buffer, DoubleBuffer data, long rows, long columns, LinearAlgebraUtils kernels) {
        super(rows, columns);
        this.buffer = buffer;
        this.kernels = kernels;
        this.data = data;
        map();
    }

    public CLDenseDoubleMatrix2D(long rows, long columns, LinearAlgebraUtils kernels) {
        super(rows, columns);
        this.data = NIOUtils.directDoubles((int)(rows * columns), kernels.getContext().getKernelsDefaultByteOrder());
        this.buffer = kernels.getContext().createDoubleBuffer(Usage.InputOutput, data, false);
        this.kernels = kernels;
        map();
    }


    CLEvent map(CLEvent... eventsToWaitFor) {
        return buffer.mapLater(kernels.getQueue(), MapFlags.ReadWrite, eventsToWaitFor).getSecond();
    }
    CLEvent unmap(CLEvent... eventsToWaitFor) {
        return buffer.unmap(kernels.getQueue(), data, eventsToWaitFor);
    }

    public CLDenseDoubleMatrix2D(long rows, long columns) {
        this(rows, columns, CLDenseDoubleMatrix2DFactory.getLinearAlgebraKernels());
    }

    public CLDenseDoubleMatrix2D(long[] size) {
        this(size[0], size[1]);
        if (size.length != 2)
            throw new IllegalArgumentException("Size is not 2D !");
    }

    @Override
    public DoubleBuffer getReadableData() {
        //return data;
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public DoubleBuffer getWritableData() {
        //return data;
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /*static ThreadLocal<DoubleBuffer> readWriteBuffer = new ThreadLocal<DoubleBuffer>() {

        @Override
        protected DoubleBuffer initialValue() {
            return NIOUtils.directDoubles(1);
        }
        
    };*/

    @Override
    public double getDouble(long row, long column) {
        waitForRead();
        return data.get((int)getStorageIndex(row, column));
        /*DoubleBuffer rwb = readWriteBuffer.get();
        long offset = getStorageIndex(row, column);
        buffer.read(kernels.getQueue(), offset, 1, rwb, true);
        return rwb.get(0);*/
    }

    @Override
    public void setDouble(double value, long row, long column) {
        waitForWrite();
        data.put((int)getStorageIndex(row, column), value);
        /*DoubleBuffer rwb = readWriteBuffer.get();
        long offset = getStorageIndex(row, column);
        buffer.write(kernels.getQueue(), offset, 1, rwb, true);*/
    }

    @Override
    public synchronized Matrix mtimes(Matrix matrix) throws MatrixException {
        synchronized (matrix) {
            try {
                if (getColumnCount() != matrix.getRowCount()) {
                    throw new MatrixException("Incompatible dimensions");
                }
                CLDoubleBuffer arg;
                List<CLEvent> eventsToWaitFor = new ArrayList<CLEvent>();
                waitForRead();
                //TODO eventsBeforeReading(eventsToWaitFor);
                CLDenseDoubleMatrix2D cm = null;
                if (matrix instanceof CLDenseDoubleMatrix2D) {
                    cm = (CLDenseDoubleMatrix2D) matrix;
                    cm.waitForRead();
                    //TODO cm.eventsBeforeReading(eventsToWaitFor);
                    arg = cm.buffer;
                } else if (matrix instanceof DoubleMatrix2D) {
                    arg = kernels.getContext().createDoubleBuffer(Usage.Input, MatrixUtils.read((DoubleMatrix2D) matrix), true);
                } else {
                    return super.mtimes(matrix);
                }
                CLDenseDoubleMatrix2D out = new CLDenseDoubleMatrix2D(getRowCount(), matrix.getColumnCount(), kernels);
                CLEvent evt = kernels.multiply(buffer, getRowCount(), getColumnCount(), arg, matrix.getRowCount(), matrix.getColumnCount(), out.buffer, events(eventsToWaitFor));
                addReadEvent(evt);
                if (cm != null) {
                    cm.addReadEvent(evt);
                }
                evt = out.map(evt);
                out.addWriteEvent(evt);
                return out;
            } catch (CLBuildException ex) {
                throw new RuntimeException("Failed to build OpenCL kernels", ex);
            }
        }
    }



    @Override
    public synchronized Matrix copy() throws MatrixException {
        //long count = buffer.getElementCount();
        waitForRead();
        //CLDenseDoubleMatrix2D copy = new CLDenseDoubleMatrix2D(rows, columns, kernels);
        DoubleBuffer data = NIOUtils.directDoubles((int)(rows * columns), kernels.getContext().getKernelsDefaultByteOrder());
        data.put(this.data.duplicate());
        return new CLDenseDoubleMatrix2D(buffer, data, rows, columns, kernels);
        /*
        CLDoubleBuffer newBuffer = kernels.getContext().createDoubleBuffer(Usage.InputOutput, count);
        CLDenseDoubleMatrix2D copy = new CLDenseDoubleMatrix2D(newBuffer, rows, columns, kernels);
        CLEvent evt = buffer.copyTo(kernels.getQueue(), 0, count, newBuffer, 0, eventsBeforeReading());
        addReadEvent(evt);
        copy.addWriteEvent(evt);
        return copy;*/
    }

    @Override
    public synchronized Matrix transpose() throws MatrixException {
        try {
//           long count = buffer.getElementCount();
            //CLDoubleBuffer newBuffer = kernels.getContext().createDoubleBuffer(Usage.InputOutput, count);
            //CLDenseDoubleMatrix2D trans = new CLDenseDoubleMatrix2D(newBuffer, columns, rows, kernels);
            CLDenseDoubleMatrix2D trans = new CLDenseDoubleMatrix2D(columns, rows, kernels);
            CLEvent evt = kernels.transpose(buffer, rows, columns, trans.buffer, eventsBeforeReading());
            addReadEvent(evt);
            evt = trans.map(evt);
            trans.addWriteEvent(evt);
            return trans;
        } catch (CLBuildException ex) {
            throw new RuntimeException("Failed to build OpenCL kernels", ex);
        }
    }

    public CLEvent[] eventsBeforeReading() {
		return eventsBefore(false);
	}
	public CLEvent[] eventsBeforeWriting() {
		return eventsBefore(true);
	}
	public void eventsBeforeReading(List<CLEvent> out) {
		eventsBefore(false, out);
	}
	public void eventsBeforeWriting(List<CLEvent> out) {
		eventsBefore(true, out);
	}
    protected CLEvent[] events(List<CLEvent> events) {
        if (events.isEmpty())
            return EMPTY_EVENTS;
        return events.toArray(new CLEvent[events.size()]);
    }
	protected synchronized CLEvent[] eventsBefore(boolean includeReads) {
		int nr = !includeReads || readEvents == null ? 0 : readEvents.size();
		int nw = writeEvents == null ? 0 : writeEvents.size();
		int n = nr + nw;
		if (n == 0)
			return EMPTY_EVENTS;

		CLEvent[] ret = new CLEvent[n];
		for (int i = 0; i < nr; i++)
			ret[i] = readEvents.get(i);
		for (int i = 0; i < nw; i++)
			ret[nr + i] = writeEvents.get(i);

		return ret;
	}
	protected synchronized void eventsBefore(boolean includeReads, List<CLEvent> out) {
		if (includeReads && readEvents != null)
			out.addAll(readEvents);
		if (writeEvents != null)
			out.addAll(writeEvents);
	}
	protected synchronized void addWriteEvent(CLEvent evt) {
		if (evt == null)
			return;
		if (readEvents == null)
			 readEvents = new ArrayList<CLEvent>();
		readEvents.add(evt);
	}
	protected synchronized void addReadEvent(CLEvent evt) {
		if (evt == null)
			return;
		if (writeEvents == null)
			writeEvents = new ArrayList<CLEvent>();
		writeEvents.add(evt);
	}

    static boolean DUMMY_WAIT = true;
	protected synchronized void dummyWait() {
        kernels.getQueue().finish();
        if (readEvents != null)
            readEvents.clear();
        if (writeEvents != null)
            writeEvents.clear();
    }
	protected void waitForRead() {
		if (DUMMY_WAIT) {
            dummyWait();
            return;
        }
        CLEvent[] evts = eventsBeforeReading();
		CLEvent.waitFor(evts);
		synchronized (this) {
			List<CLEvent> evtsList = Arrays.asList(evts);
			if (readEvents != null)
				readEvents.removeAll(evtsList);
			if (writeEvents != null)
				writeEvents.removeAll(evtsList);
		}
	}

	protected void waitForWrite() {
        if (DUMMY_WAIT) {
            dummyWait();
            return;
        }
        CLEvent[] evts = eventsBeforeReading();
		CLEvent.waitFor(evts);
		synchronized (this) {
			List<CLEvent> evtsList = Arrays.asList(evts);
			if (readEvents != null)
				readEvents.removeAll(evtsList);
			if (writeEvents != null)
				writeEvents.removeAll(evtsList);
		}
	}

    public void write(DoubleBuffer b) {
        waitForWrite();
        addWriteEvent(buffer.write(kernels.getQueue(), b, false));
    }

    public void read(DoubleBuffer b) {
        waitForRead();
        buffer.read(kernels.getQueue(), b, true);
    }

}
