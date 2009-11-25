
package com.nativelibs4java.opencl;

import com.nativelibs4java.opencl.*;

import com.nativelibs4java.util.NIOUtils;
import com.nativelibs4java.opencl.ReductionUtils.Reductor;
import java.lang.reflect.*;
import java.util.EnumSet;
import java.util.logging.*;
import org.junit.*;
import static org.junit.Assert.*;
import static com.nativelibs4java.test.MiscTestUtils.*;
import java.nio.*;
import java.util.*;
import static com.nativelibs4java.opencl.OpenCL4Java.*;
import static com.nativelibs4java.util.NIOUtils.*;

public class ReductionTest {

    public static final double ABSOLUTE_FLOAT_ERROR_TOLERANCE = 1e-4;
    public static final double RELATIVE_FLOAT_ERROR_TOLERANCE = 1e-8;

    @Test
    public void testAdd() {
        try {
			CLContext context = createBestContext();
            CLQueue queue = context.createDefaultQueue();
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
            IntBuffer out = NIOUtils.directInts(channels);
            
            Reductor<IntBuffer> reductor = ReductionUtils.createReductor(context, ReductionUtils.Operation.Add, ReductionUtils.Type.Int, channels);

            CLEvent evt = reductor.reduce(queue, in, 0, dataSize, out, maxReductionSize);
            //if (evt != null)
                evt.waitFor();
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
        }
    }
}
