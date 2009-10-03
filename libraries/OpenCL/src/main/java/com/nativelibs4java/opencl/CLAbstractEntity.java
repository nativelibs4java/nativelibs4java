/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl;
import com.nativelibs4java.opencl.library.OpenCLLibrary;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;
import com.sun.jna.*;
import com.sun.jna.ptr.*;
import java.nio.*;
import static com.nativelibs4java.opencl.OpenCL4Java.*;

abstract class CLAbstractEntity<T extends PointerType> {

    private T entity;

    CLAbstractEntity(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Null OpenCL " + getClass().getSimpleName() + " !");
        }
        this.entity = entity;
    }

    T get() {
        return entity;
    }

    @Override
    protected void finalize() throws Throwable {
        clear();
        entity = null;
    }

    protected abstract void clear();

	/**
	 * Underyling implementation pointer-based hashCode computation
	 */
	@Override
	public int hashCode() {
		return get().getPointer().hashCode();
	}

	/**
	 * Underyling implementation pointer-based equality test
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !getClass().isInstance(obj))
			return false;
		CLAbstractEntity e = (CLAbstractEntity)obj;
		return get().getPointer().equals(e.get().getPointer());
	}

}