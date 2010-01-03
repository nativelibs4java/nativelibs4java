package com.nativelibs4java.runtime;

#set ($prims = [ "int", "long", "short", "byte", "float", "double", "char" ])
#set ($primCaps = [ "Int", "Long", "Short", "Byte", "Float", "Double", "Char" ])
#set ($primBufs = [ "IntBuffer", "LongBuffer", "ShortBuffer", "ByteBuffer", "FloatBuffer", "DoubleBuffer", "CharBuffer" ])
#set ($primWraps = [ "Integer", "Long", "Short", "Byte", "Float", "Double", "Character" ])

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.*;
import java.nio.charset.Charset;

public class Pointer<T> implements Addressable, Comparable<Addressable>
        //, com.sun.jna.Pointer<Pointer<T>>
{

    static {
        JNI.initLibrary();
    }

    public static final int SIZE = JNI.POINTER_SIZE;

    protected Type type;
    protected long peer;

    public Pointer(Type type, long peer) {
        this.type = type;
        this.peer = peer;
    }

    public Pointer<T> share(long offset) {
        return new Pointer(type, peer + offset);
    }

    @Override
    public long getAddress() {
        return peer;
    }
    @Override
    public void setAddress(long address) {
        peer = address;
        cachedTarget = null;
    }

    public int compareTo(Addressable o) {
        if (o == null)
            return peer == 0 ? 0 : 1;
        long d = peer - o.getAddress();
        return d < 0 ? -1 : d == 0 ? 0 : 1;
    }

    public static final Pointer NULL = null;

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return peer == 0;
        if (!(obj instanceof Addressable))
            return false;
        return peer == ((Addressable)obj).getAddress();
    }

    @Override
    public int hashCode() {
        return new Long(peer).hashCode();
    }

    public static Pointer<?> allocate(int size) {
        return new Memory(size);
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
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            if (Pointer.class.isAssignableFrom(c)) {
                cachedTarget = (T)getPointer(pt.getActualTypeArguments()[0], 0);
            } else if (Addressable.class.isAssignableFrom(c))
                ((Addressable)cachedTarget).setAddress(getPointerAddress(0));

        }
        return cachedTarget;
    }
    
    //protected native long 	getWChar_(long offset);
    //protected native long 	getChar_(long offset);
    protected native long getPointerAddress(long offset);

    protected native void setPointerAddress(long offset, long value);

    public Pointer<?> getPointer(long offset) {
        return new Pointer(null, getPointerAddress(offset));
    }

    public <U> Pointer<U> getPointer(Type t, long offset) {
        return new Pointer<U>(t, getPointerAddress(offset));
    }
    public <U> Pointer<U> getPointer(Class<U> t, long offset) {
        return new Pointer<U>(t, getPointerAddress(offset));
    }

    public static native long getDirectBufferAddress(Buffer b);
    public static native long getDirectBufferCapacity(Buffer b);

#foreach ($prim in $prims)

    #set ($i = $velocityCount - 1)
    #set ($primCap = $primCaps.get($i))
    #set ($primWrap = $primWraps.get($i))
    #set ($primBuf = $primBufs.get($i))

    public static Pointer<${primWrap}> getDirectBufferPointer(${primBuf} b) {
        return new Pointer<${primWrap}>(${primWrap}.class, getDirectBufferAddress(b));
    }

    public native $prim get$primCap(long offset);
    public native void set$primCap(long offset, $prim value);

    public native void write(long offset, $prim[] values, int valuesOffset, int length);

    public native $primBuf get$primBuf(long offset, long length);
    public native $prim[] get${primCap}Array(long offset, int length);

#end

    public native String getString(long offset, Charset charset, boolean wide);

    public String getString(long offset, boolean wide) {
        return getString(offset, Charset.defaultCharset(), wide);
    }
    public String getString(long offset) {
        return getString(offset, Charset.defaultCharset(), false);
    }
        
    public void setPointer(long offset, Pointer value) {
        setPointerAddress(offset, value.peer);
    }

    protected static native long malloc(int size);
    protected static native void free(long pointer);

    protected static native long strlen(long pointer);
    //protected static native long wstrlen(long pointer);

    protected static native void memcpy(long dest, long source, long size);
    protected static native void wmemcpy(long dest, long source, long size);
    protected static native void memmove(long dest, long source, long size);
    protected static native void wmemmove(long dest, long source, long size);
    protected static native long memchr(long ptr, byte value, long num);
    protected static native int memcmp(long ptr1, long ptr2, long num);
    protected static native void memset(long ptr, byte value, long num);
}
