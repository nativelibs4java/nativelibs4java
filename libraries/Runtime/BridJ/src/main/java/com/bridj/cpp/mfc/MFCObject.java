/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.bridj.cpp.mfc;

import com.bridj.Pointer;
import com.bridj.ann.Library;
import com.bridj.ann.Runtime;
import com.bridj.cpp.CPPObject;

@Library(value="mfc90u.dll", versionPattern = "mfc(?:(\\d)(\\d))?u")
@Runtime(MFCRuntime.class)
public class MFCObject extends CPPObject {
	public MFCObject() {}
    public MFCObject(Pointer<? extends MFCObject> peer) {
        super(peer);
    }
    public MFCObject(int constructorId, Object... args) {
        super(constructorId, args);
    }
}