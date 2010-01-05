/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.runtime;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Olivier
 */
public class NativeLib {

    private long libraryHandle;

    public NativeLib(String libraryName) {
		this(DynCall.getLibFile(libraryName));
	}
    
	public NativeLib(File libraryFile) {//, LibraryOptions options) {
		libraryHandle = JNI.loadLibrary(libraryFile.getAbsolutePath());
		if (libraryHandle == 0)
			throw new UnsatisfiedLinkError("Could not load library '" + libraryFile + "'");

		List<Method> nativeMethods = new ArrayList<Method>();
		List<MethodCallInfo> methodInfos = new ArrayList<MethodCallInfo>();
		//List<Long> callbackPointers = new ArrayList<Method>();
		for (Class<?> c = getClass(); c != NativeLib.class; c = c.getSuperclass()) {
			for (Method method : c.getDeclaredMethods()) {
				int modifiers = method.getModifiers();
				if (Modifier.isNative(modifiers) && !Modifier.isStatic(modifiers)) {
                    nativeMethods.add(method);
                    methodInfos.add(new MethodCallInfo(method));
				}
			}
		}
		Method[] nativeMethodsArray = nativeMethods.toArray(new Method[nativeMethods.size()]);
		MethodCallInfo[] methodInfosArray = methodInfos.toArray(new MethodCallInfo[methodInfos.size()]);
		nativeCallbacks = JNI.createCallbacks(nativeMethodsArray, methodInfosArray);
	}
	long[] nativeCallbacks;

	public void finalize() {
		JNI.freeLibrary(libraryHandle);
        JNI.freeCallbacks(nativeCallbacks);
	}

	//public static long getSymbolAddress(NL4JLibrary library, String symbolName, boolean allowUndecoratedSearch);
	//public static long getDecoratedSymbolAddress(NL4JLibrary library, String symbolName);
}
