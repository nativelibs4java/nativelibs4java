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
        Pointer p = CL.clEnqueueMapBuffer(queue.get(), get(), CL_TRUE, toNL(flags), toNL(0), toNL(byteCount), 0, (PointerByReference) null, null, (IntByReference) null);
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
