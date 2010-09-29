/*
 * JavaCL - Java API and utilities for OpenCL
 * http://javacl.googlecode.com/
 *
 * Copyright (c) 2009-2010, Olivier Chafik (http://ochafik.free.fr/)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Olivier Chafik nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY OLIVIER CHAFIK AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
