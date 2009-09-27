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

/**
 * Object-oriented wrappers around most common OpenCL structures and operations
 * @see https://developer.apple.com/mac/library/documentation/Performance/Conceptual/OpenCL_MacProgGuide/TheOpenCLWorkflow/TheOpenCLWorkflow.html
 * @author Olivier Chafik
 */
public class OpenCL4Java {

    public static final OpenCLLibrary CL = OpenCLLibrary.INSTANCE;

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

    static String errorString(int err) {
        if (err == CL_SUCCESS)
            return null;
        
        List<String> candidates = new ArrayList<String>();
        for (Field f : OpenCLLibrary.class.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers())) {
                continue;
            }
            if (f.getType().equals(Integer.TYPE)) {
                try {
                    int i = (Integer) f.get(null);
                    if (i == err) {
                        candidates.add(f.getName());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return StringUtils.implode(candidates, " or ");
    }
    
    static void error(int err) {
        String str = errorString(err);
        if (str == null)
            return;
        
        throw new CLException("OpenCL Error : " + str, err);
    }
}
