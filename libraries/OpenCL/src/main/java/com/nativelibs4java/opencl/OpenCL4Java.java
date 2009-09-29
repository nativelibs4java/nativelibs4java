package com.nativelibs4java.opencl;

import com.nativelibs4java.opencl.library.OpenCLLibrary;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.ochafik.util.string.StringUtils;
import com.sun.jna.*;
import java.nio.*;
import static com.nativelibs4java.opencl.OpenCL4Java.*;
import com.sun.jna.ptr.*;
import static com.nativelibs4java.opencl.CLException.*;

/**
 * Entry point class for the OpenCL4Java Object-oriented wrappers around the OpenCL API.<br/>
 * @author Olivier Chafik
 */
public class OpenCL4Java {

    static final OpenCLLibrary CL = OpenCLLibrary.INSTANCE;

	/**
	 * Lists all available OpenCL implementations.
	 */
    public static CLPlatform[] listPlatforms() {
        IntByReference pCount = new IntByReference();
        error(CL.clGetPlatformIDs(0, (cl_platform_id[])null, pCount));

        int nPlats = pCount.getValue();
        if (nPlats == 0)
            return new CLPlatform[0];

        cl_platform_id[] ids = new cl_platform_id[nPlats];

        error(CL.clGetPlatformIDs(nPlats, ids, null));
        CLPlatform[] platforms = new CLPlatform[nPlats];

        for (int i = 0; i < nPlats; i++) {
            platforms[i] = new CLPlatform(ids[i]);
        }
        return platforms;
    }

    public static IntBuffer directInts(int size) {
        return ByteBuffer.allocateDirect(size * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
    }

    public static LongBuffer directLongs(int size) {
        return ByteBuffer.allocateDirect(size * 8).order(ByteOrder.nativeOrder()).asLongBuffer();
    }

    public static ShortBuffer directShorts(int size) {
        return ByteBuffer.allocateDirect(size * 2).order(ByteOrder.nativeOrder()).asShortBuffer();
    }

    public static ByteBuffer directBytes(int size) {
        return ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
    }

    public static FloatBuffer directFloats(int size) {
        return ByteBuffer.allocateDirect(size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    }

    public static DoubleBuffer directDoubles(int size) {
        return ByteBuffer.allocateDirect(size * 8).order(ByteOrder.nativeOrder()).asDoubleBuffer();
    }

    @SuppressWarnings("serial")
    public static class CLBuildException extends Exception {

        public CLBuildException(String string, Collection<String> errors) {
            super(string + "\n" + StringUtils.implode(errors, "\n"));
        }
    }
    
    public static int getSizeInBytes(Buffer b) {
        int c = b.capacity();
		return getComponentSizeInBytes(b) * c;
    }
	public static int getComponentSizeInBytes(Buffer b) {
        if (b instanceof IntBuffer || b instanceof FloatBuffer)
            return 4;
        if (b instanceof LongBuffer || b instanceof DoubleBuffer)
            return 8;
        if (b instanceof ShortBuffer || b instanceof CharBuffer)
            return 2;
        if (b instanceof ByteBuffer)
            return 1;
        throw new UnsupportedOperationException("Cannot guess byte size of buffers of type " + b.getClass().getName());
    }

    public static NativeLongByReference toNL(IntByReference local) {
        NativeLongByReference nl = new NativeLongByReference();
        nl.setPointer(local.getPointer());
        return nl;
    }

    public static NativeLong toNL(int i) {
        return new NativeLong(i);
    }
	public static NativeLong toNL(long i) {
        return new NativeLong(i);
    }
}
