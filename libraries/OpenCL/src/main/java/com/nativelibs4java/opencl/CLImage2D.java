/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl;
import com.nativelibs4java.opencl.library.OpenCLLibrary;
import com.ochafik.util.listenable.Pair;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;
import com.sun.jna.*;
import com.sun.jna.ptr.*;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.*;
import java.util.concurrent.BlockingDeque;
import static com.nativelibs4java.opencl.OpenCL4Java.*;
import static com.nativelibs4java.opencl.CLException.*;
import static com.nativelibs4java.util.JNAUtils.*;
import static com.nativelibs4java.util.ImageUtils.*;
import static com.nativelibs4java.util.NIOUtils.*;

/**
 * OpenCL 2D Image Memory Object<br/>
 * @see CLContext#createInput2D(com.nativelibs4java.opencl.CLImageFormat, long, long)
 * @see CLContext#createOutput2D(com.nativelibs4java.opencl.CLImageFormat, long, long)
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
		return infos.getNativeLong(get(), CL_IMAGE_ROW_PITCH);
	}

	/**
	 * Return width of the image in pixels
	 */
	@InfoName("CL_IMAGE_WIDTH")
	public long getWidth() {
		return infos.getNativeLong(get(), CL_IMAGE_WIDTH);
	}

	/**
	 * Return height of the image in pixels
	 */
	@InfoName("CL_IMAGE_HEIGHT")
	public long getHeight() {
		return infos.getNativeLong(get(), CL_IMAGE_HEIGHT);
	}

	public CLEvent read(CLQueue queue, long minX, long minY, long width, long height, long rowPitch, Buffer out, boolean blocking, CLEvent... eventsToWaitFor) {
		return read(queue, new long[] {minX, minY, 0}, new long[] {width, height, 1}, rowPitch, 0, out, blocking, eventsToWaitFor);
	}
	public CLEvent write(CLQueue queue, long minX, long minY, long width, long height, long rowPitch, Buffer in, boolean blocking, CLEvent... eventsToWaitFor) {
		return write(queue, new long[] {minX, minY, 0}, new long[] {width, height, 1}, rowPitch, 0, in, blocking, eventsToWaitFor);
	}

	public BufferedImage read(CLQueue queue) {
		BufferedImage im = new BufferedImage((int)getWidth(), (int)getHeight(), BufferedImage.TYPE_INT_ARGB);
		read(queue, im, false);
		return im;
	}
	public void read(CLQueue queue, BufferedImage imageOut, boolean allowDeoptimizingDirectWrite, CLEvent... eventsToWaitFor) {
		if (!getFormat().isIntBased())
			throw new IllegalArgumentException("Image read only supports int-based RGBA images");
		
		int width = imageOut.getWidth(null), height = imageOut.getHeight(null);
		IntBuffer dataOut = directInts(width * height);
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
}
