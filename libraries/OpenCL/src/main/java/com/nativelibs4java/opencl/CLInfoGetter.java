/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl;
import com.nativelibs4java.opencl.library.OpenCLLibrary;
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

	protected abstract int getInfo(T entity, int infoTypeEnum, NativeLong size, Pointer out, NativeLongByReference sizeOut);

    public String getString(T entity, int infoName) {
        NativeLongByReference pLen = new NativeLongByReference();
        error(getInfo(entity, infoName, toNL(0), null, pLen));

		int len = pLen.getValue().intValue();
		if (len == 0)
			return "";
        Memory buffer = new Memory(len + 1);
        error(getInfo(entity, infoName, pLen.getValue(), buffer, null));

        return buffer.getString(0);
    }

	public Memory getBytes(T entity, int infoName) {
        NativeLongByReference pLen = new NativeLongByReference();
        error(getInfo(entity, infoName, toNL(0), null, pLen));

		Memory buffer = new Memory(pLen.getValue().intValue());
        error(getInfo(entity, infoName, pLen.getValue(), buffer, null));

        return buffer;
    }

	public long[] getNativeLongs(T entity, int infoName, int n) {
		int nBytes = Native.LONG_SIZE * n;
        NativeLongByReference pLen = new NativeLongByReference(toNL(nBytes));
		Memory mem = new Memory(nBytes);
		error(getInfo(entity, infoName, toNL(nBytes), mem, null));

		if (pLen.getValue().longValue() != nBytes)
			throw new RuntimeException("Not a NativeLong[" + n + "] : len = " + pLen.getValue());
		long[] longs = new long[n];
		for (int i = 0; i < n; i++) {
			longs[i] = mem.getNativeLong(i * Native.LONG_SIZE).longValue();
		}
		return longs;
    }
	public int getInt(T entity, int infoName) {
        NativeLongByReference pLen = new NativeLongByReference();
        IntByReference pValue = new IntByReference();
		error(getInfo(entity, infoName, toNL(4), pValue.getPointer(), pLen));

		if (pLen.getValue().longValue() != 4)
			throw new RuntimeException("Not an int : len = " + pLen.getValue());
		return pValue.getValue();
    }
	public boolean getBool(T entity, int infoName) {
        NativeLongByReference pLen = new NativeLongByReference();
        IntByReference pValue = new IntByReference();
		error(getInfo(entity, infoName, toNL(4), pValue.getPointer(), pLen));

		if (pLen.getValue().longValue() != 4)
			throw new RuntimeException("Not a BOOL : len = " + pLen.getValue());
		return pValue.getValue() != 0;
    }
	public long getNativeLong(T entity, int infoName) {
        NativeLongByReference pLen = new NativeLongByReference();
        NativeLongByReference pValue = new NativeLongByReference();
		error(getInfo(entity, infoName, toNL(Native.LONG_SIZE), pValue.getPointer(), pLen));

		if (pLen.getValue().longValue() != Native.LONG_SIZE)
			throw new RuntimeException("Not a native long : len = " + pLen.getValue());
		return pValue.getValue().longValue();
    }
}
