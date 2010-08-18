/**
 * 
 */
package org.bridj;

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

import org.bridj.Demangler.DemanglingException;
import org.bridj.Demangler.MemberRef;
import org.bridj.Demangler.Symbol;
import org.bridj.ann.Virtual;
import org.bridj.cpp.GCC4Demangler;
import org.bridj.cpp.VC9Demangler;
import java.lang.reflect.Type;

import java.util.Collection;

public class NativeLibrary {
	long handle, symbols;
	String path;
	//Map<Class<?>, long[]> callbacks = new HashMap<Class<?>, long[]>();
	NativeEntities nativeEntities = new NativeEntities();
	
	Map<Long, Symbol> addrToName;
	Map<String, Symbol> nameToSym;
//	Map<String, Long> nameToAddr;
	
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
    	org.bridj.ann.Symbol mg = BridJ.getAnnotation(org.bridj.ann.Symbol.class, false, member);
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
	
	public boolean isMSVC() {
		return JNI.isWindows();
	}
	public interface SymbolAccepter {
        boolean accept(Symbol symbol);
    }
    public Symbol getFirstMatchingSymbol(SymbolAccepter accepter) {
        for (Symbol symbol : getSymbols())
            if (accepter.accept(symbol))
                return symbol;
        return null;
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
			//System.out.println("'" + name + "' = \t" + TestCPP.hex(addr) + "\n\t" + sym.getParsedRef());
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