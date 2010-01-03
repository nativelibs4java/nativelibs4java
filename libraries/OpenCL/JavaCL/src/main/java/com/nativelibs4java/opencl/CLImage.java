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
import com.nativelibs4java.opencl.library.OpenCLLibrary;
import com.nativelibs4java.opencl.library.cl_image_format;
import com.nativelibs4java.util.NIOUtils;
import com.ochafik.lang.jnaerator.runtime.NativeSize;
import com.ochafik.lang.jnaerator.runtime.NativeSizeByReference;
import com.ochafik.util.listenable.Pair;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;
import com.sun.jna.*;
import com.sun.jna.ptr.*;
import java.nio.*;
import static com.nativelibs4java.opencl.JavaCL.*;
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
			fmt.use(infos.getMemory(getEntity(), CL_IMAGE_FORMAT));
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
		return infos.getIntOrLong(getEntity(), CL_IMAGE_ELEMENT_SIZE);
	}


	protected CLEvent read(CLQueue queue, NativeSize[] origin, NativeSize[] region, long rowPitch, long slicePitch, Buffer out, boolean blocking, CLEvent... eventsToWaitFor) {
		/*if (!out.isDirect()) {

		}*/
		cl_event[] eventOut = blocking ? null : CLEvent.new_event_out(eventsToWaitFor);
		cl_event[] evts = CLEvent.to_cl_event_array(eventsToWaitFor);
        error(CL.clEnqueueReadImage(queue.getEntity(), getEntity(),
			blocking ? CL_TRUE : CL_FALSE,
			origin,
			region,
			toNS(rowPitch),
			toNS(slicePitch),
			Native.getDirectBufferPointer(out),
			evts == null ? 0 : evts.length, evts,
			eventOut
		));
		return CLEvent.createEvent(queue, eventOut);
	}

	protected CLEvent write(CLQueue queue, NativeSize[] origin, NativeSize[] region, long rowPitch, long slicePitch, Buffer in, boolean blocking, CLEvent... eventsToWaitFor) {
		boolean indirect = !in.isDirect();
		if (indirect)
			in = directCopy(in, getContext().getByteOrder());

		cl_event[] eventOut = blocking ? null : CLEvent.new_event_out(eventsToWaitFor);
		cl_event[] evts = CLEvent.to_cl_event_array(eventsToWaitFor);
        error(CL.clEnqueueReadImage(queue.getEntity(), getEntity(),
			blocking ? CL_TRUE : CL_FALSE,
			origin,
			region,
			toNS(rowPitch),
			toNS(slicePitch),
			Native.getDirectBufferPointer(in),
			evts == null ? 0 : evts.length, evts,
			eventOut
		));
		CLEvent evt = CLEvent.createEvent(queue, eventOut);

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

    protected Pair<ByteBuffer, CLEvent> map(CLQueue queue, MapFlags flags,
            NativeSize[] offset3, NativeSize[] length3,
            Long imageRowPitch,
            Long imageSlicePitch,
            boolean blocking, CLEvent... eventsToWaitFor)
    {
		//checkBounds(offset, length);
		cl_event[] eventOut = blocking ? null : CLEvent.new_event_out(eventsToWaitFor);
		IntBuffer pErr = NIOUtils.directInts(1, ByteOrder.nativeOrder());

        cl_event[] evts = CLEvent.to_cl_event_array(eventsToWaitFor);
        Pointer p = CL.clEnqueueMapImage(
            queue.getEntity(), getEntity(), blocking ? CL_TRUE : CL_FALSE,
            flags.getValue(),
			offset3,
            length3,
            imageRowPitch == null ? null : new NativeSizeByReference(toNS(imageRowPitch)),
            imageSlicePitch == null ? null : new NativeSizeByReference(toNS(imageSlicePitch)),
			evts == null ? 0 : evts.length, evts,
			eventOut,
			pErr
		);
		error(pErr.get());
        return new Pair<ByteBuffer, CLEvent>(
			p.getByteBuffer(0, getByteCount()),
			CLEvent.createEvent(queue, eventOut)
		);
    }

    /**
     * @see CLImage2D#map(com.nativelibs4java.opencl.CLQueue, com.nativelibs4java.opencl.CLMem.MapFlags, com.nativelibs4java.opencl.CLEvent[])
     * @see CLImage3D#map(com.nativelibs4java.opencl.CLQueue, com.nativelibs4java.opencl.CLMem.MapFlags, com.nativelibs4java.opencl.CLEvent[])
     * @param queue
     * @param buffer
     * @param eventsToWaitFor
     * @return
     */
    public CLEvent unmap(CLQueue queue, ByteBuffer buffer, CLEvent... eventsToWaitFor) {
        cl_event[] eventOut = CLEvent.new_event_out(eventsToWaitFor);
        cl_event[] evts = CLEvent.to_cl_event_array(eventsToWaitFor);
        error(CL.clEnqueueUnmapMemObject(queue.getEntity(), getEntity(), Native.getDirectBufferPointer(buffer), evts == null ? 0 : evts.length, evts, eventOut));
		return CLEvent.createEvent(queue, eventOut);
    }
}
