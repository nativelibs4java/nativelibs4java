/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl;

import com.nativelibs4java.opencl.CLImageFormat.ChannelDataType;
import com.nativelibs4java.opencl.CLImageFormat.ChannelOrder;
import static org.junit.Assert.assertEquals;

import java.awt.image.BufferedImage;
import java.nio.IntBuffer;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.nativelibs4java.test.MiscTestUtils;
import com.nativelibs4java.util.ImageUtils;

/**
 *
 * @author ochafik
 */
public class ImageTest extends AbstractCommon {

    @BeforeClass
    public static void setup() {
        MiscTestUtils.protectJNI();
    }

    public boolean supportsImages() {
        for (CLDevice device : context.getDevices())
            if (device.hasImageSupport())
                return true;
        return false;
    }
    @Test
    public void simpleImage2d() {
        if (!supportsImages())
            return;
        long width = 100, height = 200;
        CLImageFormat format = formatsRead2D[0];
        CLImage2D im = context.createImage2D(CLMem.Usage.InputOutput, format, width, height);
        assertEquals(width, im.getWidth());
        assertEquals(height, im.getHeight());
        assertEquals(format, im.getFormat());
    }

    int someARGBPixelValue = 0xff123456;
    int someARGBGrayPixelValue = 0xffababab;
    int someShortGrayPixelValue = 0xf1f1;

            
    @Test
    public void testCreateARGBFromImage() {
        int width = 2, height = 2;
        BufferedImage im = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        im.setRGB(1, 0, someARGBPixelValue);

        CLImage2D clim = context.createImage2D(CLMem.Usage.InputOutput, im, true);
        assertEquals(width, clim.getWidth());
        assertEquals(height, clim.getHeight());
        assertEquals(CLImageFormat.INT_ARGB_FORMAT, clim.getFormat());
        assertSameImage(im, clim.read(queue));
    }

    static void assertSameImage(BufferedImage expected, BufferedImage real) {
        int width = expected.getWidth(), height = expected.getHeight();
        assertEquals("Bad width", width, real.getWidth());
        assertEquals("Bad height", height, real.getHeight());

        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++)
                assertEquals("Different value for pixel x = " + x + ", y = " + y, expected.getRGB(x, y), real.getRGB(x, y));
    }

    @Test
    public void testARGBReadWrite() {
        int width = 2, height = 2;
        CLImage2D clim = context.createImage2D(CLMem.Usage.InputOutput, CLImageFormat.INT_ARGB_FORMAT, width, height);
        assertEquals(width, clim.getWidth());
        assertEquals(height, clim.getHeight());

        BufferedImage im = clim.read(queue);
        assertEquals(BufferedImage.TYPE_INT_ARGB, im.getType());
        int x = 0, y = 1;
        im.setRGB(x, y, someARGBPixelValue);
        clim.write(queue, im, false, true);

        assertSameImage(im, clim.read(queue));
    }

    @Test
    public void testARGBShortGrayReadWrite() {
        int width = 2, height = 2;
        CLImage2D clim = context.createImage2D(CLMem.Usage.InputOutput, new CLImageFormat(ChannelOrder.RGBA, ChannelDataType.UnsignedInt16), width, height);
        assertEquals(width, clim.getWidth());
        assertEquals(height, clim.getHeight());

        BufferedImage im = clim.read(queue);
        assertEquals(BufferedImage.TYPE_USHORT_GRAY, im.getType());
        int x = 0, y = 1;
        im.setRGB(x, y, someARGBGrayPixelValue);
        clim.write(queue, im, false, true);

        assertSameImage(im, clim.read(queue));
    }



    @Test
    public void testMaxWidth() {
        if (!supportsImages())
            return;
        context.createImage2D(CLMem.Usage.Input, formatsRead2D[0], device.getImage2DMaxWidth() - 1, 1);
        //long d = device.getImage3DMaxDepth();
        //TODO FAILING !!! context.createInput3D(formatsRead3D[0], device.getImage3DMaxWidth() - 1, 1, 1);
    }
    @Test
    public void testMaxHeight() {
        if (!supportsImages())
            return;
        context.createImage2D(CLMem.Usage.Input, formatsRead2D[0], 1, device.getImage2DMaxHeight() - 1);
        //long d = device.getImage3DMaxDepth();
        //TODO FAILING !!! context.createInput3D(formatsRead3D[0], 1, device.getImage3DMaxHeight(), 1);
    }
    @Test
    public void testMaxDepth() {
        if (!supportsImages())
            return;
        context.createImage3D(CLMem.Usage.Input, formatsRead3D[0], 1, 1, device.getImage3DMaxDepth() - 1);
    }

    /*@Test(expected=CLException.InvalidImageSize.class)
    public void testInvalidImageSize() {
            CLImage2D im = context.createImage2D(CLMem.Usage.Input, formatsRead2D[0], device.getImage2DMaxWidth() + 10, 1);
    }*/
    public void simpleImage3d() {
        long width = 100, height = 200, depth = 50;//device.getImage3DMaxDepth();
        CLImageFormat format = formatsRead3D[0];
        CLImage3D im = context.createImage3D(CLMem.Usage.Input, format, width, height, depth);
        assertEquals(width, im.getWidth());
        assertEquals(height, im.getHeight());
        assertEquals(depth, im.getDepth());
        assertEquals(format, im.getFormat());
        //im.blockingMap(queue, CLMem.MapFlags.Read);
    }

	@Test
	public void testImageSource() {
            if (!supportsImages())
                return;
            try {
			CLContext context = JavaCL.createBestContext();
            CLQueue queue = context.createDefaultQueue();
			String src = "\n" +
					"const sampler_t sampler =										\n" +
					"		CLK_NORMALIZED_COORDS_FALSE |							\n" +
					"		CLK_FILTER_NEAREST |									\n" +
					"		CLK_ADDRESS_CLAMP_TO_EDGE;								\n" +
					"																\n" +
                    "__kernel void test(                                            \n" +
                    "   __read_only image2d_t src_image,                            \n" +
                    "   __global int* output)                                       \n" +
                    "{                                                              \n" +
                    "   int i = get_global_id(0);                                   \n" +
					"   int2 coord = (0, i);	                                    \n" +
					"   int4 pixel = read_imagei(src_image, sampler, coord);        \n" +
					"	output[i] = pixel.x;// + pixel.y + pixel.z + pixel.w;		\n" +
                    "}                                                              \n";

			int width = 20, height = 2;
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			for (int i = 0; i < width; i++)
                //for (int j = 0; j < height; j++)
                //    image.setRGB(i, j, i + j);
				image.setRGB(i, 0, i);
                //image.setRGB(i, height - 1, i);
			
            CLProgram program = context.createProgram(src).build();
			CLIntBuffer cloutput = context.createIntBuffer(CLMem.Usage.Output, width);
			CLKernel kernel = program.createKernel(
				"test",
				context.createImage2D(CLMem.Usage.Input, image, true),
				cloutput
			);
            
            kernel.enqueueNDRange(queue, new int[] {width}, new int[]{1}).waitFor();

			IntBuffer output = cloutput.read(queue);
			for (int i = 0; i < width; i++) {
				int value = output.get(i);
                //System.out.println(value);
				Assert.assertEquals(i, value);
			}
        } catch (Exception ex) {
            ex.printStackTrace();
        }
	}
}