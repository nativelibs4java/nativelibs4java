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
import com.bridj.*;
import java.nio.ByteOrder;
import static com.bridj.Pointer.*;


/**
 * OpenCL Memory Buffer Object.<br/>
 * A buffer object stores a one-dimensional collection of elements.<br/>
 * Elements of a buffer object can be a scalar data type (such as an int, float), vector data type, or a user-defined structure.<br/>
 * @see CLContext
 * @author Olivier Chafik
 */
public class CLBuffer<B> extends CLMem {
	final Object owner;
    final PointerIO<B> io;
    
	CLBuffer(CLContext context, long byteCount, cl_mem entity, Object owner, PointerIO<B> io) {
        super(context, byteCount, entity);
		this.owner = owner;
        this.io = io;
	}
	public int getElementSize() {
        return io.getTargetSize();
    }
	public long getElementCount() {
        return getByteCount() / getElementSize();
    }
	public Pointer<B> map(CLQueue queue, MapFlags flags, CLEvent... eventsToWaitFor) throws CLException.MapFailure {
		return map(queue, flags, 0, getElementCount(), true, eventsToWaitFor).getFirst();
    }
	public Pointer<B> map(CLQueue queue, MapFlags flags, long offset, long length, CLEvent... eventsToWaitFor) throws CLException.MapFailure {
		return map(queue, flags, offset, length, true, eventsToWaitFor).getFirst();
    }
    
	public Pair<Pointer<B>, CLEvent> mapLater(CLQueue queue, MapFlags flags, CLEvent... eventsToWaitFor) throws CLException.MapFailure {
		return map(queue, flags, 0, getElementCount(), false, eventsToWaitFor);
    }
	public Pair<Pointer<B>, CLEvent> mapLater(CLQueue queue, MapFlags flags, long offset, long length, CLEvent... eventsToWaitFor) throws CLException.MapFailure {
		return map(queue, flags, offset, length, false, eventsToWaitFor);
    }
    
	public Pointer<B> read(CLQueue queue, CLEvent... eventsToWaitFor) {
        Pointer<B> out = allocate(io, getByteCount()).order(queue.getDevice().getKernelsDefaultByteOrder());
        read(queue, out, true, eventsToWaitFor);
		return out;
	}
	public Pointer<B> read(CLQueue queue, long offset, long length, CLEvent... eventsToWaitFor) {
		Pointer<B> out = allocate(io, getByteCount()).order(queue.getDevice().getKernelsDefaultByteOrder());
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
	 * @return
	 */
	public CLBuffer<B> createSubBuffer(Usage usage, long offset, long length) {
		try {
			int s = getElementSize();
			cl_buffer_region region = new cl_buffer_region().origin(s * offset).size(s * length);
			Pointer<Integer> pErr = allocateInt();
	        cl_mem mem = CL.clCreateSubBuffer(getEntity(), usage.getIntFlags(), CL_BUFFER_CREATE_TYPE_REGION, getPointer(region), pErr);
	        error(pErr.get());
	        return mem == null ? null : new CLBuffer<B>(context, length * s, mem, null, io);
		} catch (Throwable th) {
    		// TODO check if supposed to handle OpenCL 1.1
    		throw new UnsupportedOperationException("Cannot create sub-buffer (OpenCL 1.1 feature).", th);
    	}
	}
	
	/**
	 * enqueues a command to copy a buffer object identified by src_buffer to another buffer object identified by destination.
	 * @param queue
	 * @param srcOffset
	 * @param length
	 * @param destination
	 * @param destOffset
	 * @param eventsToWaitFor
	 * @return
	 */
	public CLEvent copyTo(CLQueue queue, long srcOffset, long length, CLMem destination, long destOffset, CLEvent... eventsToWaitFor) {
		Pointer<cl_event> eventOut = CLEvent.new_event_out(eventsToWaitFor);
		
        Pointer<cl_event> evts = CLEvent.to_cl_event_array(eventsToWaitFor);
        error(CL.clEnqueueCopyBuffer(
			queue.getEntity(),
			getEntity(),
			destination.getEntity(),
			srcOffset * getElementSize(),
			destOffset * getElementSize(),
			length,
			evts == null ? 0 : (int)evts.getRemainingElements(), evts,
			eventOut
		));
		return CLEvent.createEventFromPointer(queue, eventOut);
	}

	protected Pair<Pointer<B>, CLEvent> map(CLQueue queue, MapFlags flags, long offset, long length, boolean blocking, CLEvent... eventsToWaitFor) {
		checkBounds(offset, length);
		Pointer<cl_event> eventOut = blocking ? null : CLEvent.new_event_out(eventsToWaitFor);
		Pointer<Integer> pErr = allocateInt();
        
        Pointer<cl_event> evts = CLEvent.to_cl_event_array(eventsToWaitFor);
        Pointer p = CL.clEnqueueMapBuffer(queue.getEntity(), getEntity(), blocking ? CL_TRUE : CL_FALSE,
			flags.value(),
			offset * getElementSize(),
            length * getElementSize(),
			evts == null ? 0 : (int)evts.getRemainingElements(), evts,
			eventOut,
			pErr
		);
		error(pErr.get());
        return new Pair<Pointer<B>, CLEvent>(
			p.withIO(io).order(queue.getDevice().getKernelsDefaultByteOrder()),
			CLEvent.createEventFromPointer(queue, eventOut)
		);
    }

    public CLEvent unmap(CLQueue queue, Pointer<B> buffer, CLEvent... eventsToWaitFor) {
        Pointer<cl_event> eventOut = CLEvent.new_event_out(eventsToWaitFor);
        Pointer<cl_event> evts = CLEvent.to_cl_event_array(eventsToWaitFor);
        error(CL.clEnqueueUnmapMemObject(queue.getEntity(), getEntity(), buffer, evts == null ? 0 : (int)evts.getRemainingElements(), evts, eventOut));
		return CLEvent.createEventFromPointer(queue, eventOut);
    }

	public CLEvent read(CLQueue queue, Pointer<B> out, boolean blocking, CLEvent... eventsToWaitFor) {
        long length = -1;
        if (isGL) {
            length = out.getRemainingElements();
        }
        if (length < 0) {
            length = getElementCount();
            long s = out.getRemainingElements();
            if (length > s && s >= 0)
                length = s;
        }
		return read(queue, 0, length, out, blocking, eventsToWaitFor);
	}

	public CLEvent read(CLQueue queue, long offset, long length, Pointer<B> out, boolean blocking, CLEvent... eventsToWaitFor) {
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
            evts == null ? 0 : (int)evts.getRemainingElements(), evts,
            eventOut
        ));
        return CLEvent.createEventFromPointer(queue, eventOut);
    }

	public CLEvent write(CLQueue queue, Pointer<B> in, boolean blocking, CLEvent... eventsToWaitFor) {
        long length = -1;
        if (isGL)
            length = in.getRemainingElements();
        if (length < 0) {
            length = getElementCount();
            long s = in.getRemainingElements();
            if (length > s && s >= 0)
                length = s;
        }
		return write(queue, 0, length, in, blocking, eventsToWaitFor);
	}

	public CLEvent write(CLQueue queue, long offset, long length, Pointer<B> in, boolean blocking, CLEvent... eventsToWaitFor) {
        Pointer<cl_event> eventOut = blocking ? null : CLEvent.new_event_out(eventsToWaitFor);
        Pointer<cl_event> evts = CLEvent.to_cl_event_array(eventsToWaitFor);
        error(CL.clEnqueueWriteBuffer(
            queue.getEntity(),
            getEntity(),
            blocking ? CL_TRUE : CL_FALSE,
            offset * getElementSize(),
            length * getElementSize(),
            in,
            evts == null ? 0 : (int)evts.getRemainingElements(), evts,
            eventOut
        ));
        return CLEvent.createEventFromPointer(queue, eventOut);
    }

    public CLEvent writeBytes(CLQueue queue, long offset, long length, ByteBuffer in, boolean blocking, CLEvent... eventsToWaitFor) {
        if (!in.isDirect()) {
            blocking = true;
            in = directCopy(in, queue.getDevice().getKernelsDefaultByteOrder());
        }

        Pointer<cl_event> eventOut = blocking ? null : CLEvent.new_event_out(eventsToWaitFor);
        Pointer<cl_event> evts = CLEvent.to_cl_event_array(eventsToWaitFor);
        error(CL.clEnqueueWriteBuffer(
            queue.getEntity(),
            getEntity(),
            blocking ? CL_TRUE : 0,
            offset,
            length,
            pointerToBuffer(in),
            evts == null ? 0 : (int)evts.getRemainingElements(), evts,
            eventOut
        ));
        return CLEvent.createEventFromPointer(queue, eventOut);
    }

    private <T extends CLMem> T copyGLMark(T mem) {
        mem.isGL = this.isGL;
        return mem;
    }

	public <T> CLBuffer<T> as(Class<T> newTargetType) {
		cl_mem mem = getEntity();
		CL.clRetainMemObject(mem);
        PointerIO<T> newIo = PointerIO.getInstance(newTargetType);
		return copyGLMark(new CLBuffer<T>(context, getByteCount(), mem, owner, newIo));
	}
}
