/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.bridj;

import org.bridj.Pointer;
import org.bridj.ann.Runtime;

/**
 *
 * @author Olivier Chafik
 */
@Runtime(CRuntime.class)
public abstract class Callback<C extends Callback<C>> extends NativeObject {
	public Pointer<C> toPointer() {
		return (Pointer)Pointer.pointerTo(this);
	}
}