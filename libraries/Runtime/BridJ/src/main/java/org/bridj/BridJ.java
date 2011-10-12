package org.bridj;

import org.bridj.util.Utils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.*;

import org.bridj.BridJRuntime.TypeInfo;
import org.bridj.demangling.Demangler.Symbol;
import org.bridj.demangling.Demangler.MemberRef;
import org.bridj.ann.Library;
import java.util.Stack;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.URL;
import static org.bridj.Platform.*;
import static java.lang.System.*;

/// http://www.codesourcery.com/public/cxx-abi/cxx-vtable-ex.html
/**
 * BridJ's central class.<br>
 * <ul>
 * <li>To register a class with native methods (which can be in inner classes), just add the following static block to your class :
 * <pre>{@code
 *      static {
 *          BridJ.register();
 *      }
 * }</pre>
 * </li><li>You can also register a class explicitely with {@link BridJ#register(java.lang.Class)}
 * </li><li>To alter the name of a library, use {@link BridJ#setNativeLibraryActualName(String, String)} and {@link BridJ#addNativeLibraryAlias(String, String)}
 * </li>
 * </ul>
 * @author ochafik
 */
public class BridJ {

	static final boolean exceptionsSupported = false;
	
    static final Map<AnnotatedElement, NativeLibrary> librariesByClass = new HashMap<AnnotatedElement, NativeLibrary>();
    static final Map<String, File> librariesFilesByName = new HashMap<String, File>();
    static final Map<File, NativeLibrary> librariesByFile = new HashMap<File, NativeLibrary>();
    private static NativeEntities orphanEntities = new NativeEntities();
    static final Map<Class<?>, BridJRuntime> classRuntimes = new HashMap<Class<?>, BridJRuntime>();
    static final Map<Long, NativeObject> strongNativeObjects = new HashMap<Long, NativeObject>(),
            weakNativeObjects = new WeakHashMap<Long, NativeObject>();

    public static long sizeOf(Type type) {
        Class c = Utils.getClass(type);
        if (c.isPrimitive())
            return StructIO.primTypeLength(c);
        else if (Pointer.class.isAssignableFrom(c))
            return Pointer.SIZE;
        else if (c == CLong.class)
            return CLong.SIZE;
        else if (c == TimeT.class)
            return TimeT.SIZE;
        else if (c == SizeT.class)
            return SizeT.SIZE;
        else if (c == Integer.class || c == Float.class)
            return 4;
        else if (c == Character.class || c == Short.class)
            return 2;
        else if (c == Long.class || c == Double.class)
            return 8;
        else if (c == Boolean.class || c == Byte.class)
            return 1;
        else if (NativeObject.class.isAssignableFrom(c))
            return getRuntime(c).getTypeInfo(type).sizeOf();
        /*if (o instanceof NativeObject) {
            NativeObject no = (NativeObject)o;
            return no.typeInfo.sizeOf(no);
        }*/
        throw new RuntimeException("Unable to compute size of type " + Utils.toString(type));
    }
    static synchronized void registerNativeObject(NativeObject ob) {
        weakNativeObjects.put(Pointer.getAddress(ob, null), ob);
    }
    /// Caller should display message such as "target was GC'ed. You might need to add a BridJ.protectFromGC(NativeObject), BridJ.unprotectFromGC(NativeObject)

    static synchronized NativeObject getNativeObject(long peer) {
        NativeObject ob = weakNativeObjects.get(peer);
        if (ob == null) {
            ob = strongNativeObjects.get(peer);
        }
        return ob;
    }

    static synchronized void unregisterNativeObject(NativeObject ob) {
        long peer = Pointer.getAddress(ob, null);
        weakNativeObjects.remove(peer);
        strongNativeObjects.remove(peer);
    }

    /**
     * Keep a hard reference to a native object to avoid its garbage collection.<br>
     * See {@link BridJ#unprotectFromGC(NativeObject)} to remove the GC protection.
     */
    public static synchronized <T extends NativeObject> T protectFromGC(T ob) {
        long peer = Pointer.getAddress(ob, null);
        if (weakNativeObjects.remove(peer) != null) {
            strongNativeObjects.put(peer, ob);
        }
        return ob;
    }

	/**
     * Drop the hard reference created with {@link BridJ#protectFromGC(NativeObject)}.
     */
    public static synchronized <T extends NativeObject> T unprotectFromGC(T ob) {
		long peer = Pointer.getAddress(ob, null);
        if (strongNativeObjects.remove(peer) != null) {
			weakNativeObjects.put(peer, ob);
		}
		return ob;
    }

	public static void delete(NativeObject nativeObject) {
		unregisterNativeObject(nativeObject);
		Pointer.pointerTo(nativeObject, null).release();
	}

	/**
	 * Registers the native methods of the caller class and all its inner types.
	 * <pre>{@code
	 	\@Library("mylib")
	 	public class MyLib {
	 		static {
	 			BridJ.register();
			}
			public static native void someFunc();
		}
		}</pre>
	 */
    public static synchronized void register() {
		StackTraceElement[] stackTrace = new Exception().getStackTrace();
        if (stackTrace.length < 2) {
    		throw new RuntimeException("No useful stack trace : cannot register with register(), please use register(Class) instead.");
        }
    	String name = stackTrace[1].getClassName();
    	try {
    		Class<?> type = Class.forName(name);
    		register(type);
    	} catch (Exception ex) {
    		throw new RuntimeException("Failed to register class " + name, ex);
    	}
    }
    enum CastingType {
        None, CastingNativeObject, CastingNativeObjectReturnType
    }
	static ThreadLocal<Stack<CastingType>> currentlyCastingNativeObject = new ThreadLocal<Stack<CastingType>>() {

        @Override
		protected java.util.Stack<CastingType> initialValue() {
			Stack<CastingType> s = new Stack<CastingType>();
			s.push(CastingType.None);
			return s;
        }

        ;
		};

    @Deprecated
	public static boolean isCastingNativeObjectInCurrentThread() {
		return currentlyCastingNativeObject.get().peek() != CastingType.None;
	}

    @Deprecated
	public static boolean isCastingNativeObjectReturnTypeInCurrentThread() {
		return currentlyCastingNativeObject.get().peek() == CastingType.CastingNativeObjectReturnType;
	}

    private static WeakHashMap<Long, NativeObject> knownNativeObjects = new WeakHashMap<Long, NativeObject>();
    public static synchronized <O extends NativeObject> void setJavaObjectFromNativePeer(long peer, O object) {
        if (object == null)
            knownNativeObjects.remove(peer);
        else
            knownNativeObjects.put(peer, object);
    }
    public static synchronized Object getJavaObjectFromNativePeer(long peer) {
        return knownNativeObjects.get(peer);
    }
    
	private static <O extends NativeObject> O createNativeObjectFromPointer(Pointer<? super O> pointer, Type type, CastingType castingType) {
		Stack<CastingType> s = currentlyCastingNativeObject.get();
		s.push(castingType);
		try {
        		TypeInfo<O> typeInfo = getTypeInfo(type);
        		O instance = typeInfo.cast(pointer);
                if (BridJ.debug)
                    BridJ.log(Level.INFO, "Created native object from pointer " + pointer);
			return instance;
		} catch (Exception ex) {
            throw new RuntimeException("Failed to cast pointer to native object of type " + Utils.getClass(type).getName(), ex);
		} finally {
			s.pop();
		}
	}
	public static <O extends NativeObject> void copyNativeObjectToAddress(O value, Type type, Pointer<O> ptr) {
        	getTypeInfo(type).copyNativeObjectToAddress(value, (Pointer)ptr);
	}
    
    public static <O extends NativeObject> O createNativeObjectFromPointer(Pointer<? super O> pointer, Type type) {
        return (O)createNativeObjectFromPointer(pointer, type, CastingType.CastingNativeObject);
	}
    public static <O extends NativeObject> O createNativeObjectFromReturnValuePointer(Pointer<? super O> pointer, Type type) {
        return (O)createNativeObjectFromPointer(pointer, type, CastingType.CastingNativeObjectReturnType);
	}
    private static Map<Class<? extends BridJRuntime>, BridJRuntime> runtimes = new HashMap<Class<? extends BridJRuntime>, BridJRuntime>();

    public static synchronized <R extends BridJRuntime> R getRuntimeByRuntimeClass(Class<R> runtimeClass) {
        R r = (R) runtimes.get(runtimeClass);
        if (r == null) {
			try {
				runtimes.put(runtimeClass, r = runtimeClass.newInstance());
			} catch (Exception e) {
				throw new RuntimeException("Failed to instantiate runtime " + runtimeClass.getName(), e);
			}
        }

    	return r;
    }

    /**
     * Get the runtime associated with a class (using the {@link org.bridj.ann.Runtime} annotation, if any, looking up parents and defaulting to {@link org.bridj.CRuntime}).
     */
    public static BridJRuntime getRuntime(Class<?> type) {
        synchronized (classRuntimes) {
            BridJRuntime runtime = classRuntimes.get(type);
            if (runtime == null) {
                org.bridj.ann.Runtime runtimeAnn = getAnnotation(org.bridj.ann.Runtime.class, true, type);
                Class<? extends BridJRuntime> runtimeClass = null;
                if (runtimeAnn != null)
                		runtimeClass = runtimeAnn.value();
                else	
                		runtimeClass = CRuntime.class;
                //if (runtimeC == null) {
                //    throw new IllegalArgumentException("Class " + type.getName() + " has no " + org.bridj.ann.Runtime.class.getName() + " annotation. Unable to guess the corresponding " + BridJRuntime.class.getName() + " implementation.");
                //}
                runtime = getRuntimeByRuntimeClass(runtimeClass);
                classRuntimes.put(type, runtime);
            }
			return runtime;
        }
    }

	/**
	 * Registers the native method of a type (and all its inner types).
	 * <pre>{@code
	 	\@Library("mylib")
	 	public class MyLib {
	 		static {
	 			BridJ.register(MyLib.class);
			}
			public static native void someFunc();
		}
		}</pre>
	 */
    public static BridJRuntime register(Class<?> type) {
        BridJRuntime runtime = getRuntime(type);
		runtime.register(type);
		return runtime;
	}
    public static void unregister(Class<?> type) {
        BridJRuntime runtime = getRuntime(type);
		runtime.unregister(type);
	}
    static Map<Type, TypeInfo<?>> typeInfos = new HashMap<Type, TypeInfo<?>>();

	static <T extends NativeObject> TypeInfo<T> getTypeInfo(Type t) {
		synchronized (typeInfos) { 
			TypeInfo info = typeInfos.get(t);
            if (info == null) {
				info = getRuntime(Utils.getClass(t)).getTypeInfo(t);
                typeInfos.put(t, info);
            }
			return info;
		}
	}

    public static final boolean debug = "true".equals(getProperty("bridj.debug")) || "1".equals(getenv("BRIDJ_DEBUG"));
    public static final boolean debugNeverFree = "true".equals(getProperty("bridj.debug.neverFree")) || "1".equals(getenv("BRIDJ_DEBUG_NEVER_FREE"));
    public static final boolean debugPointers = "true".equals(getProperty("bridj.debug.pointers")) || "1".equals(getenv("BRIDJ_DEBUG_POINTERS"));
    public static final boolean verbose = debug || "true".equals(getProperty("bridj.verbose")) || "1".equals(getenv("BRIDJ_VERBOSE"));
    public static final boolean logCalls = "true".equals(getProperty("bridj.logCall")) || "1".equals(getenv("BRIDJ_LOG_CALLS"));
    static final int minLogLevel = Level.WARNING.intValue();
	static boolean shouldLog(Level level) {
        return verbose || level.intValue() >= minLogLevel;
    }
    
    static Logger logger;
    static synchronized Logger getLogger() {
    		if (logger == null)
    			logger = Logger.getLogger(BridJ.class.getName());
    		return logger;
    }
	public static boolean log(Level level, String message, Throwable ex) {
        if (!shouldLog(level))
            return true;
		getLogger().log(level, message, ex);
        return true;
	}

	public static boolean log(Level level, String message) {
		log(level, message, null);
		return true;
	}
	
	public static native void setLogNativeCalls(boolean log);
	
	static void logCall(Method m) {
		getLogger().log(Level.INFO, "Calling method " + m);
	}

	public static synchronized NativeEntities getNativeEntities(AnnotatedElement type) throws FileNotFoundException {
		NativeLibrary lib = getNativeLibrary(type);
        if (lib != null) {
			return lib.getNativeEntities();
        }
		return getOrphanEntities();
	}

	public static synchronized NativeLibrary getNativeLibrary(AnnotatedElement type) throws FileNotFoundException {
		NativeLibrary lib = librariesByClass.get(type);
		if (lib == null) {
			String name = getNativeLibraryName(type);
			lib = getNativeLibrary(name);
            if (lib != null) {
				librariesByClass.put(type, lib);
		}
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
		gc();

        for (NativeLibrary lib : librariesByFile.values()) {
			lib.release();
        }
		librariesByFile.clear();
		librariesByClass.clear();
		getOrphanEntities().release();
		gc();
	}
	//public synchronized static void release(Class<?>);

	public synchronized static void releaseLibrary(String name) {
		File file = librariesFilesByName.remove(name);
        if (file != null) {
			releaseLibrary(file);
	}
    }

	public synchronized static void releaseLibrary(File library) {
		NativeLibrary lib = librariesByFile.remove(library);
        if (lib != null) {
			lib.release();
	}
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

    static List<String> additionalPaths = new ArrayList<String>();
    public static synchronized void addLibraryPath(String path) {
    		additionalPaths.add(path);
    		paths = null; // invalidate cached paths
    }
    static synchronized List<String> getNativeLibraryPaths() {
        if (paths == null) {
            paths = new ArrayList<String>();
            paths.addAll(additionalPaths);
            paths.add(null);
            paths.add(".");
			String env;
			
			/*
			String bitsSuffix = is64Bits() ? "64" : "32";
			if (isUnix() && !isMacOSX()) {
				paths.add("/usr/lib" + bitsSuffix);
				paths.add("/lib" + bitsSuffix);
			}
			if (!isLinux() || !is64Bits()) {
				paths.add("/usr/lib");
				paths.add("/lib");
			}
			*/
			env = getenv("LD_LIBRARY_PATH");
            if (env != null)
                paths.addAll(Arrays.asList(env.split(File.pathSeparator)));
            
            env = getenv("DYLD_LIBRARY_PATH");
            if (env != null)
                paths.addAll(Arrays.asList(env.split(File.pathSeparator)));
            
			env = getenv("PATH");
            if (env != null)
                paths.addAll(Arrays.asList(env.split(File.pathSeparator)));
            
            env = getProperty("java.library.path");
            if (env != null)
                paths.addAll(Arrays.asList(env.split(File.pathSeparator)));
            
            env = getProperty("gnu.classpath.boot.library.path");
            if (env != null)
                paths.addAll(Arrays.asList(env.split(File.pathSeparator)));
            
            File javaHome = new File(getProperty("java.home"));
            paths.add(new File(javaHome, "bin").toString());
            if (isMacOSX()) {
            		paths.add(new File(javaHome, "../Libraries").toString());
            }
            
            
            if (isUnix()) {
            		String bits = is64Bits() ? "64" : "32";
            		if (isLinux()) {
            			// First try Ubuntu's multi-arch paths (cf. https://wiki.ubuntu.com/MultiarchSpec)
					String abi = isArm() ? "gnueabi" : "gnu";
					String multiArch = getMachine() + "-linux-" + abi;
					paths.add("/lib/" + multiArch);
					paths.add("/usr/lib/" + multiArch);
				
					// Add /usr/lib32 and /lib32
            			paths.add("/usr/lib" + bits);
					paths.add("/lib" + bits);
				} else if (isSolaris()) {
					// Add /usr/lib/32 and /lib/32
            			paths.add("/usr/lib/" + bits);
					paths.add("/lib/" + bits);
				}
				
				paths.add("/usr/lib");
				paths.add("/lib");
				paths.add("/usr/local/lib");
			}
        }
        return paths;
    }

    static Map<String, String> libraryActualNames = new HashMap<String, String>();
    /**
     * Define the actual name of a library.<br>
     * Works only before the library is loaded.<br>
     * For instance, library "OpenGL" is actually named "OpenGL32" on Windows : BridJ.setNativeLibraryActualName("OpenGL", "OpenGL32");
     * @param name
     * @param actualName
     */
    public static synchronized void setNativeLibraryActualName(String name, String actualName) {
        libraryActualNames.put(name, actualName);
    }
	
	
    static Map<String, List<String>> libraryAliases = new HashMap<String, List<String>>();
    /**
     * Add a possible alias for a library.<br>
	 * Aliases are prioritary over the library (or its actual name, see {@link BridJ#setNativeLibraryActualName(String, String)}), in the order they are defined.<br>
	 * Works only before the library is loaded.<br>
     * @param name
     * @param alias
     */
    public static synchronized void addNativeLibraryAlias(String name, String alias) {
        List<String> list = libraryAliases.get(name);
		if (list == null)
			libraryAliases.put(name, list = new ArrayList<String>());
		if (!list.contains(alias))
			list.add(alias);
    }
    /**
     * Given a library name (e.g. "test"), finds the shared library file in the system-specific path ("/usr/bin/libtest.so", "./libtest.dylib", "c:\\windows\\system\\test.dll"...)
	 */
    public static File getNativeLibraryFile(String libraryName) {
        if (libraryName == null)
            return null;
        
        //out.println("Getting file of '" + name + "'");
        String actualName = libraryActualNames.get(libraryName);
		List<String> aliases = libraryAliases.get(libraryName);
		List<String> possibleNames = new ArrayList<String>();
		if (aliases != null)
			possibleNames.addAll(aliases);
		possibleNames.add(actualName == null ? libraryName : actualName);
		
		//out.println("Possible names = " + possibleNames);
		List<String> paths = getNativeLibraryPaths();
		log(Level.INFO, "Looking for library '" + libraryName + "' " + (actualName != null ? "('" + actualName + "') " : "") + "in paths " + paths, null);
		
		for (String name : possibleNames) {
			String env = getenv("BRIDJ_" + name.toUpperCase() + "_LIBRARY");
			if (env == null)
				env = getProperty("bridj." + name + ".library");
			if (env != null) {
				File f = new File(env);
				if (f.exists()) {
					try {
						return f.getCanonicalFile();
					} catch (IOException ex) {
						log(Level.SEVERE, null, ex);
					}
				}
			}
			for (String path : paths) {
				File pathFile = path == null ? null : new File(path);
				File f = new File(name);
				if (pathFile != null) {
					if (isWindows()) {
						if (!f.exists()) {
							f = new File(pathFile, name + ".dll");
						}
						if (!f.exists()) {
							f = new File(pathFile, name + ".drv");
						}
					} else if (isUnix()) {
						if (isMacOSX()) {
							if (!f.exists()) {
								f = new File(pathFile, "lib" + name + ".dylib");
					}
						} else {
							if (!f.exists()) {
								f = new File(pathFile, "lib" + name + ".so");
							}
							if (!f.exists()) {
								f = new File(pathFile, name + ".so");
							}
						}
						if (!f.exists()) {
							f = new File(pathFile, "lib" + name + ".jnilib");
						}
					}
				}
	
				if (!f.exists()) {
					continue;
				}
	
				try {
					return f.getCanonicalFile();
				} catch (IOException ex) {
					log(Level.SEVERE, null, ex);
				}
			}
			if (isMacOSX()) {
				for (String s : new String[]{
					"/System/Library/Frameworks",
					"/System/Library/Frameworks/ApplicationServices.framework/Frameworks",
					new File(getProperty("user.home"), "Library/Frameworks").toString()
				}) {
					try {
						File f = new File(new File(s, name + ".framework"), name);
						if (f.exists() && !f.isDirectory())
							return f.getCanonicalFile();
					} catch (IOException ex) {
						ex.printStackTrace();
						return null;
				}
			}
			}
			try {
				File f;
				if (isAndroid())
					f = new File("lib" + name + ".so");
				else
					f = extractEmbeddedLibraryResource(name);
				
				if (f != null && f.exists())
					return f;
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}
		return null;
    }
    static Boolean directModeEnabled;

    /**
     * Query direct mode.<br>
     * In direct mode, BridJ will <i>attempt</i> to optimize calls with assembler code, so that the overhead of each call is about the same as with plain JNI.<br>
     * Set -Dbridj.direct=false in the command line (or setProperty("bridj.direct", "false")) or environment var BRIDJ_DIRECT=0 to disable
     */
    public static boolean isDirectModeEnabled() {
        if (directModeEnabled == null) {
            String prop = getProperty("bridj.direct");
            String env = getenv("BRIDJ_DIRECT");
            directModeEnabled = 
            		!"false".equalsIgnoreCase(prop) && 
            		!"false".equalsIgnoreCase(env) && 
            		!"0".equals(env) && 
            		!"no".equalsIgnoreCase(env) &&
            		!logCalls
			;
            log(Level.INFO, "directModeEnabled = " + directModeEnabled + " (" + getProperty("bridj.direct") + ")");
        }
        return directModeEnabled;
    }

    /**
     * Set direct mode.<br>
     * In direct mode, BridJ will <i>attempt</i> to optimize calls with assembler code, so that the overhead of each call is about the same as with plain JNI.<br>
     * Set -Dbridj.direct=false in the command line (or setProperty("bridj.direct", "false")) or environment var BRIDJ_DIRECT=0 to disable
     */
    static void setDirectModeEnabled(boolean v) {
        directModeEnabled = v;
    }

    /**
     * Loads the library with the name provided in argument (see {@link #getNativeLibraryFile(String)})
	 */
    public static synchronized NativeLibrary getNativeLibrary(String name) throws FileNotFoundException {
        if (name == null) {
            return null;
        }

        NativeLibrary l = libHandles.get(name);
        if (l != null) {
            return l;
        }

        File f = getNativeLibraryFile(name);
        //if (f == null) {
        //	throw new FileNotFoundException("Couldn't find library file for library '" + name + "'");
        //}
		
		return getNativeLibrary(name, f);
    }

    /**
     * Loads the shared library file under the provided name. Any subsequent call to {@link #getNativeLibrary(String)} will return this library.
	 */
    public static NativeLibrary getNativeLibrary(String name, File f) throws FileNotFoundException {
		NativeLibrary ll = NativeLibrary.load(f == null ? name : f.toString());;
		if (ll == null) {
            ll = PlatformSupport.getInstance().loadNativeLibrary(name);
            if (ll == null) {
                if ("c".equals(name)) {
                    ll = new NativeLibrary(null, 0, 0);
                }
            }
		}

		//if (ll == null && f != null)
		//	ll = NativeLibrary.load(f.getName());
        if (ll == null) {
            throw new FileNotFoundException("Library '" + name + "' was not found in path '" + getNativeLibraryPaths() + "'" + (f != null && f.exists() ? " (failed to load " + f + ")" : ""));
        }
        log(Level.INFO, "Loaded library '" + name + "' from '" + f + "'", null);
        
        libHandles.put(name, ll);
        return ll;
    }

    /**
     * Gets the name of the library declared for an annotated element. Recurses up to parents of the element (class, enclosing classes) to find any {@link org.bridj.ann.Library} annotation.
	 */
    public static String getNativeLibraryName(AnnotatedElement m) {
        Library lib = getAnnotation(Library.class, true, m);
        if (lib == null) {
        		Class c = null;
        		if (m instanceof Class)
        			c = (Class)m;
        		else if (m instanceof Member)
        			c = ((Member)m).getDeclaringClass();
        		if (c != null && !NativeObject.class.isAssignableFrom(c) && c.getEnclosingClass() == null)
        			return c.getSimpleName();
        		else
        			return null;
        }
        return lib.value();
    }

    static <A extends Annotation> A getAnnotation(Class<A> ac, boolean inherit, AnnotatedElement m, Annotation... directAnnotations) {
        if (directAnnotations != null) {
            for (Annotation ann : directAnnotations) {
                if (ac.isInstance(ann)) {
                    return ac.cast(ann);
                }
            }
        }

        if (m == null) {
            return null;
        }
        A a = m.getAnnotation(ac);
        if (a != null) {
            return a;
        }

        if (inherit) {
            if (m instanceof Member) {
                return getAnnotation(ac, inherit, ((Member) m).getDeclaringClass());
            }
            
            if (m instanceof Class<?>) {
            	Class<?> c = (Class<?>) m, dc = c.getDeclaringClass();
                Class p = c.getSuperclass();
				while (p != null) {
					a = getAnnotation(ac, true, p);
					if (a != null)
						return a;
					p = p.getSuperclass();
				}

                if (dc != null) {
	                return getAnnotation(ac, inherit, dc);
	        }
        }
        }
        return null;
    }

	public static Symbol getSymbolByAddress(long peer) {
		for (NativeLibrary lib : libHandles.values()) {
			Symbol symbol = lib.getSymbol(peer);
            if (symbol != null) {
				return symbol;
		}
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
        TypeInfo typeInfo = getTypeInfo(instance.getClass());
        instance.typeInfo = typeInfo;
        typeInfo.initialize(instance);
    }

    static void initialize(NativeObject instance, Pointer peer) {
        TypeInfo typeInfo = getTypeInfo(instance.getClass());
        instance.typeInfo = typeInfo;
        typeInfo.initialize(instance, peer);
    }

    static void initialize(NativeObject instance, int constructorId, Object[] args) {
        // TODO handle template arguments here (or above), with class => ((class, args) => Type) caching
        TypeInfo typeInfo = getTypeInfo(instance.getClass());
        instance.typeInfo = typeInfo;
        typeInfo.initialize(instance, constructorId, args);
    }

	static <T extends NativeObject> T clone(T instance) throws CloneNotSupportedException {
        return ((TypeInfo<T>)instance.typeInfo).clone(instance);
	}

	/**
	 * Some native object need manual synchronization between Java fields and native memory.<br>
	 * An example is JNA-style structures.
	 */
	public static <T extends NativeObject> T readFromNative(T instance) {
		((TypeInfo<T>)instance.typeInfo).readFromNative(instance);
		return instance;
	}
	/**
	 * Some native object need manual synchronization between Java fields and native memory.<br>
	 * An example is JNA-style structures.
	 */
	public static <T extends NativeObject> T writeToNative(T instance) {
		((TypeInfo<T>)instance.typeInfo).writeToNative(instance);
		return instance;
	}
	/**
	 * Creates a string that describes the provided native object, printing generally-relevant internal data (for instance for structures, this will typically display the fields values).<br>
	 * This is primarily useful for debugging purposes.
	 */
	public static String describe(NativeObject instance) {
		return ((TypeInfo)instance.typeInfo).describe(instance);
	}
	/**
	 * Creates a string that describes the provided native object type, printing generally-relevant internal data (for instance for structures, this will typically display name of the fields, their offsets and lengths...).<br>
	 * This is primarily useful for debugging purposes.
	 */
	public static String describe(Type nativeObjectType) {
		TypeInfo typeInfo = getTypeInfo(nativeObjectType);
		return typeInfo == null ? Utils.toString(nativeObjectType) : typeInfo.describe();
	}
	
	public static void main(String[] args) {
		List<NativeLibrary> libraries = new ArrayList<NativeLibrary>();
		try {
			File outputDir = new File(".");
			for (int iArg = 0, nArgs = args.length; iArg < nArgs; iArg++) {
				String arg = args[iArg];
				if (arg.equals("-d")) {
					outputDir = new File(args[++iArg]);
					continue;
				}
				try {
					NativeLibrary lib = getNativeLibrary(arg);
					libraries.add(lib);
					
					PrintWriter sout = new PrintWriter(new File(outputDir, new File(arg).getName() + ".symbols.txt"));
					for (Symbol sym : lib.getSymbols()) {
						sout.print(sym.getSymbol());
						sout.print(" // ");
						try {
							MemberRef mr = sym.getParsedRef();
							sout.print(" // " + mr);
						} catch (Throwable th) {
							sout.print("?");
						}
						sout.println();
					}
					sout.close();
				} catch (Throwable th) {
					th.printStackTrace();	
				}
			}
			PrintWriter out = new PrintWriter(new File(outputDir, "out.h"));
			HeadersReconstructor.reconstructHeaders(libraries, out);
			out.close();
		} catch (Exception ex) {
			ex.printStackTrace();
			exit(1);
		}
	}
}
