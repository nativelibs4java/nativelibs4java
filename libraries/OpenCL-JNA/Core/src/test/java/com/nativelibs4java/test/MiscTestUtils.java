/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.test;

import static org.junit.Assert.assertFalse;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.jna.Native;

/**
 *
 * @author ochafik
 */
public class MiscTestUtils {
	public static void protectJNI() {
            Native.setProtected(true);
        }
	public static void testGetters(Object instance) {
        if (instance == null)
            return;
		Logger log = Logger.getLogger(instance.getClass().getName());
		for (Method m : instance.getClass().getDeclaredMethods()) {
			if (Modifier.isStatic(m.getModifiers()))
				continue;
			if (!Modifier.isPublic(m.getModifiers()))
				continue;
			if (m.getParameterTypes().length != 0)
				continue;

			String name = m.getName();
			if (name.contains("ProfilingCommand"))
				continue;
			
			boolean isToString = name.equals("toString");
			if (name.startsWith("get") && name.length() > 3 ||
					name.startsWith("has") && name.length() > 3 ||
					name.startsWith("is") && name.length() > 2 ||
					isToString && !Modifier.isPublic(m.getDeclaringClass().getModifiers()))
			{
				String msg = "Failed to call " + m;
				try {
					m.invoke(instance);
				} catch (IllegalAccessException ex) {
					if (!isToString)
						log.log(Level.WARNING, msg, ex);
				} catch (InvocationTargetException ex) {
					Throwable cause = ex.getCause();
					if (!(cause instanceof UnsupportedOperationException)) {
						log.log(Level.SEVERE, msg, ex.getCause());
						assertFalse(msg, true);
					}
				} catch (Exception ex) {
					log.log(Level.SEVERE, msg, ex);
				}
			}
		}
	}
}
