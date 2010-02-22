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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.bridj.Demangler.Symbol;
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
	static Map<Long, NativeObject> 
		strongNativeObjects = new HashMap<Long, NativeObject>(),
		weakNativeObjects = new WeakHashMap<Long, NativeObject>();

	static CallbackNativeImplementer callbackNativeImplementer = new CallbackNativeImplementer(orphanEntities);
	
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
	
	static boolean hasThisAsFirstArgument(Method method, boolean checkConsistency) {
		return hasThisAsFirstArgument(method.getParameterTypes(), method.getParameterAnnotations(), checkConsistency);
	}
	static boolean hasThisAsFirstArgument(Class<?>[] paramTypes, Annotation[][] anns, boolean checkConsistency) {
		boolean hasThis = false;
		int len = anns.length;
        if (len > 0) {
        	for (int i = 0; i < len; i++) {
        		for (Annotation ann : anns[i]) {
	        		if (ann instanceof This) {
	        			hasThis = true;
	        			if (!checkConsistency)
	        				return true;
	        			if (paramTypes[0] != Long.TYPE)
	        				throw new RuntimeException("First parameter with annotation " + This.class.getName() + " must be of type long, but is of type " + paramTypes[0].getName() + ".");
	        		}
	    		}
        		if (i == 0 && !checkConsistency)
        			return false;
        	}
        }
        return hasThis;
	}
	
	static final int defaultObjectSize = 128;
	static final String PROPERTY_bridj_cpp_defaultObjectSize = "bridj.cpp.defaultObjectSize";
    public static int sizeOf(Class<?> type) {
    	String s = System.getProperty(PROPERTY_bridj_cpp_defaultObjectSize);
    	if (s != null)
    		try {
    			return Integer.parseInt(s);
	    	} catch (Throwable th) {
	    		log(Level.SEVERE, "Invalid value for property " + PROPERTY_bridj_cpp_defaultObjectSize + " : '" + s + "'");
	    	}
    	return defaultObjectSize;
    }
    
	public static void deallocate(NativeObject nativeObject) {
		unregisterNativeObject(nativeObject);
		//TODO call destructor !!! 
		Pointer.getPeer(nativeObject, null).release();
	}
	
	static Method getConstructor(Class<?> type, int constructorId, Object[] args) throws SecurityException, NoSuchMethodException {
		for (Method c : type.getDeclaredMethods()) {
			com.bridj.ann.Constructor ca = c.getAnnotation(com.bridj.ann.Constructor.class);
			if (ca == null)
				continue;
			if (constructorId < 0) {
				Class<?>[] params = c.getParameterTypes();
				int n = params.length;
				if (n == args.length + 1) {
					boolean matches = true;
					for (int i = 0; i < n; i++) {
						Class<?> param = params[i];
						if (i == 0) {
							if (param != Long.TYPE) {
								matches = false;
								break;
							}
							continue;
						}
						Object arg = args[i - 1];
						if (arg == null && param.isPrimitive() || !param.isInstance(arg)) {
							matches = false;
							break;
						}
					}
					if (matches) 
						return c;
				}
			} else if (ca != null && ca.value() == constructorId)
				return c;
		}
		throw new NoSuchMethodException("Cannot find constructor with index " + constructorId);
	}
	static Map<Class<?>, Long> defaultConstructors = new HashMap<Class<?>, Long>();
    public static <T extends NativeObject> Pointer<T> allocate(Class<?> type, int constructorId, Object... args) {
    	register(type);
        if (CPPObject.class.isAssignableFrom(type))
        	return newCPPInstance(type, constructorId, args);
        if (Callback.class.isAssignableFrom(type)) {
        	if (constructorId != -1 || args.length != 0)
        		throw new RuntimeException("Callback should have a constructorId == -1 and no constructor args !");
        	return newCallbackInstance(type);
        }
        throw new RuntimeException("Cannot allocate instance of type " + type.getName() + " (unhandled NativeObject subclass)");
    }
    
    private static <T extends NativeObject> Pointer<T> newCallbackInstance(Class<?> type) {
//    	Method method = null;
    	Class<?> parent = null;
    	while ((parent = type.getSuperclass()) != null && parent != Callback.class) {
    		type = parent;
    	}
    	
    	registerOne(type);
//    	if (Callback.class.isAssignableFrom(type)) {
//			callbackNativeImplementer.getCallbackImplType((Class) type);
//		}
		
    	
    	Method method = null;
    	for (Method dm : type.getDeclaredMethods()) {
    		int modifiers = dm.getModifiers();
    		if (!Modifier.isAbstract(modifiers))
    			continue;
    		
    		method = dm;
    		break;
    	}
    	if (method == null)
    		throw new RuntimeException("Type doesn't have any abstract method : " + type.getName());
    	
		try {
			MethodCallInfo mci = new MethodCallInfo(method);
			mci.isJavaToCCallback = true;
			return null;//(Pointer)Pointer.pointerToAddress(JNI.createJavaToCCallbacks(mci), type);
		} catch (FileNotFoundException e) {
			throw new RuntimeException("Failed to create instance of callback " + type.getName(), e);
		}
	}
	private static <T extends NativeObject> Pointer<T> newCPPInstance(Class<?> type, int constructorId, Object... args) {
    	Pointer<T> peer = null;
        try {
        	peer = (Pointer)Pointer.allocate(PointerIO.getInstance(type), sizeOf(type));
        	NativeLibrary lib = getNativeLibrary(type);
        	if (constructorId < 0) {
        		Long defaultConstructor = defaultConstructors.get(type);
        		if (defaultConstructor == null) {
        			for (Symbol symbol : lib.getSymbols()) {
        				if (symbol.matchesConstructor(type)) {
        					defaultConstructor = symbol.getAddress();
        					break;
        				}
        			}
        			if (defaultConstructor == null || defaultConstructor == 0)
        				throw new RuntimeException("Cannot find the default constructor for type " + type.getName());
        			
        			installVTablePtr(type, lib, peer);
        			JNI.callDefaultCPPConstructor(defaultConstructor, peer.getPeer(), 0);// TODO use right call convention
        			return peer;
        		}
        	}
        	Method meth = getConstructor(type, constructorId, args);
        	Object[] consArgs = new Object[args.length + 1];
        	if (CPPObject.class.isAssignableFrom(type)) {
        		if (meth.getReturnType() != Void.TYPE)
	        		throw new RuntimeException("Constructor-mapped methods must return void, but " + meth.getName() + " returns " + meth.getReturnType().getName());
        		if (!Modifier.isStatic(meth.getModifiers()))
	        		throw new RuntimeException("Constructor-mapped methods must be static, but " + meth.getName() + " is not.");
        		if (!meth.getName().equals(type.getSimpleName()))
	        		throw new RuntimeException("Constructor methods must have the same name as their class : " + meth.getName() + " is not " + type.getSimpleName());
	        	installVTablePtr(type, lib, peer);
        	} else {
        		// TODO ObjCObject : call alloc on class type !!
        	}
        	consArgs[0] = peer.getPeer();
        	System.arraycopy(args, 0, consArgs, 1, args.length);
        	boolean acc = meth.isAccessible();
        	try {
        		meth.setAccessible(true);
        		meth.invoke(null, consArgs);
        	} finally {
        		try {
        			meth.setAccessible(acc);
        		} catch (Exception ex) {}
        	}
        	return peer;
        } catch (Exception ex) {
        	if (peer != null)
        		peer.release();
        	throw new RuntimeException("Failed to allocate new instance of type " + type, ex);
        }
	}
	static void installVTablePtr(Class<?> type, NativeLibrary lib, Pointer<?> peer) {
    	Pointer<Pointer<?>> vptr = lib.getVirtualTable(type);
    	if (!lib.isMSVC());
//    		vptr = vptr.offset(16);
    		vptr = vptr.next(2);
    	peer.setPointer(0, vptr);
    }

    public static synchronized void register(Class<?>... types) {
		if (types.length == 0) {
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
	    	return;
		}
		for (Class<?> type : types)
			registerOne(type);
		
		
	}
    
    
	private static void registerOne(Class<?> type) {
		if (registeredTypes.contains(type))
			return;
		
		int typeModifiers = type.getModifiers();
		if (Callback.class.isAssignableFrom(type)) {
			if (Modifier.isAbstract(typeModifiers))
				callbackNativeImplementer.getCallbackImplType((Class) type);
		}
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
						
						MethodCallInfo mci = new MethodCallInfo(method);
						Annotation[][] anns = method.getParameterAnnotations();
						boolean isCPPInstanceMethod = false;
						if (anns.length > 0) {
							if (method.getAnnotation(Virtual.class) != null)
								isCPPInstanceMethod = true;
							else
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
								mci.forwardedPointer = methodLibrary.getSymbolAddress(method);
						        if (mci.forwardedPointer == 0) {
									for (Demangler.Symbol symbol : methodLibrary.getSymbols()) {
										if (symbol.matches(method)) {
											mci.forwardedPointer = symbol.getAddress();
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
								if (Modifier.isStatic(modifiers))
									log(Level.WARNING, "Method " + method.toGenericString() + " is native and maps to a function, but is not static.");
									
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
							builder.addVirtualMethod(mci);
						} else {
							//if (!Modifier.isStatic(modifiers))
							//	log(Level.WARNING, "Method " + method.toGenericString() + " is native and maps to a function, but is not static.");
							
							mci.forwardedPointer = methodLibrary.getSymbolAddress(method);
					        
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
    public static synchronized NativeLibrary getNativeLibrary(String name) throws FileNotFoundException {
        if (name == null)
            return null;
        
        NativeLibrary l = libHandles.get(name);
        if (l != null)
            return l;

        File f = getNativeLibraryFile(name);
        if (f == null)
        	throw new FileNotFoundException("Couldn't find library file for library '" + name + "'");
        
        NativeLibrary ll = NativeLibrary.load(f.toString());
        if (ll == null)
            throw new FileNotFoundException("Library '" + name + "' was not found in path '" + getNativeLibraryPaths() + "'");
        libHandles.put(name, ll);
        return ll;
    }
    static String getNativeLibraryName(AnnotatedElement m) {
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
}
