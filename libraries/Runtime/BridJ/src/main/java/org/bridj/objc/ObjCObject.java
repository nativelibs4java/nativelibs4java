package org.bridj.objc;

import org.bridj.NativeObject;
import org.bridj.Pointer;

@org.bridj.ann.Runtime(ObjectiveCRuntime.class)
public class ObjCObject extends NativeObject {

    ObjCObject type;

    public native <T extends ObjCObject> Pointer<T> create();

    public ObjCObject(Pointer<? extends NativeObject> peer) {
        super(peer);
    }

    public ObjCObject() {
        super();
    }

    public ObjCObject(int constructorId, Object... args) {
        super(constructorId, args);
    }

}
