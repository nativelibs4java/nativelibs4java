/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.jna;

/**
 *
 * @author Olivier
 */
public interface NativeMapped {
    Object fromNative(Object o, FromNativeContext fnc);
    Object toNative();
    Class nativeType();
}
