package org.bridj;

@org.bridj.ann.Runtime(CRuntime.class)
public class StructObject extends NativeObject {
	protected StructIO io;
	Object[] refreshableFields;

    protected StructObject() {
		super();
	}
    protected StructObject(int constructorId, Object[] args) {
    	super(constructorId, args);
    }
    protected StructObject(Pointer<? extends StructObject> peer) {
    	super(peer);
    }
}
