/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.runtime;

/**
 *
 * @author ochafik
 */
public interface PointerRefreshable extends Pointable {
    PointerRefreshable setPointer(Pointer<?> pointer);
}
