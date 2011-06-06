/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.util;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLBuildException;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLDoubleBuffer;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLPlatform.DeviceFeature;
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
import java.nio.Buffer;
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
    public LinearAlgebraUtils(boolean doubleCapable) throws IOException, CLBuildException {
        this(JavaCL.createBestContext(doubleCapable ? DeviceFeature.DoubleSupport : null).createDefaultQueue());
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
	
    
	public synchronized <T> CLEvent multiply(
            CLBuffer<T> a, int aRows, int aColumns, 
            CLBuffer<T> b, int bRows, int bColumns, 
            CLBuffer<T> out, //long outRows, long outColumns,
            CLEvent... eventsToWaitFor) throws CLBuildException
    {
        if (a.getBufferClass() == DoubleBuffer.class)
            return multiplyDoubles((CLBuffer<Double>)a, aRows, aColumns, (CLBuffer<Double>)b, bRows, bColumns, (CLBuffer<Double>)out, eventsToWaitFor);
        
        throw new UnsupportedOperationException();
    }
	public synchronized CLEvent multiplyDoubles(
            CLBuffer<Double> a, int aRows, int aColumns, 
            CLBuffer<Double> b, int bRows, int bColumns, 
            CLBuffer<Double> out, //long outRows, long outColumns,
            CLEvent... eventsToWaitFor) throws CLBuildException
    {
    		if (a == null || b == null || out == null)
    			throw new IllegalArgumentException("Null matrix");
    		
    		if (aColumns != bRows || out.getElementCount() != (aRows * bColumns))
    			throw new IllegalArgumentException("Invalid matrix sizes : multiplying matrices of sizes (A, B) and (B, C) requires output of size (A, C)");
    	
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

	Reductor<Double> addReductor;
	synchronized Reductor<Double> getAddReductor() {
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

    public synchronized <T> CLEvent transpose(CLBuffer<T> a, int aRows, int aColumns, CLBuffer<T> out, CLEvent... eventsToWaitFor) throws CLBuildException {
        if (a.getBufferClass() == DoubleBuffer.class)
            return transposeDoubles((CLBuffer<Double>)a, aRows, aColumns, (CLBuffer<Double>)out, eventsToWaitFor);
        
        throw new UnsupportedOperationException();
    }

    public synchronized CLEvent transposeDoubles(CLBuffer<Double> a, int aRows, int aColumns, CLBuffer<Double> out, CLEvent... eventsToWaitFor) throws CLBuildException {
        return kernels.transpose(queue,
            a, aRows, aColumns,
            out,
            new int[] { (int)aColumns, (int)aRows },
            null,
            eventsToWaitFor
        );
    }
    

}
