package org.bridj;

import java.io.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.Buffer;
import static org.bridj.Dyncall.*;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.bridj.Platform.*;

public class JNI {
    private static boolean inited;
    static final String BridJLibraryName = "bridj";
    
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
		
        try {
        		String forceLibFile = System.getenv("FORCE_BRIDJ_LIBRARY");
	        File libFile = forceLibFile == null ? 
				extractEmbeddedLibraryResource(BridJLibraryName) :
				new File(forceLibFile);
	        //File libFile = new File("C:\\Users\\Olivier\\Prog\\nativelibs4java\\Runtime\\BridJ\\src\\main\\cpp\\buildsys\\vs2008\\x64\\Debug\\bridj.dll");
	        //File libFile = BridJ.getNativeLibraryFile(BridJLibraryName);
	        BridJ.log(Level.INFO, "Loading library " + libFile);
	        System.load(libFile.toString());
	        //System.load("/Users/ochafik/nativelibs4java/Runtime/BridJ/src/main/cpp/bridj/build_out/darwin_universal_gcc_debug/libbridj.dylib");
	        
	        
	        init();
	        inited = true;
        } catch (Throwable ex) {
        	throw new RuntimeException("Failed to initialize " + BridJ.class.getSimpleName(), ex);
        }
    }
    private static native void init();

    static native int sizeOf_size_t();
    static native int sizeOf_wchar_t();
    static native int sizeOf_ptrdiff_t();
	static native int sizeOf_long();

    @Deprecated
    public static native long getEnv();
    static native long loadLibrary(String path);
    static native void freeLibrary(long libHandle);
    static native long loadLibrarySymbols(long libHandle);
    static native void freeLibrarySymbols(long symbolsHandle);
    static native long findSymbolInLibrary(long libHandle, String name);
    static native String[] getLibrarySymbols(long libHandle, long symbolsHandle);
    static native String findSymbolName(long libHandle, long symbolsHandle, long address);
    
	public static native long newGlobalRef(Object object);
	public static native void deleteGlobalRef(long reference);
    
	public static native long newWeakGlobalRef(Object object);
	public static native void deleteWeakGlobalRef(long reference);
    
    public static native ByteBuffer newDirectByteBuffer(long address, long capacity);
    public static native long getDirectBufferAddress(Buffer b);
    public static native long getDirectBufferCapacity(Buffer b);

#foreach ($prim in $primitives)

    @Deprecated
    static native long get${prim.WrapperName}ArrayElements(${prim.Name}[] array, boolean[] pIsCopy);
    @Deprecated
    static native void release${prim.WrapperName}ArrayElements(${prim.Name}[] array, long pointer, int mode);

    @Deprecated
    static native ${prim.Name} get_${prim.Name}(long peer);
    @Deprecated
    static native void set_${prim.Name}(long peer, ${prim.Name} value);
    @Deprecated
    static native ${prim.Name}[] get_${prim.Name}_array(long peer, int length);
    @Deprecated
    static native void set_${prim.Name}_array(long peer, ${prim.Name}[] values, int valuesOffset, int length);

	#if ($prim.Name != "byte" && $prim.Name != "boolean")
	@Deprecated
    static native ${prim.Name} get_${prim.Name}_disordered(long peer);
	@Deprecated
    static native void set_${prim.Name}_disordered(long peer, ${prim.Name} value);
    @Deprecated
    static native ${prim.Name}[] get_${prim.Name}_array_disordered(long peer, int length);
	@Deprecated
    static native void set_${prim.Name}_array_disordered(long peer, ${prim.Name}[] values, int valuesOffset, int length);
	#end
#end

	public static native void callSinglePointerArgVoidFunction(long functionPointer, long pointerArg, int callMode);
	
	static native long createCToJavaCallback(MethodCallInfo info);
	static native long getActualCToJavaCallback(long handle);
	
	static native long bindJavaMethodsToObjCMethods(MethodCallInfo... infos);
	static native long bindJavaToCCallbacks(MethodCallInfo... infos);
	static native long bindJavaMethodsToCFunctions(MethodCallInfo... infos);
	static native long bindJavaMethodsToVirtualMethods(MethodCallInfo... infos);
	
	static native void freeCToJavaCallback(long handle);
	static native void freeObjCMethodBindings(long handle, int size);
	static native void freeJavaToCCallbacks(long handle, int size);
	static native void freeCFunctionBindings(long handle, int size);
	static native void freeVirtualMethodBindings(long handle, int size);
	
	static native long createCallTempStruct();
	static native void deleteCallTempStruct(long handle);
	static native int getMaxDirectMappingArgCount();
	
	static native long mallocNulled(long size);
	static native long malloc(long size);
    static native void free(long pointer);
    static native long strlen(long pointer);
    static native long wcslen(long pointer);
    static native void memcpy(long dest, long source, long size);
    static native void memmove(long dest, long source, long size);
    static native long memchr(long ptr, byte value, long num);
    static native int memcmp(long ptr1, long ptr2, long num);
    static native void memset(long ptr, byte value, long num);
}
