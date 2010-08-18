/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bridj;

import java.util.Stack;

/**
 *
 * @author Olivier
 */
public class NativeObject {

    protected Pointer<? extends NativeObject> peer;
    protected BridJRuntime.TypeInfo typeInfo;

    public NativeObject(Pointer<? extends NativeObject> peer) {
        BridJ.initialize(this, peer);
    }

    public NativeObject() {
        BridJ.initialize(this);
    }

    public NativeObject(int constructorId, Object... args) {
        BridJ.initialize(this, constructorId, args);
    }
    /*
    @Override
    protected void finalize() throws Throwable {
    BridJ.deallocate(this);
    }*/

    public NativeObject clone() throws CloneNotSupportedException {
        return BridJ.clone(this);
    }
}
