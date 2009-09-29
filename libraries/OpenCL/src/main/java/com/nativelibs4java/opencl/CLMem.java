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

/**
 * OpenCL memory object.<br/>
 * Memory objects are categorized into two types: buffer objects, and image objects. A buffer object stores a one-dimensional collection of elements whereas an image object is used to store a two- or three- dimensional texture, frame-buffer or image.<br/>
 * Elements of a buffer object can be a scalar data type (such as an int, float), vector data type, or a user-defined structure. An image object is used to represent a buffer that can be used as a texture or a frame-buffer. The elements of an image object are selected from a list of predefined image formats. The minimum number of elements in a memory object is one.<br/>
 * The fundamental differences between a buffer and an image object are:
 * <ul>
 * <li>Elements in a buffer are stored in sequential fashion and can be accessed using a pointer by a kernel executing on a device. Elements of an image are stored in a format that is opaque to the user and cannot be directly accessed using a pointer. Built-in functions are provided by the OpenCL C programming language to allow a kernel to read from or write to an image.</li>
 * <li>For a buffer object, the data is stored in the same format as it is accessed by the kernel, but in the case of an image object the data format used to store the image elements may not be the same as the data format used inside the kernel. Image elements are always a 4- component vector (each component can be a float or signed/unsigned integer) in a kernel. The built-in function to read from an image converts image element from the format it is stored into a 4-component vector. Similarly, the built-in function to write to an image converts the image element from a 4-component vector to the appropriate image format specified such as 4 8-bit elements, for example.</li>
 * </ul>
 *
 * Kernels take memory objects as input, and output to one or more memory objects.
 * @author Olivier Chafik
 */
public class CLMem extends CLEntity<cl_mem> {

    protected final CLContext context;
    protected final int byteCount;

    public CLMem(CLContext context, int byteCount, cl_mem entity) {
        super(entity);
        this.byteCount = byteCount;
        this.context = context;
    }
    public CLContext getContext() {
        return context;
    }
    public int getByteCount() {
        return byteCount;
    }

	private void checkBounds(long offset, long length) {
		if (offset + length > byteCount)
			throw new IndexOutOfBoundsException("Trying to map a region of memory object outside allocated range");
	}
	public ByteBuffer blockingMapReadWrite(CLQueue queue, long offset, long length, CLEvent... eventsToWaitFor) {
		checkBounds(offset, length);
		return map(queue, CL_MAP_READ | CL_MAP_WRITE, offset, length, true, eventsToWaitFor).getFirst();
    }

    public ByteBuffer blockingMapWrite(CLQueue queue, long offset, long length, CLEvent... eventsToWaitFor) {
        checkBounds(offset, length);
		return map(queue, CL_MAP_WRITE, offset, length, true, eventsToWaitFor).getFirst();
    }

    public ByteBuffer blockingMapRead(CLQueue queue, long offset, long length, CLEvent... eventsToWaitFor) {
        checkBounds(offset, length);
		return map(queue, CL_MAP_READ, offset, length, true, eventsToWaitFor).getFirst();
    }

	public Pair<ByteBuffer, CLEvent> enqueueMapReadWrite(CLQueue queue, long offset, long length, CLEvent... eventsToWaitFor) {
        checkBounds(offset, length);
		return map(queue, CL_MAP_READ | CL_MAP_WRITE, offset, length, false, eventsToWaitFor);
    }

    public Pair<ByteBuffer, CLEvent> enqueueMapWrite(CLQueue queue, long offset, long length, CLEvent... eventsToWaitFor) {
        checkBounds(offset, length);
		return map(queue, CL_MAP_WRITE, offset, length, false, eventsToWaitFor);
    }

    public Pair<ByteBuffer, CLEvent> enqueueMapRead(CLQueue queue, long offset, long length, CLEvent... eventsToWaitFor) {
        checkBounds(offset, length);
		return map(queue, CL_MAP_READ, offset, length, false, eventsToWaitFor);
    }

    public ByteBuffer blockingMapReadWrite(CLQueue queue, CLEvent... eventsToWaitFor) {
		return blockingMapReadWrite(queue, 0, byteCount, eventsToWaitFor);
    }

    public ByteBuffer blockingMapWrite(CLQueue queue, CLEvent... eventsToWaitFor) {
		return blockingMapWrite(queue, 0, byteCount, eventsToWaitFor);
    }

    public ByteBuffer blockingMapRead(CLQueue queue, CLEvent... eventsToWaitFor) {
		return blockingMapRead(queue, 0, byteCount, eventsToWaitFor);
    }

	public Pair<ByteBuffer, CLEvent> enqueueMapReadWrite(CLQueue queue, CLEvent... eventsToWaitFor) {
		return enqueueMapReadWrite(queue, 0, byteCount, eventsToWaitFor);
    }

    public Pair<ByteBuffer, CLEvent> enqueueMapWrite(CLQueue queue, CLEvent... eventsToWaitFor) {
        return enqueueMapWrite(queue, 0, byteCount, eventsToWaitFor);
    }

    public Pair<ByteBuffer, CLEvent> enqueueMapRead(CLQueue queue, CLEvent... eventsToWaitFor) {
        return enqueueMapRead(queue, 0, byteCount, eventsToWaitFor);
    }

    private Pair<ByteBuffer, CLEvent> map(CLQueue queue, int flags, long offset, long length, boolean waitFor, CLEvent... eventsToWaitFor) {
		cl_event[] eventOut = waitFor ? null : new cl_event[1];
		IntByReference pErr = new IntByReference();
        Pointer p = CL.clEnqueueMapBuffer(queue.get(), get(), waitFor ? CL_TRUE : CL_FALSE, 
			flags, toNL(offset), toNL(length),
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

    @Override
    protected void clear() {
        error(CL.clReleaseMemObject(get()));
    }

    @SuppressWarnings("deprecation")
    public void read(Buffer out, CLQueue queue, boolean blocking) {
        if (out.isDirect()) {
            Pointer pres = Native.getDirectBufferPointer(out);
            error(CL.clEnqueueReadBuffer(
                    queue.get(),
                    get(),
                    blocking ? CL_TRUE : 0,
                    toNL(0),
                    toNL(getSizeInBytes(out)),
                    pres,
                    0,
                    null,
                    (PointerByReference) null//pevt
                    ));
        } else {
            ByteBuffer b = blockingMapRead(queue);
            try {
                //out.mark();
                if (out instanceof IntBuffer)
                    ((IntBuffer)out).put(b.asIntBuffer());
                else if (out instanceof LongBuffer)
                    ((LongBuffer)out).put(b.asLongBuffer());
                else if (out instanceof ShortBuffer)
                    ((ShortBuffer)out).put(b.asShortBuffer());
                else if (out instanceof CharBuffer)
                    ((CharBuffer)out).put(b.asCharBuffer());
                else if (out instanceof DoubleBuffer)
                    ((DoubleBuffer)out).put(b.asDoubleBuffer());
                else if (out instanceof FloatBuffer)
                    ((FloatBuffer)out).put(b.asFloatBuffer());
                else
                    throw new UnsupportedOperationException("Unhandled buffer type : " + out.getClass().getName());
            } finally {
                //out.reset();
                unmap(queue, b);
            }
        }
    }

    @SuppressWarnings("deprecation")
    public void write(Buffer in, CLQueue queue, boolean blocking) {
        if (in.isDirect()) {
            Pointer pres = Native.getDirectBufferPointer(in);
            error(CL.clEnqueueWriteBuffer(
                    queue.get(),
                    get(),
                    blocking ? CL_TRUE : 0,
                    toNL(0),
                    toNL(getSizeInBytes(in)),
                    pres,
                    0,
                    null,
                    (PointerByReference) null//pevt
                    ));
        } else {
            ByteBuffer b = blockingMapRead(queue);
            try {
                //out.mark();
                if (in instanceof IntBuffer)
                    b.asIntBuffer().put((IntBuffer)in);
                else if (in instanceof LongBuffer)
                    b.asLongBuffer().put((LongBuffer)in);
                else if (in instanceof ShortBuffer)
                    b.asShortBuffer().put((ShortBuffer)in);
                else if (in instanceof CharBuffer)
                    b.asCharBuffer().put((CharBuffer)in);
                else if (in instanceof DoubleBuffer)
                    b.asDoubleBuffer().put((DoubleBuffer)in);
                else if (in instanceof FloatBuffer)
                    b.asFloatBuffer().put((FloatBuffer)in);
                else
                    throw new UnsupportedOperationException("Unhandled buffer type : " + in.getClass().getName());
            } finally {
                //out.reset();
                unmap(queue, b);
            }
        }
    }
}
