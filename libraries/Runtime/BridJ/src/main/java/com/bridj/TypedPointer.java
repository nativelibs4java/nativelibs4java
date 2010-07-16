package com.bridj;


public class TypedPointer extends Pointer {
	Pointer<?> copy;
	public TypedPointer(long address) {
        //TODO
        super(PointerIO.getPointerInstance(), address);
	}
	public TypedPointer(Pointer<?> ptr) {
		//TODO
        super(PointerIO.getPointerInstance(), ptr.getPeer());
		copy = ptr;
	}
}
