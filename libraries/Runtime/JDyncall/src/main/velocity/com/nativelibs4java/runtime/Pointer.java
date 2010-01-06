package com.nativelibs4java.runtime;

import com.ochafik.lang.SyntaxUtils;
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

    static {
        JNI.initLibrary();
    }

    static byte getEndianness(ByteOrder order) {
        return order.equals(ByteOrder.BIG_ENDIAN) ? BIG_ENDIAN : LITTLE_ENDIAN;
    }
    
    protected PointerIO<T> io;
    protected byte endianness = DEFAULT_ENDIANNESS;

    /**
	 * TODO JavaDoc
	 */
    public void order(ByteOrder order) {
        endianness = getEndianness(order);
    }
    
	/**
	 * TODO JavaDoc
	 */
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
	 * TODO JavaDoc
	 */
    public static void update(UpdatablePointer<?> pointer) {
        if (pointer == null)
            return;
        pointer.update();
    }
    
	/**
	 * TODO JavaDoc
	 */
    public static UpdatablePointer<String> updatablePointerTo(final String[] strings) {
        final int len = strings.length;
        final Memory<String> mem = allocateArray(String.class, len);
        for (int i = 0; i < len; i++)
            mem.set(i, strings[i]);

        return new UpdatablePointer<String>(PointerIO.getStringInstance(), mem) {
            
            @Override
            public void update() {
                for (int i = 0; i < len; i++) {
                    strings[i] = mem.get(i);
                }
            }
        };
    }

    /**
	 * TODO JavaDoc
	 */
    public T get() {
        return get(0);
    }
    
	/**
	 * TODO JavaDoc
	 */
    public T get(int index) {
        if (io == null)
            throw new RuntimeException("Cannot get pointed value without a properly defined targetType");

        return io.get(this, index);
    }
    
    /**
	 * TODO JavaDoc
	 */
    public void set(T value) {
        set(0, value);
    }
    
	/**
	 * TODO JavaDoc
	 */
    public Pointer<T> set(int index, T value) {
        if (io == null)
            throw new RuntimeException("Cannot set pointed value without a properly defined targetType");

        io.set(this, index, value);
        return this;
    }
    
	/**
	 * TODO JavaDoc
	 */
    public static long getOrAllocateTempPeer(Pointer<?> pointer) {
        return pointer == null ? 0 : pointer.getOrAllocateTempPeer();
    }
	
	/**
	 * TODO JavaDoc
	 */
    public static long getPeer(Pointer<?> pointer) {
        return pointer == null ? 0 : pointer.getPeer();
    }
	
	/**
	 * TODO JavaDoc
	 */
    public static void deleteTempPeer(Pointer<?> pointer, long tempPeer) {
		if (pointer == null || tempPeer == 0)
			return;
		pointer.deleteTempPeer(tempPeer);
    }
	
    
    /**
	 * TODO JavaDoc
	 */
    protected void checkValidOffset(long offset, long length) {
        // Do nothing : this is meant to be overridden by subclasses
    }

	/**
	 * TODO JavaDoc
	 */
    public abstract Pointer<T> share(long offset);

	/**
	 * TODO JavaDoc
	 * Returns the long value of the address this Pointer instance points to.
	 * @throws UnsupportedOperationException if the pointer does not have a peer (@see hasPeer())
	 */
    public abstract long getPeer();
    
	/**
	 * TODO JavaDoc
	 */
    public abstract boolean hasPeer();
	
	/**
	 * TODO JavaDoc
	 */
    public abstract long getOrAllocateTempPeer();
    
	/**
	 * TODO JavaDoc
	 */
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
		boolean hp = hasPeer();
		if (hp != p.hasPeer())
			return false;
		
		if (hp)
			return getPeer() == p.getPeer();
		else
			return identityEqual(obj);
	}
	
	/**
	 * Used in case of pointers that don't have peers to compare.
	 * This can be overridden to provide an identity equality that takes underlying data into account.
	 */
	protected boolean identityEqual(Pointer p) {
		return super.equals(p);	 
	}

    /**
	 * TODO JavaDoc
	 */
	@Deprecated
    public static Pointer<?> fromAddress(long address) {
        return address == 0 ? null : new DefaultPointer(null, address);
    }

    /**
	 * TODO JavaDoc
	 */
	@Deprecated
    public static Pointer<?> fromAddress(long address, long size) {
        fromAddress(address, size, null);
    }

    /**
	 * TODO JavaDoc
	 */
    public interface Deallocator {
        void deallocate(long peer);
    }

    /**
	 * TODO JavaDoc
	 */
    @Deprecated
    public static Pointer<?> fromAddress(long address, final Deallocator deallocator) {
		return fromAddress(address, -1, deallocator);
	}
    
	/**
	 * TODO JavaDoc
	 */
    public static Pointer<?> fromAddress(long address, long size, final Deallocator deallocator) {
        return address == 0 ? null : new Memory(null, address, -1) {
            @Override
            protected void deallocate() {
                if (deallocator != null)
                    deallocator.deallocate(peer);
            }
        };
    }

    /**
	 * TODO JavaDoc
	 */
    public static Pointer<?> fromBuffer(Buffer buffer) {
		if (buffer)
			return null;
		
        long address = JNI.getDirectBufferAddress(buffer);
        if (address == 0)
            return null;

        long size = JNI.getDirectBufferCapacity(buffer);

        class BufferMemory<T> extends Memory<T> {
            public Buffer buffer;
            public BufferMemory(PointerIO<T> io, long address, long capacity, Buffer buffer) {
                super(io, address, capacity);
                this.buffer = buffer;
				
				#foreach ($prim in $primitivesNoBool)
				if (buffer instanceof ${prim.BufferName})
				#if (${prim.Name} == "byte")
					return;
				#else
					order(((${prim.BufferName})buffer).order());
				#end
				#end
            }
            @Override
            public void deallocate() {
                BufferMemory.this.buffer = null;
            }
        }
        return new BufferMemory(PointerIO.getBufferPrimitiveInstance(buffer), address, size, buffer);
    }

    public static <P> Pointer<P> fromAddress(long address, Class<P> targetClass) {
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
            return (Memory<V>)allocate(PointerIO.get${prim.CapName}Instance(), ${prim.Size} * arrayLength);
        #end
        throw new UnsupportedOperationException("Cannot allocate memory for type " + elementClass.getName());
    }

#foreach ($prim in $primitivesNoBool)
    public static Memory<${prim.WrapperName}> pointerTo(${prim.Name}... values) {
        Memory<${prim.WrapperName}> mem = allocate(PointerIO.get${prim.CapName}Instance(), ${prim.Size} * values.length);
        mem.set${prim.CapName}s(0, values, 0, values.length);
        return mem;
    }
    public static Memory<${prim.WrapperName}> allocate${prim.CapName}() {
        return allocate(PointerIO.get${prim.CapName}Instance(), ${prim.Size});
    }
    public static Memory<${prim.WrapperName}> allocate${prim.CapName}Array(int arrayLength) {
        return allocate(PointerIO.get${prim.CapName}Instance(), ${prim.Size} * arrayLength);
    }
#end

    public Type getTargetType() {
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
			setInt(byteOffset);
		}
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

	/**
	 * TODO JavaDoc
	 */
    public abstract Pointer<T> set${prim.CapName}(long byteOffset, ${prim.Name} value);
	
	/**
	 * TODO JavaDoc
	 */
    public abstract Pointer<T> set${prim.CapName}s(long byteOffset, ${prim.Name}[] values, int valuesOffset, int length);

	/**
	 * TODO JavaDoc
	 */
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

	/**
	 * TODO JavaDoc
	 */
    public abstract Pointer<T> set${prim.CapName}s(long byteOffset, ${prim.BufferName} values, long valuesOffset, long length);

	/**
	 * TODO JavaDoc
	 */
    public abstract ${prim.BufferName} get${prim.BufferName}(long byteOffset, long length);
    
    /**
	 * TODO JavaDoc
	 */
    public abstract ${prim.Name} get${prim.CapName}(long byteOffset);

    /**
	 * TODO JavaDoc
	 */
    public abstract ${prim.Name}[] get${prim.CapName}Array(long byteOffset, int length);

#end

    public enum StringType {
        Pascal, C, WideC
    }
	
	/**
	 * TODO JavaDoc
	 */
    protected abstract long strlen(long byteOffset);
	
	/**
	 * TODO JavaDoc
	 */
    public String getString(long byteOffset, Charset charset, StringType type) throws java.io.UnsupportedEncodingException {
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
		return new String(getByteArray(byteOffset, (int)len), charset.name());
	}

    /**
	 * TODO JavaDoc
	 */
    public Pointer<T> setString(long byteOffset, String s) {
        try {
            return setString(byteOffset, s, Charset.defaultCharset(), StringType.C);
        } catch (java.io.UnsupportedEncodingException ex) {
            throw new RuntimeException("Unexpected error", ex);
        }
    }
    
	/**
	 * TODO JavaDoc
	 */
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
	
	/**
	 * TODO JavaDoc
	 */
    public String getPascalString(long byteOffset) {
		try {
            return getString(byteOffset, Charset.defaultCharset(), StringType.Pascal);
        } catch (java.io.UnsupportedEncodingException ex) {
            throw new RuntimeException("Unexpected error", ex);
        }
	}
	
	/**
	 * TODO JavaDoc
	 */
    public String getWideString(long byteOffset) {
		try {
            return getString(byteOffset, Charset.defaultCharset(), StringType.WideC);
        } catch (java.io.UnsupportedEncodingException ex) {
            throw new RuntimeException("Unexpected error", ex);
        }
	}
	
	/**
	 * TODO JavaDoc
	 */
    @Deprecated
    public String getString(long byteOffset, boolean wide) {
        try {
            return getString(byteOffset, Charset.defaultCharset(), wide ? StringType.WideC : StringType.C);
        } catch (java.io.UnsupportedEncodingException ex) {
            throw new RuntimeException("Unexpected error", ex);
        }
    }
    
	/**
	 * TODO JavaDoc
	 */
    public String getString(long byteOffset) {
        return getString(byteOffset, false);
    }
        
    /**
	 * TODO JavaDoc
	 */
    public void setPointer(long byteOffset, Pointer value) {
        setSizeT(byteOffset, value.getPeer());
    }

}
