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
import static com.nativelibs4java.opencl.CLException.error;
import static com.nativelibs4java.opencl.JavaCL.CL;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_FALSE;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_IMAGE_ELEMENT_SIZE;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_IMAGE_FORMAT;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_TRUE;
import static com.nativelibs4java.util.NIOUtils.directCopy;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import com.nativelibs4java.opencl.library.cl_image_format;
import com.nativelibs4java.opencl.library.OpenCLLibrary.cl_event;
import com.nativelibs4java.opencl.library.OpenCLLibrary.cl_mem;
import com.nativelibs4java.util.NIOUtils;

import com.ochafik.util.listenable.Pair;
import com.bridj.JNI;
import com.bridj.Pointer;
import com.bridj.SizeT;
import static com.bridj.Pointer.*;


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
			format = new CLImageFormat(new cl_image_format(infos.getMemory(getEntity(), CL_IMAGE_FORMAT)));
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


	protected CLEvent read(CLQueue queue, Pointer<SizeT> origin, Pointer<SizeT> region, long rowPitch, long slicePitch, Buffer out, boolean blocking, CLEvent... eventsToWaitFor) {
		/*if (!out.isDirect()) {

		}*/
		Pointer<cl_event> eventOut = blocking ? null : CLEvent.new_event_out(eventsToWaitFor);
		Pointer<cl_event> evts = CLEvent.to_cl_event_array(eventsToWaitFor);
        error(CL.clEnqueueReadImage(queue.getEntity(), getEntity(),
			blocking ? CL_TRUE : CL_FALSE,
			origin,
			region,
			rowPitch,
			slicePitch,
			pointerToBuffer(out),
			evts == null ? 0 : (int)evts.getRemainingElements(), evts,
			eventOut
		));
		return CLEvent.createEventFromPointer(queue, eventOut);
	}

	protected CLEvent write(CLQueue queue, Pointer<SizeT> origin, Pointer<SizeT> region, long rowPitch, long slicePitch, Buffer in, boolean blocking, CLEvent... eventsToWaitFor) {
		boolean indirect = !in.isDirect();
		if (indirect)
			in = directCopy(in, getContext().getByteOrder());

		Pointer<cl_event> eventOut = blocking ? null : CLEvent.new_event_out(eventsToWaitFor);
		Pointer<cl_event> evts = CLEvent.to_cl_event_array(eventsToWaitFor);
        error(CL.clEnqueueWriteImage(queue.getEntity(), getEntity(),
			blocking ? CL_TRUE : CL_FALSE,
			origin,
			region,
			rowPitch,
			slicePitch,
			pointerToBuffer(in),
			evts == null ? 0 : (int)evts.getRemainingElements(), evts,
			eventOut
		));
		CLEvent evt = CLEvent.createEventFromPointer(queue, eventOut);

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
            Pointer<SizeT> offset3, Pointer<SizeT> length3,
            Long imageRowPitch,
            Long imageSlicePitch,
            boolean blocking, CLEvent... eventsToWaitFor)
    {
		//checkBounds(offset, length);
		Pointer<cl_event> eventOut = blocking ? null : CLEvent.new_event_out(eventsToWaitFor);
		Pointer<Integer> pErr = allocateInt();

        Pointer<cl_event> evts = CLEvent.to_cl_event_array(eventsToWaitFor);
        Pointer p = CL.clEnqueueMapImage(
            queue.getEntity(), getEntity(), blocking ? CL_TRUE : CL_FALSE,
            flags.getValue(),
			offset3,
            length3,
            imageRowPitch == null ? null : pointerToSizeT(imageRowPitch),
            imageSlicePitch == null ? null : pointerToSizeT(imageSlicePitch),
			evts == null ? 0 : (int)evts.getRemainingElements(), evts,
			eventOut,
			pErr
		);
		error(pErr.get());
        return new Pair<ByteBuffer, CLEvent>(
			p.getByteBuffer(0, getByteCount()),
			CLEvent.createEventFromPointer(queue, eventOut)
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
        Pointer<cl_event> eventOut = CLEvent.new_event_out(eventsToWaitFor);
        Pointer<cl_event> evts = CLEvent.to_cl_event_array(eventsToWaitFor);
        error(CL.clEnqueueUnmapMemObject(queue.getEntity(), getEntity(), pointerToBuffer(buffer), evts == null ? 0 : (int)evts.getRemainingElements(), evts, eventOut));
		return CLEvent.createEventFromPointer(queue, eventOut);
    }
}
