#parse("main/Header.vm")
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
        Pointer<SizeT> pLen = allocateSizeT();
        error(getInfo(entity, infoName, 0, null, pLen));

        int len = (int)pLen.getSizeT();
        if (len == 0) {
            return "";
        }
        Pointer<?> buffer = allocateBytes(len + 1);
        error(getInfo(entity, infoName, len, buffer, null));

        return buffer.getCString();
    }

    public Pointer getPointer(T entity, int infoName) {
        Pointer<SizeT> pLen = allocateSizeT();
        Pointer<Pointer<?>> mem = allocatePointer();
        error(getInfo(entity, infoName, Pointer.SIZE, mem, pLen));
        if (pLen.getSizeT() != Pointer.SIZE) {
            throw new RuntimeException("Not a pointer : len = " + pLen.get());
        }
        return mem.get();
    }

    public Pointer<?> getMemory(T entity, int infoName) {
        Pointer<SizeT> pLen = allocateSizeT();
        error(getInfo(entity, infoName, 0, null, pLen));

        int len = (int)pLen.getSizeT();
        Pointer<?> buffer = allocateBytes(len);
        error(getInfo(entity, infoName, len, buffer, null));

        return buffer;
    }

    public long[] getNativeSizes(T entity, int infoName, int n) {
        int nBytes = SizeT.SIZE * n;
        Pointer<SizeT> pLen = pointerToSizeT(nBytes);
        Pointer<SizeT> mem = allocateSizeTs(n);
        error(getInfo(entity, infoName, nBytes, mem, pLen));

        int actualLen = (int)pLen.getSizeT();
        if (actualLen != nBytes) {
            throw new RuntimeException("Not a Size[" + n + "] : len = " + actualLen);
        }
        return mem.getSizeTs(n);
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
        Pointer<SizeT> pLen = allocateSizeT();
        Pointer<Byte> mem = allocateBytes(8);
        error(getInfo(entity, infoName, 8, mem, pLen));

        long len = pLen.getSizeT();
        switch ((int)len) {
        case 1: 
        		return mem.getByte() != 0;
        	case 2:
        		return mem.getShort() != 0;
        	case 4:
        		return mem.getInt() != 0;
        	case 8:
        		return mem.getLong() != 0;
        	case 0:
        		// HACK to accommodate ATI Stream on Linux 32 bits (CLPlatform.isAvailable())
        		//if (JNI.isLinux())
        		return true;
        default:
            throw new RuntimeException("Not a BOOL : len = " + len);
        }
    }

    public long getIntOrLong(T entity, int infoName) {
        Pointer<SizeT> pLen = allocateSizeT();
        Pointer<Long> mem = allocateLong();
        error(getInfo(entity, infoName, 8, mem, pLen));

        switch ((int)pLen.getSizeT()) {
            case 4:
                return mem.getInt();
            case 8:
                return mem.getLong();
            default:
                throw new RuntimeException("Not a native long : len = " + pLen.get());
        }
    }
}
