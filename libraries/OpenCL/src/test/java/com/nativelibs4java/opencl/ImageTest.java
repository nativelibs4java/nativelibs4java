/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl;

import com.nativelibs4java.util.ImageUtils;
import java.awt.image.BufferedImage;
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
public class ImageTest extends AbstractCommon {

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

    @Test
    public void testReadWrite() {
        if (false) {
            CLImageFormat fmt = new CLImageFormat(CLImageFormat.ChannelOrder.RGBA, CLImageFormat.ChannelDataType.UnsignedInt8);
            CLImage2D clim = context.createImage2D(CLMem.Usage.InputOutput, fmt, 128, 128);
            BufferedImage im = clim.read(queue);
            queue.finish();
            int valPix = 0xff123456;
            int x = 1, y = 1;
            im.setRGB(x, y, valPix);
            clim.write(queue, im, false, true);//.waitFor();
            queue.finish();
            im = clim.read(queue);
            int[] pixs = ImageUtils.getImageIntPixels(im, false);
            int retrievedPix = im.getRGB(x, y);
            assertEquals(valPix, retrievedPix);
        }
    }


    @Test
    public void testMaxWidth() {
        if (!supportsImages())
            return;
        context.createImage2D(CLMem.Usage.Input, formatsRead2D[0], device.getImage2DMaxWidth() - 1, 1);
        long d = device.getImage3DMaxDepth();
        //TODO FAILING !!! context.createInput3D(formatsRead3D[0], device.getImage3DMaxWidth() - 1, 1, 1);
    }
    @Test
    public void testMaxHeight() {
        if (!supportsImages())
            return;
        context.createImage2D(CLMem.Usage.Input, formatsRead2D[0], 1, device.getImage2DMaxHeight() - 1);
        long d = device.getImage3DMaxDepth();
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

}