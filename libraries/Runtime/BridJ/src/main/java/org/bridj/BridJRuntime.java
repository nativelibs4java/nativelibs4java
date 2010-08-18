package org.bridj;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bridj.cpp.CPPObject;
import org.bridj.cpp.mfc.CObject;
import org.bridj.cpp.mfc.CRuntimeClass;
import java.lang.reflect.Type;

public interface BridJRuntime {

	
	public interface TypeInfo<T extends NativeObject> {
		T cast(Pointer peer);
		void initialize(T instance);
		void initialize(T instance, Pointer peer);
		void initialize(T instance, int constructorId, Object[] args);
        void destroy(T instance);
		T clone(T instance) throws CloneNotSupportedException;
		BridJRuntime getRuntime();
		Type getType();

        long sizeOf(T instance);
	}
	
	void register(Type type);
	<T extends NativeObject> TypeInfo<T> getTypeInfo(final Type type);

    boolean isAvailable();
	<T extends NativeObject> Class<? extends T> getActualInstanceClass(Pointer<T> pInstance, Type officialType);
    
}
