/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.bridj.cpp;

import java.util.Stack;

import org.bridj.NativeObject;
import org.bridj.Pointer;
import org.bridj.StructObject;
import org.bridj.ann.Runtime;
import org.bridj.cpp.mfc.MFCRuntime;
import org.bridj.BridJ;


/**
 *
 * @author Olivier
 */
@Runtime(CPPRuntime.class)
public class CPPObject extends StructObject {
	public CPPObject() {}
    public CPPObject(Pointer<? extends CPPObject> peer) {
        super(peer);
    }
    public CPPObject(int constructorId, Object... args) {
        super(constructorId, args);
    }
    /*
    @Override
    protected void finalize() throws Throwable {
    		BridJ.deallocate(this);
    }*/
}
