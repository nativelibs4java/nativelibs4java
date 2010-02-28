/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.bridj;

import com.bridj.Pointer;
import com.bridj.ann.Runtime;

/**
 *
 * @author Olivier Chafik
 */
@Runtime(CRuntime.class)
public abstract class Callback<C extends Callback<C>> extends NativeObject {
	public Pointer<C> toPointer() {
		return (Pointer)Pointer.getPeer(this);
	}
}