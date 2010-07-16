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
import com.bridj.Demangler.Symbol;
import com.bridj.ann.Virtual;
import com.bridj.cpp.GCC4Demangler;
import com.bridj.cpp.VC9Demangler;

import java.util.Collection;

public class NativeLibrary {
	long handle, symbols;
	String path;
	//Map<Class<?>, long[]> callbacks = new HashMap<Class<?>, long[]>();
	NativeEntities nativeEntities = new NativeEntities();
	
	Map<Long, Symbol> addrToName;
	Map<String, Symbol> nameToSym;
//	Map<String, Long> nameToAddr;
	
	Map<Class<?>, Pointer<Pointer<?>>> vtables = new HashMap<Class<?>, Pointer<Pointer<?>>>();
	protected NativeLibrary(String path, long handle, long symbols) {
		this.path = path;
		this.handle = handle;
		this.symbols = symbols;
	}
	
	long getSymbolsHandle() {
		return symbols;
	}
	NativeEntities getNativeEntities() {
		return nativeEntities;
	}
	public static NativeLibrary load(String path) {
		long handle = JNI.loadLibrary(path);
		if (handle == 0)
			return null;
		long symbols = JNI.loadLibrarySymbols(handle);
		return new NativeLibrary(path, handle, symbols);
	}
	
	/*public boolean methodMatchesSymbol(Class<?> declaringClass, Method method, String symbol) {
		return symbol.contains(method.getName()) && symbol.contains(declaringClass.getSimpleName());
	}*/
	
	long getHandle() {
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
		if (nameToSym != null) {
			Symbol addr = nameToSym.get(name);
//			long addr = nameToAddr.get(name);
//			if (addr != 0)
			if (addr != null)
				return addr.getAddress();
		}
		long address = JNI.findSymbolInLibrary(getHandle(), name);
		if (address == 0)
			address = JNI.findSymbolInLibrary(getHandle(), "_" + name);
		return address;
	}

    public synchronized Symbol getSymbol(AnnotatedElement member) throws FileNotFoundException {
        //libHandle = libHandle & 0xffffffffL;
    	com.bridj.ann.Symbol mg = BridJ.getAnnotation(com.bridj.ann.Symbol.class, false, member);
    	String name = null;
    	if (member instanceof Member)
            name = ((Member)member).getName();
        
        if (mg != null)
            for (String n : mg.value())
            {
            	Symbol handle = getSymbol(n);
                if (handle == null)
                    handle = getSymbol("_" + n);
                if (handle != null)
                    return handle;
            }

        if (name != null) {
            Symbol handle = getSymbol(name);
            if (handle == null)
                handle = getSymbol("_" + name);
            if (handle != null)
                return handle;
        }

        if (member instanceof Method) {
            Method method = (Method)member;
            for (Demangler.Symbol symbol : getSymbols()) {
                if (symbol.matches(method))
                    return symbol;
            }
        }
        return null;
    }
	int getPositionInVirtualTable(Method method) {
		Class<?> type = method.getDeclaringClass();
		Pointer<Pointer<?>> pVirtualTable = getVirtualTable(type);
		return getPositionInVirtualTable(pVirtualTable, method);
	}
	public int getPositionInVirtualTable(Pointer<Pointer<?>> pVirtualTable, Method method) {
		String methodName = method.getName();
		//Pointer<?> typeInfo = pVirtualTable.get(1);
		int methodsOffset = isMSVC() ? 0 : -2;///2;
		String className = getCPPClassName(method.getDeclaringClass());
		for (int iVirtual = 0;; iVirtual++) {
			Pointer<?> pMethod = pVirtualTable.get(methodsOffset + iVirtual);
			String virtualMethodName = pMethod == null ? null : getSymbolName(pMethod.getPeer());
			//System.out.println("#\n# At index " + methodsOffset + " + " + iVirtual + " of vptr for class " + className + ", found symbol " + Long.toHexString(pMethod.getPeer()) + " = '" + virtualMethodName + "'\n#");
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
	public boolean isMSVC() {
		return JNI.isWindows();
	}
	String getCPPClassName(Class<?> declaringClass) {
		return declaringClass.getSimpleName();
	}

	@SuppressWarnings("unchecked")
	public
	Pointer<Pointer<?>> getVirtualTable(Class<?> type) {
		Pointer<Pointer<?>> p = vtables.get(type);
		if (p == null) {
			String className = type.getSimpleName();
			String vtableSymbolName;
            if (JNI.isWindows())
                vtableSymbolName = "??_7" + className + "@@6B@";
            else
                vtableSymbolName = "_ZTV" + className.length() + className;

            long addr = getSymbolAddress(vtableSymbolName);
			//long addr = JNI.findSymbolInLibrary(getHandle(), vtableSymbolName);
//			System.out.println(TestCPP.hex(addr));
//			TestCPP.print(type.getName() + " vtable", addr, 5, 2);
        	
			p = (Pointer)Pointer.pointerToAddress(addr, Pointer.class);
			vtables.put(type, p);
		}
		return p;
	}
	public Collection<Demangler.Symbol> getSymbols() {
        try {
            scanSymbols();
        } catch (Exception ex) {
            assert BridJ.log(Level.SEVERE, "Failed to scan symbols of library '" + path + "'", ex);
        }
		return Collections.unmodifiableCollection(nameToSym.values());
	}
	public String getSymbolName(long address) {
		if (addrToName == null && getSymbolsHandle() != 0)//JNI.isUnix())
			return JNI.findSymbolName(getHandle(), getSymbolsHandle(), address);
	
		Demangler.Symbol symbol = getSymbol(address);
		return symbol == null ? null : symbol.symbol;
	}
	
	public Symbol getSymbol(long address) {
		try {
			scanSymbols();
			Symbol symbol = addrToName.get(address);
			return symbol;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to get name of address " + address, ex);
		}
	}
	public Symbol getSymbol(String name) {
		try {
			scanSymbols();
			Symbol symbol = nameToSym.get(name);
			if (addrToName == null) {
				if (symbol == null) {
					long addr = JNI.findSymbolInLibrary(getHandle(), name);
					if (addr != 0) {
						symbol = new Symbol(name, this);
						symbol.address = addr;
						nameToSym.put(name, symbol);
					}
				}
			}
			return symbol;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
//			throw new RuntimeException("Failed to get symbol " + name, ex);
		}
	}
	void scanSymbols() throws Exception {
		if (addrToName != null)
			return;
		
		nameToSym = new HashMap<String, Symbol>();
//		nameToAddr = new HashMap<String, Long>();
		
		String[] symbs = null;
		if (false) // TODO turn to false !!!
		try {
			if (JNI.isMacOSX()) {
				Process process = java.lang.Runtime.getRuntime().exec(new String[] {"nm", "-gj", path});
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
		
		if (symbs == null)
			return;
		
		addrToName = new HashMap<Long, Demangler.Symbol>();
		
		boolean is32 = !JNI.is64Bits();
		for (String name : symbs) {
			if (name == null)
				continue;
			
			long addr = JNI.findSymbolInLibrary(getHandle(), name);
			if (addr == 0 && name.startsWith("_")) {
				String n2 = name.substring(1);
				addr = JNI.findSymbolInLibrary(getHandle(), n2);
                if (addr == 0) {
                    n2 = "_" + name;
                    addr = JNI.findSymbolInLibrary(getHandle(), n2);
                }
                if (addr != 0)
                    name = n2;

			}
			if (addr == 0) {
				System.err.println("Symbol '" + name + "' not found.");
				continue;
			}
			//if (is32)
			//	addr = addr & 0xffffffffL;
			//System.out.println("Symbol " + Long.toHexString(addr) + " = '" + name + "'");
			
			Symbol sym = new Demangler.Symbol(name, this);
			sym.address = addr;
			addrToName.put(addr, sym);
			nameToSym.put(name, sym);
			//nameToAddr.put(name, addr);
			//System.out.println("'" + name + "' = \t" + TestCPP.hex(addr));
		}
	}

	MemberRef parseSymbol(String symbol) throws DemanglingException {
		Demangler demangler;
		if (JNI.isWindows())
			demangler = new VC9Demangler(this, symbol);
		else
			demangler = new GCC4Demangler(this, symbol);
		return demangler.parseSymbol();
	}
}