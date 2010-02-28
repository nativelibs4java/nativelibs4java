package com.bridj.objc;

import com.bridj.Pointer;

public class ObjCClass extends ObjCObject {
	public native <T extends ObjCObject> Pointer<T> alloc();
	@Selector("new:")
	public native <T extends ObjCObject> Pointer<T> new$();
}
