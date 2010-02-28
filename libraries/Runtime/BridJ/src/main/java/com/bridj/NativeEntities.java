package com.bridj;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NativeEntities {
	static class CBInfo {
		long handle;
		int size;
		public CBInfo(long handle, int size) {
			this.handle = handle;
			this.size = size;
		}
		
	}
	Map<Class<?>, CBInfo> 
		functions = new HashMap<Class<?>, CBInfo>(),
		virtualMethods = new HashMap<Class<?>, CBInfo>(),
		getters = new HashMap<Class<?>, CBInfo>(),
		setters = new HashMap<Class<?>, CBInfo>(),
		javaToNativeCallbacks = new HashMap<Class<?>, CBInfo>();
	//Map<Class<?>, long[]> getters;
	//Map<Class<?>, long[]> setters;
	
	public static class Builder {
		List<MethodCallInfo> functionInfos = new ArrayList<MethodCallInfo>();
		List<MethodCallInfo> virtualMethods = new ArrayList<MethodCallInfo>();
		List<MethodCallInfo> javaToNativeCallbacks = new ArrayList<MethodCallInfo>();
		List<MethodCallInfo> getters = new ArrayList<MethodCallInfo>();
		List<MethodCallInfo> setters = new ArrayList<MethodCallInfo>();
		//List<MethodCallInfo> getterInfos = new ArrayList<MethodCallInfo>();
		
		public void addFunction(MethodCallInfo info) {
			functionInfos.add(info);
		}
		public void addVirtualMethod(MethodCallInfo info) {
			virtualMethods.add(info);
		}
		public void addGetter(MethodCallInfo info) {
			getters.add(info);
		}
		public void addSetter(MethodCallInfo info) {
			setters.add(info);
		}
		public void addJavaToNativeCallback(MethodCallInfo info) {
			javaToNativeCallbacks.add(info);
		}
		public void addMethodFunction(MethodCallInfo info) {
			functionInfos.add(info);
		}
	}
	
	
	public void release() {
		for (CBInfo callbacks : functions.values())
		    JNI.freeCFunctionBindings(callbacks.handle, callbacks.size);
		
		for (CBInfo callbacks : javaToNativeCallbacks.values())
		    JNI.freeJavaToCCallbacks(callbacks.handle, callbacks.size);
		
		for (CBInfo callbacks : virtualMethods.values())
		    JNI.freeVirtualMethodBindings(callbacks.handle, callbacks.size);
	}
	@Override
	public void finalize() {
		release();
	}
	public void addDefinitions(Class<?> type, Builder builder) {
		int n;

		n = builder.functionInfos.size();
		if (n != 0)
			functions.put(type, new CBInfo(JNI.bindJavaMethodsToCFunctions(builder.functionInfos.toArray(new MethodCallInfo[n])), n));

		n = builder.virtualMethods.size();
		if (n != 0)
			virtualMethods.put(type, new CBInfo(JNI.bindJavaMethodsToVirtualMethods(builder.virtualMethods.toArray(new MethodCallInfo[n])), n));

		n = builder.javaToNativeCallbacks.size();
		if (n != 0)
			javaToNativeCallbacks.put(type, new CBInfo(JNI.bindJavaToCCallbacks(builder.javaToNativeCallbacks.toArray(new MethodCallInfo[n])), n));

//		n = builder.setters.size();
//		if (n != 0)
//			setters.put(type, new CBInfo(JNI.bindFieldGetters(builder.setters.toArray(new MethodCallInfo[n])), n));
		
	}
}