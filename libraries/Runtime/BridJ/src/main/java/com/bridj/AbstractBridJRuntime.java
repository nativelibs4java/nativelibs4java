/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.bridj;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Olivier
 */
public abstract class AbstractBridJRuntime implements BridJRuntime {

	protected boolean log(Level level, String message, Throwable ex) {
		Logger.getLogger(getClass().getName()).log(level, message, ex);
		return true;
	}
	protected boolean log(Level level, String message) {
		return log(level, message, null);
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

    @Override
    public <T extends NativeObject> Class<? extends T> getTypeForCast(Class<T> type) {
        return type;
    }

    public <T extends NativeObject> T clone(T instance) throws CloneNotSupportedException {
    	if (instance == null)
    		return null;
    	throw new CloneNotSupportedException("Cannot clone instance of type " + instance.getClass().getName());
    }
}
