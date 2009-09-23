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

abstract class CLEntity<T extends PointerType> {

    private T entity;

    public CLEntity(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Null OpenCL " + getClass().getSimpleName() + " !");
        }
        this.entity = entity;
    }

    public T get() {
        return entity;
    }

    public Pointer getPointer() {
        return entity.getPointer();
    }

    @Override
    protected void finalize() throws Throwable {
        clear();
        entity = null;
    }

    protected abstract void clear();
}