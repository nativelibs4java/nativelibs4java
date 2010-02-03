/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.runtime;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Olivier
 */
public class NativeLib<L> {

    private long libraryHandle;

    public NativeLib(Class<L> libraryClass) throws FileNotFoundException {
        this(DynCall.getLibFile(libraryClass));
    }
    public NativeLib(String libraryName) {
		this(DynCall.getLibFile(libraryName));
	}
    
	public NativeLib(File libraryFile) {//, LibraryOptions options) {
		libraryHandle = JNI.loadLibrary(libraryFile.getAbsolutePath());
		if (libraryHandle == 0)
			throw new UnsatisfiedLinkError("Could not load library '" + libraryFile + "'");

		List<MethodCallInfo> methodInfos = new ArrayList<MethodCallInfo>();
		for (Class<?> c = getClass(); c != NativeLib.class; c = c.getSuperclass()) {
			for (Method method : c.getDeclaredMethods()) {
				int modifiers = method.getModifiers();
				if (Modifier.isNative(modifiers) && !Modifier.isStatic(modifiers)) {
                    methodInfos.add(new MethodCallInfo(method, libraryHandle));
				}
			}
		}
		//Method[] nativeMethodsArray = nativeMethods.toArray(new Method[nativeMethods.size()]);
		//MethodCallInfo[] methodInfosArray = methodInfos.toArray(new MethodCallInfo[methodInfos.size()]);
		nativeCallbacks = JNI.createCallbacks(methodInfos);
	}
	long[] nativeCallbacks;

	public void finalize() {
		JNI.freeLibrary(libraryHandle);
        JNI.freeCallbacks(nativeCallbacks);
	}

	//public static long getSymbolAddress(NL4JLibrary library, String symbolName, boolean allowUndecoratedSearch);
	//public static long getDecoratedSymbolAddress(NL4JLibrary library, String symbolName);
}
