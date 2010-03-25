package com.bridj;


public class TypedPointer extends DefaultPointer {
	Pointer<?> copy;
	public TypedPointer(long address) {
		super(address);
	}
	public TypedPointer(Pointer<?> ptr) {
		super(ptr.getPeer());
		copy = ptr;
	}
}
