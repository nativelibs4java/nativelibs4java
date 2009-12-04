/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.blas.opencl;

import com.nativelibs4java.blas.AbstractLinearAlgebra;
import com.nativelibs4java.blas.Matrix;
import com.nativelibs4java.blas.Vector;
import com.nativelibs4java.opencl.CLBuildException;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLProgram;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.JavaCL;
import com.nativelibs4java.util.IOUtils;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author Olivier
 */
public class CLLinearAlgebra extends AbstractLinearAlgebra<CLMatrix, CLVector, CLComputationEvent> {

    CLContext context;
    CLQueue queue;
    CLProgram multiplyProg;
    CLKernel mulMatKernel, mulVecKernel;

    static final String blas1Source = "Blas1.c";
    public CLLinearAlgebra() throws IOException, CLBuildException {
        context = JavaCL.createBestContext();
        queue = context.createDefaultQueue();

        InputStream in = CLLinearAlgebra.class.getResourceAsStream(blas1Source);
        if (in == null)
            throw new FileNotFoundException(blas1Source);

        String source = IOUtils.readText(in);
        multiplyProg = context.createProgram(source).build();
        mulMatKernel = multiplyProg.createKernel("mulMat");
        mulVecKernel = multiplyProg.createKernel("mulVec");
    }

    private static final CLEvent[] emptyEvents = new CLEvent[0];
    CLEvent[] events(CLComputationEvent... eventsToWaitFor) {
        if (eventsToWaitFor == null || eventsToWaitFor.length == 0)
            return emptyEvents;
        
        CLEvent[] events = new CLEvent[eventsToWaitFor.length];
        for (int i = events.length; i-- != 0;)
            events[i] = eventsToWaitFor[i].event;
        return events;
    }

    private static final int[] unitIntArr = new int[] { 1 };
    private static final int[] unitInt2Arr = new int[] { 1, 1 };
    @Override
    public synchronized CLComputationEvent multiply(CLMatrix a, CLMatrix b, CLMatrix out, CLComputationEvent... eventsToWaitFor) {
        mulMatKernel.setArgs(
            a.buffer, a.getRows(), a.getColumns(),
            b.buffer, b.getRows(), b.getColumns(),
            out.buffer
        );
        return new CLComputationEvent(mulMatKernel.enqueueNDRange(queue, new int[] { out.getRows(), out.getColumns() }, unitInt2Arr, events(eventsToWaitFor)));
    }

    @Override
    public synchronized CLComputationEvent multiply(CLMatrix a, CLVector b, CLVector out, CLComputationEvent... eventsToWaitFor) {
        mulVecKernel.setArgs(
            a.buffer, a.getRows(), a.getColumns(),
            b.buffer, b.size(),
            out.buffer
        );
        return new CLComputationEvent(mulVecKernel.enqueueNDRange(queue, new int[] { out.size() }, unitIntArr, events(eventsToWaitFor)));
    }

    @Override
    public void multiplyNow(CLMatrix a, CLMatrix b, CLMatrix out) {
        multiply(a, b, out).waitFor();
    }

    @Override
    public void multiplyNow(CLMatrix a, CLVector b, CLVector out) {
        multiply(a, b, out).waitFor();
    }

    @Override
    public CLMatrix newMatrix(int rows, int columns) {
        return new CLMatrix(this, rows, columns);
    }

    @Override
    public CLVector newVector(int size) {
        return new CLVector(this, size);
    }

}
