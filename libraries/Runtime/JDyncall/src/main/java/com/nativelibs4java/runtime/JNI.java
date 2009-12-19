package com.nativelibs4java.runtime;

import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JNI {
    private static boolean inited;
    
    public static int POINTER_SIZE, WCHAR_T_SIZE, SIZE_T_SIZE;
    static {
        try {
            initLibrary();
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }
    public static void initLibrary() {
        if (inited)
            return;
		
		String f = com.nativelibs4java.runtime.DynCall.getLibFile("jdyncall").toString();
        System.load(f);
        //System.load("C:\\Prog\\dyncall\\dyncall\\buildsys\\vs2008\\Debug\\jdyncall.dll");
        //System.load("c:\\Users\\Olivier\\Prog\\dyncall\\dyncall\\buildsys\\vs2008\\x64\\Debug\\jdyncall.dll");

        init();
        POINTER_SIZE = sizeOf_ptrdiff_t();
        WCHAR_T_SIZE = sizeOf_wchar_t();
        SIZE_T_SIZE = sizeOf_size_t();

        inited = true;
    }
    private static native void init();

    public static native int sizeOf_size_t();
    public static native int sizeOf_wchar_t();
    public static native int sizeOf_ptrdiff_t();

    public static native long getObjectPointer(Object object);
    public static native void registerMethod(Class<?> declaringClass, Method method, long functionHandle);
    public static void registerClass(Class<?> declaringClass) {
        for (Method method : declaringClass.getDeclaredMethods()) {
            try {
                int modifiers = method.getModifiers();
                if (!Modifier.isNative(modifiers)) {
                    continue;
                }
                if (Modifier.isStatic(modifiers)) {
                }
                long functionHandle = DynCall.getSymbolAddress(method);
                if (functionHandle == 0) {
                    throw new UnsatisfiedLinkError("Failed to find symbol " + method.getName());// + " in " + declaringClass.getName());
                }
                registerMethod(declaringClass, method, functionHandle);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(JNI.class.getName()).log(Level.SEVERE, null, ex);
                throw new UnsatisfiedLinkError(ex.toString());
            }
        }
        for (Class<?> inner : declaringClass.getDeclaredClasses())
            registerClass(inner);
    }

    public static native long loadLibrary(String path);
    public static native void freeLibrary(long libHandle);
    public static native long findSymbolInLibrary(long libHandle, String name);
}
