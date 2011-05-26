/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.bridj.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.Method;
import java.io.*;
import java.nio.*;

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
    static volatile Method bufferIsDirectMethod;
    static volatile boolean hasBufferIsDirectMethod = true;
    public static boolean isDirect(Buffer b) {
    	if (b instanceof ByteBuffer)
    		return ((ByteBuffer)b).isDirect();
    	synchronized (Utils.class) {
    		if (hasBufferIsDirectMethod) {
			if (bufferIsDirectMethod == null) {
				try {
					bufferIsDirectMethod = Buffer.class.getMethod("isDirect");
				} catch (Throwable th) {
					hasBufferIsDirectMethod = false;
				}
			}
			if (bufferIsDirectMethod != null) {
				try {
					return (Boolean)bufferIsDirectMethod.invoke(b);
				} catch (Throwable th) {
				}
			}
		}
		return false;
    	}
    }

    public static String toString(Type t) {
    		if (t == null)
    			return "?";
    		if (t instanceof Class)
			return ((Class)t).getName();
		return t.toString();
	}
    public static String toString(Throwable th) {
    		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		th.printStackTrace(pw);
		return sw.toString();
	}
			
    public static boolean eq(Object a, Object b) {
        if ((a == null) != (b == null))
            return false;
        return !(a != null && !a.equals(b));
    }
    public static <T> Class<T> getClass(Type type) {
		if (type instanceof Class<?>)
			return (Class<T>)type;
		if (type instanceof ParameterizedType)
			return getClass(((ParameterizedType)type).getRawType());
		return null;
	}

    public static Type getParent(Type type) {
        if (type instanceof Class)
            return ((Class)type).getSuperclass();
        else
            // TODO handle templates !!!
            return getParent(getClass(type));
    }
	
}
