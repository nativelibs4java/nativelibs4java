/*
	Copyright (c) 2009 Olivier Chafik (http://ochafik.free.fr/)
	
	This file is part of OpenCL4Java (http://code.google.com/p/nativelibs4java/wiki/OpenCL).
	
	OpenCL4Java is free software: you can redistribute it and/or modify
	it under the terms of the GNU Lesser General Public License as published by
	the Free Software Foundation, either version 2.1 of the License, or
	(at your option) any later version.
	
	OpenCL4Java is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU Lesser General Public License for more details.
	
	You should have received a copy of the GNU Lesser General Public License
	along with OpenCL4Java.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.nativelibs4java.opencl;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_IMAGE_HEIGHT;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_IMAGE_ROW_PITCH;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_IMAGE_WIDTH;
import static com.nativelibs4java.util.ImageUtils.getImageIntPixels;
import static com.nativelibs4java.util.ImageUtils.setImageIntPixels;
import static com.nativelibs4java.util.JNAUtils.toNS;
import static com.nativelibs4java.util.NIOUtils.directInts;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static com.bridj.Pointer.*;

import com.nativelibs4java.opencl.library.OpenCLLibrary.cl_mem;
import com.ochafik.util.listenable.Pair;

/**
 * OpenCL 2D Image Memory Object<br/>
 * @see CLContext#createImage2D(com.nativelibs4java.opencl.CLMem.Usage, java.awt.Image, boolean)
 * @see CLContext#createImage2D(com.nativelibs4java.opencl.CLMem.Usage, com.nativelibs4java.opencl.CLImageFormat, long, long)
 * @see CLContext#createImage2D(com.nativelibs4java.opencl.CLMem.Usage, com.nativelibs4java.opencl.CLImageFormat, long, long, long)
 * @see CLContext#createImage2D(com.nativelibs4java.opencl.CLMem.Usage, com.nativelibs4java.opencl.CLImageFormat, long, long, long, java.nio.Buffer, boolean)
 * @see CLContext#createImage2DFromGLRenderBuffer(com.nativelibs4java.opencl.CLMem.Usage, int)
 * @see CLContext#createImage2DFromGLTexture2D(com.nativelibs4java.opencl.CLMem.Usage, com.nativelibs4java.opencl.CLContext.GLTextureTarget, int, int) 
 * @author Olivier Chafik
 */
public class CLImage2D extends CLImage {
	CLImage2D(CLContext context, cl_mem entity, CLImageFormat format) {
        super(context, entity, format);
	}

	/**
	 * Return size in bytes of a row of elements of the image object given by image.
	 */
	@InfoName("CL_IMAGE_ROW_PITCH")
	public long getRowPitch() {
		return infos.getIntOrLong(getEntity(), CL_IMAGE_ROW_PITCH);
	}

	/**
	 * Return width of the image in pixels
	 */
	@InfoName("CL_IMAGE_WIDTH")
	public long getWidth() {
		return infos.getIntOrLong(getEntity(), CL_IMAGE_WIDTH);
	}

	/**
	 * Return height of the image in pixels
	 */
	@InfoName("CL_IMAGE_HEIGHT")
	public long getHeight() {
		return infos.getIntOrLong(getEntity(), CL_IMAGE_HEIGHT);
	}

	public CLEvent read(CLQueue queue, long minX, long minY, long width, long height, long rowPitch, Buffer out, boolean blocking, CLEvent... eventsToWaitFor) {
		return read(queue, pointerToSizeTs(minX, minY, 0), pointerToSizeTs(width, height, 1), rowPitch, 0, out, blocking, eventsToWaitFor);
	}
	public CLEvent write(CLQueue queue, long minX, long minY, long width, long height, long rowPitch, Buffer in, boolean blocking, CLEvent... eventsToWaitFor) {
		return write(queue, pointerToSizeTs(minX, minY, 0), pointerToSizeTs(width, height, 1), rowPitch, 0, in, blocking, eventsToWaitFor);
	}

	public BufferedImage read(CLQueue queue) {
		BufferedImage im = new BufferedImage((int)getWidth(), (int)getHeight(), BufferedImage.TYPE_INT_ARGB);
		read(queue, im, false);
		return im;
	}
	public void read(CLQueue queue, BufferedImage imageOut, boolean allowDeoptimizingDirectWrite, CLEvent... eventsToWaitFor) {
		if (!getFormat().isIntBased())
			throw new IllegalArgumentException("Image-read only supports int-based RGBA images");

		int width = imageOut.getWidth(null), height = imageOut.getHeight(null);
		IntBuffer dataOut = directInts(width * height, getContext().getByteOrder());
		read(queue, 0, 0, width, height, 0, dataOut, true, eventsToWaitFor);
		setImageIntPixels(imageOut, allowDeoptimizingDirectWrite, dataOut);
	}
	public CLEvent write(CLQueue queue, Image image, CLEvent... eventsToWaitFor) {
		return write(queue, image, 0, 0, image.getWidth(null), image.getHeight(null), false, false, eventsToWaitFor);
	}
	public CLEvent write(CLQueue queue, Image image, boolean allowDeoptimizingDirectRead, boolean blocking, CLEvent... eventsToWaitFor) {
		return write(queue, image, 0, 0, image.getWidth(null), image.getHeight(null), allowDeoptimizingDirectRead, blocking, eventsToWaitFor);
	}
	public CLEvent write(CLQueue queue, Image image, int destX, int destY, int width, int height, boolean allowDeoptimizingDirectRead, boolean blocking, CLEvent... eventsToWaitFor) {
		//int imWidth = image.getWidth(null), height = image.getHeight(null);
		return write(queue, 0, 0, width, height, width * 4, IntBuffer.wrap(getImageIntPixels(image, allowDeoptimizingDirectRead)), blocking, eventsToWaitFor);
	}
	public void write(CLQueue queue, BufferedImage imageIn, boolean allowDeoptimizingDirectRead, CLEvent... eventsToWaitFor) {
		if (!getFormat().isIntBased())
			throw new IllegalArgumentException("Image read only supports int-based RGBA images");

		int width = imageIn.getWidth(null), height = imageIn.getHeight(null);
		int[] pixels = getImageIntPixels(imageIn, allowDeoptimizingDirectRead);
		write(queue, 0, 0, width, height, 0, IntBuffer.wrap(pixels), true, eventsToWaitFor);
	}
	public void write(CLQueue queue, BufferedImage im) {
		write(queue, im, false);
	}

    public ByteBuffer map(CLQueue queue, MapFlags flags, CLEvent... eventsToWaitFor) {
        return map(queue, flags, pointerToSizeTs(0, 0), pointerToSizeTs(getWidth(), getHeight()), getWidth(), null, true, eventsToWaitFor).getFirst();
    }
	public ByteBuffer map(CLQueue queue, MapFlags flags, long offsetX, long offsetY, long lengthX, long lengthY, long rowPitch, CLEvent... eventsToWaitFor) {
		return map(queue, flags, pointerToSizeTs(offsetX, offsetY), pointerToSizeTs(lengthX, lengthY), rowPitch, null, true, eventsToWaitFor).getFirst();
    }
	public Pair<ByteBuffer, CLEvent> mapLater(CLQueue queue, MapFlags flags, boolean blocking, CLEvent... eventsToWaitFor) {
		return map(queue, flags, pointerToSizeTs(0, 0), pointerToSizeTs(getWidth(), getHeight()), getWidth(), null, blocking, eventsToWaitFor);
    }
    public Pair<ByteBuffer, CLEvent> mapLater(CLQueue queue, MapFlags flags, long offsetX, long offsetY, long lengthX, long lengthY, long rowPitch, boolean blocking, CLEvent... eventsToWaitFor) {
		return map(queue, flags, pointerToSizeTs(offsetX, offsetY), pointerToSizeTs(lengthX, lengthY), rowPitch, null, blocking, eventsToWaitFor);
    }
}
