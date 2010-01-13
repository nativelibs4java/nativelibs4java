/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.runtime;

/**
 *
 * @author Olivier Chafik
 */
public abstract class Callback<C extends Callback<C>> implements Pointable {
	protected Pointer<?> pointer;

    @Override
    public Pointer<?> getReference() {
        return pointer;
    }


}