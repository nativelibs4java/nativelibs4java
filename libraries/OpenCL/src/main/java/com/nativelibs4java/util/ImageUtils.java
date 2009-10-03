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

/**
 *
 * @author ochafik
 */
public class ImageUtils {

	public static int[] getImageIntPixels(Image image, boolean allowUnoptimizingDirectRead) {
		return getImageIntPixels(image, 0, 0, image.getWidth(null), image.getHeight(null), allowUnoptimizingDirectRead);
	}
	public static int[] getImageIntPixels(Image image, int x, int y, int width, int height, boolean allowUnoptimizingDirectRead) {
		if (image instanceof BufferedImage) {
			BufferedImage bim = (BufferedImage)image;
			WritableRaster raster = bim.getRaster();
			if (allowUnoptimizingDirectRead &&
					raster.getParent() == null &&
					raster.getDataBuffer().getNumBanks() == 1)
			{
				DataBuffer b = bim.getRaster().getDataBuffer();
				if (b instanceof DataBufferInt)
					return ((DataBufferInt)b).getData();
			} else {
				return bim.getRGB(x, y, width, height, null, 0, width);
			}
		}
		PixelGrabber grabber = new PixelGrabber(image, x, y, width, height, true);
		try {
			grabber.grabPixels();
			return (int[])grabber.getPixels();
		} catch (InterruptedException ex) {
			throw new RuntimeException("Pixel read operation was interrupted", ex);
		}
	}
}
