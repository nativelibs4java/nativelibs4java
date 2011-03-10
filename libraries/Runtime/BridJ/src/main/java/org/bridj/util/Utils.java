/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.bridj.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Miscellaneous utility methods.
 * @author ochafik
 */
public class Utils {
    public static int getEnclosedConstructorParametersOffset(Constructor c) {
        Class<?> enclosingClass = c.getDeclaringClass().getEnclosingClass();
        Class[] params = c.getParameterTypes();
        int overrideOffset = params.length > 0 && enclosingClass != null && enclosingClass == params[0] ? 1 : 0;
        return overrideOffset;
    }

    public static <T> Class<T> getClass(Type type) {
		if (type instanceof Class<?>)
			return (Class<T>)type;
		if (type instanceof ParameterizedType)
			return getClass(((ParameterizedType)type).getRawType());
		return null;
	}
	
}
