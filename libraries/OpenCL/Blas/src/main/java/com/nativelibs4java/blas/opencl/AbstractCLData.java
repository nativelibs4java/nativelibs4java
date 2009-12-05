/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.blas.opencl;


import com.nativelibs4java.blas.AsynchronousData;
import com.nativelibs4java.blas.Data;
import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLDoubleBuffer;
import com.nativelibs4java.opencl.CLMem;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author ochafik
 */
public abstract class AbstractCLData<B extends Buffer> implements AsynchronousData<B> {
	CLBuffer<B> data;
    CLLinearAlgebra al;
	protected final int size;
	List<CLEvent> writeEvents, readEvents;

    public AbstractCLData(CLLinearAlgebra al, CLBuffer<B> data, int size) {
        this.al = al;
		this.size = size;
		this.data = data;
    }

    @Override
	public int size() {
		return size;
	}

	@Override
	public void read(B out) {
		addReadEvent(data.read(al.queue, out, true, eventsBeforeReading()));
	}

	@Override
	public void write(B in) {
		addWriteEvent(data.write(al.queue, in, true, eventsBeforeWriting()));
	}

	private final static CLEvent[] EMPTY_EVENTS = new CLEvent[0];

	public CLEvent[] eventsBeforeReading() {
		return eventsBefore(false);
	}
	public CLEvent[] eventsBeforeWriting() {
		return eventsBefore(true);
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
	protected synchronized void addWriteEvent(CLEvent evt) {
		if (readEvents == null)
			 readEvents = new ArrayList<CLEvent>();
		readEvents.add(evt);
	}
	protected synchronized void addReadEvent(CLEvent evt) {
		if (writeEvents == null)
			 writeEvents = new ArrayList<CLEvent>();
		writeEvents.add(evt);
	}

	@Override
	public void waitForRead() {
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

	@Override
	public void waitForWrite() {
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

}
