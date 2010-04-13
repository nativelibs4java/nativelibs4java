package com.bridj;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.bridj.Demangler.Symbol;
import com.bridj.ann.Library;
import java.util.Stack;
import java.io.PrintWriter;
import java.lang.reflect.Type;

/// http://www.codesourcery.com/public/cxx-abi/cxx-vtable-ex.html
public class BridJ {
	static Map<AnnotatedElement, NativeLibrary> librariesByClass = new HashMap<AnnotatedElement, NativeLibrary>();
	static Map<String, File> librariesFilesByName = new HashMap<String, File>();
	static Map<File, NativeLibrary> librariesByFile = new HashMap<File, NativeLibrary>();
	private static NativeEntities orphanEntities = new NativeEntities();
	static Map<Class<?>, BridJRuntime> registeredTypes = new HashMap<Class<?>, BridJRuntime>();
	static Map<Long, NativeObject> 
		strongNativeObjects = new HashMap<Long, NativeObject>(),
		weakNativeObjects = new WeakHashMap<Long, NativeObject>();

	static {
		java.lang.Runtime.getRuntime().addShutdownHook(new Thread() { public void run() {
			// The JVM being shut down doesn't mean the process is about to exit, so we need to clean our JNI mess
			//runShutdownHooks();
			releaseAll();
		}});
	}
	
	static synchronized void registerNativeObject(NativeObject ob) {
		weakNativeObjects.put(Pointer.getAddress(ob, null), ob);
	}
	/// Caller should display message such as "target was GC'ed. You might need to add a BridJ.protectFromGC(NativeObject), BridJ.unprotectFromGC(NativeObject)
	static synchronized NativeObject getNativeObject(long peer) {
		NativeObject ob = weakNativeObjects.get(peer);
		if (ob == null)
			ob = strongNativeObjects.get(peer);
		return ob;
	}
	static synchronized void unregisterNativeObject(NativeObject ob) {
		long peer = Pointer.getAddress(ob, null);
		weakNativeObjects.remove(peer);
		strongNativeObjects.remove(peer);
	}
	public static synchronized void protectFromGC(NativeObject ob) {
		long peer = Pointer.getAddress(ob, null);
		if (weakNativeObjects.remove(peer) != null)
			strongNativeObjects.put(peer, ob);
	}
	public static synchronized void unprotectFromGC(NativeObject ob) {
		long peer = Pointer.getAddress(ob, null);
		if (strongNativeObjects.remove(peer) != null)
			weakNativeObjects.put(peer, ob);
	}
	
	
	static boolean hasThisAsFirstArgument(Method method) {//, boolean checkConsistency) {
		return method.getAnnotation(com.bridj.ann.Constructor.class) != null;
//		return hasThisAsFirstArgument(method.getParameterTypes(), method.getParameterAnnotations(), checkConsistency);
	}
//	static boolean hasThisAsFirstArgument(Class<?>[] paramTypes, Annotation[][] anns, boolean checkConsistency) {
//		boolean hasThis = false;
//		int len = anns.length;
//        if (len > 0) {
//        	for (int i = 0; i < len; i++) {
//        		for (Annotation ann : anns[i]) {
//	        		if (ann instanceof This) {
//	        			hasThis = true;
//	        			if (!checkConsistency)
//	        				return true;
//	        			if (paramTypes[0] != Long.TYPE)
//	        				throw new RuntimeException("First parameter with annotation " + This.class.getName() + " must be of type long, but is of type " + paramTypes[0].getName() + ".");
//	        		}
//	    		}
//        		if (i == 0 && !checkConsistency)
//        			return false;
//        	}
//        }
//        return hasThis;
//	}
//	
	public static void deallocate(NativeObject nativeObject) {
		unregisterNativeObject(nativeObject);
		//TODO call destructor !!! 
		Pointer.getPeer(nativeObject, null).release();
	}
	
	/**
	 * Registers the class of the caller
	 */
    public static synchronized void register() {
		StackTraceElement[] stackTrace = new Exception().getStackTrace();
    	if (stackTrace.length < 2)
    		throw new RuntimeException("No useful stack trace : cannot register with register(), please use register(Class) instead.");
    	String name = stackTrace[1].getClassName();
    	try {
    		Class<?> type = Class.forName(name);
    		register(type);
    	} catch (Exception ex) {
    		throw new RuntimeException("Failed to register class " + name, ex);
    	}
    }
    
    
	static ThreadLocal<Stack<Boolean>> currentlyCastingNativeObject = new ThreadLocal<Stack<Boolean>>() {
        @Override
		protected java.util.Stack<Boolean> initialValue() {
			Stack<Boolean> s = new Stack<Boolean>();
			s.push(false);
			return s;
		};
	};
	
	static boolean isCastingNativeObjectInCurrentThread() {
		return currentlyCastingNativeObject.get().peek();
	}

//    static CRuntime cRuntime;
//    static synchronized CRuntime getCRuntime() {
//        if (cRuntime == null)
//            cRuntime = getRuntimeByRuntimeClass(CRuntime.class);
//        return cRuntime;
//    }
	public static <O extends NativeObject> O createNativeObjectFromPointer(Pointer<? super O> pointer, Type type) {
		Stack<Boolean> s = currentlyCastingNativeObject.get();
		s.push(true);
		Class<O> cl = type instanceof Class ? (Class<O>)type : (Class<O>)((ParameterizedType)type).getRawType();
		try {
			BridJRuntime runtime = getRuntime(cl); 
            O instance = runtime.getTypeForCast(cl).newInstance(); // TODO template parameters here !!!
			instance.peer = (Pointer)pointer;//offset(byteOffset);//Pointer.pointerToAddress(address, type);
			return instance;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to cast pointer to native object of type " + cl.getName(), ex);
		} finally {
			s.pop();
		}
	}

    private static Map<Class<? extends BridJRuntime>, BridJRuntime> runtimes = new HashMap<Class<? extends BridJRuntime>, BridJRuntime>();
    
    static synchronized <R extends BridJRuntime> R getRuntimeByRuntimeClass(Class<R> runtimeClass) {
    	R r = (R)runtimes.get(runtimeClass);
    	if (r == null)
			try {
				runtimes.put(runtimeClass, r = runtimeClass.newInstance());
			} catch (Exception e) {
				throw new RuntimeException("Failed to instantiate runtime " + runtimeClass.getName(), e);
			}
    	
    	return r;
    }

    static BridJRuntime getRuntime(Class<?> type) {
        return getRuntime(type, true);
    }
    private static BridJRuntime getRuntime(Class<?> type, boolean checkInRegisteredTypes) {

		BridJRuntime runtime = null;
        if (checkInRegisteredTypes) {
            runtime = registeredTypes.get(type);
            if (runtime != null)
                return runtime;
        }

		com.bridj.ann.Runtime runtimeAnn = getAnnotation(com.bridj.ann.Runtime.class, true, type);
		if (runtimeAnn == null)
            //return getCRuntime();
			throw new IllegalArgumentException("Class " + type.getName() + " has no " + com.bridj.ann.Runtime.class.getName() + " annotation. Unable to guess the corresponding " + BridJRuntime.class.getName() + " implementation.");

		return getRuntimeByRuntimeClass(runtimeAnn.value());
    }

	public static BridJRuntime register(Class<?> type) {
		BridJRuntime runtime = registeredTypes.get(type);
		if (runtime == null)
			runtime = getRuntime(type, false);
		
		runtime.register(type);
		registeredTypes.put(type, runtime);
		return runtime;
	}
	static boolean log(Level level, String message, Throwable ex) {
		Logger.getLogger(BridJ.class.getName()).log(level, message, ex);
		return true;
	}
	static boolean log(Level level, String message) {
		log(level, message, null);
		return true;
	}
	public static synchronized NativeEntities getNativeEntities(AnnotatedElement type) throws FileNotFoundException {
		NativeLibrary lib = getNativeLibrary(type);
		if (lib != null)
			return lib.getNativeEntities();
		return getOrphanEntities();
	}
	public static synchronized NativeLibrary getNativeLibrary(AnnotatedElement type) throws FileNotFoundException {
		NativeLibrary lib = librariesByClass.get(type);
		if (lib == null) {
			String name = getNativeLibraryName(type);
			lib = getNativeLibrary(name);
			if (lib != null)
				librariesByClass.put(type, lib);
		}
		return lib;
	}
	
	/**
	 * Reclaims all the memory allocated by BridJ in the JVM and on the native side.
	 * This is automatically called at shutdown time.
	 */
	public synchronized static void releaseAll() {
		strongNativeObjects.clear();
		weakNativeObjects.clear();
		System.gc();
		
		for (NativeLibrary lib : librariesByFile.values())
			lib.release();
		librariesByFile.clear();
		librariesByClass.clear();
		getOrphanEntities().release();
		System.gc();
	}
	//public synchronized static void release(Class<?>);
	public synchronized static void releaseLibrary(String name) {
		File file = librariesFilesByName.remove(name);
		if (file != null)
			releaseLibrary(file);
	}
	public synchronized static void releaseLibrary(File library) {
		NativeLibrary lib = librariesByFile.remove(library);
		if (lib != null)
			lib.release();
	}
//	
//	public static void register(Class<?> type) {
//		try {
//			String libraryName = getLibrary(type);
//			NativeLibrary library = getLibHandle(libraryName);
//			library.register(type);
//		} catch (FileNotFoundException ex) {
//			throw new RuntimeException("Failed to register class " + type.getName(), ex);
//		}
//	}
//

    static Map<String, NativeLibrary> libHandles = new HashMap<String, NativeLibrary>();
    static List<String> paths;
    static synchronized List<String> getNativeLibraryPaths() {
        if (paths == null) {
            paths = new ArrayList<String>();
            paths.add(".");
			String env;
			env = System.getenv("LD_LIBRARY_PATH");
            if (env != null)
                paths.addAll(Arrays.asList(env.split(File.pathSeparator)));
            env = System.getenv("DYLD_LIBRARY_PATH");
            if (env != null)
                paths.addAll(Arrays.asList(env.split(File.pathSeparator)));
            env = System.getenv("PATH");
            if (env != null)
                paths.addAll(Arrays.asList(env.split(File.pathSeparator)));
            env = System.getProperty("java.library.path");
            if (env != null)
                paths.addAll(Arrays.asList(env.split(File.pathSeparator)));
        }
        return paths;
    }

    public static File getNativeLibraryFile(String name) {
        if (name == null)
            return null;
        for (String path : getNativeLibraryPaths()) {
            File pathFile;
            try {
                pathFile = new File(path).getCanonicalFile();
            } catch (IOException ex) {
                log(Level.SEVERE, null, ex);
                continue;
            }
            File f = new File(name);
            if (JNI.isWindows()) {
				if (!f.exists())
					f = new File(pathFile, name + ".dll").getAbsoluteFile();
			} else if (JNI.isMacOSX()) {
				if (!f.exists())
					f = new File(pathFile, "lib" + name + ".dylib").getAbsoluteFile();
				if (!f.exists())
					f = new File(pathFile, "lib" + name + ".jnilib").getAbsoluteFile();
			} else if (JNI.isUnix()) {
				if (!f.exists())
					f = new File(pathFile, "lib" + name + ".so").getAbsoluteFile();
				if (!f.exists())
					f = new File(pathFile, name + ".so").getAbsoluteFile();
            }
            
            if (!f.exists())
                continue;

            try {
                return f.getCanonicalFile();
            } catch (IOException ex) {
                log(Level.SEVERE, null, ex);
            }
        }
        if (JNI.isMacOSX()) {
        	for (String s : new String[] { "/System/Library/Frameworks", new File(System.getProperty("user.home"), "Library/Frameworks").toString() }) {
        		try {
					File f = new File(new File(s, name + ".framework"), name);
					if (f.exists() && !f.isDirectory())
						return f.getCanonicalFile();
        		} catch (IOException ex) {
					return null;
				} 
        	}
        }
        try {
        	return JNI.extractEmbeddedLibraryResource(name);
        } catch (IOException ex) {
        	return null;
        }
    }
    public static synchronized NativeLibrary getNativeLibrary(String name) throws FileNotFoundException {
        if (name == null)
            return null;
        
        NativeLibrary l = libHandles.get(name);
        if (l != null)
            return l;

        File f = getNativeLibraryFile(name);
        if (f == null)
        	throw new FileNotFoundException("Couldn't find library file for library '" + name + "'");
        
        return getNativeLibrary(name, f);
    }
    public static NativeLibrary getNativeLibrary(String name, File f) throws FileNotFoundException {
		NativeLibrary ll = NativeLibrary.load(f.toString());
        if (ll == null)
            throw new FileNotFoundException("Library '" + name + "' was not found in path '" + getNativeLibraryPaths() + "'");
        libHandles.put(name, ll);
        return ll;
    }
    public static String getNativeLibraryName(AnnotatedElement m) {
        Library lib = getAnnotation(Library.class, true, m);
        return lib == null ? null : lib.value(); // TODO use package as last resort
    }
    static <A extends Annotation> A getAnnotation(Class<A> ac, boolean inherit, AnnotatedElement m, Annotation... directAnnotations) {
        if (directAnnotations != null)
            for (Annotation ann : directAnnotations)
                if (ac.isInstance(ann))
                    return ac.cast(ann);
            
        if (m == null)
            return null;
        A a = m.getAnnotation(ac);
        if (a != null)
            return a;

        if (inherit) {
	        if (m instanceof Member)
	            return getAnnotation(ac, inherit, ((Member)m).getDeclaringClass());
	
	        if (m instanceof Class<?>) {
	            Class<?> c = (Class<?>)m, dc = c.getDeclaringClass();
	            if (dc != null)
	                return getAnnotation(ac, inherit, dc);
	        }
        }
        return null;
    }
	public static Symbol getSymbolByAddress(long peer) {
		for (NativeLibrary lib : libHandles.values()) {
			Symbol symbol = lib.getSymbol(peer);
			if (symbol != null)
				return symbol;
		}
		return null;
	}
	public static void setOrphanEntities(NativeEntities orphanEntities) {
		BridJ.orphanEntities = orphanEntities;
	}
	public static NativeEntities getOrphanEntities() {
		return orphanEntities;
	}

    static void initialize(NativeObject instance) {
        (instance.runtime = register(instance.getClass())).initialize(instance);
    }


    static void initialize(NativeObject instance, Pointer peer) {
        (instance.runtime = register(instance.getClass())).initialize(instance, peer);
    }



    static void initialize(NativeObject instance, int constructorId, Object[] args) {
        (instance.runtime = register(instance.getClass())).initialize(instance, constructorId, args);
    }
	public static <T extends NativeObject> T clone(T instance) throws CloneNotSupportedException {
		return instance.runtime.clone(instance);
	}
	
	public static void main(String[] args) {
		List<NativeLibrary> libraries = new ArrayList<NativeLibrary>();
		try {
			for (String arg : args) {
				NativeLibrary lib = getNativeLibrary(arg);
				libraries.add(lib);
			}
			String file = "out.h";
			PrintWriter out = new PrintWriter(new File(file));
			HeadersReconstructor.reconstructHeaders(libraries, out);
			out.close();
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}
}
