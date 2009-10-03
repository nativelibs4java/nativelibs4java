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
public class CLBuffer extends CLMem {
	CLBuffer(CLContext context, long byteCount, cl_mem entity) {
        super(context, byteCount, entity);
	}


	private void checkBounds(long offset, long length) {
		if (offset + length > byteCount)
			throw new IndexOutOfBoundsException("Trying to map a region of memory object outside allocated range");
	}

	public ByteBuffer blockingMap(CLQueue queue, MapFlags flags, CLEvent... eventsToWaitFor) {
		return map(queue, flags, 0, getByteCount(), true, eventsToWaitFor).getFirst();
    }
	public ByteBuffer blockingMap(CLQueue queue, MapFlags flags, long offset, long length, CLEvent... eventsToWaitFor) {
		return map(queue, flags, offset, length, true, eventsToWaitFor).getFirst();
    }

	public Pair<ByteBuffer, CLEvent> enqueueMap(CLQueue queue, MapFlags flags, CLEvent... eventsToWaitFor) {
		return map(queue, flags, 0, getByteCount(), false, eventsToWaitFor);
    }
	public Pair<ByteBuffer, CLEvent> enqueueMap(CLQueue queue, MapFlags flags, long offset, long length, CLEvent... eventsToWaitFor) {
		return map(queue, flags, offset, length, false, eventsToWaitFor);
    }

	/**
	 * @deprecated Please use blockingMap instead
	 */
    @Deprecated
	public ByteBuffer blockingMapReadWrite(CLQueue queue, long offset, long length, CLEvent... eventsToWaitFor) {
		checkBounds(offset, length);
		return map(queue, MapFlags.getEnum(CL_MAP_READ | CL_MAP_WRITE), offset, length, true, eventsToWaitFor).getFirst();
    }

    /**
	 * @deprecated Please use blockingMap instead
	 */
    @Deprecated
    public ByteBuffer blockingMapWrite(CLQueue queue, long offset, long length, CLEvent... eventsToWaitFor) {
        checkBounds(offset, length);
		return map(queue, MapFlags.getEnum(CL_MAP_WRITE), offset, length, true, eventsToWaitFor).getFirst();
    }

    /**
	 * @deprecated Please use map instead
	 */
    @Deprecated
    public ByteBuffer blockingMapRead(CLQueue queue, long offset, long length, CLEvent... eventsToWaitFor) {
        checkBounds(offset, length);
		return map(queue, MapFlags.getEnum(CL_MAP_READ), offset, length, true, eventsToWaitFor).getFirst();
    }

	/**
	 * @deprecated Please use enqueueMap instead
	 */
    @Deprecated
    public Pair<ByteBuffer, CLEvent> enqueueMapReadWrite(CLQueue queue, long offset, long length, CLEvent... eventsToWaitFor) {
        checkBounds(offset, length);
		return map(queue, MapFlags.getEnum(CL_MAP_READ | CL_MAP_WRITE), offset, length, false, eventsToWaitFor);
    }

    /**
	 * @deprecated Please use enqueueMap instead
	 */
    @Deprecated
    public Pair<ByteBuffer, CLEvent> enqueueMapWrite(CLQueue queue, long offset, long length, CLEvent... eventsToWaitFor) {
        checkBounds(offset, length);
		return map(queue, MapFlags.getEnum(CL_MAP_WRITE), offset, length, false, eventsToWaitFor);
    }

    /**
	 * @deprecated Please use map instead
	 */
    @Deprecated
    public Pair<ByteBuffer, CLEvent> enqueueMapRead(CLQueue queue, long offset, long length, CLEvent... eventsToWaitFor) {
        checkBounds(offset, length);
		return map(queue, MapFlags.getEnum(CL_MAP_READ), offset, length, false, eventsToWaitFor);
    }

	/**
	 * @deprecated Please use blockingMap instead
	 */
    @Deprecated
    public ByteBuffer blockingMapReadWrite(CLQueue queue, CLEvent... eventsToWaitFor) {
		return blockingMapReadWrite(queue, 0, byteCount, eventsToWaitFor);
    }

    /**
	 * @deprecated Please use blockingMap instead
	 */
    @Deprecated
    public ByteBuffer blockingMapWrite(CLQueue queue, CLEvent... eventsToWaitFor) {
		return blockingMapWrite(queue, 0, byteCount, eventsToWaitFor);
    }

    /**
	 * @deprecated Please use blockingMap instead
	 */
    @Deprecated
    public ByteBuffer blockingMapRead(CLQueue queue, CLEvent... eventsToWaitFor) {
		return blockingMapRead(queue, 0, byteCount, eventsToWaitFor);
    }

	/**
	 * @deprecated Please use enqueueMap instead
	 */
    @Deprecated
    public Pair<ByteBuffer, CLEvent> enqueueMapReadWrite(CLQueue queue, CLEvent... eventsToWaitFor) {
		return enqueueMapReadWrite(queue, 0, byteCount, eventsToWaitFor);
    }

    /**
	 * @deprecated Please use enqueueMap instead
	 */
    @Deprecated
    public Pair<ByteBuffer, CLEvent> enqueueMapWrite(CLQueue queue, CLEvent... eventsToWaitFor) {
        return enqueueMapWrite(queue, 0, byteCount, eventsToWaitFor);
    }

	/**
	 * @deprecated Please use enqueueMap instead
	 */
    @Deprecated
    public Pair<ByteBuffer, CLEvent> enqueueMapRead(CLQueue queue, CLEvent... eventsToWaitFor) {
        return enqueueMapRead(queue, 0, byteCount, eventsToWaitFor);
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
			toNL(srcOffset),
			toNL(destOffset),
			toNL(length),
			eventsToWaitFor.length, eventsToWaitFor.length == 0 ? null : CLEvent.to_cl_event_array(eventsToWaitFor),
			eventOut
		));
		return CLEvent.createEvent(eventOut[0]);
	}

	private Pair<ByteBuffer, CLEvent> map(CLQueue queue, MapFlags flags, long offset, long length, boolean blocking, CLEvent... eventsToWaitFor) {
		checkBounds(offset, length);
		cl_event[] eventOut = blocking ? null : new cl_event[1];
		IntByReference pErr = new IntByReference();
        Pointer p = CL.clEnqueueMapBuffer(queue.get(), get(), blocking ? CL_TRUE : CL_FALSE,
			(int)flags.getValue(),
			toNL(offset), toNL(length),
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

	/**
	 * @deprecated Please use read(Queue, Buffer, ...) instead
	 */
	@Deprecated
	public CLEvent read(Buffer out, CLQueue queue, boolean blocking, CLEvent... eventsToWaitFor) {
		return read(queue, out, blocking, eventsToWaitFor);
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
				toNL(offset),
				toNL(length),
				Native.getDirectBufferPointer(out),
				eventsToWaitFor.length, CLEvent.to_cl_event_array(eventsToWaitFor),
				eventOut
            ));
			return blocking ? null : CLEvent.createEvent(eventOut[0]);
        } else {
			try {
				ByteBuffer b = blockingMap(queue, MapFlags.Read, offset, length);
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

    /**
	 * @deprecated Please use read(Queue, Buffer, ...) instead
	 */
	@Deprecated
	public CLEvent write(Buffer in, CLQueue queue, boolean blocking, CLEvent... eventsToWaitFor) {
		return write(queue, in, blocking, eventsToWaitFor);
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
				toNL(offset),
				toNL(length),
				Native.getDirectBufferPointer(in),
				eventsToWaitFor.length, CLEvent.to_cl_event_array(eventsToWaitFor),
				eventOut
			));
			return blocking ? null : CLEvent.createEvent(eventOut[0]);
        } else {
            ByteBuffer b = blockingMap(queue, MapFlags.Read, offset, length);
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
}
