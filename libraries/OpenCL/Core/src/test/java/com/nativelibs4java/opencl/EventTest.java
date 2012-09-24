package com.nativelibs4java.opencl;

import java.util.Map;
import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import org.bridj.Pointer;
import static org.bridj.Pointer.*;
import com.nativelibs4java.test.MiscTestUtils;
import java.util.List;
import org.junit.runners.Parameterized;

@SuppressWarnings("unchecked")
public class EventTest extends AbstractCommon {
    public EventTest(CLDevice device) {
        super(device);
    }
    
    @Parameterized.Parameters
    public static List<Object[]> getDeviceParameters() {
        return AbstractCommon.getDeviceParameters();
    }

    @Test
    public void simpleTest() throws CLBuildException {
		CLKernel kernel = context.createProgram(
			"__kernel void copy(__global int* a, __global int* b) {\n" +
			"   int i = get_global_id(0);\n" +
			"   b[i]=a[i];\n" +
			"} "
		).createKernel("copy");
		
		CLBuffer<Integer> 
			a = context.createBuffer(CLMem.Usage.Input, Integer.class, 4),
			b = context.createBuffer(CLMem.Usage.Output, Integer.class, 4);

		kernel.setArgs(a, b);
		
		int[] globalSizes = new int[]{4};
		CLEvent e = kernel.enqueueNDRange(queue, globalSizes);
		assertNotNull(e);
		e.waitFor();
		assertNull(kernel.enqueueNDRange(queue, globalSizes, CLEvent.FIRE_AND_FORGET));
    }
}
