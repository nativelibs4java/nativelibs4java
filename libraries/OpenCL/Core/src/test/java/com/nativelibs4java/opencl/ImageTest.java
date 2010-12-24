/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl;

import org.bridj.Pointer;
import com.nativelibs4java.opencl.CLImageFormat.ChannelDataType;
import com.nativelibs4java.opencl.CLImageFormat.ChannelOrder;
import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.nio.IntBuffer;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.nativelibs4java.test.MiscTestUtils;
import com.nativelibs4java.util.ImageUtils;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.List;

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
                assertEquals("Different value for pixel x = " + x + ", y = " + y, Integer.toHexString(expected.getRGB(x, y)), Integer.toHexString(real.getRGB(x, y)));
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
	public void testRGBAImageSource() {
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
                    "   int width,                                                  \n" +
                    "   int height,                                                 \n" +
                    "   __global float* output)                                       \n" +
                    "{                                                              \n" +
                    "   int x = get_global_id(0);                                   \n" +
					"   int y = get_global_id(1);                                   \n" +
					"   int2 coord = (int2)(x, y);	                                    \n" +
					"   float4 pixel = read_imagef(src_image, sampler, coord);        \n" +
                    "   int offset = (y * width + x) * 4;                           \n" +
					"	output[offset] = pixel.x;                                   \n" +
                    "	output[offset + 1] = pixel.y;                               \n" +
                    "	output[offset + 2] = pixel.z;                               \n" +
                    "	output[offset + 3] = pixel.w;                               \n" +
                    "}                                                              \n";

			int width = 16, height = 16;
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			for (int x = 0; x < width; x++)
                for (int y = 0; y < height; y++) {
                    int red = (x + y);
                    int green = 2 * (x + y);
                    int blue = (x + 2 * y);
                    int alpha = (2 * x + y);

                    int p = (alpha << 24) | (blue << 16) | (green << 8) | red;
                    image.setRGB(x, y, p);
                }
				//image.setRGB(i, 0, i);
                //image.setRGB(i, height - 1, i);
			
            CLProgram program = context.createProgram(src).build();
			//CLBuffer<Integer> cloutput = context.createBuffer(CLMem.Usage.Output, Integer.class, width * height * 4);
            CLBuffer<Float> cloutput = context.createBuffer(CLMem.Usage.Output, Float.class, width * height * 4);
            CLKernel kernel = program.createKernel("test");

            ChannelDataType channelDataType = CLImageFormat.INT_ARGB_FORMAT.getChannelDataType();
            for (ChannelOrder channelOrder : Arrays.asList(ChannelOrder.BGRA, ChannelOrder.RGBA)) {
                CLImageFormat imageFormat = new CLImageFormat(channelOrder, channelDataType);

                //CLImage2D climage = context.createImage2D(CLMem.Usage.Input, image, true);
                CLImage2D climage = context.createImage2D(CLMem.Usage.Input, imageFormat, width, height);
                climage.write(queue, image);
                kernel.setArgs(
                    climage,
                    width, height,
                    cloutput
                );

                kernel.enqueueNDRange(queue, new int[] {width, height}, new int[]{1, 1}).waitFor();

                //IntBuffer output = cloutput.read(queue);
                //IntBuffer output = cloutput.readBytes(queue, 0, width * height * 4 * 4).order(ByteOrder.BIG_ENDIAN).asIntBuffer();
                Pointer<Float> output = cloutput.read(queue);
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        int expected = image.getRGB(x, y);
                        float expectedRed = (x + y) / (float)0xff;
                        float expectedGreen = (2 * (x + y)) / (float)0xff;
                        float expectedBlue = (x + 2 * y) / (float)0xff;
                        float expectedAlpha = (2 * x + y) / (float)0xff;

                        int offset = (y * width + x) * 4;
                        /*int r = output.get(offset);
                        int g = output.get(offset + 1);
                        int b = output.get(offset + 2);
                        int a = output.get(offset + 3);
                        System.out.println("(x, y) = (" + x + ", "+ y + ") : expected " + Integer.toHexString(expected) + ", r = " + Integer.toHexString(r) + ", g = " + Integer.toHexString(g) + ", b = " + Integer.toHexString(b) + ", a = " + Integer.toHexString(a));
                        System.out.println("(x, y) = (" + x + ", "+ y + ") : expected " + (expectedChannel / (double)0xff) + ", r = " + (r / (double)Integer.MAX_VALUE) + ", g = " + (g / (double)Integer.MAX_VALUE) + ", b = " + (b / (double)Integer.MAX_VALUE) + ", a = " + (a / (double)Integer.MAX_VALUE));*/
                        float px = output.get(offset);
                        float py = output.get(offset + 1);
                        float pz = output.get(offset + 2);
                        float pw = output.get(offset + 3);

                        float red, green, blue, alpha;
                        switch (climage.getFormat().getChannelOrder()) {
                            case RGBA:
                                red = px;
                                green = py;
                                blue = pz;
                                alpha = pw;
                                break;
                            case ARGB:
                                alpha = px;
                                red = py;
                                green = pz;
                                blue = pw;
                                break;
                            case BGRA:
                                blue = px;
                                green = py;
                                red = pz;
                                alpha = pw;
                                break;
                            default:
                                assertTrue("Channel order not handled in this test : " + climage.getFormat(), false);
                                return;
                        }
                        float tolerance = 0.00001f;
                        String rgba = "(r = " + red + ", green = " + green + ", blue = " + blue + ", alpha = " + alpha + ")";
                        String expectedRgba = "(r = " + expectedRed + ", green = " + expectedGreen + ", blue = " + expectedBlue + ", alpha = " + expectedAlpha + ")";
                        assertEquals("[" + imageFormat + " format] bad red value for (x, y) = (" + x + ", " + y + ") : \n\t     Got " + rgba + ", \n\tExpected " + expectedRgba, expectedRed, red, tolerance);
                        assertEquals("[" + imageFormat + " format] bad green value for (x, y) = (" + x + ", " + y + ") : " + rgba + " expected " + expectedRgba, expectedGreen, green, tolerance);
                        assertEquals("[" + imageFormat + " format] bad blue value for (x, y) = (" + x + ", " + y + ") : " + rgba + " expected " + expectedRgba, expectedBlue, blue, tolerance);
                        assertEquals("[" + imageFormat + " format] bad alpha value for (x, y) = (" + x + ", " + y + ") : " + rgba + " expected " + expectedRgba, expectedAlpha, alpha, tolerance);
                        //System.out.println("(x, y) = (" + x + ", "+ y + ") : expected " + (expectedChannel / (double)0xff) + ", r = " + (r) + ", g = " + (g) + ", b = " + (b) + ", a = " + (a));

                    }
                }
            }

            /*
			for (int i = 0; i < width; i++) {
				int value = output.get(i);
                //System.out.println(value);
				Assert.assertEquals(i, value);
			}
             * 
             */
        } catch (Exception ex) {
            ex.printStackTrace();
        }
	}


    @Test
	public void testShortGrayImageSource() {
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
                    "   int width,                                                  \n" +
                    "   int height,                                                 \n" +
                    "   __global float* output)                                       \n" +
                    "{                                                              \n" +
                    "   int x = get_global_id(0);                                   \n" +
					"   int y = get_global_id(1);                                   \n" +
					"   int2 coord = (int2)(x, y);	                                    \n" +
					"   float4 pixel = read_imagef(src_image, sampler, coord);        \n" +
                    "   int offset = (y * width + x) * 4;                           \n" +
					"	output[offset] = pixel.x;                                   \n" +
                    "	output[offset + 1] = pixel.y;                               \n" +
                    "	output[offset + 2] = pixel.z;                               \n" +
                    "	output[offset + 3] = pixel.w;                               \n" +
                    "}                                                              \n";

			int width = 16, height = 16;
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_GRAY);
            short[] data = new short[width * height];
			for (short x = 0; x < width; x++)
                for (short y = 0; y < height; y++) {
                    short value = (short) (10 * (x + y));
                    data[y * height + x] = value;
                }

            image.getRaster().setDataElements(0, 0, width, height, data);

            CLProgram program = context.createProgram(src).build();
			CLBuffer<Float> cloutput = context.createBuffer(CLMem.Usage.Output, Float.class, width * height * 4);

            CLKernel kernel = program.createKernel("test");

            List<CLImageFormat> formats = Arrays.asList(context.getSupportedImageFormats(CLMem.Flags.WriteOnly, CLMem.ObjectType.Image2D));
            //System.out.println("Supported formats = " + formats);
            ChannelDataType channelDataType = ChannelDataType.UNormInt16;
            for (ChannelOrder channelOrder : Arrays.asList(ChannelOrder.RGBA)) {
                CLImageFormat imageFormat = new CLImageFormat(channelOrder, channelDataType);

                //CLImage2D climage = context.createImage2D(CLMem.Usage.Input, image, true);
                CLImage2D climage = context.createImage2D(CLMem.Usage.Input, imageFormat, width, height);
                climage.write(queue, image);
                kernel.setArgs(
                    climage,
                    width, height,
                    cloutput
                );

                kernel.enqueueNDRange(queue, new int[] {width, height}, new int[]{1, 1}).waitFor();

                //IntBuffer output = cloutput.read(queue);
                //IntBuffer output = cloutput.readBytes(queue, 0, width * height * 4 * 4).order(ByteOrder.BIG_ENDIAN).asIntBuffer();
                Pointer<Float> output = cloutput.read(queue);
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        float expected = 10f * (x + y) / (float)0xffff;
                        int offset = (y * width + x) * 4;
                        float px = output.get(offset);
                        float py = output.get(offset + 1);
                        float pz = output.get(offset + 2);
                        float pw = output.get(offset + 3);

                        float tolerance = 0.00001f;
                        
                        assertEquals("[" + imageFormat + " format] different RGB components for gray image", px, py, tolerance);
                        assertEquals("[" + imageFormat + " format] different RGB components for gray image", px, pz, tolerance);
                        assertEquals("[" + imageFormat + " format] alpha not solid for gray image", 1f, pw, tolerance);
                        
                        float actual = px;
                        assertEquals("[" + imageFormat + " format] bad value for (x, y) = (" + x + ", " + y + ") : \n\t     Got " + actual + ", \n\tExpected " + expected, expected, actual, tolerance);
                    }
                }
            }

            /*
			for (int i = 0; i < width; i++) {
				int value = output.get(i);
                //System.out.println(value);
				Assert.assertEquals(i, value);
			}
             *
             */
        } catch (Exception ex) {
            ex.printStackTrace();
        }
	}
}