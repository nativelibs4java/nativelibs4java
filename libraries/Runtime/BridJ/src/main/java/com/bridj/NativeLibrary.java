/**
 * 
 */
package com.bridj;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
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

import com.bridj.Demangler.DemanglingException;
import com.bridj.Demangler.MemberRef;
import com.bridj.ann.Mangling;
import com.bridj.ann.This;
import com.bridj.ann.Virtual;
import java.util.Collection;

public class NativeLibrary {
	long handle, symbols;
	String path;
	//Map<Class<?>, long[]> callbacks = new HashMap<Class<?>, long[]>();
	NativeEntities nativeEntities = new NativeEntities();
	
	Map<Long, Demangler.Symbol> addrToName;
	Map<String, Long> nameToAddr;
	
	Map<Class<?>, Pointer<Pointer<?>>> vtables = new HashMap<Class<?>, Pointer<Pointer<?>>>();
	protected NativeLibrary(String path, long handle, long symbols) {
		this.path = path;
		this.handle = handle;
		this.symbols = symbols;
	}
	
	long getSymbolsHandle() {
		return symbols;
	}
	public NativeEntities getNativeEntities() {
		return nativeEntities;
	}
	public static NativeLibrary load(String path) {
		long handle = JNI.loadLibrary(path);
		if (handle == 0)
			return null;
		long symbols = JNI.loadLibrarySymbols(handle);
		return new NativeLibrary(path, handle, symbols);
	}
	
	public boolean methodMatchesSymbol(Class<?> declaringClass, Method method, String symbol) {
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
		
		nativeEntities.release();
		
		JNI.freeLibrary(handle);
		JNI.freeLibrarySymbols(symbols);
		handle = 0;
	}
	public long getSymbolAddress(String name) {
		long address = JNI.findSymbolInLibrary(getHandle(), name);
		if (address == 0)
			address = JNI.findSymbolInLibrary(getHandle(), "_" + name);
		return address;
	}

    synchronized long getSymbolAddress(AnnotatedElement member) throws FileNotFoundException {
        //libHandle = libHandle & 0xffffffffL;
        Mangling mg = BridJ.getAnnotation(Mangling.class, false, member);
        if (mg != null)
            for (String name : mg.value())
            {
                long handle = getSymbolAddress(name);
                if (handle != 0)
                    return handle;
            }

        String name = null;
        if (member instanceof Member)
            name = ((Member)member).getName();
        
        if (name != null) {
            long handle = getSymbolAddress(name);
            if (handle != 0)
                return handle;
        }

        if (member instanceof Method) {
            Method method = (Method)member;
            for (Demangler.Symbol symbol : getSymbols()) {
                if (symbol.matches(method))
                    return symbol.getAddress();
            }
        }
        return 0;
    }
	public int getPositionInVirtualTable(Method method) {
		Class<?> type = method.getDeclaringClass();
		Pointer<Pointer<?>> pVirtualTable = getVirtualTable(type);
		return getPositionInVirtualTable(pVirtualTable, method);
	}
	public int getPositionInVirtualTable(Pointer<Pointer<?>> pVirtualTable, Method method) {
		String methodName = method.getName();
		//Pointer<?> typeInfo = pVirtualTable.get(1);
		int methodsOffset = isMSVC() ? 0 : 2;
		String className = getCPPClassName(method.getDeclaringClass());
		for (int iVirtual = 0;; iVirtual++) {
			Pointer<?> pMethod = pVirtualTable.get(methodsOffset + iVirtual);
			if (pMethod == null)
				break;
			
			String virtualMethodName = getSymbolName(pMethod.getPeer());
			if (virtualMethodName == null)
				return -1;
			
			if (virtualMethodName != null && virtualMethodName.contains(methodName)) {
				// TODO cross check !!!
				return iVirtual;
			} else if (isMSVC() && !virtualMethodName.contains(className))
				break; // no NULL terminator in MSVC++ vtables, so we have to guess when we've reached the end
		}
		return -1;
	}
	boolean isMSVC() {
		return JNI.isWindows();
	}
	private String getCPPClassName(Class<?> declaringClass) {
		return declaringClass.getSimpleName();
	}

	@SuppressWarnings("unchecked")
	public Pointer<Pointer<?>> getVirtualTable(Class<?> type) {
		Pointer<Pointer<?>> p = vtables.get(type);
		if (p == null) {
			String className = type.getSimpleName();
			String vtableSymbolName;
            if (JNI.isWindows())
                vtableSymbolName = "??_7" + className + "@@6B@";
            else
                vtableSymbolName = "_ZTV" + className.length() + className;

			long addr = JNI.findSymbolInLibrary(getHandle(), vtableSymbolName);
			p = (Pointer)Pointer.pointerToAddress(addr, Pointer.class);
			vtables.put(type, p);
		}
		return p;
	}
	Collection<Demangler.Symbol> getSymbols() {
        try {
            scanSymbols();
        } catch (Exception ex) {
            Logger.getLogger(NativeLibrary.class.getName()).log(Level.SEVERE, null, ex);
        }
		return Collections.unmodifiableCollection(addrToName.values());
	}
	public String getSymbolName(long address) {
		if (addrToName == null && getSymbolsHandle() != 0)//JNI.isUnix())
			return JNI.findSymbolName(getHandle(), getSymbolsHandle(), address);
		
		try {
			scanSymbols();
			Demangler.Symbol symbol = addrToName.get(address);
			return symbol == null ? null : symbol.symbol;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to get name of address " + address, ex);
		}
	}
	void scanSymbols() throws Exception {
		if (addrToName != null)
			return;
		
		addrToName = new HashMap<Long, Demangler.Symbol>();
		nameToAddr = new HashMap<String, Long>();
		
		String[] symbs = null;
		if (true) // TODO turn to false !!!
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
			symbs = JNI.getLibrarySymbols(getHandle(), getSymbolsHandle());
		
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
			addrToName.put(addr, new Demangler.Symbol(name, this));
			nameToAddr.put(name, addr);
			//System.out.println("'" + name + "' = \t" + TestCPP.hex(addr));
		}
	}

	public void getCPPConstructor(Constructor constructor) {
		//TODO
	}

	public MemberRef parseSymbol(String symbol) throws DemanglingException {
		Demangler demangler;
		if (JNI.isWindows())
			demangler = new VC9Demangler(symbol);
		else
			demangler = new GCC4Demangler(symbol);
		return demangler.parseSymbol();
	}
}