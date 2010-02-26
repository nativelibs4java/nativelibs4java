/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.bridj.cs.dotnet;

import com.bridj.AbstractBridJRuntime;
import com.bridj.JNI;
import com.bridj.NativeObject;
import com.bridj.Pointer;
import com.bridj.cs.CSharpRuntime;

/**
 *
 * @author Olivier
 */
public class DotNetRuntime extends AbstractBridJRuntime implements CSharpRuntime {

    @Override
    public boolean isAvailable() {
        return JNI.isWindows();
    }

    @Override
    public <T extends NativeObject> Class<? extends T> getActualInstanceClass(Pointer<T> pInstance, Class<T> officialType) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void register(Class<?> type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T extends NativeObject> Pointer<T> allocate(Class<?> type, int constructorId, Object... args) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
