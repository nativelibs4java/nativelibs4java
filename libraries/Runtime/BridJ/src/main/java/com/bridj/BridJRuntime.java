package com.bridj;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.bridj.cpp.CPPObject;
import com.bridj.cpp.mfc.CObject;
import com.bridj.cpp.mfc.CRuntimeClass;

public interface BridJRuntime {

    boolean isAvailable();
	<T extends NativeObject> Class<? extends T> getActualInstanceClass(Pointer<T> pInstance, Class<T> officialType);
	void register(Class<?> type);
	
	<T extends NativeObject> Pointer<T> allocate(Class<?> type, int constructorId, Object... args);
    
	
}
