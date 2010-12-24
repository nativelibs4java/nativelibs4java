/*
 * JavaCL - Java API and utilities for OpenCL
 * http://javacl.googlecode.com/
 *
 * Copyright (c) 2009-2010, Olivier Chafik (http://ochafik.free.fr/)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Olivier Chafik nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY OLIVIER CHAFIK AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.nativelibs4java.opencl;
import com.nativelibs4java.opencl.ImageIOUtils.ImageInfo;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_IMAGE_HEIGHT;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_IMAGE_ROW_PITCH;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_IMAGE_WIDTH;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.bridj.Pointer;
import static org.bridj.Pointer.*;

import com.nativelibs4java.opencl.library.OpenCLLibrary.cl_mem;
import com.nativelibs4java.util.Pair;

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
		Pointer<?> ptrOut = pointerToBuffer(out);
		CLEvent evt = read(queue, minX, minY, width, height, rowPitch, ptrOut, blocking, eventsToWaitFor);
		ptrOut.updateBuffer(out); // in case the buffer wasn't direct !
		return evt;
	}
	public CLEvent read(CLQueue queue, long minX, long minY, long width, long height, long rowPitch, Pointer<?> out, boolean blocking, CLEvent... eventsToWaitFor) {
		return read(queue, pointerToSizeTs(minX, minY, 0), pointerToSizeTs(width, height, 1), rowPitch, 0, out, blocking, eventsToWaitFor);
	}
	public CLEvent write(CLQueue queue, long minX, long minY, long width, long height, long rowPitch, Buffer in, boolean blocking, CLEvent... eventsToWaitFor) {
		return write(queue, minX, minY, width, height, rowPitch, pointerToBuffer(in), blocking, eventsToWaitFor);
	}
	public CLEvent write(CLQueue queue, long minX, long minY, long width, long height, long rowPitch, Pointer<?> in, boolean blocking, CLEvent... eventsToWaitFor) {
		return write(queue, pointerToSizeTs(minX, minY, 0), pointerToSizeTs(width, height, 1), rowPitch, 0, in, blocking, eventsToWaitFor);
	}

	public BufferedImage read(CLQueue queue, CLEvent... eventsToWaitFor) {
        ImageInfo info = ImageIOUtils.getBufferedImageInfo(getFormat());
        int imageType = info == null ? 0 : info.bufferedImageType;
        if (imageType == 0)
            throw new UnsupportedOperationException("Cannot convert image of format " + getFormat() + " to a BufferedImage.");
            //imageType = BufferedImage.TYPE_INT_ARGB;
        
		BufferedImage im = new BufferedImage((int)getWidth(), (int)getHeight(), imageType);
		read(queue, im, false, eventsToWaitFor);
		return im;
	}
	public void read(CLQueue queue, BufferedImage imageOut, boolean allowDeoptimizingDirectWrite, CLEvent... eventsToWaitFor) {
		//if (!getFormat().isIntBased())
		//	throw new IllegalArgumentException("Image-read only supports int-based RGBA images");
        ImageInfo info = ImageIOUtils.getBufferedImageInfo(getFormat());
        int width = imageOut.getWidth(null), height = imageOut.getHeight(null);

        Pointer<?> dataOut = allocateArray(info.bufferElementsClass, width * height * info.channelCount).order(getContext().getByteOrder());
		//Buffer dataOut = info.createBuffer(width, height, true);
		//IntBuffer dataOut = directInts(width * height, getContext().getByteOrder());
		read(queue, 0, 0, width, height, 0, dataOut, true, eventsToWaitFor);
        info.dataSetter.setData(imageOut, dataOut.getBuffer(), allowDeoptimizingDirectWrite);
	}
	public CLEvent write(CLQueue queue, Image image, CLEvent... eventsToWaitFor) {
		return write(queue, image, 0, 0, image.getWidth(null), image.getHeight(null), false, false, eventsToWaitFor);
	}
	public CLEvent write(CLQueue queue, Image image, boolean allowDeoptimizingDirectRead, boolean blocking, CLEvent... eventsToWaitFor) {
		return write(queue, image, 0, 0, image.getWidth(null), image.getHeight(null), allowDeoptimizingDirectRead, blocking, eventsToWaitFor);
	}
	public CLEvent write(CLQueue queue, Image image, int destX, int destY, int width, int height, boolean allowDeoptimizingDirectRead, boolean blocking, CLEvent... eventsToWaitFor) {
		//int imWidth = image.getWidth(null), height = image.getHeight(null);
        ImageInfo info = ImageIOUtils.getBufferedImageInfo(getFormat());
		return write(queue, 0, 0, width, height, width * info.pixelByteSize, info.dataGetter.getData(image, null, false, allowDeoptimizingDirectRead, getContext().getByteOrder()), blocking, eventsToWaitFor);
	}
	public void write(CLQueue queue, BufferedImage imageIn, boolean allowDeoptimizingDirectRead, CLEvent... eventsToWaitFor) {
		//if (!getFormat().isIntBased())
		//	throw new IllegalArgumentException("Image read only supports int-based RGBA images");

		int width = imageIn.getWidth(null), height = imageIn.getHeight(null);
		ImageInfo<BufferedImage> info = ImageIOUtils.getBufferedImageInfo(getFormat());
		write(queue, 0, 0, width, height, 0, info.dataGetter.getData(imageIn, null, false, allowDeoptimizingDirectRead, getContext().getByteOrder()), true, eventsToWaitFor);
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
