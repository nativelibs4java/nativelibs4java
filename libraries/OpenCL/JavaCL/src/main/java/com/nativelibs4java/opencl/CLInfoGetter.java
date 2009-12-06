/*
Copyright (c) 2009 Olivier Chafik (http://ochafik.free.fr/)

This file is part of OpenCL4Java (http://code.google.com/p/nativelibs4java/wiki/OpenCL).

OpenCL4Java is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 2.1 of the License, or
(at your option) any later version.

OpenCL4Java is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with OpenCL4Java.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.nativelibs4java.opencl;

import com.nativelibs4java.opencl.library.OpenCLLibrary;
import com.nativelibs4java.util.JNAUtils;
import com.ochafik.lang.jnaerator.runtime.NativeSize;
import com.ochafik.lang.jnaerator.runtime.NativeSizeByReference;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;
import com.sun.jna.*;
import com.sun.jna.ptr.*;
import java.nio.*;
import static com.nativelibs4java.opencl.JavaCL.*;
import static com.nativelibs4java.opencl.CLException.*;
import static com.nativelibs4java.util.JNAUtils.*;

/**
 *
 * @author ochafik
 */
abstract class CLInfoGetter<T extends PointerType> {

    protected abstract int getInfo(T entity, int infoTypeEnum, NativeSize size, Pointer out, NativeSizeByReference sizeOut);

    public String getString(T entity, int infoName) {
        NativeSizeByReference pLen = new NativeSizeByReference();
        error(getInfo(entity, infoName, toNS(0), null, pLen));

        int len = pLen.getValue().intValue();
        if (len == 0) {
            return "";
        }
        Memory buffer = new Memory(len + 1);
        error(getInfo(entity, infoName, pLen.getValue(), buffer, null));

        return buffer.getString(0);
    }

    public Pointer getPointer(T entity, int infoName) {
        NativeSizeByReference pLen = new NativeSizeByReference();
        Memory mem = new Memory(Pointer.SIZE);
        error(getInfo(entity, infoName, toNS(Pointer.SIZE), mem, pLen));
        if (pLen.getValue().intValue() != Pointer.SIZE) {
            throw new RuntimeException("Not a pointer : len = " + pLen.getValue());
        }
        return mem.getPointer(0);
    }

    public Memory getMemory(T entity, int infoName) {
        NativeSizeByReference pLen = new NativeSizeByReference();
        error(getInfo(entity, infoName, toNS(0), null, pLen));

        Memory buffer = new Memory(pLen.getValue().intValue());
        error(getInfo(entity, infoName, pLen.getValue(), buffer, null));

        return buffer;
    }

    public long[] getNativeSizes(T entity, int infoName, int n) {
        int nBytes = NativeSize.SIZE * n;
        NativeSizeByReference pLen = new NativeSizeByReference(toNS(nBytes));
        Memory mem = new Memory(nBytes);
        error(getInfo(entity, infoName, toNS(nBytes), mem, null));

        if (pLen.getValue().longValue() != nBytes) {
            throw new RuntimeException("Not a Size[" + n + "] : len = " + pLen.getValue());
        }
        long[] longs = new long[n];
        for (int i = 0; i < n; i++) {
            longs[i] = readNS(mem, i * NativeSize.SIZE).longValue();
        }
        return longs;
    }

    public int getInt(T entity, int infoName) {
        return (int)getIntOrLong(entity, infoName);
    }

    public boolean getBool(T entity, int infoName) {
        NativeSizeByReference pLen = new NativeSizeByReference();
        IntByReference pValue = new IntByReference();
        error(getInfo(entity, infoName, toNS(4), pValue.getPointer(), pLen));

        if (pLen.getValue().longValue() != 4) {
            throw new RuntimeException("Not a BOOL : len = " + pLen.getValue());
        }
        return pValue.getValue() != 0;
    }

    public long getIntOrLong(T entity, int infoName) {
        NativeSizeByReference pLen = new NativeSizeByReference();
        Memory mem = new Memory(8);
        error(getInfo(entity, infoName, toNS(8), mem, pLen));

        switch (pLen.getValue().intValue()) {
            case 4:
                return mem.getInt(0);
            case 8:
                return mem.getLong(0);
            default:
                throw new RuntimeException("Not a native long : len = " + pLen.getValue());
        }
    }
}
