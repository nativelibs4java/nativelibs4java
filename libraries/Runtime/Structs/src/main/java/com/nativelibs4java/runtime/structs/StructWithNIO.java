/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.runtime.structs;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import java.nio.ByteBuffer;

/**
 *
 * @author ochafik
 */
public class StructWithNIO<S extends Struct<S>> extends Struct<S> {

	protected ByteBuffer buffer;

    public StructWithNIO(StructIOWithNIO io) {
        super(io);
    }

    public synchronized ByteBuffer getBuffer() {
        if (buffer == null)
            throw new UnsupportedOperationException("Struct is not backed by an NIO buffer (TODO: implement creation of a buffer out of a pointer)");

        return buffer;
    }

    @Override
    public synchronized S setPointer(com.sun.jna.Pointer pointer) {
        super.setPointer(pointer);
        this.buffer = null;
        return (S)this;
    }

    public synchronized void setBuffer(ByteBuffer buffer) {
        Pointer pointer = null;
        if (buffer == null || buffer.isDirect() && ((pointer = Native.getDirectBufferPointer(buffer)) == null || pointer.equals(Pointer.NULL)))
            throw new NullPointerException("Cannot set null pointer as struct address !");

        this.buffer = buffer;
        this.pointer = pointer;
    }


    public synchronized boolean isDirect() {
        return pointer != null;
    }

    public synchronized boolean hasBuffer() {
        return buffer != null;
    }
}
