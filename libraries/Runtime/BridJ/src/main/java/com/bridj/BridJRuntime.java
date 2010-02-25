package com.bridj;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.bridj.c.Callback;
import com.bridj.cpp.CPPObject;
import com.bridj.cpp.mfc.CObject;
import com.bridj.cpp.mfc.CRuntimeClass;

public abstract class BridJRuntime {
	
	protected void log(Level level, String message, Throwable ex) {
		Logger.getLogger(getClass().getName()).log(level, message, ex);
	}
	protected void log(Level level, String message) {
		log(level, message, null);
	}
	
	public abstract <T extends NativeObject> Class<? extends T> getActualInstanceClass(Pointer<T> pInstance, Class<T> officialType);
	public abstract void register(Class<?> type);
	
	public <T extends NativeObject> Pointer<T> allocate(Class<?> type, int constructorId, Object... args) {
        throw new RuntimeException("Cannot allocate instance of type " + type.getName() + " (unhandled NativeObject subclass)");
    }
    
	protected Method getConstructor(Class<?> type, int constructorId, Object[] args) throws SecurityException, NoSuchMethodException {
		for (Method c : type.getDeclaredMethods()) {
			com.bridj.ann.Constructor ca = c.getAnnotation(com.bridj.ann.Constructor.class);
			if (ca == null)
				continue;
			if (constructorId < 0) {
				Class<?>[] params = c.getParameterTypes();
				int n = params.length;
				if (n == args.length + 1) {
					boolean matches = true;
					for (int i = 0; i < n; i++) {
						Class<?> param = params[i];
						if (i == 0) {
							if (param != Long.TYPE) {
								matches = false;
								break;
							}
							continue;
						}
						Object arg = args[i - 1];
						if (arg == null && param.isPrimitive() || !param.isInstance(arg)) {
							matches = false;
							break;
						}
					}
					if (matches) 
						return c;
				}
			} else if (ca != null && ca.value() == constructorId)
				return c;
		}
		throw new NoSuchMethodException("Cannot find constructor with index " + constructorId);
	}
}
