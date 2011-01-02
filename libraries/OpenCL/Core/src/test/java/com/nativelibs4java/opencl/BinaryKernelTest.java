package com.nativelibs4java.opencl;

import static com.nativelibs4java.util.NIOUtils.directBuffer;
import java.util.Map;
import static org.junit.Assert.assertEquals;

import java.nio.IntBuffer;

import org.junit.BeforeClass;
import org.junit.Test;

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

		CLIntBuffer a=context.createIntBuffer(CLMem.Usage.Input, 4);
		CLIntBuffer b=context.createIntBuffer(CLMem.Usage.Output, 4);

		IntBuffer source = directBuffer(4, context.getByteOrder(), IntBuffer.class);
		for (int i=0; i<4; i++)
			source.put(i, 3*i+10);

		a.write(queue, source, true);

		kernel.setArgs(a, b);
		kernel.enqueueNDRange(queue, new int[]{4}).waitFor();

		IntBuffer target = b.read(queue);

		assertEquals(target.capacity(), source.capacity());
		for (int i=0; i<4; i++)
			assertEquals(source.get(i), target.get(i));
    }
}