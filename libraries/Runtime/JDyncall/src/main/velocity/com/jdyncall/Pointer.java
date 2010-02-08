package com.jdyncall;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.*;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public abstract class Pointer<T> implements Comparable<Pointer<?>>
        //, com.sun.jna.Pointer<Pointer<T>>
{
    public static final Pointer NULL = null;
	public static final int SIZE = JNI.POINTER_SIZE;
    public static final byte
        LITTLE_ENDIAN = (byte)'l',
        BIG_ENDIAN = (byte)'b',
        DEFAULT_ENDIANNESS = getEndianness(ByteOrder.nativeOrder());

    protected static final int
		TYPE_FROM_DIRECT_BUFFER				= 0,
		TYPE_FROM_INDIRECT_BUFFER			= 1,
		TYPE_FROM_RAW_ADDRESS				= 2,
		TYPE_FROM_ALLOCATED_ADDRESS			= 3,
		TYPE_FROM_POINTER_GET_POINTER_VALUE	= 4;

    static {
        JNI.initLibrary();
    }

	static byte getEndianness(ByteOrder order) {
        return order.equals(ByteOrder.BIG_ENDIAN) ? BIG_ENDIAN : LITTLE_ENDIAN;
    }
    
    protected PointerIO<T> io;
    protected byte endianness = DEFAULT_ENDIANNESS;
	
    public void order(ByteOrder order) {
        endianness = getEndianness(order);
    }
    
	public final ByteOrder order() {
        switch (endianness) {
            case BIG_ENDIAN:
                return ByteOrder.BIG_ENDIAN;
            case LITTLE_ENDIAN:
                return ByteOrder.LITTLE_ENDIAN;
            default:
                return ByteOrder.nativeOrder();
        }
    }

    /**
     * Create a copy of this pointer
     * @return
     * @throws CloneNotSupportedException
     */
    @Override
    public Pointer<T> clone() {
        throw new UnsupportedOperationException(); // TODO
    }

	public interface UpdatablePointer<P> {
		Pointer<P> getPointer();
		void update();
	}
	
	/**
	 * The update will take place inside the release() call
	 */
    public static Pointer<String> pointerTo(final String[] strings) {
        final int len = strings.length;
        final Pointer<String> mem = allocateArray(String.class, len);
        for (int i = 0; i < len; i++)
            mem.set(i, strings[i]);

		class UpdatableStringArrayPointer extends Memory<String> {
			public UpdatableStringArrayPointer(Memory mem) {
				super(PointerIO.getStringInstance(), mem);
			}
			public Pointer<String> getPointer() {
				return this;
			}
            @Override
            public void release() {
                for (int i = 0; i < len; i++) {
                    strings[i] = mem.get(i);
                }
                super.release();
            }
        };
		return new UpdatableStringArrayPointer((Memory)mem);
    }

	public abstract Pointer<Pointer<T>> getReference();

    /**
	 * Dereference this pointer (*ptr).
     * @throws RuntimeException if the pointer's target type is unknown (@see Pointer.setTargetType())
	 */
    public T get() {
        return get(0);
    }
    
	public T get(int index) {
        PointerIO<T> io = getIO();
        if (io == null)
            throw new RuntimeException("Cannot get pointed value without a properly defined targetType");

        return io.get(this, index);
    }
    
    public void set(T value) {
        set(0, value);
    }
    
	public Pointer<T> set(int index, T value) {
        PointerIO<T> io = getIO();
        if (io == null) {
            if (value == null)
                throw new RuntimeException("Cannot set pointed value without a properly defined targetType");
            else
                setTargetType(value.getClass());
        }
        io.set(this, index, value);
        return this;
    }
    
	public static long getOrAllocateTempPeer(Pointer<?> pointer) {
        return pointer == null ? 0 : pointer.getOrAllocateTempPeer();
    }
	
	public static long getPeer(Pointer<?> pointer) {
        return pointer == null ? 0 : pointer.getPeer();
    }
	
	public static void deleteTempPeer(Pointer<?> pointer, long tempPeer, boolean refresh) {
		if (pointer == null || tempPeer == 0)
			return;
		pointer.deleteTempPeer(tempPeer, refresh);
    }
	
    
    public abstract Pointer<T> share(long offset);

	/**
	 * TODO JavaDoc
	 * Returns the long value of the address this Pointer instance points to.
	 * @throws UnsupportedOperationException if the pointer does not have a peer (@see isDirect())
	 */
    public abstract long getPeer();
    
	public abstract boolean isDirect();
	
	public abstract long getOrAllocateTempPeer();
    
	public abstract void deleteTempPeer(long tempPeer, boolean refresh);
	
	/**
	 * TODO JavaDoc
	 * Ask for early memory reclamation in case of manually allocated pointer.
	 */
    public void release() {}

    /**
	 * Compare to another Pointer instance
	 */
    public abstract int compareTo(Pointer<?> o);

    /**
	 * Test equality of the 
	 */
	@Override
    public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Pointer))
			return false;
		
		Pointer p = (Pointer)obj;
		boolean hp = isDirect();
		if (hp != p.isDirect())
			return false;
		
		if (hp)
			return getPeer() == p.getPeer();
		else
			return identityEqual(p);
	}
	
	/**
	 * Used in case of pointers that don't have peers to compare.
	 * This can be overridden to provide an identity equality that takes underlying data into account.
	 */
	protected boolean identityEqual(Pointer p) {
		return super.equals(p);	 
	}

    @Deprecated
    public static Pointer<?> pointerToAddress(long address) {
        return address == 0 ? null : new DefaultPointer(null, address);
    }

    @Deprecated
    public static Pointer<?> pointerToAddress(long address, long size) {
        return pointerToAddress(address, size, null);
    }

    public interface Deallocator {
        void deallocate(long peer);
    }

    @Deprecated
    public static Pointer<?> pointerToAddress(long address, final Deallocator deallocator) {
		return pointerToAddress(address, -1, deallocator);
	}
    
	public static Pointer<?> pointerToAddress(long address, long size, final Deallocator deallocator) {
        return address == 0 ? null : new Memory(null, address, -1) {
            @Override
            protected void free(long peer) {
                if (deallocator != null)
                    deallocator.deallocate(peer);
            }
        };
    }
	
	@Deprecated
    public static <P> Pointer<P> pointerToAddress(long address, Class<P> targetClass) {
        return new DefaultPointer(PointerIO.getInstance(targetClass), address);
    }

    public static <V> Pointer<V> allocate(Class<V> elementClass) {
        return allocateArray(elementClass, 1);
    }
    
    public static <V> Pointer<V> allocate(PointerIO<V> io, int byteSize) {
        return new Memory<V>(io, byteSize);
    }
    
    public static <V> Pointer<V> allocateArray(Class<V> elementClass, int arrayLength) {
        #foreach ($prim in $primitivesNoBool)
        if (elementClass == ${prim.WrapperName}.TYPE || elementClass == ${prim.WrapperName}.class)
            return (Pointer<V>)allocate(PointerIO.get${prim.CapName}Instance(), ${prim.Size} * arrayLength);
        #end
        throw new UnsupportedOperationException("Cannot allocate memory for type " + elementClass.getName());
    }

	public static Pointer<?> pointerTo(Buffer buffer) {
		return pointerTo(buffer, 0);
    }

    protected static Pointer<?> pointerTo(Buffer buffer, long byteOffset) {
        if (buffer == null)
			return null;
		
		#foreach ($prim in $primitivesNoBool)
		if (buffer instanceof ${prim.BufferName})
			return (Pointer)pointerTo((${prim.BufferName})buffer, byteOffset);
		#end
        throw new UnsupportedOperationException();
	}

#foreach ($prim in $primitivesNoBool)
    public static Pointer<${prim.WrapperName}> pointerTo(${prim.Name}... values) {
        Pointer<${prim.WrapperName}> mem = allocate(PointerIO.get${prim.CapName}Instance(), ${prim.Size} * values.length);
        mem.set${prim.CapName}s(0, values, 0, values.length);
        return mem;
    }
	
	public static Pointer<${prim.WrapperName}> pointerTo(${prim.BufferName} buffer) {
		return pointerTo(buffer);
	}
	
    public static Pointer<${prim.WrapperName}> pointerTo(${prim.BufferName} buffer, long byteOffset) {
        if (buffer == null)
			return null;
		
		PointerIO<${prim.WrapperName}> io = PointerIO.get${prim.CapName}Instance();
		Pointer<${prim.WrapperName}> pointer;
		if (buffer.isDirect()) {
			long address = JNI.getDirectBufferAddress(buffer);
			long size = JNI.getDirectBufferCapacity(buffer);
			if (address == 0 || size == 0)
				return null;
			
			pointer = new Memory(io, address + byteOffset, size, buffer);
		} else {
			#if (${prim.Name} == "byte")
			pointer = new IndirectBufferPointer<${prim.WrapperName}>(io, buffer, byteOffset);
			pointer.order(buffer.order());
			#else
			throw new UnsupportedOperationException("Cannot create pointers to indirect ${prim.BufferName} buffers (try with a ByteBuffer or with any direct buffer)");
			#end
		}
		
		return pointer;
    }
	
	/*
	public static Pointer<${prim.WrapperName}> pointerTo(${prim.BufferName} buffer) {
		if (buffer == null)
			return null;

        PointerIO<${prim.WrapperName}> io = PointerIO.get${prim.CapName}Instance();
		if (buffer.isDirect()) {
			long address = JNI.getDirectBufferAddress(buffer);
			if (address == 0)
				return null;
	
			long size = JNI.getDirectBufferCapacity(buffer);
	
			Pointer<T> pointer = new Memory(PointerIO.getBufferPrimitiveInstance(buffer), address, size, buffer);
			#if (${prim.Name} != "byte")
			pointer.order(buffer.order());
			#end
			return pointer;
		}
		
		#if (${prim.Name} != "byte")
		return new IndirectBufferPointer<${prim.WrapperName}>(io, buffer);
		#else
		throw new UnsupportedOperationException("Cannot create pointers to indirect ${prim.BufferName} buffers (try with a ByteBuffer or with any direct buffer)");
		#end
    }
	*/
	
	
    public static Pointer<${prim.WrapperName}> allocate${prim.CapName}() {
        return allocate(PointerIO.get${prim.CapName}Instance(), ${prim.Size});
    }
    public static Pointer<${prim.WrapperName}> allocate${prim.CapName}Array(int arrayLength) {
        return allocate(PointerIO.get${prim.CapName}Instance(), ${prim.Size} * arrayLength);
    }
#end

    protected PointerIO<T> getIO() {
        return io;
    }
    public Type getTargetType() {
        PointerIO<T> io = getIO();
        return io == null ? null : io.getTargetType();
    }
    public <N> Pointer<N> setTargetType(Type type) {
        io = PointerIO.change(io, type);
        return (Pointer<N>)this;
    }
    public <N> Pointer<N> setTargetClass(Class<N> type) {
        return setTargetType((Type)type);
    }

    
    public Pointer<?> getPointer(long byteOffset) {
        return new DefaultPointer(null, getSizeT(byteOffset));
    }

    public <U> Pointer<U> getPointer(long offset, Type t) {
        return new DefaultPointer(PointerIO.getInstanceByType(t), getSizeT(offset));
    }
    public <U> Pointer<U> getPointer(long offset, Class<U> t) {
        return new DefaultPointer<U>(PointerIO.getInstance(t), getSizeT(offset));
    }
	
	static final boolean is64 = JNI.POINTER_SIZE == 8; 
	
	protected long getSizeT(long byteOffset) {
		return is64 ? getLong(byteOffset) : getInt(byteOffset);
	}
	
    protected Pointer<T> setSizeT(long byteOffset, long value) {
		if (is64)
			setLong(byteOffset, value);
		else {
			setInt(byteOffset, SizeT.safeIntCast(value));
		}
		return this;
	}

    static Class<?> getPrimitiveType(Buffer buffer) {

        #foreach ($prim in $primitivesNoBool)
		if (buffer instanceof ${prim.BufferName})
			return ${prim.WrapperName}.TYPE;
		#end
        throw new UnsupportedOperationException();
    }
    
    public void setValues(long byteOffset, Buffer values, int valuesOffset, int length) {
        #foreach ($prim in $primitivesNoBool)
        if (values instanceof ${prim.BufferName}) {
            set${prim.CapName}s(byteOffset, (${prim.BufferName})values, valuesOffset, length);
            return;
        }
        #end
        throw new UnsupportedOperationException();
    }


#foreach ($prim in $primitivesNoBool)

	public abstract Pointer<T> set${prim.CapName}(long byteOffset, ${prim.Name} value);
	
	public abstract Pointer<T> set${prim.CapName}s(long byteOffset, ${prim.Name}[] values, int valuesOffset, int length);

	public Pointer<T> set${prim.CapName}s(long byteOffset, ${prim.Name}[] values) {
        return set${prim.CapName}s(byteOffset, values, 0, values.length);
    }

    /**
     * TODO JavaDoc
	 * @deprecated Use @see set${prim.CapName}s(long, ${prim.Name}[], int, int) instead (this method is here only for compatibility with code written for JNA, do not use for new developments)
     */
    @Deprecated
    public Pointer<T> write(long byteOffset, ${prim.Name}[] values, int valuesOffset, int length) {
        return set${prim.CapName}s(byteOffset, values, valuesOffset, length);
    }

	public abstract Pointer<T> set${prim.CapName}s(long byteOffset, ${prim.BufferName} values, long valuesOffset, long length);

	public abstract ${prim.BufferName} get${prim.BufferName}(long byteOffset, long length);
    
    public abstract ${prim.Name} get${prim.CapName}(long byteOffset);

    public abstract ${prim.Name}[] get${prim.CapName}Array(long byteOffset, int length);

#end

    public enum StringType {
        Pascal, C, WideC
    }
	
	protected abstract long strlen(long byteOffset);
	
	public String getString(long byteOffset, Charset charset, StringType type) throws UnsupportedEncodingException {
        long len;
        if (type == StringType.Pascal) {
            len = getByte(byteOffset) & 0xff;
            byteOffset++;
		} else {
            len = strlen(byteOffset);
            if (len >= Integer.MAX_VALUE)
                throw new IllegalArgumentException("No null-terminated string at this address");

            if (type == StringType.WideC)
                throw new UnsupportedOperationException("Wide strings are not supported yet");
        }
		return new String(getByteArray(byteOffset, SizeT.safeIntCast(len)), charset.name());
	}

    public Pointer<T> setString(long byteOffset, String s) {
        try {
            return setString(byteOffset, s, Charset.defaultCharset(), StringType.C);
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("Unexpected error", ex);
        }
    }
    
	public Pointer<T> setString(long byteOffset, String s, Charset charset, StringType type) throws UnsupportedEncodingException {
        if (type == StringType.WideC)
            throw new UnsupportedOperationException("Wide strings are not supported yet");
        
        byte[] bytes = s.getBytes(charset.name());
        int bytesCount = bytes.length;

        if (type == StringType.Pascal) {
            if (bytesCount > 255)
                throw new IllegalArgumentException("Pascal strings cannot be more than 255 chars long (tried to write string of byte length " + bytesCount + ")");
            byteOffset++;
        }
        setBytes(byteOffset, bytes, 0, bytesCount);
        if (type == StringType.C)
            setByte(byteOffset + bytesCount, (byte)0);

        return this;
    }
	
	public String getPascalString(long byteOffset) {
		try {
            return getString(byteOffset, Charset.defaultCharset(), StringType.Pascal);
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("Unexpected error", ex);
        }
	}
	
	public String getWideString(long byteOffset) {
		try {
            return getString(byteOffset, Charset.defaultCharset(), StringType.WideC);
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("Unexpected error", ex);
        }
	}
	
	@Deprecated
    public String getString(long byteOffset, boolean wide) {
        try {
            return getString(byteOffset, Charset.defaultCharset(), wide ? StringType.WideC : StringType.C);
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("Unexpected error", ex);
        }
    }
    
	public String getString(long byteOffset) {
        return getString(byteOffset, false);
    }
        
    public void setPointer(long byteOffset, Pointer value) {
        setSizeT(byteOffset, value.getPeer());
    }

}
