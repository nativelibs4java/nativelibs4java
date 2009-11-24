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
 * OpenCL Memory Buffer Object.<br/>
 * A buffer object stores a one-dimensional collection of elements.<br/>
 * Elements of a buffer object can be a scalar data type (such as an int, float), vector data type, or a user-defined structure.<br/>
 * @see CLContext#createInput(long)
 * @see CLContext#createOutput(long)
 * @author Olivier Chafik
 */
public abstract class CLBuffer<B extends Buffer> extends CLMem {
	Buffer buffer;
	CLBuffer(CLContext context, long byteCount, cl_mem entity, Buffer buffer) {
        super(context, byteCount, entity);
		this.buffer = buffer;
	}
	public abstract int getElementSize();
	public abstract long getElementCount();

    public abstract B read(CLQueue queue, CLEvent... eventsToWaitFor);
	public abstract B read(CLQueue queue, long offset, long length, CLEvent... eventsToWaitFor);

	public abstract B map(CLQueue queue, MapFlags flags, CLEvent... eventsToWaitFor);
	public abstract B map(CLQueue queue, MapFlags flags, long offset, long length, CLEvent... eventsToWaitFor);
	public abstract Pair<B, CLEvent> mapLater(CLQueue queue, MapFlags flags, CLEvent... eventsToWaitFor);
	public abstract Pair<B, CLEvent> mapLater(CLQueue queue, MapFlags flags, long offset, long length, CLEvent... eventsToWaitFor);

	protected void checkBounds(long offset, long length) {
		if (offset + length > byteCount)
			throw new IndexOutOfBoundsException("Trying to map a region of memory object outside allocated range");
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
		cl_event[] eventOut = new cl_event[1];
		error(CL.clEnqueueCopyBuffer(
			queue.get(),
			get(),
			destination.get(),
			toNS(srcOffset),
			toNS(destOffset),
			toNS(length),
			eventsToWaitFor.length, eventsToWaitFor.length == 0 ? null : CLEvent.to_cl_event_array(eventsToWaitFor),
			eventOut
		));
		return CLEvent.createEvent(eventOut[0]);
	}

	protected Pair<ByteBuffer, CLEvent> map(CLQueue queue, MapFlags flags, long offset, long length, boolean blocking, CLEvent... eventsToWaitFor) {
		checkBounds(offset, length);
		cl_event[] eventOut = blocking ? null : new cl_event[1];
		IntByReference pErr = new IntByReference();
        Pointer p = CL.clEnqueueMapBuffer(queue.get(), get(), blocking ? CL_TRUE : CL_FALSE,
			flags.getValue(),
			toNS(offset), toNS(length),
			eventsToWaitFor.length, CLEvent.to_cl_event_array(eventsToWaitFor),
			eventOut,
			pErr
		);
		error(pErr.getValue());
        return new Pair<ByteBuffer, CLEvent>(
			p.getByteBuffer(0, byteCount),
			eventOut == null ? null : CLEvent.createEvent(eventOut[0])
		);
    }

    public CLEvent unmap(CLQueue queue, Buffer buffer, CLEvent... eventsToWaitFor) {
        cl_event[] eventOut = new cl_event[1];
        error(CL.clEnqueueUnmapMemObject(queue.get(), get(), Native.getDirectBufferPointer(buffer), eventsToWaitFor.length, CLEvent.to_cl_event_array(eventsToWaitFor), eventOut));
		return CLEvent.createEvent(eventOut[0]);
    }

	public CLEvent read(CLQueue queue, Buffer out, boolean blocking, CLEvent... eventsToWaitFor) {
		long length = getByteCount();
		long s = getSizeInBytes(out);
		if (length > s)
			length = s;
		return read(queue, 0, length, out, blocking, eventsToWaitFor);
	}

	@SuppressWarnings("deprecation")
    public CLEvent read(CLQueue queue, long offset, long length, final Buffer out, boolean blocking, CLEvent... eventsToWaitFor) {
        if (out.isDirect()) {
            cl_event[] eventOut = blocking ? null : new cl_event[1];
			error(CL.clEnqueueReadBuffer(
				queue.get(),
				get(),
				blocking ? CL_TRUE : 0,
				toNS(offset),
				toNS(length),
				Native.getDirectBufferPointer(out),
				eventsToWaitFor.length, CLEvent.to_cl_event_array(eventsToWaitFor),
				eventOut
            ));
			return blocking ? null : CLEvent.createEvent(eventOut[0]);
        } else {
			try {
				ByteBuffer b = map(queue, MapFlags.Read, offset, length, true).getFirst();
				CLEvent.waitFor(eventsToWaitFor);
				try {
					//out.mark();
					put(b, out);
				} finally {
					//out.reset();
					CLEvent evt = unmap(queue, b);
					if (blocking)
						evt.waitFor();
					return blocking ? null : evt;
				}
			} catch (CLException.MapFailure ex) {
				final ByteBuffer directOut = directBytes((int)getSizeInBytes(out));
				// force blocking for now :
				blocking = true;
				CLEvent evt = read(queue, offset, length, directOut, blocking, eventsToWaitFor);
				if (blocking) {
					put(directOut, out);
				} else {
					evt.invokeUponCompletion(new Runnable() {
						public void run() {
							put(directOut, out);
						}
					});
				}
				return evt;
			}
        }
    }

	public CLEvent write(CLQueue queue, Buffer in, boolean blocking, CLEvent... eventsToWaitFor) {
		long length = getByteCount();
		long s = getSizeInBytes(in);
		if (length > s)
			length = s;
		return write(queue, 0, length, in, blocking, eventsToWaitFor);
	}


	@SuppressWarnings("deprecation")
    public CLEvent write(CLQueue queue, long offset, long length, Buffer in, boolean blocking, CLEvent... eventsToWaitFor) {
        if (in.isDirect()) {
            cl_event[] eventOut = blocking ? null : new cl_event[1];
			error(CL.clEnqueueWriteBuffer(
				queue.get(),
				get(),
				blocking ? CL_TRUE : 0,
				toNS(offset),
				toNS(length),
				Native.getDirectBufferPointer(in),
				eventsToWaitFor.length, CLEvent.to_cl_event_array(eventsToWaitFor),
				eventOut
			));
			return blocking ? null : CLEvent.createEvent(eventOut[0]);
        } else {
            ByteBuffer b = map(queue, MapFlags.Read, offset, length, true).getFirst();
			CLEvent.waitFor(eventsToWaitFor);
            try {
                //out.mark();
				put(in, b);
            } finally {
                //out.reset();
                CLEvent evt = unmap(queue, b);
				if (blocking)
					evt.waitFor();
				return blocking ? null : evt;
            }
        }
    }
	public ByteBuffer readBytes(CLQueue queue, CLEvent... eventsToWaitFor) {
		return readBytes(queue, 0, getByteCount(), eventsToWaitFor);
	}

	public ByteBuffer readBytes(CLQueue queue, long offset, long length, CLEvent... eventsToWaitFor) {
		ByteBuffer out = directBytes((int)getByteCount());
		int capa = out.capacity();
		read(queue, offset, length, out, true, eventsToWaitFor);
		return out;
	}

	public CLIntBuffer asCLIntBuffer() {
		cl_mem mem = get();
		CL.clRetainMemObject(get());
		return new CLIntBuffer(context, byteCount, mem, buffer);
	}
	public CLShortBuffer asCLShortBuffer() {
		cl_mem mem = get();
		CL.clRetainMemObject(mem);
		return new CLShortBuffer(context, byteCount, mem, buffer);
	}
	public CLLongBuffer asCLLongBuffer() {
		cl_mem mem = get();
		CL.clRetainMemObject(mem);
		return new CLLongBuffer(context, byteCount, mem, buffer);
	}
	public CLByteBuffer asCLByteBuffer() {
		cl_mem mem = get();
		CL.clRetainMemObject(mem);
		return new CLByteBuffer(context, byteCount, mem, buffer);
	}
	public CLFloatBuffer asCLFloatBuffer() {
		cl_mem mem = get();
		CL.clRetainMemObject(mem);
		return new CLFloatBuffer(context, byteCount, mem, buffer);
	}
	public CLDoubleBuffer asCLDoubleBuffer() {
		cl_mem mem = get();
		CL.clRetainMemObject(mem);
		return new CLDoubleBuffer(context, byteCount, mem, buffer);
	}
	public CLCharBuffer asCLCharBuffer() {
		cl_mem mem = get();
		CL.clRetainMemObject(mem);
		return new CLCharBuffer(context, byteCount, mem, buffer);
	}
}
