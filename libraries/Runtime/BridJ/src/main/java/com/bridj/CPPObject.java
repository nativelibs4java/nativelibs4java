/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.bridj;

import java.util.Stack;


/**
 *
 * @author Olivier
 */
@Runtime(CPPRuntime.class)
public class CPPObject extends NativeObject {
	public CPPObject() {}
    public CPPObject(Pointer<? extends CPPObject> peer) {
        super(peer);
    }
    public CPPObject(int constructorId, Object... args) {
        super(constructorId, args);
    }
}
