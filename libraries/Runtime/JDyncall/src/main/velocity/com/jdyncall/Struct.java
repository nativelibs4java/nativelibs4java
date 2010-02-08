package com.jdyncall;

import java.nio.ByteBuffer;
public abstract class Struct<S extends Struct<S>> implements
	PointerRefreshable
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
            clone.getReference().getByteBuffer(0, sz).put(getReference().getByteBuffer(0, sz));
            return clone;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to clone struct of type " + getClass().getName(), ex);
        }
    }

	public synchronized Pointer<S> getReference() {
        return pointer == null ? pointer = Pointer.allocate(io.getPointerIO(), io.getStructSize()) : pointer;
    }

	public synchronized S setPointer(Pointer<?> pointer) {
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
	
}
