package org.bridj.objc;

import org.bridj.NativeObject;
import org.bridj.Pointer;

@org.bridj.ann.Runtime(ObjectiveCRuntime.class)
public class ObjCObject extends NativeObject {

    ObjCObject type;

    public native <T extends ObjCObject> Pointer<T> create();
    public native Pointer<NSString> stringValue(); 
    public native Pointer<NSString> description(); 

    public ObjCObject(Pointer<? extends NativeObject> peer) {
        super(peer);
    }

    public ObjCObject() {
        super();
    }

    public ObjCObject(int constructorId, Object... args) {
        super(constructorId, args);
    }

    public String toString() {
    		Pointer<NSString> p = description();
    		if (p == null)
    			p = stringValue();
    		
    		return p.get().toString();
    }
}
