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
    static ThreadLocal<Stack<Boolean>> currentlyCasting = new ThreadLocal<Stack<Boolean>>() {
        @Override
		protected java.util.Stack<Boolean> initialValue() {
			Stack<Boolean> s = new Stack<Boolean>();
			s.push(false);
			return s;
		};
	};

	Pointer<? extends NativeObject> peer;

	public NativeObject(Pointer<? extends NativeObject> peer) {
		BridJ.register(getClass());
		this.peer = peer;
	}
	public NativeObject() {
		if (currentlyCasting.get().peek())
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
    
	public static synchronized <O extends NativeObject> O castAddress(long address, Class<O> type) {
		Stack<Boolean> s = currentlyCasting.get();
		s.push(true);
		try {
			O instance = type.newInstance();
			instance.peer = Pointer.pointerToAddress(address, type);
			return instance;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to cast address " + address + " to pointer of type " + type.getName(), ex);
		} finally {
			s.pop();
		}
	}
}
