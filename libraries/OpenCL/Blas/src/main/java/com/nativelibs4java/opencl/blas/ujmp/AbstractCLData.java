/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.blas.ujmp;

import com.nativelibs4java.opencl.CLEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author ochafik
 */
public class AbstractCLData {

    private final static CLEvent[] EMPTY_EVENTS = new CLEvent[0];

	List<CLEvent> writeEvents, readEvents;
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

}
