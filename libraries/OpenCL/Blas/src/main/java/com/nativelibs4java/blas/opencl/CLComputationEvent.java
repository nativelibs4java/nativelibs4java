package com.nativelibs4java.blas.opencl;

import com.nativelibs4java.blas.*;
import com.nativelibs4java.opencl.CLEvent;

public class CLComputationEvent implements ComputationEvent {

    protected final CLEvent event;
    public CLComputationEvent(CLEvent event) {
        this.event = event;
    }

    public void waitFor() {
        event.waitFor();
    }
}
