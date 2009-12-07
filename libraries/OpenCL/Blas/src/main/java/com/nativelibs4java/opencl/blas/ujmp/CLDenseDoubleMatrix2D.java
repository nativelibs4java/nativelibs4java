/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.blas.ujmp;

import com.nativelibs4java.opencl.CLBuildException;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLDoubleBuffer;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLMem.Usage;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.JavaCL;
import com.nativelibs4java.opencl.blas.LinearAlgebraKernels;
import com.nativelibs4java.util.NIOUtils;
import java.io.IOException;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ujmp.core.Matrix;
import org.ujmp.core.exceptions.MatrixException;

/**
 *
 * @author ochafik
 */
public class CLDenseDoubleMatrix2D extends AbstractNIODenseDoubleMatrix2D {

    protected final LinearAlgebraKernels kernels;
    protected CLDoubleBuffer buffer;


    private final static CLEvent[] EMPTY_EVENTS = new CLEvent[0];

	List<CLEvent> writeEvents, readEvents;

    public CLDenseDoubleMatrix2D(CLDoubleBuffer buffer, long rows, long columns, LinearAlgebraKernels kernels) {
        super(rows, columns);
        this.buffer = buffer;
        this.kernels = kernels;
    }

    public CLDenseDoubleMatrix2D(long rows, long columns, LinearAlgebraKernels kernels) {
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

    @Override
    public DoubleBuffer getReadableData() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public DoubleBuffer getWritableData() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    static ThreadLocal<DoubleBuffer> readWriteBuffer = new ThreadLocal<DoubleBuffer>() {

        @Override
        protected DoubleBuffer initialValue() {
            return NIOUtils.directDoubles(1);
        }
        
    };

    @Override
    public double getDouble(long row, long column) {
        waitForRead();
        DoubleBuffer rwb = readWriteBuffer.get();
        long offset = getStorageIndex(row, column);
        buffer.read(kernels.getQueue(), offset, 1, rwb, true);
        return rwb.get(0);
    }

    @Override
    public void setDouble(double value, long row, long column) {
        waitForWrite();
        DoubleBuffer rwb = readWriteBuffer.get();
        long offset = getStorageIndex(row, column);
        buffer.write(kernels.getQueue(), offset, 1, rwb, true);
    }


    @Override
    public synchronized Matrix copy() throws MatrixException {
        long count = buffer.getElementCount();
        CLDoubleBuffer newBuffer = kernels.getContext().createDoubleBuffer(Usage.InputOutput, count);
        CLDenseDoubleMatrix2D copy = new CLDenseDoubleMatrix2D(newBuffer, rows, columns, kernels);
        CLEvent evt = buffer.copyTo(kernels.getQueue(), 0, count, newBuffer, 0, eventsBeforeReading());
        addReadEvent(evt);
        copy.addWriteEvent(evt);
        return copy;
    }

    @Override
    public synchronized Matrix transpose() throws MatrixException {
        long count = buffer.getElementCount();
        CLDoubleBuffer newBuffer = kernels.getContext().createDoubleBuffer(Usage.InputOutput, count);
        CLDenseDoubleMatrix2D trans = new CLDenseDoubleMatrix2D(newBuffer, columns, rows, kernels);
        CLEvent evt = kernels.transpose(buffer, rows, columns, newBuffer, eventsBeforeReading());
        addReadEvent(evt);
        trans.addWriteEvent(evt);

        return trans;
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

	protected void waitForRead() {
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
		CLEvent[] evts = eventsBeforeWriting();
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
