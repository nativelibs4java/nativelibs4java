/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl;

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
public class ImageTest {

    public ImageTest() {
    }

	CLPlatform platform;
	CLContext context;
	CLQueue queue;
	CLDevice device;
	CLImageFormat[] formatsRead2D, formatsRead3D, formatsWrite2D, formatsWrite3D;

    @Before
    public void setUp() {
		platform = OpenCL4Java.listPlatforms()[0];
		context = platform.createContext(platform.listAllDevices(true));
		queue = context.createDefaultQueue();
		device = context.getDevices()[0];
		formatsRead2D = context.getSupportedImageFormats(CLMem.Flags.ReadOnly, CLMem.ObjectType.Image2D);
		formatsWrite2D = context.getSupportedImageFormats(CLMem.Flags.WriteOnly, CLMem.ObjectType.Image2D);
		formatsRead3D = context.getSupportedImageFormats(CLMem.Flags.ReadOnly, CLMem.ObjectType.Image3D);
		formatsWrite3D = context.getSupportedImageFormats(CLMem.Flags.WriteOnly, CLMem.ObjectType.Image3D);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void simpleImage2d() {
		long width = 100, height = 200;
		CLImageFormat format = formatsRead2D[0];
		CLImage2D im = context.createInput2D(format, width, height);
		assertEquals(width, im.getWidth());
		assertEquals(height, im.getHeight());
		assertEquals(format, im.getFormat());
	}
 
	@Test
	public void testMaxWidth() {
		context.createInput2D(formatsRead2D[0], device.getImage2DMaxWidth(), 1);
		long d = device.getImage3DMaxDepth();
		//TODO FAILING !!! context.createInput3D(formatsRead3D[0], device.getImage3DMaxWidth() - 1, 1, 1);
	}
	@Test
	public void testMaxHeight() {
		context.createInput2D(formatsRead2D[0], 1, device.getImage2DMaxHeight());
		long d = device.getImage3DMaxDepth();
		//TODO FAILING !!! context.createInput3D(formatsRead3D[0], 1, device.getImage3DMaxHeight(), 1);
	}
	@Test
	public void testMaxDepth() {
		context.createInput3D(formatsRead3D[0], 1, 1, device.getImage3DMaxDepth());
	}

	@Test(expected=CLException.InvalidImageSize.class)
	public void testInvalidImageSize() {
		CLImage2D im = context.createInput2D(formatsRead2D[0], device.getImage2DMaxWidth() + 1, 1);
	}
	public void simpleImage3d() {
		long width = 100, height = 200, depth = 50;//device.getImage3DMaxDepth();
		CLImageFormat format = formatsRead3D[0];
		CLImage3D im = context.createInput3D(format, width, height, depth);
		assertEquals(width, im.getWidth());
		assertEquals(height, im.getHeight());
		assertEquals(depth, im.getDepth());
		assertEquals(format, im.getFormat());
		//im.blockingMap(queue, CLMem.MapFlags.Read);
	}

}