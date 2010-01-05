package com.nativelibs4java.runtime;

import com.ochafik.lang.SyntaxUtils;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.*;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public class Pointer<T> implements Comparable<Pointable>
        //, com.sun.jna.Pointer<Pointer<T>>
{
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
    protected long peer;
    protected byte endianness = DEFAULT_ENDIANNESS;

    public Pointer(PointerIO<T> io, long peer) {
        this(peer);
        this.io = io;
    }
    public Pointer(long peer) {
        this.peer = peer;
    }

    public void setByteOrder(ByteOrder order) {
        endianness = getEndianness(order);
    }
    public final ByteOrder getByteOrder() {
        switch (endianness) {
            case BIG_ENDIAN:
                return ByteOrder.BIG_ENDIAN;
            case LITTLE_ENDIAN:
                return ByteOrder.LITTLE_ENDIAN;
            default:
                return ByteOrder.nativeOrder();
        }
    }

    public static void update(UpdatablePointer<?> pointer) {
        if (pointer == null)
            return;
        pointer.update();
    }
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


    public T get() {
        return get(0);
    }
    public T get(int index) {
        if (io == null)
            throw new RuntimeException("Cannot get pointed value without a properly defined targetType");

        return io.get(this, index);
    }
    
    public void set(T value) {
        set(0, value);
    }
    public Pointer<T> set(int index, T value) {
        if (io == null)
            throw new RuntimeException("Cannot set pointed value without a properly defined targetType");

        io.set(this, index, value);
        return this;
    }
    public static long getPeer(Pointer<?> pointer) {
        return pointer == null ? 0 : pointer.peer;
    }
    
    protected void checkValidOffset(long offset, long length) {
        // Do nothing : this is meant to be overridden by subclasses
    }

    public Pointer<T> share(long offset) {
        return new Pointer(io, peer + offset);
    }

    public long getPeer() {
        return peer;
    }
    public void setPeer(long address) {
        if (peer == address)
            return;

        if (peer == 0)
            throw new IllegalArgumentException("Cannot set null peer to a pointer : use null pointer instead");

        peer = address;
    }

    public void release() {}

    public int compareTo(Pointable o) {
		Pointer po;
        if (o == null || (po = o.getPointer()) == null)
            return peer == 0 ? 0 : 1;
        long d = peer - po.peer;
        return d < 0 ? -1 : d == 0 ? 0 : 1;
    }

    public static final Pointer NULL = null;

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return peer == 0;
        if (!(obj instanceof Pointable))
            return false;
        return peer == ((Pointable)obj).getPointer().peer;
    }

    @Override
    public int hashCode() {
        return new Long(peer).hashCode();
    }

    public static Pointer<?> fromAddress(long address) {
        return new Pointer(null, address);
    }

    public interface Deallocator {
        void deallocate(long peer);
    }
    public static Pointer<?> fromAddress(long address, final Deallocator deallocator) {
        return new Memory(null, address, -1) {
            @Override
            protected void deallocate() {
                if (deallocator != null)
                    deallocator.deallocate(peer);
            }
        };
    }

    public static Pointer<?> fromBuffer(Buffer buffer) {
        long address = JNI.getDirectBufferAddress(buffer);
        if (address == 0)
            return null;

        long size = JNI.getDirectBufferCapacity(buffer);

        class BufferMemory<T> extends Memory<T> {
            public Buffer buffer;
            public BufferMemory(PointerIO<T> io, long address, long capacity, Buffer buffer) {
                super(io, address, capacity);
                this.buffer = buffer;
            }
            @Override
            public void deallocate() {
                BufferMemory.this.buffer = null;
            }
        }
        return new BufferMemory(PointerIO.getBufferPrimitiveInstance(buffer), address, size, buffer);
    }

    public static <P> Pointer<P> fromAddress(long address, Class<P> targetClass) {
        return new Pointer(PointerIO.getInstance(targetClass), address);
    }

    public static Memory<?> allocateMemory(int size) {
        return new Memory(size);
    }

    public static <V> Memory<V> allocate(Class<V> elementClass) {
        return allocateArray(elementClass, 1);
    }
    
    public static <V> Memory<V> allocateArray(Class<V> elementClass, int arrayLength) {
        #foreach ($prim in $primitivesNoBool)
        if (elementClass == ${prim.WrapperName}.TYPE || elementClass == ${prim.WrapperName}.class)
            return (Memory<V>)new Memory<${prim.WrapperName}>(PointerIO.get${prim.CapName}Instance(), ${prim.Size} * arrayLength);
        #end
        throw new UnsupportedOperationException("Cannot allocate memory for type " + elementClass.getName());
    }

#foreach ($prim in $primitivesNoBool)
    public static Memory<${prim.WrapperName}> pointerTo(${prim.Name}... values) {
        Memory<${prim.WrapperName}> mem = new Memory<${prim.WrapperName}>(PointerIO.get${prim.CapName}Instance(), ${prim.Size} * values.length);
        mem.set${prim.CapName}s(0, values, 0, values.length);
        return mem;
    }
    public static Memory<${prim.WrapperName}> allocate${prim.CapName}() {
        return new Memory<${prim.WrapperName}>(PointerIO.get${prim.CapName}Instance(), ${prim.Size});
    }
    public static Memory<${prim.WrapperName}> allocate${prim.CapName}Array(int arrayLength) {
        return new Memory<${prim.WrapperName}>(PointerIO.get${prim.CapName}Instance(), ${prim.Size} * arrayLength);
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

    

    //protected native long 	getWChar_(long offset);
    //protected native long 	getChar_(long offset);
    protected native long getSizeT(long byteOffset);
    protected native Pointer<T> setSizeT(long byteOffset, long value);

    public Pointer<?> getPointer(long byteOffset) {
        return new Pointer(null, getSizeT(byteOffset));
    }

    public <U> Pointer<U> getPointer(long offset, Type t) {
        return new Pointer(PointerIO.getInstanceByType(t), getSizeT(offset));
    }
    public <U> Pointer<U> getPointer(long offset, Class<U> t) {
        return new Pointer<U>(PointerIO.getInstance(t), getSizeT(offset));
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

    protected static native long get${prim.WrapperName}ArrayElements(${prim.Name}[] array, long pIsCopy);
    protected static native void release${prim.WrapperName}ArrayElements(${prim.Name}[] array, long pointer, int mode);

    protected static native ${prim.Name} get_${prim.Name}(long peer, byte endianness);
    protected static native void set_${prim.Name}(long peer, ${prim.Name} value, byte endianness);

    protected static native ${prim.Name}[] get_${prim.Name}_array(long peer, int length, byte endianness);
    protected static native void set_${prim.Name}_array(long peer, ${prim.Name}[] values, int valuesOffset, int length, byte endianness);

    public Pointer<T> set${prim.CapName}(long byteOffset, ${prim.Name} value) {
        checkValidOffset(byteOffset, ${prim.Size});
        set_${prim.Name}(peer + byteOffset, value, endianness);
        return this;
    }

    public Pointer<T> set${prim.CapName}s(long byteOffset, ${prim.Name}[] values, int valuesOffset, int length) {
        checkValidOffset(byteOffset, ${prim.Size} * length);
        set_${prim.Name}_array(peer + byteOffset, values, valuesOffset, length, endianness);
        return this;
    }

    public Pointer<T> set${prim.CapName}s(long byteOffset, ${prim.Name}[] values) {
        return set${prim.CapName}s(byteOffset, values, 0, values.length);
    }

    /**
     * @deprecated Use @see set${prim.CapName}s(long, ${prim.Name}[], int, int) instead (this method is here only for compatibility with code written for JNA, do not use for new developments)
     */
    @Deprecated
    public Pointer<T> write(long byteOffset, ${prim.Name}[] values, int valuesOffset, int length) {
        return set${prim.CapName}s(byteOffset, values, valuesOffset, length);
    }

    public Pointer<T> set${prim.CapName}s(long byteOffset, ${prim.BufferName} values, long valuesOffset, long length) {
        checkValidOffset(byteOffset, ${prim.Size} * length);
        if (values.isDirect()) {
            long len = length * ${prim.Size}, off = valuesOffset * ${prim.Size};
            long cap = JNI.getDirectBufferCapacity(values);
            if (cap < off + len)
                throw new IndexOutOfBoundsException("The provided buffer has a capacity (" + cap + " bytes) smaller than the requested write operation (" + len + " bytes starting at byte offset " + off + ")");
            memcpy(peer + byteOffset, JNI.getDirectBufferAddress(values) + off, len);
        } else if (values.isReadOnly()) {
            get${prim.BufferName}(byteOffset, length).put(values.duplicate());
        } else {
            set${prim.CapName}s(byteOffset, values.array(), (int)(values.arrayOffset() + valuesOffset), (int)length);
        }
        return this;
    }

    public ${prim.BufferName} get${prim.BufferName}(long byteOffset, long length) {
        checkValidOffset(byteOffset, ${prim.Size} * length);
        ByteBuffer buffer = JNI.newDirectByteBuffer(peer + byteOffset, length);
        buffer.order(getByteOrder());
        #if ($prim.Name == "byte")
        return buffer;
        #else
        return buffer.as${prim.BufferName}();
        #end
    }
    
    public ${prim.Name} get${prim.CapName}(long byteOffset) {
        checkValidOffset(byteOffset, ${prim.Size});
        return get_${prim.Name}(peer + byteOffset, endianness);
    }

    public ${prim.Name}[] get${prim.CapName}Array(long byteOffset, int length) {
        checkValidOffset(byteOffset, ${prim.Size} * length);
        return get_${prim.Name}_array(peer + byteOffset, length, endianness);
    }

#end

    public enum StringType {
        Pascal, C, WideC
    }
	public String getString(long byteOffset, Charset charset, StringType type) throws java.io.UnsupportedEncodingException {
        long len;
        if (type == StringType.Pascal) {
            len = getByte(byteOffset) & 0xff;
            byteOffset++;
        } else {
            len = strlen(peer + byteOffset);
            if (len >= Integer.MAX_VALUE)
                throw new IllegalArgumentException("No null-terminated string at this address");

            if (type == StringType.WideC)
                throw new UnsupportedOperationException("Wide strings are not supported yet");
        }
		return new String(getByteArray(byteOffset, (int)len), charset.name());
	}

    public Pointer<T> setString(long byteOffset, String s) {
        try {
            return setString(byteOffset, s, Charset.defaultCharset(), StringType.C);
        } catch (java.io.UnsupportedEncodingException ex) {
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
        } catch (java.io.UnsupportedEncodingException ex) {
            throw new RuntimeException("Unexpected error", ex);
        }
	}
	public String getWideString(long byteOffset) {
		try {
            return getString(byteOffset, Charset.defaultCharset(), StringType.WideC);
        } catch (java.io.UnsupportedEncodingException ex) {
            throw new RuntimeException("Unexpected error", ex);
        }
	}
	
	@Deprecated
    public String getString(long byteOffset, boolean wide) {
        try {
            return getString(byteOffset, Charset.defaultCharset(), wide ? StringType.WideC : StringType.C);
        } catch (java.io.UnsupportedEncodingException ex) {
            throw new RuntimeException("Unexpected error", ex);
        }
    }
    public String getString(long byteOffset) {
        return getString(byteOffset, false);
    }
        
    public void setPointer(long byteOffset, Pointer value) {
        setSizeT(byteOffset, value.peer);
    }

    protected static native long malloc(long size);
    protected static native void free(long pointer);

    protected static native long strlen(long pointer);
    protected static native long wcslen(long pointer);

    protected static native void memcpy(long dest, long source, long size);
    protected static native void wmemcpy(long dest, long source, long size);
    protected static native void memmove(long dest, long source, long size);
    protected static native void wmemmove(long dest, long source, long size);
    protected static native long memchr(long ptr, byte value, long num);
    protected static native int memcmp(long ptr1, long ptr2, long num);
    protected static native void memset(long ptr, byte value, long num);
}
