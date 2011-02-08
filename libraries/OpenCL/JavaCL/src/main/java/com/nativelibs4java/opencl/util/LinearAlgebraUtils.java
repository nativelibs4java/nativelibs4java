/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.util;

import com.nativelibs4java.opencl.CLBuildException;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLDoubleBuffer;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLProgram;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.JavaCL;
import com.nativelibs4java.opencl.util.ReductionUtils;
import com.nativelibs4java.opencl.util.ReductionUtils.Reductor;
import com.nativelibs4java.util.IOUtils;
import com.ochafik.util.listenable.Pair;
import static com.nativelibs4java.util.NIOUtils.*;
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
	
	public synchronized CLEvent multiply(
            CLDoubleBuffer a, long aRows, long aColumns, 
            CLDoubleBuffer b, long bRows, long bColumns, 
            CLDoubleBuffer out, //long outRows, long outColumns,
            CLEvent... eventsToWaitFor) throws CLBuildException
    {
        long outRows = aRows;
        long outColumns = bColumns;
        return kernels.mulMat(queue,
            a, (int)aColumns,
            b, (int)bColumns,
            out,
            new int[] { (int)outRows, (int)outColumns },
            null,
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

	Reductor<DoubleBuffer> addReductor;
	synchronized Reductor<DoubleBuffer> getAddReductor() {
		if (addReductor == null) {
			try {
				addReductor = ReductionUtils.createReductor(getContext(), ReductionUtils.Operation.Add, OpenCLType.Double, 1);
			} catch (CLBuildException ex) {
				Logger.getLogger(LinearAlgebraUtils.class.getName()).log(Level.SEVERE, null, ex);
				throw new RuntimeException("Failed to create an addition reductor !", ex);
			}
		}
		return addReductor;
	}

    public synchronized CLEvent transpose(CLDoubleBuffer a, long aRows, long aColumns, CLDoubleBuffer out, CLEvent... eventsToWaitFor) throws CLBuildException {
        return kernels.transpose(queue,
            a, aRows, aColumns,
            out,
            new int[] { (int)aColumns, (int)aRows },
            null,
            eventsToWaitFor
        );
    }

}
