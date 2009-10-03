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
	CLImageFormat[] formatsRead2D, formatsRead3D, formatsWrite2D, formatsWrite3D;

    @Before
    public void setUp() {
		platform = OpenCL4Java.listPlatforms()[0];
		context = platform.createContext(platform.listAllDevices(true));
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
    public void simpleImage3d() {
		long width = 100, height = 200, depth = 50;//context.getDevices()[0].getImage3DMaxDepth();
		CLImageFormat format = formatsRead3D[0];
		CLImage3D im = context.createInput3D(format, width, height, depth);
		assertEquals(width, im.getWidth());
		assertEquals(height, im.getHeight());
		assertEquals(depth, im.getDepth());
		assertEquals(format, im.getFormat());
	}

}