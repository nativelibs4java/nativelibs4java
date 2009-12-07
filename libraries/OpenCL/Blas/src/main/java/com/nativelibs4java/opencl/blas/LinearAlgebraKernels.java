/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.blas;

import com.nativelibs4java.opencl.CLBuildException;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLDoubleBuffer;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLProgram;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.JavaCL;
import com.nativelibs4java.opencl.ReductionUtils;
import com.nativelibs4java.opencl.ReductionUtils.Reductor;
import com.nativelibs4java.util.IOUtils;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.DoubleBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ochafik
 */
public class LinearAlgebraKernels {

    protected CLContext context;
    protected CLQueue queue;
    protected CLProgram prog;
    protected CLKernel mulMatKernel, mulVecKernel, transposeKernel;

    static final String blas1Source = "LinearAlgebraKernels.c";

    public LinearAlgebraKernels() throws IOException, CLBuildException {
        this(JavaCL.createBestContext());
    }

    public LinearAlgebraKernels(CLContext context) throws IOException, CLBuildException {
        this(context, context.createDefaultQueue());
    }

    public LinearAlgebraKernels(CLContext context, CLQueue queue) throws IOException, CLBuildException {
        this.context = context;
        this.queue = queue;
        InputStream in = LinearAlgebraKernels.class.getResourceAsStream(blas1Source);
        if (in == null)
            throw new FileNotFoundException(blas1Source);

        String source = IOUtils.readText(in);
        prog = context.createProgram(source).build();
        mulMatKernel = prog.createKernel("mulMat");
        mulVecKernel = prog.createKernel("mulVec");
        transposeKernel = prog.createKernel("transpose");
    }

    public CLContext getContext() {
        return context;
    }

    public CLQueue getQueue() {
        return queue;
    }

    
	private static final int[] unitIntArr = new int[] { 1 };
    private static final int[] unitInt2Arr = new int[] { 1, 1 };

	public synchronized CLEvent multiply(
            CLDoubleBuffer a, long aRows, long aColumns, 
            CLDoubleBuffer b, long bRows, long bColumns, 
            CLDoubleBuffer out, //long outRows, long outColumns,
            CLEvent... eventsToWaitFor)
    {
        long outRows = aRows;
        if (bColumns == 1) {
            mulVecKernel.setArgs(
                a, /*aRows,*/ aColumns,
                b, bRows,
                out
            );
            return mulMatKernel.enqueueNDRange(queue, new int[] { (int)outRows }, unitIntArr, eventsToWaitFor);
        }
        long outColumns = bColumns;
        mulMatKernel.setArgs(
            a, /*aRows,*/ aColumns,
            b, /*bRows,*/ bColumns,
            out
        );
        return mulMatKernel.enqueueNDRange(queue, new int[] { (int)outRows, (int)outColumns }, unitInt2Arr, eventsToWaitFor);
    }

    /*synchronized CLEvent dot(CLVector a b out, CLEvent... eventsToWaitFor) {
		CLEvent.waitFor(eventsToWaitFor);
		a.waitForRead();
		b.waitForRead();
		out.waitForWrite();
		FV aa  = newVector(fallBackLibrary, a);
		FV bb  = newVector(fallBackLibrary, b);
		out.write(aa.dot(bb, null).read());
		return null;
    }*/

	Reductor<DoubleBuffer> addReductor;
	synchronized Reductor<DoubleBuffer> getAddReductor() {
		if (addReductor == null) {
			try {
				addReductor = ReductionUtils.createReductor(context, ReductionUtils.Operation.Add, ReductionUtils.Type.Double, 1);
			} catch (CLBuildException ex) {
				Logger.getLogger(LinearAlgebraKernels.class.getName()).log(Level.SEVERE, null, ex);
				throw new RuntimeException("Failed to create an addition reductor !", ex);
			}
		}
		return addReductor;
	}

    public synchronized CLEvent transpose(CLDoubleBuffer a, long aRows, long aColumns, CLDoubleBuffer out, CLEvent... eventsToWaitFor) {
        transposeKernel.setArgs(
            a, aRows, aColumns,
            out
        );
        return transposeKernel.enqueueNDRange(queue, new int[] { (int)aColumns, (int)aRows }, unitInt2Arr, eventsToWaitFor);
    }

}
