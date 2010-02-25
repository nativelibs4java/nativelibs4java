package com.bridj.c;

import java.io.FileNotFoundException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import com.bridj.BridJ;
import com.bridj.BridJRuntime;
import com.bridj.MethodCallInfo;
import com.bridj.NativeEntities;
import com.bridj.NativeLibrary;
import com.bridj.NativeObject;
import com.bridj.Pointer;
import com.bridj.NativeEntities.Builder;
import com.bridj.ann.This;
import com.bridj.ann.Virtual;
import com.bridj.cpp.CPPObject;
import com.bridj.util.AutoHashMap;

public class CRuntime extends BridJRuntime {

	Set<Method> registeredMethods = new HashSet<Method>();
	CallbackNativeImplementer callbackNativeImplementer = new CallbackNativeImplementer(BridJ.getOrphanEntities());
	
	@Override
	public <T extends NativeObject> Class<? extends T> getActualInstanceClass(Pointer<T> pInstance, Class<T> officialType) {
		return officialType;
	}

	@Override
	public void register(Class<?> type) {
		int typeModifiers = type.getModifiers();
		if (Callback.class.isAssignableFrom(type)) {
			if (Modifier.isAbstract(typeModifiers))
			callbackNativeImplementer.getCallbackImplType((Class) type);
		}
		
		AutoHashMap<NativeEntities, NativeEntities.Builder> builders = new AutoHashMap<NativeEntities, NativeEntities.Builder>(NativeEntities.Builder.class);
		for (; type != null && type != Object.class; type = type.getSuperclass()) {
			try {
				boolean isCPPClass = CPPObject.class.isAssignableFrom(type);
				NativeLibrary typeLibrary = BridJ.getNativeLibrary(type);
				for (Method method : type.getDeclaredMethods()) {
					try {
						int modifiers = method.getModifiers();
						if (!Modifier.isNative(modifiers))
							continue;
						
						NativeEntities.Builder builder = builders.get(BridJ.getNativeEntities(method));
						NativeLibrary methodLibrary = BridJ.getNativeLibrary(method);
						
						registerNativeMethod(type, typeLibrary, method, methodLibrary, builder);
						
					} catch (Exception ex) {
						log(Level.SEVERE, "Method " + method.toGenericString() + " cannot be mapped : " + ex, ex);
					}
				}
			} catch (Exception ex) {
				throw new RuntimeException("Failed to register class " + type.getName(), ex);
			}
		}
	}

	protected void registerNativeMethod(Class<?> type, NativeLibrary typeLibrary, Method method, NativeLibrary methodLibrary, Builder builder) throws FileNotFoundException {
		MethodCallInfo mci = new MethodCallInfo(method);
		mci.setForwardedPointer(methodLibrary.getSymbolAddress(method));
	    builder.addFunction(mci);
	}
	

	@Override
	public <T extends NativeObject> Pointer<T> allocate(Class<?> type, int constructorId, Object... args) {
	    if (Callback.class.isAssignableFrom(type)) {
        	if (constructorId != -1 || args.length != 0)
        		throw new RuntimeException("Callback should have a constructorId == -1 and no constructor args !");
        	return newCallbackInstance(type);
        }
        return super.allocate(type, constructorId, args);
	}
	
	static final int defaultObjectSize = 128;
	public static final String PROPERTY_bridj_c_defaultObjectSize = "bridj.c.defaultObjectSize";
	
	public int sizeOf(Class<?> type) {
    	String s = System.getProperty(PROPERTY_bridj_c_defaultObjectSize);
    	if (s != null)
    		try {
    			return Integer.parseInt(s);
	    	} catch (Throwable th) {
	    		log(Level.SEVERE, "Invalid value for property " + PROPERTY_bridj_c_defaultObjectSize + " : '" + s + "'");
	    	}
    	return defaultObjectSize;
    }
    
    private <T extends NativeObject> Pointer<T> newCallbackInstance(Class<?> type) {
//    	Method method = null;
    	Class<?> parent = null;
    	while ((parent = type.getSuperclass()) != null && parent != Callback.class) {
    		type = parent;
    	}
    	
    	register(type);
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
			mci.setJavaToCCallback(true);
			return null;//(Pointer)Pointer.pointerToAddress(JNI.createJavaToCCallbacks(mci), type);
		} catch (FileNotFoundException e) {
			throw new RuntimeException("Failed to create instance of callback " + type.getName(), e);
		}
	}
}