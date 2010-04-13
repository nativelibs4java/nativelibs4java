package com.bridj;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.bridj.cpp.CPPObject;
import com.bridj.cpp.mfc.CObject;
import com.bridj.cpp.mfc.CRuntimeClass;

public interface BridJRuntime {

	
	void register(Class<?> type);

    void initialize(NativeObject instance);
    void initialize(NativeObject instance, Pointer peer);
    void initialize(NativeObject instance, int constructorId, Object... args);

    void destroy(NativeObject instance);

    <T extends NativeObject> T clone(T instance) throws CloneNotSupportedException;
    
    <T extends NativeObject> Class<? extends T> getTypeForCast(Class<T> type);
    
    boolean isAvailable();
	<T extends NativeObject> Class<? extends T> getActualInstanceClass(Pointer<T> pInstance, Class<T> officialType);
    
}
