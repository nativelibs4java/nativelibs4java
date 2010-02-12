/**
 * 
 */
package com.bridj;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.bridj.ann.This;
import com.bridj.ann.Virtual;

public class NativeLibrary {
	long handle;
	String path;
	Map<Class<?>, long[]> callbacks = new HashMap<Class<?>, long[]>();
	
	Map<Long, String> addrToName;
	Map<String, Long> nameToAddr;
	
	Map<Class<? extends CPPObject>, Pointer<Pointer<?>>> vtables = new HashMap<Class<? extends CPPObject>, Pointer<Pointer<?>>>();
	protected NativeLibrary(String path, long handle) {
		this.path = path;
		this.handle = handle;
	}
	
	public static NativeLibrary load(String path) {
		long handle = JNI.loadLibrary(path);
		if (handle == 0)
			return null;
		
		return new NativeLibrary(path, handle);
	}
	
	public void register(Class<?> type) throws FileNotFoundException {
		if (callbacks.get(type) != null)
			return; // already registered
		do {
			
			try {
				List<MethodCallInfo> methodInfos = new ArrayList<MethodCallInfo>();
			
				boolean isCPPClass = CPPObject.class.isAssignableFrom(type);
				Pointer<Pointer<?>> pVirtualTable = isCPPClass ? getVirtualTable((Class<? extends CPPObject>)type) : null;
				for (Method method : type.getDeclaredMethods()) {
					try {
						int modifiers = method.getModifiers();
						if (!Modifier.isNative(modifiers))
							continue;
						
						MethodCallInfo mci = new MethodCallInfo(method, this);
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
//								Logger.getLogger(getClass().getName()).log(Level.WARNING, "Method " + method.toGenericString() + " is native and maps to a C++ instance method. It should not be static.");
//								continue;
//							}
							
							if (!isCPPClass) {
								Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Method " + method.toGenericString() + " should have been declared in a " + CPPObject.class.getName() + " subclass.");
								continue;
							}
							Virtual va = method.getAnnotation(Virtual.class);
							if (va == null) {
								if (mci.forwardedPointer == 0) {
									for (String symbol : getSymbols()) {
										if (methodMatchesSymbol(type, method, symbol)) {
											mci.forwardedPointer = getSymbolAddress(symbol);
											if (mci.forwardedPointer != 0)
												break;
										}
									}
									if (mci.forwardedPointer == 0) {
										Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Method " + method.toGenericString() + " is not virtual but its address could not be resolved in the library.");
										continue;
									}
								}
							} else {
								if (pVirtualTable == null) {
									Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Method " + method.toGenericString() + " is virtual but the virtual table of class " + type.getName() + " was not found.");
									continue;
								}
								int virtualIndex = va.value() < 0 ? getPositionInVirtualTable(pVirtualTable, method) : va.value();
								if (virtualIndex < 0) {
									Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Method " + method.toGenericString() + " is virtual but its position could not be found in the virtual table.");
									continue;
								} else {
									mci.virtualIndex = virtualIndex;
								}
							}
						} else {
							if (!Modifier.isStatic(modifiers))
								Logger.getLogger(getClass().getName()).log(Level.WARNING, "Method " + method.toGenericString() + " is native and maps to a function, but is not static.");
							
						}
						methodInfos.add(mci);
					} catch (Exception ex) {
						Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Method " + method.toGenericString() + " cannot be mapped : " + ex, ex);
					}
				}
				callbacks.put(type, JNI.createCallbacks(methodInfos));
			} catch (Exception ex) {
				throw new RuntimeException("Failed to register class " + type.getName(), ex);
			}
			type = type.getSuperclass();
		} while (type != null && type != Object.class);
	}

	public boolean methodMatchesSymbol(Class declaringClass, Method method, String symbol) {
		return symbol.contains(method.getName()) && symbol.contains(declaringClass.getSimpleName());
	}

	
	public long getHandle() {
		if (handle == 0)
			throw new RuntimeException("Library was released and cannot be used anymore");
		return handle;
	}
	@Override
	protected void finalize() throws Throwable {
		release();
	}
	public synchronized void release() {
		if (handle == 0)
			return;
		
		for (long[] callbacks : this.callbacks.values())
		    JNI.freeCallbacks(callbacks);
		
		JNI.freeLibrary(handle);
		handle = 0;
	}
	public long getSymbolAddress(String name) {
		long address = JNI.findSymbolInLibrary(getHandle(), name);
		if (address == 0)
			address = JNI.findSymbolInLibrary(getHandle(), "_" + name);
		return address;
	}
	public int getPositionInVirtualTable(Method method) {
		Class type = method.getDeclaringClass();
		Pointer<Pointer<?>> pVirtualTable = getVirtualTable(type);
		return getPositionInVirtualTable(pVirtualTable, method);
	}
	public int getPositionInVirtualTable(Pointer<Pointer<?>> pVirtualTable, Method method) {
		String methodName = method.getName();
		Pointer<?> typeInfo = pVirtualTable.get(1);
		int methodsOffset = 2;
		for (int iVirtual = 0;; iVirtual++) {
			Pointer<?> pMethod = pVirtualTable.get(methodsOffset + iVirtual);
			if (pMethod == null)
				break;
			
			String virtualMethodName = getSymbolName(pMethod.getPeer());
			if (virtualMethodName.contains(methodName)) {
				// TODO cross check !!!
				return iVirtual;
			}
		}
		return -1;
	}
	public Pointer<Pointer<?>> getVirtualTable(Class<? extends CPPObject> type) {
		Pointer<Pointer<?>> p = vtables.get(type);
		if (p == null) {
			String className = type.getSimpleName();
			String vtableSymbolName = "_ZTV" + className.length() + className;
			long addr = JNI.findSymbolInLibrary(getHandle(), vtableSymbolName);
			if (addr == 0) {
				vtableSymbolName = "_@..." + className.length() + className;
				addr = JNI.findSymbolInLibrary(getHandle(), vtableSymbolName);
			}
			p = (Pointer)Pointer.pointerToAddress(addr, Pointer.class);
			vtables.put(type, p);
		}
		return p;
	}
	public Set<String> getSymbols() throws Exception {
		scanSymbols();
		return Collections.unmodifiableSet(nameToAddr.keySet());
	}
	public String getSymbolName(long address) {
		return JNI.findSymbolName(getHandle(), address);
	}
	void scanSymbols() throws Exception {
		if (addrToName != null)
			return;
		
		addrToName = new HashMap<Long, String>();
		nameToAddr = new HashMap<String, Long>();
		
		String[] symbs = null;
		try {
			if (JNI.isMacOSX()) {
				Process process = Runtime.getRuntime().exec(new String[] {"nm", "-gj", path});
				BufferedReader rin = new BufferedReader(new InputStreamReader(process.getInputStream()));
				String line;
				List<String> symbsList = new ArrayList<String>();
				while ((line = rin.readLine()) != null) {
					symbsList.add(line);
				}
				symbs = symbsList.toArray(new String[symbsList.size()]);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		if (symbs == null)
			symbs = JNI.getLibrarySymbols(getHandle());
		
		for (String name : symbs) {
			long addr = JNI.findSymbolInLibrary(getHandle(), name);
			if (name.startsWith("_")) {
				name = name.substring(1);
				addr = JNI.findSymbolInLibrary(getHandle(), name);
			}
			if (addr == 0) {
				System.out.println("Symbol '" + name + "' not found.");
				continue;
			}
			addrToName.put(addr, name);
			nameToAddr.put(name, addr);
			//System.out.println("'" + name + "' = \t" + TestCPP.hex(addr));
		}
	}
}