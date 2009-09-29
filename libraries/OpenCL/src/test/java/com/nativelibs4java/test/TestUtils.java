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
public class TestUtils {
	
	public static void testGetters(Object instance) {
		for (Method m : instance.getClass().getDeclaredMethods()) {
			if (m.getParameterTypes().length > 0)
				continue;
			if (Modifier.isStatic(m.getModifiers()))
				continue;
			String name = m.getName();
			if (name.startsWith("get") && name.length() > 3 ||
					name.startsWith("has") && name.length() > 3 ||
					name.startsWith("is") && name.length() > 2 ||
					name.equals("toString"))
			{
				try {
					m.invoke(instance);
				} catch (IllegalAccessException ex) {
					Logger.getLogger(InfoGettersTest.class.getName()).log(Level.SEVERE, null, ex);
				} catch (IllegalArgumentException ex) {
					Logger.getLogger(InfoGettersTest.class.getName()).log(Level.SEVERE, null, ex);
				} catch (InvocationTargetException ex) {
					String msg = "Failed to call " + m;
					Logger.getLogger(instance.getClass().getName()).log(Level.SEVERE, msg, ex.getCause());
					assertFalse(msg, true);
				}
			}
		}
	}
}
