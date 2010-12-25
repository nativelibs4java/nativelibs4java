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
import static com.nativelibs4java.util.JNAUtils.readNS;
import static com.nativelibs4java.util.JNAUtils.toNS;

import com.ochafik.lang.jnaerator.runtime.NativeSize;
import com.ochafik.lang.jnaerator.runtime.NativeSizeByReference;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.ptr.IntByReference;

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
        NativeSizeByReference pLen = new NativeSizeByReference();
        Memory mem = new Memory(8);
        error(getInfo(entity, infoName, toNS(8), mem, pLen));

        switch ((int)pLen.getValue().longValue()) {
        case 1: 
        		return mem.getByte(0) != 0;
        	case 2:
        		return mem.getShort(0) != 0;
        	case 4:
        		return mem.getInt(0) != 0;
        	case 8:
        		return mem.getLong(0) != 0;
        	default:
            throw new RuntimeException("Not a BOOL : len = " + pLen.getValue());
        }
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
