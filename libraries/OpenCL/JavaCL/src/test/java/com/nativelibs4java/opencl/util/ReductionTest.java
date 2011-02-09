
package com.nativelibs4java.opencl.util;

import com.nativelibs4java.opencl.*;
import static com.nativelibs4java.opencl.JavaCL.createBestContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.bridj.Pointer;
import static org.bridj.Pointer.*;
import java.util.Arrays;

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
        //context = createBestContext(CLPlatform.DeviceEvaluationStrategy.BestDoubleSupportThenBiggestMaxComputeUnits);//
        context = createBestContext();
        //System.out.println("Context = " + Arrays.asList(context.getDevices()));
        queue = context.createDefaultQueue();
    }
    
    @Test
    public void testMinMax() {
        try {
			CLBuffer<Integer> input = context.createBuffer(CLMem.Usage.Input, pointerToInts(
                1110, 22, 35535, 3, 1
            ), true);

            int maxReductionSize = 2;
            Pointer<Integer> result = allocateInt().order(context.getByteOrder());
            
            Reductor<Integer> reductor = ReductionUtils.createReductor(context, ReductionUtils.Operation.Min, OpenCLType.Int, 1);
            reductor.reduce(queue, input, input.getElementCount(), result, maxReductionSize);
            queue.finish();
            assertEquals(1, (int)result.get());

            reductor = ReductionUtils.createReductor(context, ReductionUtils.Operation.Max, OpenCLType.Int, 1);
            reductor.reduce(queue, input, input.getElementCount(), result, maxReductionSize);
            queue.finish();
            assertEquals(35535, (int)result.get());
            
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
			
			CLBuffer<Float> clBufferInput = context.createBuffer(CLMem.Usage.Input, pointerToFloats(array), true);
			
			ReductionUtils.Reductor<Float> reductor = ReductionUtils.createReductor(
				context, 
				ReductionUtils.Operation.Add, 
				OpenCLType.Float,
				1
			);
			Pointer<Float> result = reductor.reduce(queue, clBufferInput, 4097, 64);
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
            
            Pointer<Integer> inBuf = allocateInts(channels * dataSize).order(context.getByteOrder());
            for (int i = 0; i < dataSize; i++) {
                for (int c = 0; c < channels; c++)
                    inBuf.set(i * channels + c, i);
            }
            
            CLBuffer<Integer> in = context.createBuffer(CLMem.Usage.Input, Integer.class, channels * dataSize);
            in.write(queue, inBuf, true);

            Pointer<Integer> check = in.read(queue);
            for (int i = 0; i < dataSize; i++)
                assertEquals((int)inBuf.get(i), (int)check.get(i));
            
            Pointer<Integer> out = allocateInts(channels).order(context.getByteOrder());
            
            Reductor<Integer> reductor = ReductionUtils.createReductor(context, ReductionUtils.Operation.Add, OpenCLType.Int, channels);

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
