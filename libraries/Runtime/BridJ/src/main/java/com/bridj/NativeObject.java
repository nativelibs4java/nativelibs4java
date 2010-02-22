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

	public NativeObject(Pointer<? extends NativeObject> peer) {
		BridJ.register(getClass());
		this.peer = peer;
	}
	
	public NativeObject() {
		if (Pointer.isCastingInCurrentThread())
            BridJ.register(getClass());
        else
            this.peer = BridJ.allocate(getClass(), -1);
    }
    public NativeObject(int constructorId, Object[] args) {
        this.peer = BridJ.allocate(getClass(), constructorId, args);
    }
    
    @Override
    protected void finalize() throws Throwable {
    	BridJ.deallocate(this);
    }
}
