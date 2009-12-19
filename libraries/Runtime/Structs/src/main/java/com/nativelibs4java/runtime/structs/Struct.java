package com.nativelibs4java.runtime.structs;
import com.nativelibs4java.runtime.structs.StructIO.FieldIO.Refreshable;
import com.sun.jna.*;
import java.nio.*;
public class Struct<S extends Struct<S>> implements Refreshable {
    
    private final StructIO<S> io;
	Pointer pointer;
	Refreshable[] refreshableFields;

	public Struct(StructIO<S> io) {
		this.io = io;
		io.build();
		refreshableFields = io.createRefreshableFieldsArray();
	}

    public int size() {
		return io.getStructSize();
	}

    @Override
    public synchronized Pointer getPointer() {
        return pointer;
    }

    public synchronized void setPointer(Pointer pointer) {
        if (pointer == null || pointer.equals(Pointer.NULL))
            throw new NullPointerException("Cannot set null pointer as struct address !");
        
        this.pointer = pointer;
    }

    @Override
    public void setPointer(Pointer p, long offset) {
        setPointer(p.share(offset));
    }

}
