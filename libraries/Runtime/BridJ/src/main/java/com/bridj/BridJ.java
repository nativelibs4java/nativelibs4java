
package com.bridj;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.bridj.ann.Library;
import com.bridj.ann.Mangling;
import com.bridj.ann.NoInheritance;
import com.bridj.ann.This;
import com.bridj.ann.Virtual;

/// http://www.codesourcery.com/public/cxx-abi/cxx-vtable-ex.html
public class BridJ {
	static Map<AnnotatedElement, NativeLibrary> librariesByClass = new HashMap<AnnotatedElement, NativeLibrary>();
	static Map<String, File> librariesFilesByName = new HashMap<String, File>();
	static Map<File, NativeLibrary> librariesByFile = new HashMap<File, NativeLibrary>();
	static NativeEntities orphanEntities = new NativeEntities();
	static Set<Class<?>> registeredTypes = new HashSet<Class<?>>();
	
	static {
		Runtime.getRuntime().addShutdownHook(new Thread() { public void run() {
			// The JVM being shut down doesn't mean the process is about to exit, so we need to clean our JNI mess
			//runShutdownHooks();
			releaseAll();
		}});
	}
	public static synchronized void register(Class<?> type) {
		if (registeredTypes.contains(type))
			return;
		
		AutoHashMap<NativeEntities, NativeEntities.Builder> builders = new AutoHashMap<NativeEntities, NativeEntities.Builder>(NativeEntities.Builder.class);
		for (; type != null && type != Object.class; type = type.getSuperclass()) {
			try {
				boolean isCPPClass = CPPObject.class.isAssignableFrom(type);
				NativeLibrary typeLibrary = getNativeLibrary(type);
				for (Method method : type.getDeclaredMethods()) {
					try {
						int modifiers = method.getModifiers();
						if (!Modifier.isNative(modifiers))
							continue;
						
						NativeEntities.Builder builder = builders.get(getNativeEntities(method));
						NativeLibrary methodLibrary = getNativeLibrary(method);
						
						MethodCallInfo mci = new MethodCallInfo(method, methodLibrary);
						Annotation[][] anns = method.getParameterAnnotations();
						boolean isCPPInstanceMethod = false;
						if (anns.length > 0) {
							for (Annotation ann : anns[0]) {
								if (ann instanceof This) {
									isCPPInstanceMethod = true;
									break;
								}
							}
						}
						
						if (isCPPInstanceMethod) {
//							if (Modifier.isStatic(modifiers)) {
//								log(Level.WARNING, "Method " + method.toGenericString() + " is native and maps to a C++ instance method. It should not be static.");
//								continue;
//							}
							
							if (!isCPPClass) {
								log(Level.SEVERE, "Method " + method.toGenericString() + " should have been declared in a " + CPPObject.class.getName() + " subclass.");
								continue;
							}
							Virtual va = method.getAnnotation(Virtual.class);
							if (va == null) {
								if (mci.forwardedPointer == 0) {
									for (String symbol : methodLibrary.getSymbols()) {
										if (methodLibrary.methodMatchesSymbol(type, method, symbol)) {
											mci.forwardedPointer = methodLibrary.getSymbolAddress(symbol);
											if (mci.forwardedPointer != 0)
												break;
										}
									}
									if (mci.forwardedPointer == 0) {
										log(Level.SEVERE, "Method " + method.toGenericString() + " is not virtual but its address could not be resolved in the library.");
										continue;
									}
								}
							} else {
								int virtualIndex = va.value();
								if (virtualIndex < 0) {
									Pointer<Pointer<?>> pVirtualTable = isCPPClass && typeLibrary != null ? typeLibrary.getVirtualTable((Class<? extends CPPObject>)type) : null;
									if (pVirtualTable == null) {
										log(Level.SEVERE, "Method " + method.toGenericString() + " is virtual but the virtual table of class " + type.getName() + " was not found.");
										continue;
									}
									
									virtualIndex = typeLibrary.getPositionInVirtualTable(pVirtualTable, method);
									if (virtualIndex < 0) {
										log(Level.SEVERE, "Method " + method.toGenericString() + " is virtual but its position could not be found in the virtual table.");
										continue;
									}
								}
								mci.virtualIndex = virtualIndex;
							}
							builder.addCPPMethod(mci);
						} else {
							if (!Modifier.isStatic(modifiers))
								log(Level.WARNING, "Method " + method.toGenericString() + " is native and maps to a function, but is not static.");
							builder.addFunction(mci);
						}
						
					} catch (Exception ex) {
						log(Level.SEVERE, "Method " + method.toGenericString() + " cannot be mapped : " + ex, ex);
					}
				}
				registeredTypes.add(type);
			} catch (Exception ex) {
				throw new RuntimeException("Failed to register class " + type.getName(), ex);
			}
		}
		for (Map.Entry<NativeEntities, NativeEntities.Builder> e : builders.entrySet()) {
			e.getKey().addDefinitions(type, e.getValue());
		}
	}
	static void log(Level level, String message, Throwable ex) {
		Logger.getLogger(BridJ.class.getName()).log(level, message, ex);
	}
	static void log(Level level, String message) {
		log(level, message, null);
	}
	protected static synchronized NativeEntities getNativeEntities(AnnotatedElement type) throws FileNotFoundException {
		NativeLibrary lib = getNativeLibrary(type);
		if (lib != null)
			return lib.getNativeEntities();
		return orphanEntities;
	}
	public static synchronized NativeLibrary getNativeLibrary(AnnotatedElement type) throws FileNotFoundException {
		NativeLibrary lib = librariesByClass.get(type);
		if (lib == null) {
			String name = getLibrary(type);
			lib = getLibHandle(name);
			if (lib != null)
				librariesByClass.put(type, lib);
		}
		return lib;
	}
	//static List<Runnable> releaseHooks = new ArrayList<Runnable>();
	public synchronized static void addShutdownHook(NativeLibrary library, Runnable runnable) {
		//releaseHooks.add(runnable);
	}
	
	/**
	 * Reclaims all the memory allocated by BridJ in the JVM and on the native side.
	 * This is automatically called at shutdown time.
	 */
	public synchronized static void releaseAll() {
		for (NativeLibrary lib : librariesByFile.values())
			lib.release();
		librariesByFile.clear();
		librariesByClass.clear();
		orphanEntities.release();
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
    static synchronized long getSymbolAddress(AnnotatedElement member) throws FileNotFoundException {
        String lib = getLibrary(member);
        NativeLibrary libHandle = getLibHandle(lib);
        if (libHandle == null)
            return 0;
        return getSymbolAddress(libHandle, member);
    }
    static synchronized long getSymbolAddress(NativeLibrary library, AnnotatedElement member) throws FileNotFoundException {
        //libHandle = libHandle & 0xffffffffL;
        Mangling mg = getAnnotation(Mangling.class, false, member);
        if (mg != null)
            for (String name : mg.value())
            {
                long handle = library.getSymbolAddress(name);
                if (handle != 0)
                    return handle;
            }

        String name = null;
        if (member instanceof Member)
            name = ((Member)member).getName();
        else if (member instanceof Class<?>)
            name = ((Class<?>)member).getSimpleName();

        if (name != null) {
            long handle = library.getSymbolAddress(name);
            if (handle != 0)
                return handle;
        }
        return 0;
    }
    static List<String> paths;
    static synchronized List<String> getPaths() {
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

    static synchronized File getLibFile(Class<?> member) throws FileNotFoundException {
        return getLibFile(getLibrary(member));
    }

    static File getLibFile(String name) {
        if (name == null)
            return null;
        for (String path : getPaths()) {
            File pathFile;
            try {
                pathFile = new File(path).getCanonicalFile();
            } catch (IOException ex) {
                Logger.getLogger(BridJ.class.getName()).log(Level.SEVERE, null, ex);
                continue;
            }
            File f = new File(name);
        	if (!f.exists())
                f = new File(pathFile, name + ".dll").getAbsoluteFile();
            if (!f.exists())
                f = new File(pathFile, "lib" + name + ".so").getAbsoluteFile();
            if (!f.exists())
                f = new File(pathFile, "lib" + name + ".dylib").getAbsoluteFile();
            if (!f.exists())
                f = new File(pathFile, "lib" + name + ".jnilib").getAbsoluteFile();
            if (!f.exists())
                continue;

            try {
                return f.getCanonicalFile();
            } catch (IOException ex) {
                Logger.getLogger(BridJ.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try {
        	return JNI.extractEmbeddedLibraryResource(name);
        } catch (IOException ex) {
        	return null;
        }
    }
    public static synchronized NativeLibrary getLibHandle(String name) throws FileNotFoundException {
        if (name == null)
            return null;
        
        NativeLibrary l = libHandles.get(name);
        if (l != null)
            return l;

        File f = getLibFile(name);
        NativeLibrary ll = NativeLibrary.load(f.toString());
        if (ll == null)
            throw new FileNotFoundException("Library '" + name + "' was not found in path '" + getPaths() + "'");
        libHandles.put(name, ll);
        return ll;
    }
    static String getLibrary(AnnotatedElement m) {
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

        if (ac.getAnnotation(NoInheritance.class) != null)
            return null;

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
    public static int sizeOf(Class<?> type) {
    	return 40;
    }
    
    //static <T extends CPPObject> long getPeer(T instance, Class<T> targetClass) {
    public static long getPeer(Object instance, Class targetClass) {
    	return ((CPPObject)instance).$this.getPeer();
    }
    
    public static <O extends CPPObject> Pointer<O> newCPPInstance(Class<? extends CPPObject> type) throws Exception {
		return newCPPInstance(type.getConstructor());
	}
	public static <O extends CPPObject> Pointer<O> newCPPInstance(Constructor constructor) throws Exception {
		Class<?> type = constructor.getDeclaringClass();
		int size = sizeOf(type);
		Pointer p = Pointer.pointerToAddress(JNI.malloc(size), type);
		NativeLibrary lib = getNativeLibrary(type); 
			//getLibrary(type);
		//TODO lib.getCPPConstructor(constructor);
		return p;
	}
}
