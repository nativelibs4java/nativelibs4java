/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.bridj;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for implementation of runtimes
 * @author Olivier
 */
public abstract class AbstractBridJRuntime implements BridJRuntime {
    protected boolean log(Level level, String message, Throwable ex) {
        if (!BridJ.shouldLog(level))
            return true;
        
		Logger.getLogger(getClass().getName()).log(level, message, ex);
		return true;
	}
	protected boolean log(Level level, String message) {
		return log(level, message, null);
	}
	@Override
	public void unregister(Type type) {
		// TODO !!!
	}
    protected java.lang.reflect.Constructor findConstructor(Class<?> type, int constructorId) throws SecurityException, NoSuchMethodException {
		for (java.lang.reflect.Constructor<?> c : type.getDeclaredConstructors()) {
            org.bridj.ann.Constructor ca = c.getAnnotation(org.bridj.ann.Constructor.class);
			if (ca == null)
				continue;
            if (ca.value() == constructorId)
                return c;
        }
        if (constructorId < 0)// && args.length == 0)
            return type.getConstructor();
		throw new NoSuchMethodException("Cannot find constructor with index " + constructorId);
	}

}
