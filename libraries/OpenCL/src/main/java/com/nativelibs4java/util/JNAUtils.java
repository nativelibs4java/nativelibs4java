/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.util;

import com.ochafik.lang.jnaerator.runtime.Size;
import com.ochafik.lang.jnaerator.runtime.SizeByReference;
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
    public static SizeByReference castToSize(IntByReference local) {
        SizeByReference nl = new SizeByReference();
        nl.setPointer(local.getPointer());
        return nl;
    }

	/**
	 * Casts a LongByReference to a SizeByReference
	 */
    public static SizeByReference castToSize(LongByReference local) {
        SizeByReference nl = new SizeByReference();
        nl.setPointer(local.getPointer());
        return nl;
    }

	public static Size readSize(Pointer p, long offset) {
		if (Size.SIZE == 4)
			return new Size(p.getInt(offset));
		else if (Size.SIZE == 8)
			return new Size(p.getLong(offset));
		else
			throw new RuntimeException("sizeof(size_t) must be either 4 or 8");
	}

	public static Size[] readSizes(Pointer p, int n) {
		Size[] sizes = new Size[n];
		int sz = Size.SIZE;
		for (int i = 0; i < n; i++)
			sizes[i] = readSize(p, i * sz);
		return sizes;
	}

	/**
	 * Return a new Size with the provided int value
	 */
	public static Size[] toSize(int[] ints) {
		if (ints == null)
			return null;
		int n = ints.length;
		Size[] nls = new Size[n];
        for (int i = 0; i < n; i++)
            nls[i] = toSize(ints[i]);

        return nls;
    }

	/**
	 * Return a new Size with the provided long value
	 */
	public static Size[] toSize(long[] ints) {
		if (ints == null)
			return null;
		int n = ints.length;
		Size[] nls = new Size[n];
        for (int i = 0; i < n; i++)
            nls[i] = toSize(ints[i]);

        return nls;
    }

	/**
	 * Return a new Size with the provided int value
	 */
	public static Size toSize(int i) {
        return new Size(i);
    }

	/**
	 * Return a new Size with the provided long value
	 */
	public static Size toSize(long i) {
        return new Size(i);
    }
}
