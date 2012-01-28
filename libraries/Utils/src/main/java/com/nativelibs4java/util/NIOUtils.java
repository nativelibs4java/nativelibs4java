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

	public static Class<? extends Buffer> getBufferClass(Class<?> primitiveClass) {
		if (primitiveClass == Byte.class || primitiveClass == byte.class)
			return ByteBuffer.class;
		if (primitiveClass == Short.class || primitiveClass == short.class)
			return ShortBuffer.class;
		if (primitiveClass == Character.class || primitiveClass == char.class)
			return CharBuffer.class;
		if (primitiveClass == Integer.class || primitiveClass == int.class)
			return IntBuffer.class;
		if (primitiveClass == Long.class || primitiveClass == long.class)
			return LongBuffer.class;
		if (primitiveClass == Float.class || primitiveClass == float.class)
			return FloatBuffer.class;
		if (primitiveClass == Double.class || primitiveClass == double.class)
			return DoubleBuffer.class;
		throw new UnsupportedOperationException("Unhandled primitive type : " + primitiveClass.getName());
	}
	public static Class<?> getPrimitiveClass(Class<? extends Buffer> bufferClass) {
		if (bufferClass == ByteBuffer.class) return Byte.class;
		if (bufferClass == ShortBuffer.class) return Short.class;
		if (bufferClass == CharBuffer.class) return Character.class;
		if (bufferClass == IntBuffer.class) return Integer.class;
		if (bufferClass == LongBuffer.class) return Long.class;
		if (bufferClass == FloatBuffer.class) return Float.class;
		if (bufferClass == DoubleBuffer.class) return Double.class;
		throw new UnsupportedOperationException("Unhandled buffer type : " + bufferClass.getName());
	}
	
	/**
	 * Bulk-copy all of the input buffer into the output buffer
	 * @param input
	 * @param output
	 */
	public static void put(Buffer input, Buffer output) {
		if (input instanceof ByteBuffer)
			put((ByteBuffer)input, output);
		else if (output instanceof ByteBuffer)
			put(input, (ByteBuffer)output);
		else if (input instanceof IntBuffer && output instanceof IntBuffer)
			((IntBuffer)output).duplicate().put((IntBuffer)input);
		else if (input instanceof LongBuffer && output instanceof LongBuffer)
			((LongBuffer)output).duplicate().put((LongBuffer)input);
		else if (input instanceof ShortBuffer && output instanceof ShortBuffer)
			((ShortBuffer)output).duplicate().put((ShortBuffer)input);
		else if (input instanceof CharBuffer && output instanceof CharBuffer)
			((CharBuffer)output).duplicate().put((CharBuffer)input);
		else if (input instanceof DoubleBuffer && output instanceof DoubleBuffer)
			((DoubleBuffer)output).duplicate().put((DoubleBuffer)input);
		else if (input instanceof FloatBuffer && output instanceof FloatBuffer)
			((FloatBuffer)output).duplicate().put((FloatBuffer)input);
		else
			throw new UnsupportedOperationException("Unhandled buffer type : " + input.getClass().getName());
	}
		
	/**
	 * Bulk-copy all of the input buffer into the output buffer
	 * @param input
	 * @param outputBytes
	 */
	public static void put(Buffer input, ByteBuffer outputBytes) {
			
		if (input instanceof ByteBuffer)
            outputBytes.duplicate().put(((ByteBuffer)input).duplicate());
		else if (input instanceof IntBuffer)
			outputBytes.asIntBuffer().put(((IntBuffer)input).duplicate());
		else if (input instanceof LongBuffer)
			outputBytes.asLongBuffer().put(((LongBuffer)input).duplicate());
		else if (input instanceof ShortBuffer)
			outputBytes.asShortBuffer().put(((ShortBuffer)input).duplicate());
		else if (input instanceof CharBuffer)
			outputBytes.asCharBuffer().put(((CharBuffer)input).duplicate());
        else if (input instanceof DoubleBuffer)
			outputBytes.asDoubleBuffer().put(((DoubleBuffer)input).duplicate());
		else if (input instanceof FloatBuffer)
			outputBytes.asFloatBuffer().put(((FloatBuffer)input).duplicate());
		else
			throw new UnsupportedOperationException("Unhandled buffer type : " + input.getClass().getName());

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
        else if (output instanceof ByteBuffer)
            ((ByteBuffer)output).put(inputBytes);
		else if (output instanceof DoubleBuffer)
			((DoubleBuffer)output).put(inputBytes.asDoubleBuffer());
		else if (output instanceof FloatBuffer)
			((FloatBuffer)output).put(inputBytes.asFloatBuffer());
		else if (output instanceof CharBuffer)
			((CharBuffer)output).put(inputBytes.asCharBuffer());
		else
			throw new UnsupportedOperationException("Unhandled buffer type : " + output.getClass().getName());

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
	 * @param order byte order of the direct buffer
	 * @return view on new direct buffer
	 */
	public static IntBuffer directInts(int size, ByteOrder order) {
        return ByteBuffer.allocateDirect(size * 4).order(order == null ? ByteOrder.nativeOrder() : order).asIntBuffer();
    }

    /**
	 * Creates a direct lpng buffer of the specified size (in elements) and a native byte order
	 * @param size size of the buffer in elements
	 * @param order byte order of the direct buffer
	 * @return view on new direct buffer
	 */
	public static LongBuffer directLongs(int size, ByteOrder order) {
        return ByteBuffer.allocateDirect(size * 8).order(order == null ? ByteOrder.nativeOrder() : order).asLongBuffer();
    }

    /**
	 * Creates a direct short buffer of the specified size (in elements) and a native byte order
	 * @param size size of the buffer in elements
	 * @param order byte order of the direct buffer
	 * @return view on new direct buffer
	 */
	public static ShortBuffer directShorts(int size, ByteOrder order) {
        return ByteBuffer.allocateDirect(size * 2).order(order == null ? ByteOrder.nativeOrder() : order).asShortBuffer();
    }

    /**
	 * Creates a direct byte buffer of the specified size (in elements) and a native byte order
	 * @param size size of the buffer in elements
	 * @param order byte order of the direct buffer
	 * @return new direct buffer
	 */
	public static ByteBuffer directBytes(int size, ByteOrder order) {
        return ByteBuffer.allocateDirect(size).order(order == null ? ByteOrder.nativeOrder() : order);
    }

    /**
	 * Creates a direct float buffer of the specified size (in elements) and a native byte order
	 * @param size size of the buffer in elements
	 * @param order byte order of the direct buffer
	 * @return view on new direct buffer
	 */
	public static FloatBuffer directFloats(int size, ByteOrder order) {
        return ByteBuffer.allocateDirect(size * 4).order(order == null ? ByteOrder.nativeOrder() : order).asFloatBuffer();
    }

    /**
	 * Creates a direct char buffer of the specified size (in elements) and a native byte order
	 * @param size size of the buffer in elements
	 * @param order byte order of the direct buffer
	 * @return view on new direct buffer
	 */
	public static CharBuffer directChars(int size, ByteOrder order) {
        return ByteBuffer.allocateDirect(size * 4).order(order == null ? ByteOrder.nativeOrder() : order).asCharBuffer();
    }

    /**
	 * Creates a direct double buffer of the specified size (in elements) and a native byte order
	 * @param size size of the buffer in elements
	 * @param order byte order of the direct buffer
	 * @return view on new direct buffer
	 */
	public static DoubleBuffer directDoubles(int size, ByteOrder order) {
        return ByteBuffer.allocateDirect(size * 8).order(order == null ? ByteOrder.nativeOrder() : order).asDoubleBuffer();
    }
    
    /**
	 * Creates a direct buffer of the specified size (in elements) and type..
	 * @param size size of the buffer in elements
	 * @param order byte order of the direct buffer
	 * @param bufferClass type of the buffer. Must be one of IntBuffer.class, LongBuffer.class, ShortBuffer.class, ByteBuffer.class, DoubleBuffer.class, FloatBuffer.class
	 * @return view on new direct buffer
	 */
	 @SuppressWarnings("unchecked")
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
        if (CharBuffer.class.isAssignableFrom(bufferClass))
            return (B)directChars(size, order);

        throw new UnsupportedOperationException("Cannot create direct buffers of type " + bufferClass.getName());
	}
	/**
	 * Creates a indirect buffer of the specified size (in elements) and type..
	 * @param size size of the buffer in elements
	 * @param bufferClass type of the buffer. Must be one of IntBuffer.class, LongBuffer.class, ShortBuffer.class, ByteBuffer.class, DoubleBuffer.class, FloatBuffer.class
	 * @return view on new direct buffer
	 */
	 @SuppressWarnings("unchecked")
	public static <B extends Buffer> B indirectBuffer(int size, Class<B> bufferClass) {
        if (IntBuffer.class.isAssignableFrom(bufferClass))
            return (B)IntBuffer.allocate(size);
		if (LongBuffer.class.isAssignableFrom(bufferClass))
            return (B)LongBuffer.allocate(size);
		if (ShortBuffer.class.isAssignableFrom(bufferClass))
            return (B)ShortBuffer.allocate(size);
		if (ByteBuffer.class.isAssignableFrom(bufferClass))
            return (B)ByteBuffer.allocate(size);
		if (DoubleBuffer.class.isAssignableFrom(bufferClass))
            return (B)DoubleBuffer.allocate(size);
		if (FloatBuffer.class.isAssignableFrom(bufferClass))
            return (B)FloatBuffer.allocate(size);
		if (CharBuffer.class.isAssignableFrom(bufferClass))
            return (B)CharBuffer.allocate(size);

        throw new UnsupportedOperationException("Cannot create indirect buffers of type " + bufferClass.getName());
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
        else if (buffer instanceof CharBuffer)
            ((CharBuffer)buffer).put(position, (char)((Number)value).shortValue());
        else
            throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
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
        else if (buffer instanceof CharBuffer)
            return (V)(Character)((CharBuffer)buffer).get(position);
        else
            throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
	public static <B extends Buffer> Object getArray(B buffer) {
        int length = buffer.capacity();
        if (buffer instanceof IntBuffer) {
            int[] a = new int[length];
            ((IntBuffer)buffer).duplicate().get(a);
            return a;
        } else if (buffer instanceof LongBuffer) {
            long[] a = new long[length];
            ((LongBuffer)buffer).duplicate().get(a);
            return a;
        } else if (buffer instanceof ShortBuffer) {
            short[] a = new short[length];
            ((ShortBuffer)buffer).duplicate().get(a);
            return a;
        } else if (buffer instanceof ByteBuffer) {
            byte[] a = new byte[length];
            ((ByteBuffer)buffer).duplicate().get(a);
            return a;
        } else if (buffer instanceof DoubleBuffer) {
            double[] a = new double[length];
            ((DoubleBuffer)buffer).duplicate().get(a);
            return a;
        } else if (buffer instanceof FloatBuffer) {
            float[] a = new float[length];
            ((FloatBuffer)buffer).duplicate().get(a);
            return a;
        } else
            throw new UnsupportedOperationException();
    }
    public static <B extends Buffer> B wrapArray(Object a) {
        if (a instanceof int[])
            return (B)IntBuffer.wrap((int[])a);
		if (a instanceof long[])
            return (B)LongBuffer.wrap((long[])a);
		if (a instanceof short[])
            return (B)ShortBuffer.wrap((short[])a);
		if (a instanceof byte[])
            return (B)ByteBuffer.wrap((byte[])a);
		if (a instanceof float[])
            return (B)FloatBuffer.wrap((float[])a);
		if (a instanceof double[])
            return (B)DoubleBuffer.wrap((double[])a);
        throw new UnsupportedOperationException("Cannot wrap primitive arrays of type " + a.getClass().getName());
	}

}
