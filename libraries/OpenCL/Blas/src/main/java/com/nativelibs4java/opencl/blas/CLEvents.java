/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nativelibs4java.opencl.blas;

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
    
    List<Listener> listeners = new ArrayList<Listener>();
    
    public interface Listener {
        void writing(CLEvents evts);
        void reading(CLEvents evts);
    }
    public interface Action {
        CLEvent perform(CLEvent[] events);
    }
    
    public synchronized void addListener(Listener l) {
        listeners.add(l);
    }
    public synchronized void removeListener(Listener l) {
        listeners.remove(l);
    }
    static final CLEvent[] EMPTY_EVENTS = new CLEvent[0];
    protected synchronized CLEvent clearEvents(Action action) {
        int nReads = readEvents.size();
        boolean hasWrite = lastWriteEvent != null;
        int n = nReads + (hasWrite ? 1 : 0);
        CLEvent[] evts = n == 0 ? EMPTY_EVENTS : readEvents.toArray(new CLEvent[n]);
        if (hasWrite)
            evts[nReads] = lastWriteEvent;
        CLEvent evt = action.perform(evts);
        lastWriteEvent = null;
        readEvents.clear();
        return evt;
    }
    public synchronized CLEvent performRead(Action action) {
        for (Listener listener : listeners)
            listener.writing(this);
        CLEvent evt = action.perform(lastWriteEvent == null ? EMPTY_EVENTS : new CLEvent[] { lastWriteEvent });
        if (evt != null) {
            readEvents.add(evt);
            lastWriteEvent = null; // read completed only if the optional write also completed
        }
        return evt;
    }
    
    public synchronized void performRead(Runnable action) {
        for (Listener listener : listeners)
            listener.reading(this);
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
