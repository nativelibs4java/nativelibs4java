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
		return read(queue, new long[] {minX, minY, 0}, new long[] {width, height, 0}, rowPitch, 0, out, blocking, eventsToWaitFor);
	}
	public CLEvent write(CLQueue queue, long minX, long minY, long width, long height, long rowPitch, Buffer in, boolean blocking, CLEvent... eventsToWaitFor) {
		return write(queue, new long[] {minX, minY, 0}, new long[] {width, height, 0}, rowPitch, 0, in, blocking, eventsToWaitFor);
	}

	public CLEvent write(CLQueue queue, Image image, int destX, int destY, int width, int height, boolean allowUnoptimizingDirectRead) {
		return write(queue, image, 0, 0, image.getWidth(null), image.getHeight(null), allowUnoptimizingDirectRead);
	}
	public CLEvent write(CLQueue queue, Image image, int destX, int destY, int width, int height, boolean allowUnoptimizingDirectRead, boolean blocking, CLEvent... eventsToWaitFor) {
		//int imWidth = image.getWidth(null), height = image.getHeight(null);
		return write(queue, 0, 0, width, height, width * 4, IntBuffer.wrap(getImageIntPixels(image, allowUnoptimizingDirectRead)), blocking, eventsToWaitFor);
	}
	protected CLEvent read(CLQueue queue, long[] origin, long[] region, long rowPitch, long slicePitch, Buffer out, boolean blocking, CLEvent... eventsToWaitFor) {
		/*if (!out.isDirect()) {

		}*/
		cl_event[] eventOut = blocking ? null : new cl_event[1];
		error(CL.clEnqueueReadImage(queue.get(), get(),
			blocking ? CL_TRUE : CL_FALSE,
			toNL(origin),
			toNL(region),
			toNL(rowPitch),
			toNL(slicePitch),
			Native.getDirectBufferPointer(out),
			eventsToWaitFor.length, CLEvent.to_cl_event_array(eventsToWaitFor),
			eventOut
		));
		return blocking ? null : CLEvent.createEvent(eventOut[0]);
	}

	protected CLEvent write(CLQueue queue, long[] origin, long[] region, long rowPitch, long slicePitch, Buffer in, boolean blocking, CLEvent... eventsToWaitFor) {
		boolean indirect = !in.isDirect();
		if (indirect)
			in = directCopy(in);

		cl_event[] eventOut = blocking ? null : new cl_event[1];
		error(CL.clEnqueueReadImage(queue.get(), get(),
			blocking ? CL_TRUE : CL_FALSE,
			toNL(origin),
			toNL(region),
			toNL(rowPitch),
			toNL(slicePitch),
			Native.getDirectBufferPointer(in),
			eventsToWaitFor.length, CLEvent.to_cl_event_array(eventsToWaitFor),
			eventOut
		));
		CLEvent evt = blocking ? null : CLEvent.createEvent(eventOut[0]);
		
		if (indirect && !blocking) {
			final Buffer toHold = in;
			evt.invokeUponCompletion(new Runnable() {
				public void run() {
					// Make sure the GC held a reference to directData until the write was completed !
					toHold.rewind();
				}
			});
		}

		return evt;
	}
}
