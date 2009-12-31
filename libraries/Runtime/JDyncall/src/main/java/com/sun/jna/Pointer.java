/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.jna;

import java.nio.ByteBuffer;

/**
 *
 * @author Olivier
 */
public interface Pointer<P extends Pointer> {
    public static final Pointer NULL = com.nativelibs4java.runtime.Pointer.NULL;

    P share(long offset);
    ByteBuffer getByteBuffer(long offset, long length);
}
