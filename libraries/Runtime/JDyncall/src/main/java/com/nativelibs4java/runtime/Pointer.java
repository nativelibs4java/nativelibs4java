package com.nativelibs4java.runtime;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Pointer<T> implements Addressable, Comparable<Addressable> {

    static {
        JNI.initLibrary();
    }
    protected Type type;
    protected long peer;

    public Pointer(Type type, long peer) {
        this.type = type;
        this.peer = peer;
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
        long ptr = doAllocate(size);
        return ptr == 0 ? null : new Pointer(null, ptr) {

            public void finalize() {
                doFree(peer);
            }
        };
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
    public native int getInt(long offset);

    public native short getShort(long offset);

    public native long getLong(long offset);

    public native byte getByte(long offset);

    public native double getDouble(long offset);

    public native float getFloat(long offset);

    //protected native long 	getWChar_(long offset);
    //protected native long 	getChar_(long offset);
    protected native long getPointerAddress(long offset);

    protected native void setPointerAddress(long offset, long value);

    public <U> Pointer<U> getPointer(Type t, long offset) {
        return new Pointer<U>(t, getPointerAddress(offset));
    }
    public <U> Pointer<U> getPointer(Class<U> t, long offset) {
        return new Pointer<U>(t, getPointerAddress(offset));
    }

    public void setPointer(long offset, Pointer value) {
        setPointerAddress(offset, value.peer);
    }

    public native void setInt(long offset, int value);

    public native void setShort(long offset, short value);

    public native void setLong(long offset, long value);

    public native void setByte(long offset, int value);

    public native void setDouble(long offset, double value);

    public native void setFloat(long offset, float value);

    protected static native long doAllocate(int size);

    protected static native void doFree(long pointer);
}
