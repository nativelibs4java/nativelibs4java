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

import com.nativelibs4java.util.Pair;
import org.bridj.*;
import static org.bridj.Pointer.*;


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
		return read(queue, origin, region, rowPitch, slicePitch, pointerToBuffer(out), blocking, eventsToWaitFor);
	}
	protected CLEvent read(CLQueue queue, Pointer<SizeT> origin, Pointer<SizeT> region, long rowPitch, long slicePitch, Pointer<?> out, boolean blocking, CLEvent... eventsToWaitFor) {
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
			out,
			evts == null ? 0 : (int)evts.getValidElements(), evts,
			eventOut
		));
		return CLEvent.createEventFromPointer(queue, eventOut);
	}

	protected CLEvent write(CLQueue queue, Pointer<SizeT> origin, Pointer<SizeT> region, long rowPitch, long slicePitch, Buffer in, boolean blocking, CLEvent... eventsToWaitFor) {
		return write(queue, origin, region, rowPitch, slicePitch, pointerToBuffer(in), blocking, eventsToWaitFor);
	}
	protected CLEvent write(CLQueue queue, Pointer<SizeT> origin, Pointer<SizeT> region, long rowPitch, long slicePitch, Pointer<?> in, boolean blocking, CLEvent... eventsToWaitFor) {
		Pointer<cl_event> eventOut = blocking ? null : CLEvent.new_event_out(eventsToWaitFor);
		Pointer<cl_event> evts = CLEvent.to_cl_event_array(eventsToWaitFor);
        error(CL.clEnqueueWriteImage(queue.getEntity(), getEntity(),
			blocking ? CL_TRUE : CL_FALSE,
			origin,
			region,
			rowPitch,
			slicePitch,
			in,
			evts == null ? 0 : (int)evts.getValidElements(), evts,
			eventOut
		));
		CLEvent evt = CLEvent.createEventFromPointer(queue, eventOut);

		if (!blocking) {
			final Pointer<?> toHold = in;
			evt.invokeUponCompletion(new Runnable() {
				public void run() {
					// Make sure the GC held a reference to directData until the write was completed !
					toHold.order();
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
            flags.value(),
			offset3,
            length3,
            imageRowPitch == null ? null : pointerToSizeT(imageRowPitch),
            imageSlicePitch == null ? null : pointerToSizeT(imageSlicePitch),
			evts == null ? 0 : (int)evts.getValidElements(), evts,
			eventOut,
			pErr
		);
		error(pErr.get());
        return new Pair<ByteBuffer, CLEvent>(
			p.getByteBuffer(getByteCount()),
			CLEvent.createEventFromPointer(queue, eventOut)
		);
    }

    /**
     * @see CLImage2D#map(com.nativelibs4java.opencl.CLQueue, com.nativelibs4java.opencl.CLMem.MapFlags, com.nativelibs4java.opencl.CLEvent[])
     * @see CLImage3D#map(com.nativelibs4java.opencl.CLQueue, com.nativelibs4java.opencl.CLMem.MapFlags, com.nativelibs4java.opencl.CLEvent[])
     * @param queue
     * @param buffer
     * @param eventsToWaitFor
     * @return Event which completion indicates that the OpenCL was unmapped
     */
    public CLEvent unmap(CLQueue queue, ByteBuffer buffer, CLEvent... eventsToWaitFor) {
        Pointer<cl_event> eventOut = CLEvent.new_event_out(eventsToWaitFor);
        Pointer<cl_event> evts = CLEvent.to_cl_event_array(eventsToWaitFor);
        error(CL.clEnqueueUnmapMemObject(queue.getEntity(), getEntity(), pointerToBuffer(buffer), evts == null ? 0 : (int)evts.getValidElements(), evts, eventOut));
		return CLEvent.createEventFromPointer(queue, eventOut);
    }
}
