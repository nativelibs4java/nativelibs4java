/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.util;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.PixelGrabber;
import java.awt.image.WritableRaster;
import java.nio.IntBuffer;

/**
 *
 * @author ochafik
 */
public class ImageUtils {

	public static int[] getImageIntPixels(Image image, boolean allowDeoptimizingDirectRead) {
		return getImageIntPixels(image, 0, 0, image.getWidth(null), image.getHeight(null), allowDeoptimizingDirectRead);
	}
	public static int[] getImageIntPixels(Image image, int x, int y, int width, int height, boolean allowDeoptimizingDirectRead) {
		if (image instanceof BufferedImage) {
			BufferedImage bim = (BufferedImage)image;
			WritableRaster raster = bim.getRaster();
			if (allowDeoptimizingDirectRead &&
					raster.getParent() == null &&
					raster.getDataBuffer().getNumBanks() == 1)
			{
				DataBuffer b = bim.getRaster().getDataBuffer();
				if (b instanceof DataBufferInt) {
					int[] array = ((DataBufferInt)b).getData();
					return array;
				}
			}
			return bim.getRGB(x, y, width, height, null, 0, width);
		}
		PixelGrabber grabber = new PixelGrabber(image, x, y, width, height, true);
		try {
			grabber.grabPixels();
			return (int[])grabber.getPixels();
		} catch (InterruptedException ex) {
			throw new RuntimeException("Pixel read operation was interrupted", ex);
		}
	}

	public static void setImageIntPixels(BufferedImage image, boolean allowDeoptimizingDirectRead, IntBuffer pixels) {
		setImageIntPixels(image, 0, 0, image.getWidth(null), image.getHeight(null), allowDeoptimizingDirectRead, pixels);
	}
	public static void setImageIntPixels(BufferedImage bim, int x, int y, int width, int height, boolean allowDeoptimizingDirectRead, IntBuffer pixels) {
		WritableRaster raster = bim.getRaster();
		if (allowDeoptimizingDirectRead &&
				raster.getParent() == null &&
				raster.getDataBuffer().getNumBanks() == 1)
		{
			DataBuffer b = bim.getRaster().getDataBuffer();
			if (b instanceof DataBufferInt) {
				IntBuffer.wrap(((DataBufferInt)b).getData()).put(pixels);
				return;
			}
		}

		IntBuffer b = IntBuffer.allocate(width * height);
		b.put(pixels);
		b.rewind();
		int[] array = b.array();
		bim.setRGB(x, y, width, height, array, 0, width);
	}
}
