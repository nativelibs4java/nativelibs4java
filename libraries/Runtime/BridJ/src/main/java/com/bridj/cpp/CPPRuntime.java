/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.bridj.cpp;

import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import com.bridj.BridJ;
import com.bridj.JNI;
import com.bridj.MethodCallInfo;
import com.bridj.NativeLibrary;
import com.bridj.NativeObject;
import com.bridj.Pointer;
import com.bridj.PointerIO;
import com.bridj.TestCPP;

import static com.bridj.Dyncall.CallingConvention.*;

import com.bridj.Demangler.Symbol;
import com.bridj.NativeEntities.Builder;
import com.bridj.ann.Virtual;
import com.bridj.CRuntime;
import com.bridj.util.AutoHashMap;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Olivier
 */
public class CPPRuntime extends CRuntime {
	@Override
	public <T extends NativeObject> Class<? extends T> getActualInstanceClass(Pointer<T> pInstance, Class<T> officialType) {
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
		// Bundle auto-generated type mappings files : bridj::CPPTest=com.bridj.test.cpp.CPPTest
		// 
		return officialType;
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
        if (!CPPObject.class.isAssignableFrom(type))
            return;

        for (Method m : type.getDeclaredMethods())
            if (m.getAnnotation(Virtual.class) != null)
                out.add(m);

        type = type.getSuperclass();
        if (type != CPPObject.class)
            listVirtualMethods(type, out);
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
                builder.addMethodFunction(mci);
                log(Level.INFO, "Registering " + method + " as C++ method " + symbol.getName());
            }
		} else {
			int virtualIndex = va.value();
			if (Modifier.isStatic(modifiers))
				log(Level.WARNING, "Method " + method.toGenericString() + " is native and maps to a function, but is not static.");
				
			if (virtualIndex < 0) {
				Pointer<Pointer<?>> pVirtualTable = isCPPClass && typeLibrary != null ? typeLibrary.getVirtualTable((Class<? extends CPPObject>)type) : null;
				if (pVirtualTable == null) {
					log(Level.SEVERE, "Method " + method.toGenericString() + " is virtual but the virtual table of class " + type.getName() + " was not found.");
					return;
				}
				
				virtualIndex = typeLibrary.getPositionInVirtualTable(pVirtualTable, method);
				if (virtualIndex < 0) {
					log(Level.SEVERE, "Method " + method.toGenericString() + " is virtual but its position could not be found in the virtual table.");
					return;
				}
			}
            int virtualOffset = getVirtualMethodsCount(type.getSuperclass());
            int absoluteVirtualIndex = virtualOffset + virtualIndex;
			mci.setIndex(absoluteVirtualIndex);
			log(Level.INFO, "Registering " + method.toGenericString() + " as virtual C++ method with relative virtual index = " + virtualIndex + ", absolute index = " + absoluteVirtualIndex);
            //if (!JNI.is64Bits() && JNI.isWindows())
            //    mci.setDcCallingConvention(DC_CALL_C_X86_WIN32_THIS_MS);
            builder.addVirtualMethod(mci);
		}
	}
	
	static Map<Class<?>, Long> defaultConstructors = new HashMap<Class<?>, Long>();
    
	protected <T extends CPPObject> Pointer<T> newCPPInstance(Class<T> type, int constructorId, Object... args) {
    	Pointer<T> peer = null;
        try {
        	peer = (Pointer)Pointer.allocate(PointerIO.getInstance(type), sizeOf(type, type, null)); // TODO handle templates here
        	NativeLibrary lib = BridJ.getNativeLibrary(type);
        	if (constructorId < 0) {
        		Long defaultConstructor = defaultConstructors.get(type);
        		if (defaultConstructor == null) {
        			Symbol constructorSymbol = null;
        			for (Symbol symbol : lib.getSymbols()) {
        				if (symbol.matchesConstructor(type)) {
        					constructorSymbol = symbol;
        					defaultConstructor = symbol.getAddress();
        					break;
        				}
        			}
        			if (defaultConstructor == null || defaultConstructor == 0)
        				throw new RuntimeException("Cannot find the default constructor for type " + type.getName());
        			
        			log(Level.INFO, "Registering constructor of " + type.getName() + " as " + constructorSymbol.getName());
        			defaultConstructors.put(type, defaultConstructor);
        		}
    			installVTablePtr(type, lib, peer);
                int convention = DC_CALL_C_DEFAULT;
                if (!JNI.is64Bits() && JNI.isWindows())
                    convention = DC_CALL_C_X86_WIN32_THIS_MS;
    			JNI.callDefaultCPPConstructor(defaultConstructor, peer.getPeer(), convention);// TODO use right call convention
    			TestCPP.print(type.getSimpleName(), peer.getPeer(), 10, 2);
    			return peer;
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
        	ex.printStackTrace();
        	if (peer != null)
        		peer.release();
        	throw new RuntimeException("Failed to allocate new instance of type " + type, ex);
        }
	}
	static void installVTablePtr(Class<?> type, NativeLibrary lib, Pointer<?> peer) {
    	Pointer<Pointer<?>> vptr = lib.getVirtualTable(type);
//    	if (!lib.isMSVC());
////    		vptr = vptr.offset(16);
//    		vptr = vptr.next(2);
    	peer.setPointer(0, vptr);
    }

    @Override
    public void destroy(NativeObject instance) {
        // TODO call C++ destructor if needed
        super.destroy(instance);
    }

    @SuppressWarnings("unchecked")
	@Override
    public void initialize(NativeObject instance, int constructorId, Object... args) {
        if (instance instanceof CPPObject) {
            //instance.peer = allocate(instance.getClass(), constructorId, args);
        	setNativeObjectPeer(instance, newCPPInstance((Class<? extends CPPObject>)instance.getClass(), constructorId, args));
        }
        else
            super.initialize(instance, constructorId, args);
	}
    
    public <T extends NativeObject> T clone(T instance) throws CloneNotSupportedException {
    	if (instance instanceof CPPObject) {
			// TODO use copy constructor !!!
    	}
    	return super.clone(instance);
    }
}
