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
	 * Bulk-copy all of the input buffer into output byte buffer
	 * @param inputBytes
	 * @param output
	 */
	public static void put(Buffer input, ByteBuffer outputBytes) {

		if (input instanceof IntBuffer)
			outputBytes.asIntBuffer().put((IntBuffer)input);
		else if (input instanceof LongBuffer)
			outputBytes.asLongBuffer().put((LongBuffer)input);
		else if (input instanceof ShortBuffer)
			outputBytes.asShortBuffer().put((ShortBuffer)input);
		else if (input instanceof CharBuffer)
			outputBytes.asCharBuffer().put((CharBuffer)input);
		else if (input instanceof DoubleBuffer)
			outputBytes.asDoubleBuffer().put((DoubleBuffer)input);
		else if (input instanceof FloatBuffer)
			outputBytes.asFloatBuffer().put((FloatBuffer)input);
		else
			throw new UnsupportedOperationException("Unhandled buffer type : " + input.getClass().getName());

		outputBytes.rewind();
	}
	
	/**
	 * Bulk-copy all input bytes into output buffer
	 * @param inputBytes
	 * @param output
	 */
	public static void put(ByteBuffer inputBytes, Buffer output) {

		if (output instanceof IntBuffer)
			((IntBuffer)output).put(inputBytes.asIntBuffer());
		else if (output instanceof LongBuffer)
			((LongBuffer)output).put(inputBytes.asLongBuffer());
		else if (output instanceof ShortBuffer)
			((ShortBuffer)output).put(inputBytes.asShortBuffer());
		else if (output instanceof CharBuffer)
			((CharBuffer)output).put(inputBytes.asCharBuffer());
		else if (output instanceof DoubleBuffer)
			((DoubleBuffer)output).put(inputBytes.asDoubleBuffer());
		else if (output instanceof FloatBuffer)
			((FloatBuffer)output).put(inputBytes.asFloatBuffer());
		else
			throw new UnsupportedOperationException("Unhandled buffer type : " + output.getClass().getName());

		output.rewind();
	}

	public static ByteBuffer directCopy(Buffer b) {
		ByteBuffer copy = ByteBuffer.allocateDirect((int)getSizeInBytes(b)).order(ByteOrder.nativeOrder());
		put(b, copy);
		return copy;
	}
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
	public static long getSizeInBytes(Buffer b) {
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
