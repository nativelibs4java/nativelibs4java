/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nativelibs4java.opencl;

import com.nativelibs4java.opencl.library.*;
import java.util.Map;
import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import org.bridj.Pointer;
import static org.bridj.Pointer.*;
import com.nativelibs4java.test.MiscTestUtils;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Kazo Csaba
 */
@SuppressWarnings("unchecked")
public class OverheadTest extends AbstractCommon {

    static void gc() {
        try {
            System.gc();
            Thread.sleep(100);
            System.gc();
            Thread.sleep(100);
        } catch (InterruptedException ex) {}
    }
    static long time(String title, int n, Runnable payload, Runnable finalizer) {
        gc();
        
        long start = System.nanoTime();
        for (int i = 0; i < n; i++) {
            payload.run();
        }
        if (finalizer != null)
            finalizer.run();
        long timeMillis = (System.nanoTime() - start) / 1000000;
        
        if (title != null)
            System.out.println("Time[ " + title + " ; n = " + n + "] = " + timeMillis + " ms");
        return timeMillis;
    }
    @Test
    public void compareVariousSetArgsPerformance() throws CLBuildException {
		if (!context.getCacheBinaries()) {
			System.out.println("Skipping binaries caching test");
			return;
		}
		CLProgram program = context.createProgram(
				  "__kernel void copy(__global int* a, __global int* b) {\n" +
				  "   int i = get_global_id(0);\n" +
				  "   b[i]=a[i];\n" +
				  "} ");
        
        
		program.build();
        final CLKernel kernel = program.createKernel("copy");
        
        final CLBuffer<Integer> a=context.createBuffer(CLMem.Usage.Input, Integer.class, 4);
		final CLBuffer<Integer> b=context.createBuffer(CLMem.Usage.Output, Integer.class, 4);

        
        
        Runnable setWithSetArgs = new Runnable() { public void run() {
           kernel.setArgs(a, b);
        }};
        Runnable setWithSpecializedSetArg = new Runnable() { public void run() {
           kernel.setArg(0, a);
           kernel.setArg(0, b);
        }};
        Runnable setWithCLAPI = new Runnable() {
            private final OpenCLLibrary CL = new OpenCLLibrary();
            public void run() {
                CL.clSetKernelArg(kernel.getEntity(), 0, Pointer.SIZE, a.getEntity());
                CL.clSetKernelArg(kernel.getEntity(), 1, Pointer.SIZE, b.getEntity());
            }
        };
        Runnable setWithRawCLAPI = new Runnable() { 
            private final long aPeer = getPeer(a.getEntity());
            private final long bPeer = getPeer(b.getEntity());
            private final long kEntity = getPeer(kernel.getEntity());
            private final OpenCLLibrary CL = new OpenCLLibrary();
            public void run() {
                CL.clSetKernelArg(kEntity, 0, Pointer.SIZE, aPeer);
                CL.clSetKernelArg(kEntity, 1, Pointer.SIZE, bPeer);
            }
        };
        
        int nWarmup = 8000, nTest = 50000;
        time(null, nWarmup, setWithCLAPI, null);
        time(null, nWarmup, setWithSetArgs, null);
        time(null, nWarmup, setWithSpecializedSetArg, null);
        time(null, nWarmup, setWithRawCLAPI, null);
        
        int nSamples = 10;
        double totSetArgs = 0, totCLSetKernelArg = 0, totSetArg = 0, totCLSetKernelArgRaw = 0;
        for (int i = 0; i < nSamples; i++) {
            totCLSetKernelArg += time("clSetKernelArg", nTest, setWithCLAPI, null);
            totSetArgs += time("CLKernel.setArgs", nTest, setWithSetArgs, null);
            totSetArg += time("CLKernel.setArg", nTest, setWithSpecializedSetArg, null);
            totCLSetKernelArgRaw += time("clSetKernelArg raw", nTest, setWithRawCLAPI, null);
            System.out.println();
        }
        
        final double maxSlower = 1.5;
        double slowerSetArg = totSetArg / totCLSetKernelArgRaw;
        double slowerSetArgs = totSetArgs / totCLSetKernelArgRaw;
        assertTrue("CLKernel.setArg was supposed not to be more than " + maxSlower + "x slower than hand-optimized version, was " + slowerSetArg + "x slower.", slowerSetArg <= maxSlower);
        assertTrue("CLKernel.setArgs was supposed not to be more than " + maxSlower + "x slower than hand-optimized version, was " + slowerSetArgs + "x slower.", slowerSetArgs <= maxSlower);
    }
}