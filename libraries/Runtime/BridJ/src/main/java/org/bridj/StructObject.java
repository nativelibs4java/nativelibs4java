package org.bridj;

/**
 * Base class for C structs.
 * @author Olivier
 */
@org.bridj.ann.Runtime(CRuntime.class)
public abstract class StructObject extends NativeObject {
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
