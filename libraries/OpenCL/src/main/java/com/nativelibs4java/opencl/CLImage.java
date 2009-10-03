/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl;
import com.nativelibs4java.opencl.library.OpenCLLibrary;
import com.nativelibs4java.opencl.library.cl_image_format;
import com.ochafik.util.listenable.Pair;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;
import com.sun.jna.*;
import com.sun.jna.ptr.*;
import java.nio.*;
import static com.nativelibs4java.opencl.OpenCL4Java.*;
import static com.nativelibs4java.opencl.CLException.*;
import static com.nativelibs4java.util.JNAUtils.*;
import static com.nativelibs4java.util.NIOUtils.*;


/**
 * OpenCL Image Memory Object.<br/>
 * An image object is used to store a two- or three- dimensional texture, frame-buffer or image<br/>
 * An image object is used to represent a buffer that can be used as a texture or a frame-buffer. The elements of an image object are selected from a list of predefined image formats.
 * @author Oliveir Chafik
 */
public abstract class CLImage extends CLMem {
	protected static CLInfoGetter<cl_mem> infos = new CLInfoGetter<cl_mem>() {
		@Override
		protected int getInfo(cl_mem entity, int infoTypeEnum, NativeLong size, Pointer out, NativeLongByReference sizeOut) {
			return CL.clGetImageInfo(entity, infoTypeEnum, size, out, sizeOut);
		}
	};

	CLImageFormat format;
	CLImage(CLContext context, cl_mem entity, CLImageFormat format) {
        super(context, -1, entity);
		this.format = format;
	}



	/**
	 * Return image format descriptor specified when image is created with CLContext.create{Input|Output|InputOutput}{2D|3D}.
	 */
	@InfoName("CL_IMAGE_FORMAT")
	public CLImageFormat getFormat() {
		if (format == null) {
			/// TODO: DOES NOT SEEM TO WORK ON MAC OS X 10.6.1 / CPU
			cl_image_format fmt = new cl_image_format();
			fmt.use(infos.getMemory(get(), CL_IMAGE_FORMAT));
			format = new CLImageFormat(fmt);
		}
		return format;
	}

	/**
	 * Return size of each element of the image memory object given by image. <br/>
	 * An element is made up of n channels. The value of n is given in cl_image_format descriptor.
	 */
	@InfoName("CL_IMAGE_ELEMENT_SIZE")
	public long getElementSize() {
		return infos.getNativeLong(get(), CL_IMAGE_ELEMENT_SIZE);
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
