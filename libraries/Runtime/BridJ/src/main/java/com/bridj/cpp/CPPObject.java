/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.bridj.cpp;

import java.util.Stack;

import com.bridj.NativeObject;
import com.bridj.Pointer;
import com.bridj.ann.Runtime;
import com.bridj.cpp.mfc.MFCRuntime;


/**
 *
 * @author Olivier
 */
@Runtime(CPPRuntime.class)
public class CPPObject extends NativeObject {
	public CPPObject() {}
    public CPPObject(Pointer<? extends CPPObject> peer, CPPRuntime cppRuntime) {
        super(peer, cppRuntime);
    }
    public CPPObject(int constructorId, Object... args) {
        super(constructorId, args);
    }
}
