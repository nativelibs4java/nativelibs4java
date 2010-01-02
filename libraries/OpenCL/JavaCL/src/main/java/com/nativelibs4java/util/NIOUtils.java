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

        public static IntBuffer directCopy(IntBuffer b, ByteOrder order) {
            return directCopy((Buffer)b, order).asIntBuffer();
        }
	public static LongBuffer directCopy(LongBuffer b, ByteOrder order) {
            return directCopy((Buffer)b, order).asLongBuffer();
        }
	public static ShortBuffer directCopy(ShortBuffer b, ByteOrder order) {
            return directCopy((Buffer)b, order).asShortBuffer();
        }
	public static CharBuffer directCopy(CharBuffer b, ByteOrder order) {
            return directCopy((Buffer)b, order).asCharBuffer();
        }
	public static DoubleBuffer directCopy(DoubleBuffer b, ByteOrder order) {
            return directCopy((Buffer)b, order).asDoubleBuffer();
        }
	public static FloatBuffer directCopy(FloatBuffer b, ByteOrder order) {
            return directCopy((Buffer)b, order).asFloatBuffer();
        }
	public static ByteBuffer directCopy(Buffer b, ByteOrder order) {
		ByteBuffer copy = ByteBuffer.allocateDirect((int)getSizeInBytes(b)).order(order == null ? ByteOrder.nativeOrder() : order);
		put(b, copy);
		return copy;
	}
    
	/**
	 * Creates a direct int buffer of the specified size (in elements) and a native byte order
	 * @param size size of the buffer in elements
	 * @return view on new direct buffer
	 */
	public static IntBuffer directInts(int size, ByteOrder order) {
        return ByteBuffer.allocateDirect(size * 4).order(order == null ? ByteOrder.nativeOrder() : order).asIntBuffer();
    }

    /**
	 * Creates a direct lpng buffer of the specified size (in elements) and a native byte order
	 * @param size size of the buffer in elements
	 * @return view on new direct buffer
	 */
	public static LongBuffer directLongs(int size, ByteOrder order) {
        return ByteBuffer.allocateDirect(size * 8).order(order == null ? ByteOrder.nativeOrder() : order).asLongBuffer();
    }

    /**
	 * Creates a direct short buffer of the specified size (in elements) and a native byte order
	 * @param size size of the buffer in elements
	 * @return view on new direct buffer
	 */
	public static ShortBuffer directShorts(int size, ByteOrder order) {
        return ByteBuffer.allocateDirect(size * 2).order(order == null ? ByteOrder.nativeOrder() : order).asShortBuffer();
    }

    /**
	 * Creates a direct byte buffer of the specified size (in elements) and a native byte order
	 * @param size size of the buffer in elements
	 * @return new direct buffer
	 */
	public static ByteBuffer directBytes(int size, ByteOrder order) {
        return ByteBuffer.allocateDirect(size).order(order == null ? ByteOrder.nativeOrder() : order);
    }

    /**
	 * Creates a direct float buffer of the specified size (in elements) and a native byte order
	 * @param size size of the buffer in elements
	 * @return view on new direct buffer
	 */
	public static FloatBuffer directFloats(int size, ByteOrder order) {
        return ByteBuffer.allocateDirect(size * 4).order(order == null ? ByteOrder.nativeOrder() : order).asFloatBuffer();
    }

    /**
	 * Creates a direct double buffer of the specified size (in elements) and a native byte order
	 * @param size size of the buffer in elements
	 * @return view on new direct buffer
	 */
	public static DoubleBuffer directDoubles(int size, ByteOrder order) {
        return ByteBuffer.allocateDirect(size * 8).order(order == null ? ByteOrder.nativeOrder() : order).asDoubleBuffer();
    }
    

    public static <B extends Buffer> B directBuffer(int size, ByteOrder order, Class<B> bufferClass) {
        if (IntBuffer.class.isAssignableFrom(bufferClass))
            return (B)directInts(size, order);
		if (LongBuffer.class.isAssignableFrom(bufferClass))
            return (B)directLongs(size, order);
		if (ShortBuffer.class.isAssignableFrom(bufferClass))
            return (B)directShorts(size, order);
		if (ByteBuffer.class.isAssignableFrom(bufferClass))
            return (B)directBytes(size, order);
		if (DoubleBuffer.class.isAssignableFrom(bufferClass))
            return (B)directDoubles(size, order);
		if (FloatBuffer.class.isAssignableFrom(bufferClass))
            return (B)directFloats(size, order);

        throw new UnsupportedOperationException("Cannot create direct buffers of type " + bufferClass.getName());
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

    public static <B extends Buffer, V> void put(B buffer, int position, V value) {
        if (buffer instanceof IntBuffer)
            ((IntBuffer)buffer).put(position, ((Number)value).intValue());
        else if (buffer instanceof LongBuffer)
            ((LongBuffer)buffer).put(position, ((Number)value).longValue());
        else if (buffer instanceof ShortBuffer)
            ((ShortBuffer)buffer).put(position, ((Number)value).shortValue());
        else if (buffer instanceof ByteBuffer)
            ((ByteBuffer)buffer).put(position, ((Number)value).byteValue());
        else if (buffer instanceof DoubleBuffer)
            ((DoubleBuffer)buffer).put(position, ((Number)value).doubleValue());
        else if (buffer instanceof FloatBuffer)
            ((FloatBuffer)buffer).put(position, ((Number)value).floatValue());
        else
            throw new UnsupportedOperationException();
    }

    public static <B extends Buffer, V> V get(B buffer, int position) {
        if (buffer instanceof IntBuffer)
            return (V)(Integer)((IntBuffer)buffer).get(position);
        else if (buffer instanceof LongBuffer)
            return (V)(Long)((LongBuffer)buffer).get(position);
        else if (buffer instanceof ShortBuffer)
            return (V)(Short)((ShortBuffer)buffer).get(position);
        else if (buffer instanceof ByteBuffer)
            return (V)(Byte)((ByteBuffer)buffer).get(position);
        else if (buffer instanceof DoubleBuffer)
            return (V)(Double)((DoubleBuffer)buffer).get(position);
        else if (buffer instanceof FloatBuffer)
            return (V)(Float)((FloatBuffer)buffer).get(position);
        else
            throw new UnsupportedOperationException();
    }
}
