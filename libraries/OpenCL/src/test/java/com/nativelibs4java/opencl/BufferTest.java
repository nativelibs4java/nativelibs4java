/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl;

import com.nativelibs4java.util.ImageUtils;
import static com.nativelibs4java.util.NIOUtils.*;
import java.awt.image.BufferedImage;
import java.nio.IntBuffer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author ochafik
 */
public class BufferTest extends AbstractCommon {

	@Test
	public void testReadWrite() {
		int n = 10;
		CLIntBuffer buf = context.createIntBuffer(CLMem.Usage.InputOutput, n);

		IntBuffer initial = directInts(n);
		for (int i = 0; i < n; i++)
			initial.put(i, i + 1);
		
		buf.write(queue, initial, true);
		
		IntBuffer retrieved = buf.read(queue);
		assertEquals(buf.getByteCount() / 4, retrieved.capacity());

		retrieved.rewind();
		initial.rewind();

		for (int i = 0; i < n; i++) {
			int ini = initial.get(i);
			int ret = retrieved.get(i);
			assertEquals(initial.get(i), retrieved.get(i));
		}
	}


	@Test
	public void testMaxWidth() {
		//try {
			context.createInput(device.getMaxMemAllocSize());
		//} catch (CLException ex) {

		//}
	}
}