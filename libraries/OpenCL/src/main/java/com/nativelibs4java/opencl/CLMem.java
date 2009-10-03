/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
*/

package com.nativelibs4java.opencl;
import com.nativelibs4java.opencl.library.OpenCLLibrary;
import com.nativelibs4java.util.EnumValue;
import com.nativelibs4java.util.EnumValues;
import com.ochafik.util.listenable.Pair;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;
import com.sun.jna.*;
import com.sun.jna.ptr.*;
import java.nio.*;
import java.util.EnumSet;
import static com.nativelibs4java.opencl.OpenCL4Java.*;
import static com.nativelibs4java.opencl.CLException.*;
import static com.nativelibs4java.util.JNAUtils.*;
import static com.nativelibs4java.util.NIOUtils.*;

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


	public enum Flags {
		/**
		 * This flag specifies that the memory object will be read and written by a kernel. This is the default.
		 */
		@EnumValue(CL_MEM_READ_WRITE)		MEM_READ_WRITE,
		/**
		 * This flags specifies that the memory object will be written but not read by a kernel.<br/>
		 * Reading from a buffer or image object created with CL_MEM_WRITE_ONLY inside a kernel is undefined.
		 */
		@EnumValue(CL_MEM_WRITE_ONLY)		MEM_WRITE_ONLY,
		/**
		 * This flag specifies that the memory object is a read-only memory object when used inside a kernel. <br/>
		 * Writing to a buffer or image object created with CL_MEM_READ_ONLY inside a kernel is undefined.
		 */
		@EnumValue(CL_MEM_READ_ONLY)		MEM_READ_ONLY,
		/**
		 * This flag is valid only if host_ptr is not NULL. If specified, it indicates that the application wants the OpenCL implementation to use memory referenced by host_ptr as the storage bits for the memory object. <br/>
		 * OpenCL implementations are allowed to cache the buffer contents pointed to by host_ptr in device memory. This cached copy can be used when kernels are executed on a device. <br/>
		 * The result of OpenCL commands that operate on multiple buffer objects created with the same host_ptr or overlapping host regions is considered to be undefined.
		 */
		@EnumValue(CL_MEM_USE_HOST_PTR)		MEM_USE_HOST_PTR,
		/**
		 * This flag specifies that the application wants the OpenCL implementation to allocate memory from host accessible memory. <br/>
		 * CL_MEM_ALLOC_HOST_PTR and CL_MEM_USE_HOST_PTR are mutually exclusive.<br/>
		 * CL_MEM_COPY_HOST_PTR: This flag is valid only if host_ptr is not NULL. If specified, it indicates that the application wants the OpenCL implementation to allocate memory for the memory object and copy the data from memory referenced by host_ptr.<br/>
		 * CL_MEM_COPY_HOST_PTR and CL_MEM_USE_HOST_PTR are mutually exclusive.<br/>
		 * CL_MEM_COPY_HOST_PTR can be used with CL_MEM_ALLOC_HOST_PTR to initialize the contents of the cl_mem object allocated using host-accessible (e.g. PCIe) memory.
		 */
		@EnumValue(CL_MEM_ALLOC_HOST_PTR)		MEM_ALLOC_HOST_PTR;

		public long getValue() { return EnumValues.getValue(this); }
		public static Flags getEnum(long v) { return EnumValues.getEnum(v, Flags.class); }
	}
	public enum ObjectType {
		@EnumValue(CL_MEM_OBJECT_BUFFER) Buffer,
		@EnumValue(CL_MEM_OBJECT_IMAGE2D) Image2D,
		@EnumValue(CL_MEM_OBJECT_IMAGE3D) Image3D;

		public long getValue() { return EnumValues.getValue(this); }
		public static ObjectType getEnum(long v) { return EnumValues.getEnum(v, ObjectType.class); }
	}

	public enum MapFlags {
		@EnumValue(CL_MAP_READ) Read,
		@EnumValue(CL_MAP_WRITE) Write,
		@EnumValue(CL_MAP_READ | CL_MAP_WRITE) ReadWrite;

		public long getValue() { return EnumValues.getValue(this); }
		public static MapFlags getEnum(long v) { return EnumValues.getEnum(v, MapFlags.class); }
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
		return map(queue, flags, 0, getByteCount(), true, eventsToWaitFor);
    }
	public Pair<ByteBuffer, CLEvent> enqueueMap(CLQueue queue, MapFlags flags, long offset, long length, CLEvent... eventsToWaitFor) {
		return map(queue, flags, offset, length, true, eventsToWaitFor);
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

    @Override
    protected void clear() {
        error(CL.clReleaseMemObject(get()));
    }

	@SuppressWarnings("deprecation")
    public CLEvent read(Buffer out, CLQueue queue, boolean blocking, CLEvent... eventsToWaitFor) {
        if (out.isDirect()) {
            cl_event[] eventOut = blocking ? null : new cl_event[1];
			error(CL.clEnqueueReadBuffer(
				queue.get(),
				get(),
				blocking ? CL_TRUE : 0,
				toNL(0),
				toNL(getSizeInBytes(out)),
				Native.getDirectBufferPointer(out),
				eventsToWaitFor.length, CLEvent.to_cl_event_array(eventsToWaitFor),
				eventOut
            ));
			return blocking ? null : CLEvent.createEvent(eventOut[0]);
        } else {
            ByteBuffer b = blockingMapRead(queue);
			CLEvent.waitFor(eventsToWaitFor);
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
                CLEvent evt = unmap(queue, b);
				if (blocking)
					evt.waitFor();
				return blocking ? null : evt;
            }
        }
    }

    @SuppressWarnings("deprecation")
    public CLEvent write(Buffer in, CLQueue queue, boolean blocking, CLEvent... eventsToWaitFor) {
        if (in.isDirect()) {
            cl_event[] eventOut = blocking ? null : new cl_event[1];
			error(CL.clEnqueueWriteBuffer(
				queue.get(),
				get(),
				blocking ? CL_TRUE : 0,
				toNL(0),
				toNL(getSizeInBytes(in)),
				Native.getDirectBufferPointer(in),
				eventsToWaitFor.length, CLEvent.to_cl_event_array(eventsToWaitFor),
				eventOut
			));
			return blocking ? null : CLEvent.createEvent(eventOut[0]);
        } else {
            ByteBuffer b = blockingMapRead(queue);
			CLEvent.waitFor(eventsToWaitFor);
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
                CLEvent evt = unmap(queue, b);
				if (blocking)
					evt.waitFor();
				return blocking ? null : evt;
            }
        }
    }

}
