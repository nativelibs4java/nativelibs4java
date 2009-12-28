package com.nativelibs4java.runtime.structs;
import com.nativelibs4java.runtime.structs.StructIO.FieldIO.Refreshable;
import com.sun.jna.*;

public class Struct<S extends Struct<S>> implements Refreshable<S>, NativeMapped {
    
    protected final StructIO<S> io;
	protected volatile Pointer pointer;
	Object[] refreshableFields;

	protected Struct(StructIO<S> io) {
		this.io = io;
		io.build();
		refreshableFields = io.createRefreshableFieldsArray();
	}

    public int size() {
		return io.getStructSize();
	}

    public S clone() {
        try {
            S clone = (S) getClass().newInstance();
            int sz = io.getStructSize();
            clone.getPointer().getByteBuffer(0, sz).put(getPointer().getByteBuffer(0, sz));
            return clone;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to clone struct of type " + getClass().getName(), ex);
        }
    }

    @Override
    public synchronized Pointer getPointer() {
        if (pointer == null)
            pointer = new Memory(io.getStructSize());
        return pointer;
    }

    @Override
    public synchronized S setPointer(Pointer pointer) {
        if (pointer == null || pointer.equals(Pointer.NULL))
            throw new NullPointerException("Cannot set null pointer as struct address !");
        
        this.pointer = pointer;
        return (S)this;
    }

    public void write() {
        io.write((S)this);
    }
    
    public void read() {
        io.read((S)this);
    }

    @Override
    public Object fromNative(Object o, FromNativeContext fnc) {
        read();
        setPointer((Pointer)o);
        return this;
    }

    @Override
    public Object toNative() {
        write();
        return getPointer();
    }

    @Override
    public Class nativeType() {
        return Pointer.class;
    }

}
