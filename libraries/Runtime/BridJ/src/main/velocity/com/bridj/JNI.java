package com.bridj;

import java.io.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.Buffer;
import static com.bridj.Dyncall.*;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JNI {
    private static boolean inited;
    static final String osName = System.getProperty("os.name", "");
    static final String BridJLibraryName = "bridj";
    
    public static int POINTER_SIZE, WCHAR_T_SIZE, SIZE_T_SIZE, CLONG_SIZE;
    static {
        try {
            initLibrary();
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }
    public static boolean isLinux() {
    	return isUnix() && osName.toLowerCase().contains("linux");
    }
    public static boolean isMacOSX() {
    	return isUnix() && (osName.startsWith("Mac") || osName.startsWith("Darwin"));
    }
    public static boolean isSolaris() {
    	return isUnix() && (osName.startsWith("SunOS") || osName.startsWith("Solaris"));
    }
    public static boolean isBSD() {
    	return isUnix() && (osName.contains("BSD") || isMacOSX());
    }
    public static boolean isUnix() {
    	return File.separatorChar == '/';
    }
    public static boolean isWindows() {
    	return File.separatorChar == '\\';
    }
    
    public static boolean isWindows7() {
    	return osName.equals("Windows 7");
    }
    public static Boolean is64Bits() {
    	String arch = System.getProperty("sun.arch.data.model");
        if (arch == null)
            arch = System.getProperty("os.arch");
        return
    		arch.contains("64") ||
    		arch.equalsIgnoreCase("sparcv9");
    }
    
    static String getEmbeddedLibraryResource(String name) {
    	if (isWindows())
    		return (is64Bits() ? "win64/" : "win32/") + name + ".dll";
    	if (isMacOSX())
    		return "darwin_universal/lib" + name + ".dylib";
    	if (isLinux())
    		return (is64Bits() ? "linux_x86_64/" : "linux_x86/") + name + ".so";
    	
    	throw new RuntimeException("Platform not supported ! (os.name='" + osName + "', os.arch='" + System.getProperty("os.arch") + "')");
    }
    public static File extractEmbeddedLibraryResource(String name) throws IOException {
    	String libraryResource = getEmbeddedLibraryResource(name);
        int i = libraryResource.lastIndexOf('.');
        String ext = i < 0 ? "" : libraryResource.substring(i);
        int len;
        byte[] b = new byte[8196];
        InputStream in = JNI.class.getClassLoader().getResourceAsStream(libraryResource);
        if (in == null) {
        	File f = new File(libraryResource);
        	if (!f.exists())
        		f = new File(f.getName());
        	if (f.exists())
        		return f.getCanonicalFile();
        //f = BridJ.getNativeLibraryFile(name);
        //    if (f.exists())
        //        return f.getCanonicalFile();
        	throw new FileNotFoundException(libraryResource);
        }
        File libFile = File.createTempFile(new File(libraryResource).getName(), ext);
        libFile.deleteOnExit();
        OutputStream out = new BufferedOutputStream(new FileOutputStream(libFile));
        while ((len = in.read(b)) > 0)
        	out.write(b, 0, len);
        out.close();
        in.close();
        
        return libFile;
    }
    public static void initLibrary() {
        if (inited)
            return;
		
        try {
	        File libFile = extractEmbeddedLibraryResource(BridJLibraryName);
	        //File libFile = BridJ.getNativeLibraryFile(BridJLibraryName);
	        System.load(libFile.toString());
	        //System.load("/Users/ochafik/nativelibs4java/Runtime/BridJ/src/main/cpp/bridj/build_out/darwin_universal_gcc_debug/libbridj.dylib");
	        
	        init();
	        POINTER_SIZE = sizeOf_ptrdiff_t();
	        WCHAR_T_SIZE = sizeOf_wchar_t();
	        CLONG_SIZE = sizeOf_long();
	        SIZE_T_SIZE = sizeOf_size_t();
	
	        inited = true;
        } catch (Throwable ex) {
        	throw new RuntimeException("Failed to initialize " + BridJ.class.getSimpleName(), ex);
        }
    }
    private static native void init();

    public static native int sizeOf_size_t();
    public static native int sizeOf_wchar_t();
    public static native int sizeOf_ptrdiff_t();
	public static native int sizeOf_long();

    public static native long loadLibrary(String path);
    public static native void freeLibrary(long libHandle);
    public static native long loadLibrarySymbols(long libHandle);
    public static native void freeLibrarySymbols(long symbolsHandle);
    public static native long findSymbolInLibrary(long libHandle, String name);
    public static native String[] getLibrarySymbols(long libHandle, long symbolsHandle);
    public static native String findSymbolName(long libHandle, long symbolsHandle, long address);

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
    protected static native ${prim.Name} get_${prim.Name}(long peer);
    @Deprecated
    protected static native ${prim.Name} get_${prim.Name}_disordered(long peer);
    @Deprecated
    protected static native void set_${prim.Name}(long peer, ${prim.Name} value);
	@Deprecated
    protected static native void set_${prim.Name}_disordered(long peer, ${prim.Name} value);

    @Deprecated
    protected static native ${prim.Name}[] get_${prim.Name}_array(long peer, int length);
    @Deprecated
    protected static native ${prim.Name}[] get_${prim.Name}_array_disordered(long peer, int length);
    @Deprecated
    protected static native void set_${prim.Name}_array(long peer, ${prim.Name}[] values, int valuesOffset, int length);
	@Deprecated
    protected static native void set_${prim.Name}_array_disordered(long peer, ${prim.Name}[] values, int valuesOffset, int length);
#end

	public static native void callDefaultCPPConstructor(long constructor, long thisPtr, int callMode);
	
	public static native long createCToJavaCallback(MethodCallInfo info);
	public static native long getActualCToJavaCallback(long handle);
	
	public static native long bindGetters(MethodCallInfo... infos);
	public static native long bindJavaMethodsToObjCMethods(MethodCallInfo... infos);
	public static native long bindJavaToCCallbacks(MethodCallInfo... infos);
	public static native long bindJavaMethodsToCFunctions(MethodCallInfo... infos);
	public static native long bindJavaMethodsToCPPMethods(MethodCallInfo... infos);
	public static native long bindJavaMethodsToVirtualMethods(MethodCallInfo... infos);
	
	public static native void freeGetters(long handle, int size);
	public static native void freeCToJavaCallback(long handle);
	public static native void freeObjCMethodBindings(long handle, int size);
	public static native void freeJavaToCCallbacks(long handle, int size);
	public static native void freeCPPMethodBindings(long handle, int size);
	public static native void freeCFunctionBindings(long handle, int size);
	public static native void freeVirtualMethodBindings(long handle, int size);
	
	public static native long createCallTempStruct();
	public static native void deleteCallTempStruct(long handle);
	public static native int getMaxDirectMappingArgCount();
	/*public static native long createCallback(
		int callbackType,
		Class<?> declaringClass,
		Callback javaCallbackInstance,
		Method method,
		boolean startsWithThis,
		String methodName,
		int callMode,
		long forwardedPointer, 
		int virtualTableOffset,
		int virtualIndex,
		boolean direct, 
		String javaSignature, 
		String dcSignature,
		int nParams,
		int returnValueType, 
		int paramsValueTypes[]
	);
	public static native void freeCallback(long nativeCallback);*/

	public static native long malloc(long size);
    public static native void free(long pointer);
    public static native long strlen(long pointer);
    public static native long wcslen(long pointer);
    public static native void memcpy(long dest, long source, long size);
    //public static native void wmemcpy(long dest, long source, long size);
    public static native void memmove(long dest, long source, long size);
    //public static native void wmemmove(long dest, long source, long size);
    public static native long memchr(long ptr, byte value, long num);
    public static native int memcmp(long ptr1, long ptr2, long num);
    public static native void memset(long ptr, byte value, long num);
}
