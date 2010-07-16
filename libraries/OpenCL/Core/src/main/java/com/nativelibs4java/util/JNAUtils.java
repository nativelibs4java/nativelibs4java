/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.util;

import com.ochafik.lang.jnaerator.runtime.NativeSize;
import com.ochafik.lang.jnaerator.runtime.NativeSizeByReference;
import com.sun.jna.*;
import com.sun.jna.ptr.*;

/**
 * JNA-related util methods
 * @author ochafik
 */
public class JNAUtils {

    /**
     * Casts an IntByReference to a NativeLongByReference
     */
    public static NativeSizeByReference castToNS(IntByReference local) {
        NativeSizeByReference nl = new NativeSizeByReference();
        nl.setPointer(local.getPointer());
        return nl;
    }

    /**
     * Casts a LongByReference to a NativeSizeByReference
     */
    public static NativeSizeByReference castToNS(LongByReference local) {
        NativeSizeByReference nl = new NativeSizeByReference();
        nl.setPointer(local.getPointer());
        return nl;
    }

    public static NativeSize readNS(Pointer p, long offset) {
        if (NativeSize.SIZE == 4)
            return new NativeSize(p.getInt(offset));
        else if (NativeSize.SIZE == 8)
            return new NativeSize(p.getLong(offset));
        else
            throw new RuntimeException("sizeof(size_t) must be either 4 or 8");
    }
    public static void writeNS(Pointer p, long offset, long value) {
        if (NativeSize.SIZE == 4)
            p.setInt(offset, (int)value);
        else if (NativeSize.SIZE == 8)
            p.setLong(offset, value);
        else
            throw new RuntimeException("sizeof(size_t) must be either 4 or 8");
    }

    public static NativeSize[] readNSArray(Pointer p, int n) {
        NativeSize[] sizes = new NativeSize[n];
        for (int i = 0; i < n; i++)
            sizes[i] = readNS(p, i * NativeSize.SIZE);
        return sizes;
    }

    public static void writeNSArray(Pointer p, long... values) {
        if (values == null)
            return;
        for (int i = 0, n = values.length; i < n; i++)
            writeNS(p, i * NativeSize.SIZE, values[i]);
    }

    /**
     * Converts int/long values array to a Memory object containing contiguous size_t values
     * @see NativeSize#SIZE
     */
    public static Memory toNSArray(long... values) {
        if (values == null)
            return null;
        Memory mem = new Memory(values.length * NativeSize.SIZE);
        writeNSArray(mem, values);
        return mem;
    }

    /**
     * Return a new Size with the provided int value
     */
    public static NativeSize[] toNS(int[] ints) {
        if (ints == null)
                return null;
        int n = ints.length;
        NativeSize[] nls = new NativeSize[n];
        for (int i = 0; i < n; i++)
            nls[i] = toNS(ints[i]);

        return nls;
    }

    /**
     * Return a new NativeSize array with the provided long values
     */
    public static NativeSize[] toNS(long... ints) {
        if (ints == null)
            return null;
        int n = ints.length;
        NativeSize[] nls = new NativeSize[n];
        for (int i = 0; i < n; i++)
            nls[i] = toNS(ints[i]);

        return nls;
    }

    /**
     * Return a new NativeSize with the provided int value
     */
    public static NativeSize toNS(int i) {
        return new NativeSize(i);
    }

    /**
     * Return a new NativeSize with the provided long value
     */
    public static NativeSize toNS(long i) {
        return new NativeSize(i);
    }
}
