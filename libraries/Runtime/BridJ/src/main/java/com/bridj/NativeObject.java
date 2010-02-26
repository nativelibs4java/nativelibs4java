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
public class NativeObject {
    Pointer<? extends NativeObject> peer;
    BridJRuntime runtime;

	public NativeObject(Pointer<? extends NativeObject> peer, BridJRuntime runtime) {
		BridJ.register(getClass());
		this.peer = peer;
		this.runtime = runtime;
	}
	
	public NativeObject() {
		Class<?> c = getClass();
		this.runtime = BridJ.register(c);
		if (!Pointer.isCastingInCurrentThread())
            this.peer = runtime.allocate(c, -1);
    }
    public NativeObject(int constructorId, Object[] args) {
    	Class<?> c = getClass();
		this.runtime = BridJ.register(c);
        this.peer = (Pointer)runtime.allocate(c, constructorId, args);
    }
    
    @Override
    protected void finalize() throws Throwable {
    	BridJ.deallocate(this);
    }
}
