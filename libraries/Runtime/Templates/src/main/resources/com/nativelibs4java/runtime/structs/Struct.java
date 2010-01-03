package com.nativelibs4java.runtime.structs;
import com.nativelibs4java.runtime.structs.StructIO.FieldIO.Refreshable;
import java.nio.ByteBuffer;

import ${memoryClass};
import ${pointerClass};

public abstract class Struct<S extends Struct<S>> implements Refreshable<S>
	#if ($useJNA) , com.sun.jna.NativeMapped #end
{
    
    protected final StructIO<S> io;
	protected volatile Pointer pointer;
	Object[] refreshableFields;

    protected Struct() {
        this.io = StructIO.getInstance(getClass());
		io.build();
		refreshableFields = io.createRefreshableFieldsArray();
    }
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
	
#if ($useJNA)

    @Override
    public Object fromNative(Object o, com.sun.jna.FromNativeContext fnc) {
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

#end
}
