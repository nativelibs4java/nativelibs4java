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
import com.ochafik.lang.jnaerator.runtime.Size;
import com.ochafik.lang.jnaerator.runtime.SizeByReference;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;
import com.sun.jna.*;
import com.sun.jna.ptr.*;
import java.nio.*;
import static com.nativelibs4java.opencl.OpenCL4Java.*;
import static com.nativelibs4java.opencl.CLException.*;
import static com.nativelibs4java.util.JNAUtils.*;

/**
 *
 * @author ochafik
 */
abstract class CLInfoGetter<T extends PointerType> {

    protected abstract int getInfo(T entity, int infoTypeEnum, Size size, Pointer out, SizeByReference sizeOut);

    public String getString(T entity, int infoName) {
        SizeByReference pLen = new SizeByReference();
        error(getInfo(entity, infoName, toSize(0), null, pLen));

        int len = pLen.getValue().intValue();
        if (len == 0) {
            return "";
        }
        Memory buffer = new Memory(len + 1);
        error(getInfo(entity, infoName, pLen.getValue(), buffer, null));

        return buffer.getString(0);
    }

    public Memory getMemory(T entity, int infoName) {
        SizeByReference pLen = new SizeByReference();
        error(getInfo(entity, infoName, toSize(0), null, pLen));

        Memory buffer = new Memory(pLen.getValue().intValue());
        error(getInfo(entity, infoName, pLen.getValue(), buffer, null));

        return buffer;
    }

    public long[] getSizes(T entity, int infoName, int n) {
        int nBytes = Size.SIZE * n;
        SizeByReference pLen = new SizeByReference(toSize(nBytes));
        Memory mem = new Memory(nBytes);
        error(getInfo(entity, infoName, toSize(nBytes), mem, null));

        if (pLen.getValue().longValue() != nBytes) {
            throw new RuntimeException("Not a Size[" + n + "] : len = " + pLen.getValue());
        }
        long[] longs = new long[n];
        for (int i = 0; i < n; i++) {
            longs[i] = readSize(mem, i * Size.SIZE).longValue();
        }
        return longs;
    }

    public int getInt(T entity, int infoName) {
        return (int)getIntOrLong(entity, infoName);
    }

    public boolean getBool(T entity, int infoName) {
        SizeByReference pLen = new SizeByReference();
        IntByReference pValue = new IntByReference();
        error(getInfo(entity, infoName, toSize(4), pValue.getPointer(), pLen));

        if (pLen.getValue().longValue() != 4) {
            throw new RuntimeException("Not a BOOL : len = " + pLen.getValue());
        }
        return pValue.getValue() != 0;
    }

    public long getIntOrLong(T entity, int infoName) {
        SizeByReference pLen = new SizeByReference();
        Memory mem = new Memory(8);
        error(getInfo(entity, infoName, toSize(8), mem, pLen));

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
