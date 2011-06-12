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

/**
 * Low-level calls to JNI and to BridJ's native library.
 * @author ochafik
 * @deprecated These methods can cause serious issues (segmentation fault, system crashes) if used without care : there are little to no checks performed on the arguments.
 */
@Deprecated
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
        		boolean loaded = false;
	        		
        		String forceLibFile = System.getenv("BRIDJ_LIBRARY");
        		if (forceLibFile == null)
        			forceLibFile = System.getProperty("bridj.library");
        		
        		String lib = null;
        		
        		if (forceLibFile != null) {
        			try {
        				System.load(lib = forceLibFile);
        				loaded = true;
        			} catch (Exception ex) {
        				BridJ.log(Level.SEVERE, "Failed to load forced library " + forceLibFile, ex);
        			}
        		}
        			
        		if (!loaded) {
        			if (Platform.isAndroid()) {
	        			try {
						System.loadLibrary(lib = "bridj_android");
						loaded = true;
					} catch (Exception ex) {
						try {
							System.loadLibrary(lib = "bridj");
							loaded = true;
						} catch (Exception ex2) {}
					}
				}
				if (!loaded) {
					File libFile = null;
					try {
						libFile = extractEmbeddedLibraryResource(BridJLibraryName);
						BridJ.log(Level.INFO, "Loading library " + libFile);
						System.load(lib = libFile.toString());
	        				loaded = true;
					} catch (IOException ex) {
						// Failed to extract embedded library resource : just try again in the path...
						System.loadLibrary("bridj");
					}
	        		}
	        }
	        BridJ.log(Level.INFO, "Loaded library " + lib);
	        
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
    static native long loadLibrarySymbols(String libPath);
    static native void freeLibrarySymbols(long symbolsHandle);
    static native long findSymbolInLibrary(long libHandle, String name);
    static native String[] getLibrarySymbols(long libHandle, long symbolsHandle);
    static native String findSymbolName(long libHandle, long symbolsHandle, long address);

    /**
     * Create a JNI global reference to a Java object : long value that can be safely passed to C programs and stored, which prevent the object from being garbage-collected and which validity runs until {@link JNI#deleteGlobalRef(long)} is called
     */
	public static native long newGlobalRef(Object object);
	/**
     * Delete a global reference created by {@link JNI#newGlobalRef(java.lang.Object)}
     */
	public static native void deleteGlobalRef(long reference);
    
	/**
     * Create a JNI weak global reference to a Java object : long value that can be safely passed to C programs and stored, which validity runs until {@link JNI#deleteWeakGlobalRef(long)} is called.<br>
     * Unlike global references, weak global references don't prevent objects from being garbage-collected.
     */
	public static native long newWeakGlobalRef(Object object);
	/**
     * Delete a weak global reference created by {@link JNI#newWeakGlobalRef(java.lang.Object)}
     */
	public static native void deleteWeakGlobalRef(long reference);

    /**
     * Wrap a native address as a direct byte buffer of the specified byte capacity.<br>
     * Memory is not reclaimed when the buffer is garbage-collected.
     */
    public static native ByteBuffer newDirectByteBuffer(long address, long capacity);
    /**
     * Get the native address pointed to by a direct buffer.
     */
    public static native long getDirectBufferAddress(Buffer b);
    /**
     * Get the capacity in bytes of a direct buffer.
     */
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
