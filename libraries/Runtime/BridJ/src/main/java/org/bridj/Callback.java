/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.bridj;

import org.bridj.Pointer;
import org.bridj.ann.Runtime;

/**
 * Native C callback (Java object with a only one abstract method exposed as a C function pointer to the native world).<br>
 * Here's an example of callback definition (use JNAerator to generate them automatically) :
 * <pre>{@code
 *  // typedef int (*MyCallback)(int a, int b);
 *  public static abstract class MyCallback extends Callback {
		public abstract int doSomething(int a, int b);
	}
 * }</pre>
 * @author Olivier Chafik
 */
@Runtime(CRuntime.class)
public abstract class Callback<C extends Callback<C>> extends NativeObject {
	public Pointer<C> toPointer() {
		return (Pointer)Pointer.pointerTo(this);
	}
}