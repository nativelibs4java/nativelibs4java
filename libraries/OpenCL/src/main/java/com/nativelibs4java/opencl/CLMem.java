/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
*/

package com.nativelibs4java.opencl;
import com.nativelibs4java.opencl.library.OpenCLLibrary;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;
import com.sun.jna.*;
import com.sun.jna.ptr.*;
import java.nio.*;
import static com.nativelibs4java.opencl.OpenCL4Java.*;

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

    public ByteBuffer mapReadWrite(CLQueue queue) {
        return map(queue, CL_MAP_READ | CL_MAP_WRITE);
    }

    public ByteBuffer mapWrite(CLQueue queue) {
        return map(queue, CL_MAP_WRITE);
    }

    public ByteBuffer mapRead(CLQueue queue) {
        return map(queue, CL_MAP_READ);
    }

    private ByteBuffer map(CLQueue queue, int flags) {
        Pointer p = CL.clEnqueueMapBuffer(queue.get(), get(), CL_TRUE, flags, toNL(0), toNL(byteCount), 0, (PointerByReference) null, null, (IntByReference) null);
        return p.getByteBuffer(0, byteCount);
    }

    public void unmap(CLQueue queue, Buffer buffer) {
        error(CL.clEnqueueUnmapMemObject(queue.get(), get(), Native.getDirectBufferPointer(buffer), 0, (PointerByReference) null, null));
    }

    @Override
    protected void clear() {
        CL.clReleaseMemObject(get());
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
            ByteBuffer b = mapRead(queue);
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
            ByteBuffer b = mapRead(queue);
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
