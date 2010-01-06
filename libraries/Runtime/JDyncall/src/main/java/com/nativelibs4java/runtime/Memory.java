package com.nativelibs4java.runtime;

import java.lang.reflect.Type;

/**
 *
 * @author Olivier
 */
public class Memory<T> extends DefaultPointer<T> {
    final long size;
    Memory(PointerIO<T> io, long peer, long size) {//, Deallocator deallocator) {
        super(io, peer);
        this.size = size;
    }
    public Memory(long size) {
        this(null, size);
    }
    public Memory(PointerIO<T> io, long size) {
        this(io, JNI.malloc(size), size);
    }
    public long getSize() {
        return size;
    }

    @Override
    protected void checkValidOffset(long offset, long length) {
        if (size < 0)
            return;
        if (offset < 0 || offset + length >= size)
            throw new IndexOutOfBoundsException("Cannot access to memory data of length " + length + " at offset " + offset + " : allocated memory size is " + size);
    }

    protected void deallocate() {
        JNI.free(peer);
    }
    
    @Override
    protected void finalize() throws Throwable {
        if (peer != 0)
            deallocate();
    }

    @Override
    public synchronized void release() {
        if (peer == 0)
            throw new RuntimeException("Memory was already released !");

        deallocate();
        peer = 0;
    }

    public void setPeer(long peer) {
        throw new UnsupportedOperationException("Cannot change the peer of a Memory object");
    }

    protected static class SharedPointer<T> extends DefaultPointer<T> {

        public SharedPointer(PointerIO<T> io, long peer, Memory memoryReferenceNotToGC) {
            super(io, peer);
            this.memoryReferenceNotToGC = memoryReferenceNotToGC;
        }

        protected Memory memoryReferenceNotToGC;

        @Override
        public Pointer<T> share(long offset) {
            return new SharedPointer(io, peer + offset, memoryReferenceNotToGC);
        }
		
		@Override
        public void release() {
			memoryReferenceNotToGC.release();
		}
    }

    @Override
    public Pointer<T> share(long offset) {
        return new SharedPointer(io, peer + offset, this);
    }
}
