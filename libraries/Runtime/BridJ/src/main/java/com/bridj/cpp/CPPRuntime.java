/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.bridj.cpp;

import java.io.FileNotFoundException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import com.bridj.BridJ;
import com.bridj.Callback;
import com.bridj.Demangler;
import com.bridj.JNI;
import com.bridj.MethodCallInfo;
import com.bridj.NativeEntities;
import com.bridj.NativeLibrary;
import com.bridj.NativeObject;
import com.bridj.Pointer;
import com.bridj.PointerIO;

import static com.bridj.Dyncall.CallingConvention.*;

import com.bridj.Demangler.Symbol;
import com.bridj.NativeEntities.Builder;
import com.bridj.ann.This;
import com.bridj.ann.Virtual;
import com.bridj.CRuntime;
import com.bridj.util.AutoHashMap;

/**
 *
 * @author Olivier
 */
public class CPPRuntime extends CRuntime {
	@Override
	public <T extends NativeObject> Class<? extends T> getActualInstanceClass(Pointer<T> pInstance, Class<T> officialType) {
		String className = null;
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
	
	@Override
	protected void registerNativeMethod(Class<?> type, NativeLibrary typeLibrary, Method method, NativeLibrary methodLibrary, Builder builder) throws FileNotFoundException {

		int modifiers = method.getModifiers();
		boolean isCPPClass = CPPObject.class.isAssignableFrom(method.getDeclaringClass());
				
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
		if (isCPPInstanceMethod && !isCPPClass) {
			log(Level.SEVERE, "Method " + method.toGenericString() + " should have been declared in a " + CPPObject.class.getName() + " subclass.");
			return;
		}
				
		if (!isCPPInstanceMethod) {
			super.registerNativeMethod(type, typeLibrary, method, methodLibrary, builder);
			return;
		}
		
		MethodCallInfo mci = new MethodCallInfo(method);
		
		Virtual va = method.getAnnotation(Virtual.class);
		if (va == null) {
			mci.setForwardedPointer(methodLibrary.getSymbolAddress(method));
	        if (mci.getForwardedPointer() == 0) {
				for (Demangler.Symbol symbol : methodLibrary.getSymbols()) {
					if (symbol.matches(method)) {
						mci.setForwardedPointer(symbol.getAddress());
						if (mci.getForwardedPointer() != 0)
							break;
					}
				}
				if (mci.getForwardedPointer() == 0) {
					log(Level.SEVERE, "Method " + method.toGenericString() + " is not virtual but its address could not be resolved in the library.");
					return;
				}
			}
	        mci.setDcCallingConvention(!JNI.is64Bits() && JNI.isWindows() ? DC_CALL_C_X86_WIN32_THIS_MS : DC_CALL_C_DEFAULT);
			builder.addMethodFunction(mci);
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
			mci.setVirtualIndex(virtualIndex);
			builder.addVirtualMethod(mci);
		}
	}
	
	static Map<Class<?>, Long> defaultConstructors = new HashMap<Class<?>, Long>();
    
	protected <T extends NativeObject> Pointer<T> newCPPInstance(Class<?> type, int constructorId, Object... args) {
    	Pointer<T> peer = null;
        try {
        	peer = (Pointer)Pointer.allocate(PointerIO.getInstance(type), sizeOf(type));
        	NativeLibrary lib = BridJ.getNativeLibrary(type);
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

    @Override
    public void initialize(NativeObject instance, int constructorId, Object[] args) {
        if (CPPObject.class.isInstance(instance)) {
            //instance.peer = allocate(instance.getClass(), constructorId, args);
        	setNativeObjectPeer(instance, newCPPInstance(instance.getClass(), constructorId, args));
        }
        else
            super.initialize(instance, constructorId, args);
	}
}
