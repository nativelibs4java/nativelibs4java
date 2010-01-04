package com.nativelibs4java.runtime;

import com.ochafik.lang.SyntaxUtils;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.*;
import java.nio.charset.Charset;

public class Pointer<T> implements Comparable<Pointable>
        //, com.sun.jna.Pointer<Pointer<T>>
{

    static {
        JNI.initLibrary();
    }

    public static final int SIZE = JNI.POINTER_SIZE;

    protected Type type;
    protected long peer;

    Pointer(Type type, long peer) {
        this.type = type;
        this.peer = peer;
    }

    public Pointer<T> share(long offset) {
        return new Pointer(type, peer + offset);
    }

    public long getPeer() {
        return peer;
    }
    public void setPeer(long address) {
        if (peer == address)
            return;
        
        peer = address;
        if (!(cachedTarget instanceof Pointable))
            cachedTarget = null;
    }

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

    public static Pointer<?> wrapAddress(long address) {
        return new Pointer(null, address);
    }

    public static <P> Pointer<P> wrapAddress(long address, Class<P> targetClass) {
        return new Pointer(targetClass, address);
    }

    public static Pointer<?> allocateArray(int size) {
        return new Memory(size);
    }

    public static <V> Pointer<V> allocateArray(Class<V> elementClass, int size) {
        #foreach ($prim in $primitivesNoBool)
        if (elementClass == ${prim.WrapperName}.TYPE || elementClass == ${prim.WrapperName}.class)
            return allocateArray(${prim.Size}).setTargetClass(elementClass);
        #end
        throw new UnsupportedOperationException("Cannot allocate memory for type " + elementClass.getName());
    }

    public static <V> Pointer<V> allocate(Class<V> elementClass) {
        #foreach ($prim in $primitivesNoBool)
        if (elementClass == ${prim.WrapperName}.TYPE || elementClass == ${prim.WrapperName}.class)
            return (Pointer<V>)new Memory<${prim.WrapperName}>(${prim.WrapperName}.class, ${prim.Size});
        #end
        throw new UnsupportedOperationException("Cannot allocate memory for type " + elementClass.getName());
    }

#foreach ($prim in $primitivesNoBool)
    public static Pointer<${prim.WrapperName}> pointerTo(${prim.Name}... values) {
        return new Memory<${prim.WrapperName}>(${prim.WrapperName}.class, ${prim.Size} * values.length).write(0, values, 0, values.length);
    }
#end

    public Type getTargetType() {
        return type;
    }
    public void setTargetType(Type type) {
        if (SyntaxUtils.equal(this.type, type))
            return;

        this.type = type;
        this.cachedTarget = null;
    }
    public <N> Pointer<N> setTargetClass(Class<N> type) {
        setTargetType(type);
        return (Pointer<N>)this;
    }
    
    public Class<T> getTargetClass() {
        if (type instanceof Class<?>)
            return (Class<T>)type;
        else if (type instanceof ParameterizedType)
            return (Class<T>)((ParameterizedType)type).getRawType();
        else
            throw new UnsupportedOperationException("Can't extract class from type " + type.toString());
    }
    T cachedTarget;
    public T getTarget() {
        if (type == null)
            throw new RuntimeException("Pointer is not typed, cannot call getTarget() on it.");
        
        if (cachedTarget == null) {
            Class<T> c;
            ParameterizedType pt = null;
            if (type instanceof Class<?>)
                c = (Class<T>)type;
            else {
                pt = (ParameterizedType)type;
                c = (Class<T>)pt.getRawType();
            }
            try {
                cachedTarget = c.newInstance();
                if (cachedTarget instanceof Pointer) {
                    Pointer ptr = (Pointer)cachedTarget;
                    ptr.setTargetType(pt.getActualTypeArguments()[0]);
                    ptr.setPeer(getPointerAddress(0));
                    return cachedTarget;
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        if (cachedTarget instanceof Pointer) {
            Pointer ptr = (Pointer)cachedTarget;
            ptr.setPeer(getPointerAddress(0));
        } else if (cachedTarget instanceof PointerRefreshable) {
            ((PointerRefreshable)cachedTarget).setPointer(getPointer(0));
        }

        return cachedTarget;
    }
    
    //protected native long 	getWChar_(long offset);
    //protected native long 	getChar_(long offset);
    protected native long getPointerAddress(long byteOffset);

    protected native Pointer<T> setPointerAddress(long byteOffset, long value);

    public Pointer<?> getPointer(long byteOffset) {
        return new Pointer(null, getPointerAddress(byteOffset));
    }

    public <U> Pointer<U> getPointer(Type t, long offset) {
        return new Pointer<U>(t, getPointerAddress(offset));
    }
    public <U> Pointer<U> getPointer(Class<U> t, long offset) {
        return new Pointer<U>(t, getPointerAddress(offset));
    }

    public static native long getDirectBufferAddress(Buffer b);
    public static native long getDirectBufferCapacity(Buffer b);

	static Class<?> getPrimitiveType(Buffer buffer) {

        #foreach ($prim in $primitivesNoBool)
		if (buffer instanceof ${prim.BufferName})
			return ${prim.WrapperName}.TYPE;
		#end
        throw new UnsupportedOperationException();
    }
    public static Pointer<?> getDirectBufferPointer(Buffer b) {
        return new Pointer(getPrimitiveType(b), getDirectBufferAddress(b));
    }

    public void write(long byteOffset, Buffer values, int valuesOffset, int length) {
        #foreach ($prim in $primitivesNoBool)
        if (values instanceof ${prim.BufferName})
            write(byteOffset, values, valuesOffset, length);
        #end
        throw new UnsupportedOperationException();
    }

#foreach ($prim in $primitivesNoBool)

    public static Pointer<${prim.WrapperName}> getDirectBufferPointer(${prim.BufferName} b) {
        return new Pointer<${prim.WrapperName}>(${prim.WrapperName}.class, getDirectBufferAddress(b));
    }

    public native ${prim.Name} get${prim.CapName}(long byteOffset);

    public native Pointer<T> set${prim.CapName}(long byteOffset, ${prim.Name} value);
    public Pointer<T> write(long byteOffset, ${prim.Name} value) {
        return set${prim.CapName}(byteOffset, value);
    }

    public native Pointer<T> write(long byteOffset, ${prim.Name}[] values, int valuesOffset, int length);
    
    public Pointer<T> write(long byteOffset, ${prim.BufferName} values, int valuesOffset, int length) {
        if (values.isDirect()) {
            memcpy(peer + byteOffset, getDirectBufferAddress(values) + valuesOffset * ${prim.Size}, length * ${prim.Size});
        } else if (values.isReadOnly()) {
            get${prim.BufferName}(byteOffset, length).put(values.duplicate());
        } else {
            write(byteOffset, values.array(), values.arrayOffset() + valuesOffset, length);
        }
        return this;
    }

    public native ${prim.BufferName} get${prim.BufferName}(long byteOffset, long length);
    public native ${prim.Name}[] get${prim.CapName}Array(long byteOffset, int length);

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
        setPointerAddress(byteOffset, value.peer);
    }

    protected static native long malloc(int size);
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
