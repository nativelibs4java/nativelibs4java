package com.bridj;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class NativeEntities {
	Map<Class<?>, long[]> functions = new HashMap<Class<?>, long[]>();
	//Map<Class<?>, long[]> getters;
	//Map<Class<?>, long[]> setters;
	
	public static class Builder {
		List<MethodCallInfo> functionInfos = new ArrayList<MethodCallInfo>();
		//List<MethodCallInfo> setterInfos = new ArrayList<MethodCallInfo>();
		//List<MethodCallInfo> getterInfos = new ArrayList<MethodCallInfo>();
		
		public void addFunction(MethodCallInfo info) {
			functionInfos.add(info);
		}
		public void addCPPMethod(MethodCallInfo info) {
			addFunction(info);
		}
		public void addGetter(MethodCallInfo info) {
			addFunction(info);
		}
		public void addSetter(MethodCallInfo info) {
			addFunction(info);
		}
	}
	
	
	public void release() {
		for (long[] callbacks : functions.values())
		    JNI.freeCallbacks(callbacks);
	}
	@Override
	public void finalize() {
		release();
	}
	public void addDefinitions(Class<?> type, Builder builder) {
		if (!builder.functionInfos.isEmpty())
			functions.put(type, JNI.createCallbacks(builder.functionInfos));
		
	}
}