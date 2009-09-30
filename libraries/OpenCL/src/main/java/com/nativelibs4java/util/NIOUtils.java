/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.util;
import java.nio.*;
/**
 * NIO Buffer util methods
 * @author ochafik
 */
public class NIOUtils
{
	/**
	 * Creates a direct int buffer of the specified size (in elements) and a native byte order
	 * @param size size of the buffer in elements
	 * @return view on new direct buffer
	 */
	public static IntBuffer directInts(int size) {
        return ByteBuffer.allocateDirect(size * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
    }

    /**
	 * Creates a direct lpng buffer of the specified size (in elements) and a native byte order
	 * @param size size of the buffer in elements
	 * @return view on new direct buffer
	 */
	public static LongBuffer directLongs(int size) {
        return ByteBuffer.allocateDirect(size * 8).order(ByteOrder.nativeOrder()).asLongBuffer();
    }

    /**
	 * Creates a direct short buffer of the specified size (in elements) and a native byte order
	 * @param size size of the buffer in elements
	 * @return view on new direct buffer
	 */
	public static ShortBuffer directShorts(int size) {
        return ByteBuffer.allocateDirect(size * 2).order(ByteOrder.nativeOrder()).asShortBuffer();
    }

    /**
	 * Creates a direct byte buffer of the specified size (in elements) and a native byte order
	 * @param size size of the buffer in elements
	 * @return new direct buffer
	 */
	public static ByteBuffer directBytes(int size) {
        return ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
    }

    /**
	 * Creates a direct float buffer of the specified size (in elements) and a native byte order
	 * @param size size of the buffer in elements
	 * @return view on new direct buffer
	 */
	public static FloatBuffer directFloats(int size) {
        return ByteBuffer.allocateDirect(size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    }

    /**
	 * Creates a direct double buffer of the specified size (in elements) and a native byte order
	 * @param size size of the buffer in elements
	 * @return view on new direct buffer
	 */
	public static DoubleBuffer directDoubles(int size) {
        return ByteBuffer.allocateDirect(size * 8).order(ByteOrder.nativeOrder()).asDoubleBuffer();
    }

	/**
	 * Get the size in bytes of a buffer
	 */
	public static int getSizeInBytes(Buffer b) {
        int c = b.capacity();
		return getComponentSizeInBytes(b) * c;
    }

	/**
	 * Get the size in bytes of a primitive component of a buffer
	 */
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
}
