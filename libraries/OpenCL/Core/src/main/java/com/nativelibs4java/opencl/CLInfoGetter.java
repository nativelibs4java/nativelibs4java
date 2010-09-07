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

import static com.nativelibs4java.opencl.CLException.error;

import org.bridj.*;
import static org.bridj.Pointer.*;

/**
 *
 * @author ochafik
 */
abstract class CLInfoGetter<T extends Pointer> {

    protected abstract int getInfo(T entity, int infoTypeEnum, long size, Pointer out, Pointer<SizeT> sizeOut);

    public String getString(T entity, int infoName) {
        Pointer<SizeT> pLen = allocate(SizeT.class);
        error(getInfo(entity, infoName, 0, null, pLen));

        int len = (int)pLen.getSizeT(0);
        if (len == 0) {
            return "";
        }
        Pointer<?> buffer = allocateBytes(len + 1);
        error(getInfo(entity, infoName, pLen.get().intValue(), buffer, null));

        return buffer.getCString(0);
    }

    public Pointer getPointer(T entity, int infoName) {
        Pointer<SizeT> pLen = allocate(SizeT.class);
        Pointer<Pointer<?>> mem = allocatePointer();
        error(getInfo(entity, infoName, Pointer.SIZE, mem, pLen));
        if (pLen.getSizeT(0) != Pointer.SIZE) {
            throw new RuntimeException("Not a pointer : len = " + pLen.get());
        }
        return mem.get();
    }

    public Pointer<?> getMemory(T entity, int infoName) {
        Pointer<SizeT> pLen = allocate(SizeT.class);
        error(getInfo(entity, infoName, 0, null, pLen));

        int len = pLen.get().intValue();
        Pointer<?> buffer = allocateBytes(len);
        error(getInfo(entity, infoName, len, buffer, null));

        return buffer;
    }

    public long[] getNativeSizes(T entity, int infoName, int n) {
        int nBytes = SizeT.SIZE * n;
        Pointer<SizeT> pLen = pointerToSizeT(nBytes);
        Pointer<SizeT> mem = allocateSizeTs(n);
        error(getInfo(entity, infoName, nBytes, mem, pLen));

        if (pLen.get().longValue() != nBytes) {
            throw new RuntimeException("Not a Size[" + n + "] : len = " + pLen.get());
        }
        return mem.getSizeTs(0, n);
    }

    public int getOptionalFeatureInt(T entity, int infoName) {
    	try {
    		return getInt(entity, infoName);
    	} catch (CLException.InvalidValue ex) {
    		throw new UnsupportedOperationException("Cannot get value " + infoName, ex);
    	} catch (CLException.InvalidOperation ex) {
    		throw new UnsupportedOperationException("Cannot get value " + infoName, ex);
    	}
    }
    public int getInt(T entity, int infoName) {
        return (int)getIntOrLong(entity, infoName);
    }

    public boolean getBool(T entity, int infoName) {
        Pointer<SizeT> pLen = allocate(SizeT.class);
        Pointer<Integer> pValue = allocateInt();
        error(getInfo(entity, infoName, 4, pValue, pLen));

        if (pLen.get().longValue() != 4) {
            throw new RuntimeException("Not a BOOL : len = " + pLen.get());
        }
        return pValue.get() != 0;
    }

    public long getIntOrLong(T entity, int infoName) {
        Pointer<SizeT> pLen = allocate(SizeT.class);
        Pointer<Long> mem = allocateLong();
        error(getInfo(entity, infoName, 8, mem, pLen));

        switch (pLen.get().intValue()) {
            case 4:
                return mem.getInt(0);
            case 8:
                return mem.getLong(0);
            default:
                throw new RuntimeException("Not a native long : len = " + pLen.get());
        }
    }
}
