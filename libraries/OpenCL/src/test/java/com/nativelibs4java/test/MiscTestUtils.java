/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.test;

import com.nativelibs4java.opencl.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.junit.Assert.*;

/**
 *
 * @author ochafik
 */
public class MiscTestUtils {
	
	public static void testGetters(Object instance) {
		Logger log = Logger.getLogger(instance.getClass().getName());
		for (Method m : instance.getClass().getDeclaredMethods()) {
			if (Modifier.isStatic(m.getModifiers()))
				continue;
			if (!Modifier.isPublic(m.getModifiers()))
				continue;
			if (m.getParameterTypes().length != 0)
				continue;

			String name = m.getName();
			//if (name.contains("Profili"))
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
					log.log(Level.SEVERE, msg, ex.getCause());
					assertFalse(msg, true);
				} catch (Exception ex) {
					log.log(Level.SEVERE, msg, ex);
				}
			}
		}
	}
}
