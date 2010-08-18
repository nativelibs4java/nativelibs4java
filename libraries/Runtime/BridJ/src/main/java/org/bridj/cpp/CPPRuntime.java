/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bridj.cpp;

import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.bridj.BridJ;
import org.bridj.JNI;
import org.bridj.MethodCallInfo;
import org.bridj.NativeLibrary;
import org.bridj.NativeObject;
import org.bridj.Pointer;
import org.bridj.PointerIO;
import org.bridj.TestCPP;

import static org.bridj.Dyncall.CallingConvention.*;

import org.bridj.Demangler.Symbol;
import org.bridj.NativeEntities.Builder;
import org.bridj.ann.Virtual;
import org.bridj.CRuntime;
import org.bridj.Dyncall;
import org.bridj.NativeLibrary.SymbolAccepter;
import org.bridj.Utils;
import org.bridj.util.AutoHashMap;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import static org.bridj.Pointer.*;

/**
 *
 * @author Olivier
 */
public class CPPRuntime extends CRuntime {

    @Override
    public <T extends NativeObject> Class<? extends T> getActualInstanceClass(Pointer<T> pInstance, Type officialType) {
        //String className = null;
        // For C++ classes in general, take type info at offset -1 of vtable (if first field matches the address of a known static or dynamic virtual table) and use it to create the correct instance.
//		Pointer<?> vptr = pInstance.getPointer(0);
//		Symbol symbol = BridJ.getSymbolByAddress(vptr.getPeer());
//		if (symbol != null && symbol.isVirtualTable()) {
//			if (symbol.enclosingType.matches(officialType))
//				return officialType;
//			
//			try {
//				Class<?> type = BridJ.getCPPRuntime().getCPPClass(symbol.enclosingType);
//				if (officialType == null || officialType.isAssignableFrom(type))
//					return type;
//			} catch (ClassNotFoundException ex) {}
//			return officialType;
//			
//			/*long tinf = JNI.get_pointer(ptr - Pointer.SIZE);
//			symbol = BridJ.getSymbolByAddress(tinf);
//			if (symbol != null && symbol.isTypeInfo()) {
//				
//			}*/
//		}
        // For Objective-C classes, use "const char* className = class_getName([yourObject class]);" and match to registered classes or more
        // Bundle auto-generated type mappings files : bridj::CPPTest=org.bridj.test.cpp.CPPTest
        // 
        return Utils.getClass(officialType);
    }
    Map<Class<?>, Integer> virtualMethodsCounts = new HashMap<Class<?>, Integer>();

    public int getVirtualMethodsCount(Class<?> type) {
        Integer count = virtualMethodsCounts.get(type);
        if (count == null) {
            List<Method> mets = new ArrayList<Method>();
            listVirtualMethods(type, mets);

            // TODO unify this !
            virtualMethodsCounts.put(type, count = mets.size());
        }
        return count;
    }

    public void listVirtualMethods(Class<?> type, List<Method> out) {
        if (!CPPObject.class.isAssignableFrom(type)) {
            return;
        }

        for (Method m : type.getDeclaredMethods()) {
            if (m.getAnnotation(Virtual.class) != null) {
                out.add(m);
            }
        }

        type = type.getSuperclass();
        if (type != CPPObject.class) {
            listVirtualMethods(type, out);
        }
    }

    @Override
    protected void registerNativeMethod(Class<?> type, NativeLibrary typeLibrary, Method method, NativeLibrary methodLibrary, Builder builder) throws FileNotFoundException {

        int modifiers = method.getModifiers();
        boolean isCPPClass = CPPObject.class.isAssignableFrom(method.getDeclaringClass());

//		Annotation[][] anns = method.getParameterAnnotations();
        if (!isCPPClass) {
            super.registerNativeMethod(type, typeLibrary, method, methodLibrary, builder);
            return;
        }

        MethodCallInfo mci = new MethodCallInfo(method);

        Virtual va = method.getAnnotation(Virtual.class);
        if (va == null) {
            methodLibrary.getSymbol(method);
            Symbol symbol = methodLibrary.getSymbol(method);
            mci.setForwardedPointer(symbol == null ? 0 : symbol.getAddress());
            if (mci.getForwardedPointer() == 0) {
//				for (Demangler.Symbol symbol : methodLibrary.getSymbols()) {
//					if (symbol.matches(method)) {
//						mci.setForwardedPointer(symbol.getAddress());
//						if (mci.getForwardedPointer() != 0)
//							break;
//					}
//				}
//				if (mci.getForwardedPointer() == 0) {
                assert log(Level.SEVERE, "Method " + method.toGenericString() + " is not virtual but its address could not be resolved in the library.");
                return;
//				}
            }
            if (Modifier.isStatic(modifiers)) {
                builder.addFunction(mci);
                assert log(Level.INFO, "Registering " + method + " as function or static C++ method " + symbol.getName());
            } else {
                //if (!JNI.is64Bits() && JNI.isWindows())
                //    mci.setDcCallingConvention(DC_CALL_C_X86_WIN32_THIS_MS);
                //builder.addMethodFunction(mci);
                builder.addFunction(mci);
                log(Level.INFO, "Registering " + method + " as C++ method " + symbol.getName());
            }
        } else {
            int virtualIndex = va.value();
            if (Modifier.isStatic(modifiers)) {
                log(Level.WARNING, "Method " + method.toGenericString() + " is native and maps to a function, but is not static.");
            }

            if (virtualIndex < 0) {
                Pointer<Pointer<?>> pVirtualTable = isCPPClass && typeLibrary != null ? (Pointer)pointerToAddress(getVirtualTable(type, typeLibrary), Pointer.class) : null;
                if (pVirtualTable == null) {
                    log(Level.SEVERE, "Method " + method.toGenericString() + " is virtual but the virtual table of class " + type.getName() + " was not found.");
                    return;
                }

                virtualIndex = getPositionInVirtualTable(pVirtualTable, method, typeLibrary);
                if (virtualIndex < 0) {
                    log(Level.SEVERE, "Method " + method.toGenericString() + " is virtual but its position could not be found in the virtual table.");
                    return;
                }
            }
            Class<?> superclass = type.getSuperclass();
            int virtualOffset = getVirtualMethodsCount(superclass);
            boolean isNewVirtual = true;
            if (superclass != null) {
            	try {
            		// TODO handle polymorphism in overloads :
            		superclass.getMethod(method.getName(), method.getParameterTypes());
            		isNewVirtual = false;
            	} catch (NoSuchMethodException ex) {}
            }
            int absoluteVirtualIndex = isNewVirtual ? virtualOffset + virtualIndex : virtualIndex;
            mci.setVirtualIndex(absoluteVirtualIndex);
            log(Level.INFO, "Registering " + method.toGenericString() + " as virtual C++ method with relative virtual index = " + virtualIndex + ", absolute index = " + absoluteVirtualIndex);
            //if (!JNI.is64Bits() && JNI.isWindows())
            //    mci.setDcCallingConvention(DC_CALL_C_X86_WIN32_THIS_MS);
            builder.addVirtualMethod(mci);
        }
    }
    int getPositionInVirtualTable(Method method, NativeLibrary library) {
		Class<?> type = method.getDeclaringClass();
		Pointer<Pointer<?>> pVirtualTable = (Pointer)pointerToAddress(getVirtualTable(type, library), Pointer.class);
		return getPositionInVirtualTable(pVirtualTable, method, library);
	}
    String getCPPClassName(Class<?> declaringClass) {
		return declaringClass.getSimpleName();
	}

	public int getPositionInVirtualTable(Pointer<Pointer<?>> pVirtualTable, Method method, NativeLibrary library) {
		String methodName = method.getName();
		//Pointer<?> typeInfo = pVirtualTable.get(1);
		int methodsOffset = library.isMSVC() ? 0 : -2;///2;
		String className = getCPPClassName(method.getDeclaringClass());
		for (int iVirtual = 0;; iVirtual++) {
			Pointer<?> pMethod = pVirtualTable.get(methodsOffset + iVirtual);
			String virtualMethodName = pMethod == null ? null : library.getSymbolName(pMethod.getPeer());
			//System.out.println("#\n# At index " + methodsOffset + " + " + iVirtual + " of vptr for class " + className + ", found symbol " + Long.toHexString(pMethod.getPeer()) + " = '" + virtualMethodName + "'\n#");
			if (virtualMethodName == null)
				return -1;
			
			if (virtualMethodName != null && virtualMethodName.contains(methodName)) {
				// TODO cross check !!!
				return iVirtual;
			} else if (library.isMSVC() && !virtualMethodName.contains(className))
				break; // no NULL terminator in MSVC++ vtables, so we have to guess when we've reached the end
		}
		return -1;
	}
    Map<Type, Long> defaultConstructors = new HashMap<Type, Long>();
    Map<Type, Long> destructors = new HashMap<Type, Long>();

    static int getDefaultDyncallCppConvention() {
        int convention = DC_CALL_C_DEFAULT;
        if (!JNI.is64Bits() && JNI.isWindows()) {
            convention = DC_CALL_C_X86_WIN32_THIS_MS;
        }
        return convention;
    }
    
    static Boolean enableDestructors;
    static boolean enableDestructors() {
		if (enableDestructors == null) {
			String prop = System.getProperty("bridj.destructors"), env = System.getenv("BRIDJ_DESTRUCTORS"); 
			boolean forceTrue = "true".equals(prop) || "1".equals(env);
			boolean forceFalse = "false".equals(prop) || "0".equals(env);
			boolean shouldBeStable = true;//JNI.isWindows();
			enableDestructors = forceTrue || shouldBeStable && !forceFalse;
		}
		return enableDestructors;
    }

    protected <T extends CPPObject> Pointer<T> newCPPInstance(final Type type, int constructorId, Object... args) {
        Pointer<T> peer = null;
        try {
            final Class<T> typeClass = Utils.getClass(type);
            NativeLibrary lib = BridJ.getNativeLibrary(typeClass);
            Pointer.Releaser releaser = null;

            if (enableDestructors()) {
				Long destructor = destructors.get(type);
				if (destructor == null) {
					Symbol symbol = lib.getFirstMatchingSymbol(new SymbolAccepter() { public boolean accept(Symbol symbol) {
						return symbol.matchesDestructor(typeClass);
					}});
					if (symbol != null)
						log(Level.INFO, "Registering destructor of " + typeClass.getName() + " as " + symbol.getName());
					destructors.put(type, destructor = symbol == null ? 0 : symbol.getAddress());
				}
				final long destructorAddr = destructor;
				if (destructorAddr != 0) {
					releaser = new Pointer.Releaser() { @Override public void release(Pointer<?> p) {
						//System.out.println("Destructing instance of C++ type " + type + "...");
						JNI.callSinglePointerArgVoidFunction(destructorAddr, p.getPeer(), getDefaultDyncallCppConvention());
					}};
				}
			}
            // TODO handle templates here
            peer = (Pointer) Pointer.allocateBytes(PointerIO.getInstance(type), sizeOf(type, null), releaser); 

            if (constructorId < 0) {
                Long defaultConstructor = defaultConstructors.get(type);
                if (defaultConstructor == null) {
                    Symbol symbol = lib.getFirstMatchingSymbol(new SymbolAccepter() { public boolean accept(Symbol symbol) {
                    return symbol.matchesConstructor(typeClass);
                }});
                    if (symbol != null)
                        log(Level.INFO, "Registering default constructor of " + typeClass.getName() + " as " + symbol.getName());
                    defaultConstructors.put(type, defaultConstructor = symbol == null ? 0 : symbol.getAddress());
                    if (defaultConstructor.longValue() == 0)
                        throw new RuntimeException("Cannot find the default constructor for type " + typeClass.getName());
                }
                installVTablePtr(type, lib, peer);
                JNI.callSinglePointerArgVoidFunction(defaultConstructor, peer.getPeer(), getDefaultDyncallCppConvention());// TODO use right call convention
                //TestCPP.print(typeClass.getSimpleName(), peer.getPeer(), 10, 5);
                //TestCPP.print(typeClass.getSimpleName() + "'s vtable", peer.getSizeT(0), 10, 5);
                return peer;
            }
            Method meth = getConstructor(typeClass, constructorId, args);
            Object[] consArgs = new Object[args.length + 1];
            if (CPPObject.class.isAssignableFrom(typeClass)) {
                if (meth.getReturnType() != Void.TYPE) {
                    throw new RuntimeException("Constructor-mapped methods must return void, but " + meth.getName() + " returns " + meth.getReturnType().getName());
                }
                if (!Modifier.isStatic(meth.getModifiers())) {
                    throw new RuntimeException("Constructor-mapped methods must be static, but " + meth.getName() + " is not.");
                }
                if (!meth.getName().equals(typeClass.getSimpleName())) {
                    throw new RuntimeException("Constructor methods must have the same name as their class : " + meth.getName() + " is not " + typeClass.getSimpleName());
                }
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
                } catch (Exception ex) {
                }
            }
            return peer;
        } catch (Exception ex) {
            ex.printStackTrace();
            if (peer != null) {
                peer.release();
            }
            throw new RuntimeException("Failed to allocate new instance of type " + type, ex);
        }
    }
    
    
    /*
	Map<Type, Pointer<Pointer<?>>> vtablePtrs = new HashMap<Type, Pointer<Pointer<?>>>();
	@SuppressWarnings("unchecked")
	public
	//Pointer<Pointer<?>>
    long getVirtualTable(Type type, NativeLibrary library) {
		Pointer<Pointer<?>> p = vtablePtrs.get(type);
		if (p == null) {
			Class<?> typeClass = Utils.getClass(type);
			// TODO ask for real template name
			String className = typeClass.getSimpleName();
			String vtableSymbol;
            if (JNI.isWindows())
                vtableSymbol = "??_7" + className + "@@6B@";
            else
                vtableSymbol = "_ZTV" + className.length() + className;

            long addr = library.getSymbolAddress(vtableSymbol);
			//long addr = JNI.findSymbolInLibrary(getHandle(), vtableSymbolName);
//			System.out.println(TestCPP.hex(addr));
//			TestCPP.print(type.getName() + " vtable", addr, 5, 2);
        	
			p = (Pointer)Pointer.pointerToAddress(addr, Pointer.class);
			vtablePtrs.put(type, p);
		}
		return p.getPeer();
	}*/
    
    Map<Type, Long> vtables = new HashMap<Type, Long>();
	long getVirtualTable(Type type, NativeLibrary library) {
        Long vtable = vtables.get(type);
        if (vtable == null) {
            final Class<?> typeClass = Utils.getClass(type);
            if (false) {
	            String className = typeClass.getSimpleName();
				String vtableSymbol;
	            if (JNI.isWindows())
	                vtableSymbol = "??_7" + className + "@@6B@";
	            else
	                vtableSymbol = "_ZTV" + className.length() + className;
	
				vtables.put(type, vtable = library.getSymbolAddress(vtableSymbol));
			} else {
				Symbol symbol = library.getFirstMatchingSymbol(new SymbolAccepter() { public boolean accept(Symbol symbol) { 
					return symbol.matchesVirtualTable(typeClass);
				}});
				if (symbol != null)
					log(Level.INFO, "Registering vtable of " + typeClass.getName() + " as " + symbol.getName());
				vtables.put(type, vtable = symbol == null ? 0 : symbol.getAddress());//*/
			}
        }
        return vtable;
    }
    boolean installVTablePtr(Type type, NativeLibrary lib, Pointer<?> peer) {
        long vtable = getVirtualTable(type, lib);
        //Pointer<Pointer<?>> vptr = lib.getVirtualTable(type);
//    	if (!lib.isMSVC());
////    		vptr = vptr.offset(16);
//    		vptr = vptr.next(2);
        if (vtable != 0)
            peer.setSizeT(0, vtable);
        return vtable != 0;
    }

    /// Needs not be fast : TypeInfo will be cached in BridJ anyway !
    @Override
    public <T extends NativeObject> TypeInfo<T> getTypeInfo(final Type type) {
        return new CTypeInfo<T>(type) {

            @Override
            public long sizeOf(T instance) {
                // TODO handle template size here (depends on template args)
                return super.sizeOf(instance);
            }

            @SuppressWarnings("unchecked")
            @Override
            public void initialize(T instance, int constructorId, Object... args) {
                if (instance instanceof CPPObject) {
                    //instance.peer = allocate(instance.getClass(), constructorId, args);
                    setNativeObjectPeer(instance, newCPPInstance((Class<? extends CPPObject>) typeClass, constructorId, args));
                } else {
                    super.initialize(instance, constructorId, args);
                }
            }

            @Override
            public T clone(T instance) throws CloneNotSupportedException {
                if (instance instanceof CPPObject) {
                    // TODO use copy constructor !!!
                }
                return super.clone(instance);
            }

            @Override
            public void destroy(T instance) {
                //TODO call destructor here ? (and call here from finalizer manually created by autogenerated classes
            }
        };
    }
}
