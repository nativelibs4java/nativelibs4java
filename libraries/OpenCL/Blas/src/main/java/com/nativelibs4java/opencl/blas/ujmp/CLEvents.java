/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nativelibs4java.opencl.blas.ujmp;

import com.nativelibs4java.opencl.CLEvent;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ochafik
 */
public class CLEvents {
    CLEvent lastWriteEvent;
    List<CLEvent> readEvents = new ArrayList<CLEvent>();
    
    public interface Action {
        CLEvent perform(CLEvent[] events);
    }
    
    protected synchronized CLEvent clearEvents(Action action) {
        int nReads = readEvents.size();
        CLEvent[] evts = readEvents.toArray(new CLEvent[nReads + 1]);
        evts[nReads] = lastWriteEvent;
        CLEvent evt = action.perform(evts);
        lastWriteEvent = null;
        readEvents.clear();
        return evt;
    }
    public synchronized CLEvent performRead(Action action) {
        CLEvent evt = action.perform(new CLEvent[] { lastWriteEvent });
        readEvents.add(evt);
        return evt;
    }
    
    public synchronized void performRead(Runnable action) {
        waitForRead();
        action.run();
    }
    
    public synchronized CLEvent performWrite(Action action) {
        return lastWriteEvent = clearEvents(action);
    }
    
    /**
     * Wait until all write operations are completed so that the data is readable.
     */
    public synchronized void waitForRead() {
        CLEvent.waitFor(lastWriteEvent);
        lastWriteEvent = null;
    }
    /**
     * Wait for all associated operations to complete (read or write).
     */
    public synchronized void waitFor() {
        clearEvents(new Action() {
            public CLEvent perform(CLEvent[] evts) {
                CLEvent.waitFor(evts);
                return null;
            }
        });
    }
}
