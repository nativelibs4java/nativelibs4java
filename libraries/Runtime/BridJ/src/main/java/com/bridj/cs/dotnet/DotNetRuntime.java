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
import java.lang.reflect.Type;

/**
 * {@link http://msdn.microsoft.com/en-us/library/system.runtime.interopservices.marshal.getdelegateforfunctionpointer(VS.80).aspx}
 * @author Olivier
 */
public class DotNetRuntime extends AbstractBridJRuntime implements CSharpRuntime {

    @Override
    public boolean isAvailable() {
        return JNI.isWindows();
    }

    @Override
    public <T extends NativeObject> Class<? extends T> getActualInstanceClass(Pointer<T> pInstance, Type officialType) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void register(Type type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T extends NativeObject> TypeInfo<T> getTypeInfo(Type type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
