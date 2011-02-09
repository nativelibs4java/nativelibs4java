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
	
	public synchronized CLEvent multiplyDouble(
            CLBuffer<Double> a, long aRows, long aColumns, 
            CLBuffer<Double> b, long bRows, long bColumns, 
            CLBuffer<Double> out, //long outRows, long outColumns,
            CLEvent... eventsToWaitFor) throws CLBuildException
    {
        long outRows = aRows;
        long outColumns = bColumns;
        return kernels.mulMatDouble(queue,
            a, (int)aColumns,
            b, (int)bColumns,
            out,
            new int[] { (int)outRows, (int)outColumns },
            null,
            eventsToWaitFor
        );
    }
    public synchronized CLEvent multiplyFloat(
            CLBuffer<Float> a, long aRows, long aColumns, 
            CLBuffer<Float> b, long bRows, long bColumns, 
            CLBuffer<Float> out, //long outRows, long outColumns,
            CLEvent... eventsToWaitFor) throws CLBuildException
    {
        long outRows = aRows;
        long outColumns = bColumns;
        return kernels.mulMatFloat(queue,
            a, (int)aColumns,
            b, (int)bColumns,
            out,
            new int[] { (int)outRows, (int)outColumns },
            null,
            eventsToWaitFor
        );
    }
	Reductor<Double> addReductorDouble;
	synchronized Reductor<Double> getAddReductorDouble() {
		if (addReductorDouble == null) {
			try {
				addReductorDouble = ReductionUtils.createReductor(getContext(), ReductionUtils.Operation.Add, OpenCLType.Double, 1);
			} catch (CLBuildException ex) {
				Logger.getLogger(LinearAlgebraUtils.class.getName()).log(Level.SEVERE, null, ex);
				throw new RuntimeException("Failed to create an addition reductor !", ex);
			}
		}
		return addReductorDouble;
	}

    public synchronized CLEvent transposeDouble(CLBuffer<Double> a, long aRows, long aColumns, CLBuffer<Double> out, CLEvent... eventsToWaitFor) throws CLBuildException {
        return kernels.transposeDouble(queue,
            a, aRows, aColumns,
            out,
            new int[] { (int)aColumns, (int)aRows },
            null,
            eventsToWaitFor
        );
    }
	Reductor<Float> addReductorFloat;
	synchronized Reductor<Float> getAddReductorFloat() {
		if (addReductorFloat == null) {
			try {
				addReductorFloat = ReductionUtils.createReductor(getContext(), ReductionUtils.Operation.Add, OpenCLType.Float, 1);
			} catch (CLBuildException ex) {
				Logger.getLogger(LinearAlgebraUtils.class.getName()).log(Level.SEVERE, null, ex);
				throw new RuntimeException("Failed to create an addition reductor !", ex);
			}
		}
		return addReductorFloat;
	}

    public synchronized CLEvent transposeFloat(CLBuffer<Float> a, long aRows, long aColumns, CLBuffer<Float> out, CLEvent... eventsToWaitFor) throws CLBuildException {
        return kernels.transposeFloat(queue,
            a, aRows, aColumns,
            out,
            new int[] { (int)aColumns, (int)aRows },
            null,
            eventsToWaitFor
        );
    }

}
