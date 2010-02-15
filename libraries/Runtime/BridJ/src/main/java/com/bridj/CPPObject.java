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
public class CPPObject {
	static ThreadLocal<Stack<Boolean>> currentlyCastingACPPClass = new ThreadLocal<Stack<Boolean>>() { 
		protected java.util.Stack<Boolean> initialValue() {
			Stack<Boolean> s = new Stack<Boolean>();
			s.push(false);
			return s;
		};
	};
	
	protected Pointer<? extends CPPObject> $this;
	
	public static synchronized <O extends CPPObject> O castAddress(long address, Class<O> type) {
		Stack<Boolean> s = currentlyCastingACPPClass.get();
		s.push(true);
		try {
			O instance = type.newInstance();
			instance.$this = Pointer.pointerToAddress(address, type);
			return instance;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to cast address " + address + " to pointer of type " + type.getName(), ex);
		} finally {
			s.pop();
		}
	}
	public CPPObject() {
		BridJ.register(getClass());
		if (currentlyCastingACPPClass.get().peek())
			return;
		try {
			this.$this = BridJ.newCPPInstance(getClass());
		} catch (Exception ex) {
			throw new RuntimeException("Failed to create new C++ object instance for type " + getClass().getName(), ex);
		}
	}
	public CPPObject(Pointer $this) {
		BridJ.register(getClass());
		this.$this = $this;
	}
	
	public Pointer<? extends CPPObject> getReference() {
		return $this;
	}
	//protected CPPObjectIO $io;
	//public CPPObject(CPPObjectIO $io, long $this) {
	//	this.$io = $io;
	//	this.$this = $this;
	//	$io.use();
	//}
}
