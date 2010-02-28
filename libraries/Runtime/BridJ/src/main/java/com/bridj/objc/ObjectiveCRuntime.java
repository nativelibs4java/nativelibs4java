package com.bridj.objc;

import java.io.FileNotFoundException;
import java.lang.reflect.Method;

import com.bridj.JNI;
import com.bridj.CRuntime;
import com.bridj.MethodCallInfo;
import com.bridj.NativeLibrary;
import com.bridj.NativeEntities.Builder;

public class ObjectiveCRuntime extends CRuntime {

    public boolean isAvailable() {
        return JNI.isMacOSX();
    }
    
    @Override
    protected void registerNativeMethod(Class<?> type,
    		NativeLibrary typeLibrary, Method method,
    		NativeLibrary methodLibrary, Builder builder)
    		throws FileNotFoundException {
    	
    	MethodCallInfo mci = new MethodCallInfo(method);
    	Selector sel = method.getAnnotation(Selector.class);
    	if (sel != null)
    		mci.setSymbolName(sel.value());
    	else
    		mci.setSymbolName(method.getName() + ":");
    	builder.addObjCMethod(mci);
    }
}
