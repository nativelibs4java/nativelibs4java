package org.bridj.objc;

import org.bridj.Pointer;

public class ObjCClass extends ObjCObject {
	public native <T extends ObjCObject> Pointer<T> alloc();
	@Selector("new:")
	public native <T extends ObjCObject> Pointer<T> new$();
}
