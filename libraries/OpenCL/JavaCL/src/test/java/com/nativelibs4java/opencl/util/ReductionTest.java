
package com.nativelibs4java.opencl.util;

import com.nativelibs4java.opencl.*;
import static com.nativelibs4java.opencl.JavaCL.createBestContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.IntBuffer;
import java.nio.FloatBuffer;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.nativelibs4java.opencl.util.ReductionUtils.Reductor;
import com.nativelibs4java.util.NIOUtils;

public class ReductionTest {

    //@BeforeClass
    //public static void setup() {
    //    com.sun.jna.Native.setProtected(true);
    //}

    CLContext context;
    CLQueue queue;

    @Before
    public void init() {
        context = createBestContext();
        queue = context.createDefaultQueue();
    }
    
    @Test
    public void testMinMax() {
        try {
			CLIntBuffer input = context.createIntBuffer(CLMem.Usage.Input, IntBuffer.wrap(new int[] {
                1110, 22, 35535, 3, 1
            }), true);

            int maxReductionSize = 2;
            IntBuffer result = NIOUtils.directInts(1, context.getByteOrder());
            
            Reductor<IntBuffer> reductor = ReductionUtils.createReductor(context, ReductionUtils.Operation.Min, ReductionUtils.Type.Int, 1);
            reductor.reduce(queue, input, input.getElementCount(), result, maxReductionSize);
            queue.finish();
            assertEquals(1, result.get(0));

            reductor = ReductionUtils.createReductor(context, ReductionUtils.Operation.Max, ReductionUtils.Type.Int, 1);
            reductor.reduce(queue, input, input.getElementCount(), result, maxReductionSize);
            queue.finish();
            assertEquals(35535, result.get(0));
            
        } catch (Exception ex) {
            ex.printStackTrace();
            assertTrue(ex.toString(), false);
        }
    }
    
    /// http://code.google.com/p/nativelibs4java/issues/detail?id=26
    @Test
    public void testIssue26() {
        try {
			float[] array = new float[4097];
			for (int i = 0; i < array.length; i++)
				array[i] = 1;
			
			CLFloatBuffer clBufferInput = context.createFloatBuffer(CLMem.Usage.Input, FloatBuffer.wrap(array), true);
			
			ReductionUtils.Reductor<FloatBuffer> reductor = ReductionUtils.createReductor(
				context, 
				ReductionUtils.Operation.Add, 
				ReductionUtils.Type.Float, 
				1
			);
			FloatBuffer result = reductor.reduce(queue, clBufferInput, 4097, 64);
			float sum = result.get(0);
			float expected = 4097;
			System.err.println("[Test of issue 26] Expected " + expected + ", got " + sum);
			assertEquals(expected, sum, 0);
        } catch (Exception ex) {
            ex.printStackTrace();
            assertTrue(ex.toString(), false);
        }
    }
    @Test
    public void testAddReduction() {
        try {
			int dataSize = 12345;
            int channels = 1;
            int maxReductionSize = 64;
            
            IntBuffer inBuf = NIOUtils.directInts(channels * dataSize, context.getByteOrder());
            for (int i = 0; i < dataSize; i++) {
                for (int c = 0; c < channels; c++)
                    inBuf.put(i * channels + c, i);
            }
            
            CLIntBuffer in = context.createIntBuffer(CLMem.Usage.Input, channels * dataSize);
            in.write(queue, inBuf, true);

            IntBuffer check = in.read(queue);
            for (int i = 0; i < dataSize; i++)
                assertEquals(inBuf.get(i), check.get(i));
            
            IntBuffer out = NIOUtils.directInts(channels, context.getByteOrder());
            
            Reductor<IntBuffer> reductor = ReductionUtils.createReductor(context, ReductionUtils.Operation.Add, ReductionUtils.Type.Int, channels);

            //CLEvent evt = 
        	reductor.reduce(queue, in, dataSize, out, maxReductionSize);
            //if (evt != null)
            queue.finish();
            //    evt.waitFor();
            //CLEvent[] evts = reductor.reduce(queue, in, 0, dataSize, out, maxReductionSize);
                //queue.enqueueWaitForEvents(evts);

            int expected = dataSize * (dataSize - 1) / 2;
            System.out.println("Expecting " + expected);
            for (int i = 0; i < channels; i++) {
                int value = out.get(i);
                System.out.println("out." + i + " = " + value);
                assertEquals(expected, value);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            assertTrue(ex.toString(), false);
        }
    }
}
