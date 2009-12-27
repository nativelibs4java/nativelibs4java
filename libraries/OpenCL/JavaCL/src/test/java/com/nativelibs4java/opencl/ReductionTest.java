
package com.nativelibs4java.opencl;

import com.nativelibs4java.opencl.*;

import com.nativelibs4java.util.NIOUtils;
import com.nativelibs4java.opencl.ReductionUtils.Reductor;
import com.nativelibs4java.test.MiscTestUtils;
import java.lang.reflect.*;
import java.util.EnumSet;
import java.util.logging.*;
import org.junit.*;
import static org.junit.Assert.*;
import static com.nativelibs4java.test.MiscTestUtils.*;
import java.nio.*;
import java.util.*;
import static com.nativelibs4java.opencl.JavaCL.*;
import static com.nativelibs4java.util.NIOUtils.*;

public class ReductionTest {

    @BeforeClass
    public static void setup() {
        MiscTestUtils.protectJNI();
    }

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
            IntBuffer result = NIOUtils.directInts(1);
            
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
    @Test
    public void testAddReduction() {
        try {
			int dataSize = 12345;
            int channels = 1;
            int maxReductionSize = 64;
            
            IntBuffer inBuf = NIOUtils.directInts(channels * dataSize);
            for (int i = 0; i < dataSize; i++) {
                for (int c = 0; c < channels; c++)
                    inBuf.put(i * channels + c, i);
            }
            
            CLIntBuffer in = context.createIntBuffer(CLMem.Usage.Input, channels * dataSize);
            in.write(queue, inBuf, true);

            IntBuffer check = in.read(queue);
            for (int i = 0; i < dataSize; i++)
                assertEquals(inBuf.get(i), check.get(i));
            
            IntBuffer out = NIOUtils.directInts(channels);
            
            Reductor<IntBuffer> reductor = ReductionUtils.createReductor(context, ReductionUtils.Operation.Add, ReductionUtils.Type.Int, channels);

            CLEvent evt = reductor.reduce(queue, in, dataSize, out, maxReductionSize);
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
