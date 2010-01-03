/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.runtime;

import java.lang.reflect.Type;

/**
 *
 * @author Olivier
 */
public class Memory<T> extends Pointer<T> {
    public Memory(int size) {
        super(null, malloc(size));
    }

    @Override
    protected void finalize() throws Throwable {
        free(peer);
    }

    protected static class SharedPointer<T> extends Pointer<T> {

        public SharedPointer(Type targetType, long peer, Memory memoryReferenceNotToGC) {
            super(targetType, peer);
            this.memoryReferenceNotToGC = memoryReferenceNotToGC;
        }

        protected Memory memoryReferenceNotToGC;

        @Override
        public Pointer<T> share(long offset) {
            return new SharedPointer(type, peer + offset, memoryReferenceNotToGC);
        }
    }

    @Override
    public Pointer<T> share(long offset) {
        return new SharedPointer(type, peer + offset, this);
    }
}
