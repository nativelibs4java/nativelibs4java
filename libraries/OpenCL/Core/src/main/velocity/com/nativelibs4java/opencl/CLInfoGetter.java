#parse("main/Header.vm")
package com.nativelibs4java.opencl;

import static com.nativelibs4java.opencl.CLException.error;

import org.bridj.*;
import static org.bridj.Pointer.*;

import org.bridj.ann.Ptr;

/**
 *
 * @author ochafik
 */
abstract class CLInfoGetter {

    protected abstract int getInfo(long entity, int infoTypeEnum, long size, Pointer out, Pointer<SizeT> sizeOut);

    public String getString(@Ptr long entity, int infoName) {
        #declareReusablePtrs()
        Pointer<SizeT> pLen = ptrs.sizeT1;
        error(getInfo(entity, infoName, 0, null, pLen));

        long len = pLen.getSizeT();
        if (len == 0) {
            return "";
        }
        Pointer<?> buffer = allocateBytes(len + 1);
        error(getInfo(entity, infoName, len, buffer, null));
        String s = buffer.getCString();
        buffer.release();
        return s;
    }

    public Pointer getPointer(@Ptr long entity, int infoName) {
        #declareReusablePtrs()
        Pointer<SizeT> pLen = ptrs.sizeT1;
        Pointer<Pointer<?>> mem = ptrs.ptr1;
        error(getInfo(entity, infoName, Pointer.SIZE, mem, pLen));
        if (pLen.getSizeT() != Pointer.SIZE) {
            throw new RuntimeException("Not a pointer : len = " + pLen.get());
        }
        return mem.get();
    }

    public Pointer<?> getMemory(@Ptr long entity, int infoName) {
        #declareReusablePtrs()
        Pointer<SizeT> pLen = ptrs.sizeT1;
        error(getInfo(entity, infoName, 0, null, pLen));

        int len = (int)pLen.getSizeT();
        Pointer<?> buffer = allocateBytes(len);
        error(getInfo(entity, infoName, len, buffer, null));

        return buffer;
    }

    public long[] getNativeSizes(@Ptr long entity, int infoName, int n) {
        long nBytes = SizeT.SIZE * n;
        #declareReusablePtrs()
        Pointer<SizeT> pLen = ptrs.sizeT3_1.pointerToSizeTs(nBytes);
        Pointer<SizeT> mem = ptrs.sizeT3_2.allocatedSizeTs(n);
        error(getInfo(entity, infoName, nBytes, mem, pLen));

        int actualLen = (int)pLen.getSizeT();
        if (actualLen != nBytes) {
            throw new RuntimeException("Not a Size[" + n + "] : len = " + actualLen);
        }
        return mem.getSizeTs(n);
    }

    public int getOptionalFeatureInt(@Ptr long entity, int infoName) {
    	try {
    		return getInt(entity, infoName);
    	} catch (CLException.InvalidValue ex) {
    		throw new UnsupportedOperationException("Cannot get value " + infoName, ex);
    	} catch (CLException.InvalidOperation ex) {
    		throw new UnsupportedOperationException("Cannot get value " + infoName, ex);
    	}
    }
    public int getInt(@Ptr long entity, int infoName) {
        return (int)getIntOrLong(entity, infoName);
    }

    public boolean getBool(@Ptr long entity, int infoName) {
        #declareReusablePtrs()
        Pointer<SizeT> pLen = ptrs.sizeT1;
        Pointer<?> mem = ptrs.sizeT3_1.allocatedBytes(8);
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

    public long getIntOrLong(@Ptr long entity, int infoName) {
        #declareReusablePtrs()
        Pointer<SizeT> pLen = ptrs.sizeT1;
        Pointer<Long> mem = ptrs.long1;
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
