package com.bridj;

@com.bridj.ann.Runtime(CRuntime.class)
public class StructObject extends NativeObject {
	protected StructIO io;
	Object[] refreshableFields;

    public StructObject() {
		super();
	}
    public StructObject(int constructorId, Object[] args) {
    	super(constructorId, args);
    }
    public StructObject(Pointer<? extends StructObject> peer) {
    	super(peer);
    }
}
