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
import com.nativelibs4java.util.Pair;
import static com.nativelibs4java.opencl.CLException.error;
import static com.nativelibs4java.opencl.JavaCL.CL;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;
import static com.nativelibs4java.util.NIOUtils.directBytes;
import static com.nativelibs4java.util.NIOUtils.directCopy;

import java.nio.ByteBuffer;

import com.nativelibs4java.opencl.library.cl_buffer_region;
import com.nativelibs4java.opencl.library.OpenCLLibrary.cl_event;
import com.nativelibs4java.opencl.library.OpenCLLibrary.cl_mem;
import org.bridj.*;
import java.nio.ByteOrder;
import org.bridj.util.Utils;
import static org.bridj.Pointer.*;


/**
 * OpenCL Memory Buffer Object.<br/>
 * A buffer object stores a one-dimensional collection of elements.<br/>
 * Elements of a buffer object can be a scalar data type (such as an int, float), vector data type, or a user-defined structure.<br/>
 * @see CLContext
 * @author Olivier Chafik
 */
public class CLBuffer<T> extends CLMem {
	final Object owner;
    final PointerIO<T> io;
    
	CLBuffer(CLContext context, long byteCount, cl_mem entity, Object owner, PointerIO<T> io) {
        super(context, byteCount, entity);
		this.owner = owner;
        this.io = io;
	}
    
	public Class<T> getElementClass() {
        return Utils.getClass(io.getTargetType());
    }
	public int getElementSize() {
        return (int)io.getTargetSize();
    }
	public long getElementCount() {
        return getByteCount() / getElementSize();
    }
	public Pointer<T> map(CLQueue queue, MapFlags flags, CLEvent... eventsToWaitFor) throws CLException.MapFailure {
		return map(queue, flags, 0, getElementCount(), true, eventsToWaitFor).getFirst();
    }
	public Pointer<T> map(CLQueue queue, MapFlags flags, long offset, long length, CLEvent... eventsToWaitFor) throws CLException.MapFailure {
		return map(queue, flags, offset, length, true, eventsToWaitFor).getFirst();
    }
    
	public Pair<Pointer<T>, CLEvent> mapLater(CLQueue queue, MapFlags flags, CLEvent... eventsToWaitFor) throws CLException.MapFailure {
		return map(queue, flags, 0, getElementCount(), false, eventsToWaitFor);
    }
	public Pair<Pointer<T>, CLEvent> mapLater(CLQueue queue, MapFlags flags, long offset, long length, CLEvent... eventsToWaitFor) throws CLException.MapFailure {
		return map(queue, flags, offset, length, false, eventsToWaitFor);
    }
    
	public Pointer<T> read(CLQueue queue, CLEvent... eventsToWaitFor) {
        Pointer<T> out = allocateArray(io, getElementCount()).order(queue.getDevice().getKernelsDefaultByteOrder());
        read(queue, out, true, eventsToWaitFor);
		return out;
	}
	public Pointer<T> read(CLQueue queue, long offset, long length, CLEvent... eventsToWaitFor) {
		Pointer<T> out = allocateArray(io, getElementCount()).order(queue.getDevice().getKernelsDefaultByteOrder());
        read(queue, offset, length, out, true, eventsToWaitFor);
		return out;
	}

	protected void checkBounds(long offset, long length) {
		if (offset + length * getElementSize() > getByteCount())
			throw new IndexOutOfBoundsException("Trying to map a region of memory object outside allocated range");
	}

	/**
	 * Can be used to create a new buffer object (referred to as a sub-buffer object) from an existing buffer object.
	 * @param usage is used to specify allocation and usage information about the image memory object being created and is described in table 5.3 of the OpenCL spec.
	 * @param offset
	 * @param length
	 * @since OpenCL 1.1
	 * @return sub-buffer that is a "window" of this buffer starting at the provided offset, with the provided length
	 */
	public CLBuffer<T> createSubBuffer(Usage usage, long offset, long length) {
		try {
			int s = getElementSize();
			cl_buffer_region region = new cl_buffer_region().origin(s * offset).size(s * length);
			Pointer<Integer> pErr = allocateInt();
	        cl_mem mem = CL.clCreateSubBuffer(getEntity(), usage.getIntFlags(), CL_BUFFER_CREATE_TYPE_REGION, pointerTo(region), pErr);
	        error(pErr.get());
	        return mem == null ? null : new CLBuffer<T>(context, length * s, mem, null, io);
		} catch (Throwable th) {
    		// TODO check if supposed to handle OpenCL 1.1
    		throw new UnsupportedOperationException("Cannot create sub-buffer (OpenCL 1.1 feature).", th);
    	}
	}
	
	/**
	 * enqueues a command to copy a buffer object identified by src_buffer to another buffer object identified by destination.
	 * @param destination
	 * @param eventsToWaitFor
	 * @return event which indicates the copy operation has completed
	 */
	public CLEvent copyTo(CLQueue queue, CLMem destination, CLEvent... eventsToWaitFor) {
		return copyTo(queue, 0, getElementCount(), destination, 0, eventsToWaitFor);	
	}
	
	/**
	 * enqueues a command to copy a buffer object identified by src_buffer to another buffer object identified by destination.
	 * @param queue
	 * @param srcOffset
	 * @param length
	 * @param destination
	 * @param destOffset
	 * @param eventsToWaitFor
	 * @return event which indicates the copy operation has completed
	 */
	public CLEvent copyTo(CLQueue queue, long srcOffset, long length, CLMem destination, long destOffset, CLEvent... eventsToWaitFor) {
		Pointer<cl_event> eventOut = CLEvent.new_event_out(eventsToWaitFor);
		long 
			byteCount = getByteCount(),
			destByteCount = destination.getByteCount(),
			eltSize = getElementSize(),
			actualSrcOffset = srcOffset * eltSize, 
			actualDestOffset = destOffset * eltSize, 
			actualLength = length * eltSize;
		
		if (	actualSrcOffset < 0 ||
			actualSrcOffset >= byteCount ||
			actualSrcOffset + actualLength > byteCount ||
			actualDestOffset < 0 ||
			actualDestOffset >= destByteCount ||
			actualDestOffset + actualLength > destByteCount
		)
			throw new IndexOutOfBoundsException("Invalid copy parameters : srcOffset = " + srcOffset + ", destOffset = " + destOffset + ", length = " + length + " (element size = " + eltSize + ", source byte count = " + byteCount + ", destination byte count = " + destByteCount + ")"); 
		
        Pointer<cl_event> evts = CLEvent.to_cl_event_array(eventsToWaitFor);
        error(CL.clEnqueueCopyBuffer(
			queue.getEntity(),
			getEntity(),
			destination.getEntity(),
			actualSrcOffset,
			actualDestOffset,
			actualLength,
			evts == null ? 0 : (int)evts.getValidElements(), evts,
			eventOut
		));
		return CLEvent.createEventFromPointer(queue, eventOut);
	}

	protected Pair<Pointer<T>, CLEvent> map(CLQueue queue, MapFlags flags, long offset, long length, boolean blocking, CLEvent... eventsToWaitFor) {
		checkBounds(offset, length);
		Pointer<cl_event> eventOut = blocking ? null : CLEvent.new_event_out(eventsToWaitFor);
		Pointer<Integer> pErr = allocateInt();
        
        Pointer<cl_event> evts = CLEvent.to_cl_event_array(eventsToWaitFor);
        Pointer p = CL.clEnqueueMapBuffer(queue.getEntity(), getEntity(), blocking ? CL_TRUE : CL_FALSE,
			flags.value(),
			offset * getElementSize(),
            length * getElementSize(),
			evts == null ? 0 : (int)evts.getValidElements(), evts,
			eventOut,
			pErr
		);
		error(pErr.get());
        return new Pair<Pointer<T>, CLEvent>(
			p.as(io).validElements(length).order(queue.getDevice().getKernelsDefaultByteOrder()),
			CLEvent.createEventFromPointer(queue, eventOut)
		);
    }

    public CLEvent unmap(CLQueue queue, Pointer<T> buffer, CLEvent... eventsToWaitFor) {
        Pointer<cl_event> eventOut = CLEvent.new_event_out(eventsToWaitFor);
        Pointer<cl_event> evts = CLEvent.to_cl_event_array(eventsToWaitFor);
        error(CL.clEnqueueUnmapMemObject(queue.getEntity(), getEntity(), buffer, evts == null ? 0 : (int)evts.getValidElements(), evts, eventOut));
		return CLEvent.createEventFromPointer(queue, eventOut);
    }

	public CLEvent read(CLQueue queue, Pointer<T> out, boolean blocking, CLEvent... eventsToWaitFor) {
        long length = -1;
        if (isGL) {
            length = out.getValidElements();
        }
        if (length < 0) {
            length = getElementCount();
            long s = out.getValidElements();
            if (length > s && s >= 0)
                length = s;
        }
		return read(queue, 0, length, out, blocking, eventsToWaitFor);
	}

	public CLEvent read(CLQueue queue, long offset, long length, Pointer<T> out, boolean blocking, CLEvent... eventsToWaitFor) {
        //if (out.isReadOnly())
        //    throw new IllegalArgumentException("Output buffer for read operation is read-only !");
        Pointer<cl_event> eventOut = blocking ? null : CLEvent.new_event_out(eventsToWaitFor);
        Pointer<cl_event> evts = CLEvent.to_cl_event_array(eventsToWaitFor);
        error(CL.clEnqueueReadBuffer(
            queue.getEntity(),
            getEntity(),
            blocking ? CL_TRUE : 0,
            offset * getElementSize(),
            length * getElementSize(),
            out,
            evts == null ? 0 : (int)evts.getValidElements(), evts,
            eventOut
        ));
        return CLEvent.createEventFromPointer(queue, eventOut);
    }

	public CLEvent write(CLQueue queue, Pointer<T> in, boolean blocking, CLEvent... eventsToWaitFor) {
        long length = -1;
        if (isGL)
            length = in.getValidElements();
        if (length < 0) {
            length = getElementCount();
            long s = in.getValidElements();
            if (length > s && s >= 0)
                length = s;
        }
		return write(queue, 0, length, in, blocking, eventsToWaitFor);
	}

	public CLEvent write(CLQueue queue, long offset, long length, Pointer<T> in, boolean blocking, CLEvent... eventsToWaitFor) {
        Pointer<cl_event> eventOut = blocking ? null : CLEvent.new_event_out(eventsToWaitFor);
        Pointer<cl_event> evts = CLEvent.to_cl_event_array(eventsToWaitFor);
        error(CL.clEnqueueWriteBuffer(
            queue.getEntity(),
            getEntity(),
            blocking ? CL_TRUE : CL_FALSE,
            offset * getElementSize(),
            length * getElementSize(),
            in,
            evts == null ? 0 : (int)evts.getValidElements(), evts,
            eventOut
        ));
        return CLEvent.createEventFromPointer(queue, eventOut);
    }

    public CLEvent writeBytes(CLQueue queue, long offset, long length, ByteBuffer in, boolean blocking, CLEvent... eventsToWaitFor) {
    		return writeBytes(queue, offset, length, pointerToBuffer(in), blocking, eventsToWaitFor);
    }
    public CLEvent writeBytes(CLQueue queue, long offset, long length, Pointer<?> in, boolean blocking, CLEvent... eventsToWaitFor) {
        Pointer<cl_event> eventOut = blocking ? null : CLEvent.new_event_out(eventsToWaitFor);
        Pointer<cl_event> evts = CLEvent.to_cl_event_array(eventsToWaitFor);
        error(CL.clEnqueueWriteBuffer(
            queue.getEntity(),
            getEntity(),
            blocking ? CL_TRUE : 0,
            offset,
            length,
            in,
            evts == null ? 0 : (int)evts.getValidElements(), evts,
            eventOut
        ));
        return CLEvent.createEventFromPointer(queue, eventOut);
    }

    private <T extends CLMem> T copyGLMark(T mem) {
        mem.isGL = this.isGL;
        return mem;
    }
        
    public CLBuffer<T> emptyClone(CLMem.Usage usage) {
    		return (CLBuffer)getContext().createBuffer(usage, io, getElementCount());
    }
    
    #foreach ($prim in $primitivesNoBool)

	public CLBuffer<${prim.WrapperName}> asCL${prim.BufferName}() {
		return as(${prim.WrapperName}.class);
	}
	
	#end
	
	public <T> CLBuffer<T> as(Class<T> newTargetType) {
		cl_mem mem = getEntity();
		CL.clRetainMemObject(mem);
        PointerIO<T> newIo = PointerIO.getInstance(newTargetType);
		return copyGLMark(new CLBuffer<T>(context, getByteCount(), mem, owner, newIo));
	}
	
}
