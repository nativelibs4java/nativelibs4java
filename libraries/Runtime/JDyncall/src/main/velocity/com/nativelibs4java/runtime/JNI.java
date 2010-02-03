package com.nativelibs4java.runtime;

import java.io.FileNotFoundException;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.nativelibs4java.runtime.MethodCallInfo;

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

    public static native long loadLibrary(String path);
    public static native void freeLibrary(long libHandle);
    public static native long findSymbolInLibrary(long libHandle, String name);

	public static native long newGlobalRef(Object object);
	public static native void deleteGlobalRef(long reference);

	public static native long newWeakGlobalRef(Object object);
	public static native void deleteWeakGlobalRef(long reference);

    public static native ByteBuffer newDirectByteBuffer(long address, long capacity);
    public static native long getDirectBufferAddress(Buffer b);
    public static native long getDirectBufferCapacity(Buffer b);

#foreach ($prim in $primitivesNoBool)

    @Deprecated
    public static native long get${prim.WrapperName}ArrayElements(${prim.Name}[] array, boolean[] pIsCopy);
    @Deprecated
    public static native void release${prim.WrapperName}ArrayElements(${prim.Name}[] array, long pointer, int mode);

    @Deprecated
    protected static native ${prim.Name} get_${prim.Name}(long peer, byte endianness);
    @Deprecated
    protected static native void set_${prim.Name}(long peer, ${prim.Name} value, byte endianness);

    @Deprecated
    protected static native ${prim.Name}[] get_${prim.Name}_array(long peer, int length, byte endianness);
    @Deprecated
    protected static native void set_${prim.Name}_array(long peer, ${prim.Name}[] values, int valuesOffset, int length, byte endianness);
#end

	public static long[] createCallbacks(List<MethodCallInfo> methodInfos) {
		long[] ret = new long[methodInfos.size()];
		for (int i = 0, n = methodInfos.size(); i < n; i++) {
			MethodCallInfo info = methodInfos.get(i);
			ret[i] = createCallback(
				info.method.getDeclaringClass(),
				info.method.getName(),
				0,
				info.forwardedPointer, 
				info.direct, 
				info.getJavaSignature(), 
				info.getDcSignature(),
				info.paramsValueTypes.length,
				info.returnValueType,
				info.paramsValueTypes
			);
		}
		return ret;
	}
	public static void freeCallbacks(long[] nativeCallbacks) {
		for (int i = 0, n = nativeCallbacks.length; i < n; i++)
			freeCallback(nativeCallbacks[i]);
	}
	
	public static native int getMaxDirectMappingArgCount();
	public static native long createCallback(
		Class<?> declaringClass,
		String methodName,
		int callMode,
		long forwardedPointer, 
		boolean direct, 
		String javaSignature, 
		String dcSignature,
		int nParams,
		int returnValueType, 
		int paramsValueTypes[]
	);
	public static native void freeCallback(long nativeCallback);

	public static native long malloc(long size);
    public static native void free(long pointer);
    public static native long strlen(long pointer);
    public static native long wcslen(long pointer);
    public static native void memcpy(long dest, long source, long size);
    public static native void wmemcpy(long dest, long source, long size);
    public static native void memmove(long dest, long source, long size);
    public static native void wmemmove(long dest, long source, long size);
    public static native long memchr(long ptr, byte value, long num);
    public static native int memcmp(long ptr1, long ptr2, long num);
    public static native void memset(long ptr, byte value, long num);
}
