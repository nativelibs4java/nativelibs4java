/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.util;

import com.nativelibs4java.opencl.CLBuildException;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLProgram;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.JavaCL;
import com.nativelibs4java.opencl.util.ReductionUtils;
import com.nativelibs4java.opencl.util.ReductionUtils.Reductor;
import com.nativelibs4java.util.IOUtils;
import com.nativelibs4java.util.Pair;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.DoubleBuffer;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ochafik
 */
@SuppressWarnings("unused")
public class LinearAlgebraUtils {

	final LinearAlgebraKernels kernels;
	final CLQueue queue;
    public LinearAlgebraUtils() throws IOException, CLBuildException {
        this(JavaCL.createBestContext().createDefaultQueue());
    }

    public LinearAlgebraUtils(CLQueue queue) throws IOException, CLBuildException {
		this.queue = queue;
        kernels = new LinearAlgebraKernels(queue.getContext());
    }

    public CLContext getContext() {
        return getQueue().getContext();
    }

    public CLQueue getQueue() {
        return queue;
    }
	
	private static final int[] unitIntArr = new int[] { 1 };
    private static final int[] unitInt2Arr = new int[] { 1, 1 };

	public synchronized CLEvent multiply(
            CLBuffer<Double> a, long aRows, long aColumns, 
            CLBuffer<Double> b, long bRows, long bColumns, 
            CLBuffer<Double> out, //long outRows, long outColumns,
            CLEvent... eventsToWaitFor) throws CLBuildException
    {
        long outRows = aRows;
        long outColumns = bColumns;
        return kernels.mulMat(queue,
            a, (int)aColumns,
            b, (int)bColumns,
            out,
            new int[] { (int)outRows, (int)outColumns },
            unitInt2Arr,
            eventsToWaitFor
        );
    }
    /*
    public synchronized CLEvent multiplyLongs(
            CLBuffer<Long> a, long aRows, long aColumns, 
            CLBuffer<Long> b, long bRows, long bColumns, 
            CLBuffer<Long> out, //long outRows, long outColumns,
            CLEvent... eventsToWaitFor) throws CLBuildException
    {
        long outRows = aRows;
        long outColumns = bColumns;
        return kernels.mulMatLong(queue,
            a, (int)aColumns,
            b, (int)bColumns,
            out,
            new int[] { (int)outRows, (int)outColumns },
            unitInt2Arr,
            eventsToWaitFor
        );
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

	Reductor<Double> addReductor;
	synchronized Reductor<Double> getAddReductor() {
		if (addReductor == null) {
			try {
				addReductor = ReductionUtils.createReductor(getContext(), ReductionUtils.Operation.Add, ReductionUtils.Type.Double, 1);
			} catch (CLBuildException ex) {
				Logger.getLogger(LinearAlgebraUtils.class.getName()).log(Level.SEVERE, null, ex);
				throw new RuntimeException("Failed to create an addition reductor !", ex);
			}
		}
		return addReductor;
	}

    public synchronized CLEvent transpose(CLBuffer<Double> a, long aRows, long aColumns, CLBuffer<Double> out, CLEvent... eventsToWaitFor) throws CLBuildException {
        return kernels.transpose(queue,
            a, aRows, aColumns,
            out,
            new int[] { (int)aColumns, (int)aRows },
            unitInt2Arr,
            eventsToWaitFor
        );
    }

}
