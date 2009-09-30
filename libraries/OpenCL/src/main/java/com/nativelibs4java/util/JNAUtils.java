/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.util;

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
    public static NativeLongByReference castToNL(IntByReference local) {
        NativeLongByReference nl = new NativeLongByReference();
        nl.setPointer(local.getPointer());
        return nl;
    }

	/**
	 * Casts a LongByReference to a NativeLongByReference
	 */
    public static NativeLongByReference castToNL(LongByReference local) {
        NativeLongByReference nl = new NativeLongByReference();
        nl.setPointer(local.getPointer());
        return nl;
    }


	/**
	 * Return a new NativeLong with the provided int value
	 */
	public static NativeLong toNL(int i) {
        return new NativeLong(i);
    }

	/**
	 * Return a new NativeLong with the provided long value
	 */
	public static NativeLong toNL(long i) {
        return new NativeLong(i);
    }
}
