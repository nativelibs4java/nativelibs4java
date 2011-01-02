package com.nativelibs4java.opencl;

import java.util.Map;
import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import org.bridj.Pointer;
import static org.bridj.Pointer.*;
import com.nativelibs4java.test.MiscTestUtils;

/**
 *
 * @author Kazo Csaba
 */
@SuppressWarnings("unchecked")
public class BinaryKernelTest extends AbstractCommon {

    @BeforeClass
    public static void setup() {
        MiscTestUtils.protectJNI();
    }

    @Test
    public void simpleTest() throws CLBuildException {
		CLProgram program = context.createProgram(
				  "__kernel void copy(__global int* a, __global int* b) {\n" +
				  "   int i = get_global_id(0);\n" +
				  "   b[i]=a[i];\n" +
				  "} ");
		program.build();
		Map<CLDevice, byte[]> binaries = program.getBinaries();
		program.release();
		
		CLProgram binaryProgram = context.createProgram(binaries, null);
		CLKernel kernel = binaryProgram.createKernel("copy");

		CLBuffer<Integer> a=context.createBuffer(CLMem.Usage.Input, Integer.class, 4);
		CLBuffer<Integer> b=context.createBuffer(CLMem.Usage.Output, Integer.class, 4);

		Pointer<Integer> source = allocateInts(4).order(context.getByteOrder());
		for (int i=0; i<4; i++)
			source.set(i, 3*i+10);

		a.write(queue, source, true);

		kernel.setArgs(a, b);
		kernel.enqueueNDRange(queue, new int[]{4}).waitFor();

		Pointer<Integer> target = b.read(queue);

		assertEquals(target.getValidElements(), source.getValidElements());
		for (int i=0; i<4; i++)
			assertEquals(source.get(i), target.get(i));
    }
}