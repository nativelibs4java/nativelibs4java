package com.bridj;

import com.bridj.BridJ;
import com.bridj.CRuntime;
import com.bridj.Callback;
import com.bridj.Demangler;
import com.bridj.JNI;
import com.bridj.NativeLibrary;
import com.bridj.Pointer;
import com.bridj.Demangler.Symbol;
import com.bridj.ann.Library;
import com.bridj.ann.Ptr;
import com.bridj.ann.Virtual;
import com.bridj.cpp.CPPObject;
import com.bridj.cpp.CPPRuntime;
import com.bridj.cpp.VC9Demangler;
import com.bridj.cpp.com.COMRuntime;
import com.bridj.cpp.com.IShellWindows;
import com.bridj.cpp.com.IUnknown;
import com.bridj.cpp.com.shell32.IShellFolder;
import com.bridj.objc.NSAutoReleasePool;
import com.bridj.objc.ObjCObject;

import java.io.FileNotFoundException;

///http://www.codesourcery.com/public/cxx-abi/cxx-vtable-ex.html
@Library("test")
@com.bridj.ann.Runtime(CPPRuntime.class)
public class TestCPP {
//	static String libraryPath = //BridJ.getNativeLibraryFile("test").toString()
//		JNI.isMacOSX() ? 
//				//"/Users/ochafik/nativelibs4java/Runtime/BridJ/src/test/resources/darwin_universal/libtest.dylib"
//				"/Users/ochafik/nativelibs4java/Runtime/BridJ/src/test/cpp/test/build_out/darwin_universal_gcc_debug/libtest.dylib" :
//		//"F:\\Experiments\\tmp\\key\\svn\\nativelibs4java\\Runtime\\BridJ\\src\\test\\resources\\win32\\test.dll" +
//        "C:\\Users\\Olivier\\Prog\\nativelibs4java\\Runtime\\BridJ\\src\\main\\cpp\\buildsys\\vs2008\\x64\\Debug\\test.dll"
//	;

	static {
		//BridJ.register();
	}
	
	static NativeLibrary library;
	
	public static void print(String name, long addr, int n, int minI) {
		System.out.println(name);
		for (int i = -1; i < n; i++) {
			long v = getPtr(addr + i * Pointer.SIZE);
			Symbol sym = BridJ.getSymbolByAddress(v);
			String sname = sym == null ? null : sym.getName();
			System.out.println("\tOffset " + i + ":\t" + hex(v) + " \t('" + sname + "')");
			if (v == 0 && i >= minI)
				break;
		}
		System.out.println();
	}
	public static void main(String[] args) throws Exception {
		try {
            IShellWindows win = COMRuntime.newInstance(IShellWindows.class);
            IUnknown iu = win.QueryInterface(IUnknown.class);
            if (iu == null) {
                throw new RuntimeException("Interface does not handle IUnknown !");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        library = BridJ.getNativeLibrary("test");
			//NativeLibrary.load(libraryPath);

		new VC9Demangler(null, "?sinInt@@YANH@Z").parseSymbol();
        new VC9Demangler(null, "?forwardCall@@YAHP6AHHH@ZHH@Z").parseSymbol();

        BridJ.register();
        
        //new VC9Demangler(null, "??0Ctest2@@QEAA@XZ").parseSymbol();
//        NSAutoReleasePool object = new NSAutoReleasePool();
		
        
		for (Demangler.Symbol symbol : library.getSymbols()) {
            String name = symbol.getName();
			long addr = symbol.getAddress();
			System.out.println(name + " = \t" + hex(addr));
			
			if (name.startsWith("_ZTV") || name.startsWith("_ZTI") || name.startsWith("??_")) {
				print("vtable", addr, 10, 1);
			} else 
				System.out.println("'" + name + "'");
		}
		
		boolean is64 = JNI.is64Bits();
		long crea = Ctest.createTest();
		crea = Pointer.pointerToAddress(crea).getPointer(0).getPeer();
		print("Ctest.createTest()", crea, 10, 0);
		Ctest test = new Ctest();
		//long thisPtr = test.$this.getPeer();
		//System.out.println(hex(thisPtr));
		print("Ctest.this", Pointer.getPeer(test, Ctest.class).getPointer(0).getPeer(), 10, 2);
		int res = test.testAdd(1, 2);
		System.out.println("res = " + res);
		
		testNativeTargetCallbacks();
        testJavaTargetCallbacks();


        /*double dres = PerfLib.testASinB(1, 2);
        res = PerfLib.testAddJNI(1, 2);
        System.out.println("Done");*/
	}
	@Library("test")
	static class Ctest extends CPPObject {
		static {
			BridJ.register();//Ctest.class);
		}

//		@Constructor
//		private static native void Ctest(@This long thisPtr);
		
		static native @Ptr long createTest();
		
		
		@Virtual
//		protected static native int testAdd(@This long thisPtr, int a, int b);
		public native int testAdd(int a, int b);
		
//		public int testAdd(int a, int b) {
//			//print("this", Pointer.getAddress(this, Ctest.class), 10, 10);
//			//print("*this", $this.getPointer(0).getPeer(), 10, 10);
//			return testAdd(Pointer.getAddress(this, Ctest.class), a, b);
//		}
	}
	
	static String hex(long v) {
		String s = Long.toString(v, 16);
		while ((s.length() < 16))
			s = "0" + s;
		return s;
	}
	static long getPtr(long peer) {
		Pointer<?> ptr = Pointer.pointerToAddress(peer);
		return ptr.getSizeT(0);
	}
	
	
	public static void testJavaTargetCallbacks() {
		int res;

        res = forwardCall(new MyCallback() {
			@Override
			public int doSomething(int a, int b) {
				return a + b * 10;
			}
		}.toPointer(), 1, 2);
        if (res != (1 + 2 * 10))
            throw new RuntimeException();

        res = forwardCall(new MyCallback() {
			@Override
			public int doSomething(int a, int b) {
				return a + b;
			}
		}.toPointer(), 1, 2);
        if (res != (1 + 2))
            throw new RuntimeException();

	}
	
	public static void testNativeTargetCallbacks() {
		Pointer<com.bridj.TestCPP.MyCallback> ptr = getAdder();
		MyCallback adder = ptr.toNativeObject(MyCallback.class);
		int res = adder.doSomething(1, 2);

        if (res != 3)
            throw new RuntimeException("Expected 3, got "+ res);
	}
	
	static native int forwardCall(Pointer<MyCallback> cb, int a, int b);
	static native Pointer<MyCallback> getAdder();
	
	public static abstract class MyCallback extends Callback {
		public abstract int doSomething(int a, int b); 
	}
	
}

/*

@Library("test")
@com.bridj.ann.Runtime(CRuntime.class)
class PerfLib {
    static {
        String f = BridJ.getNativeLibraryFile(BridJ.getNativeLibraryName(PerfLib.class)).toString();
        System.load(f);
    }
    public static class DynCallTest {
        public DynCallTest() throws FileNotFoundException {
            BridJ.register(getClass());
        }
        public native int testAddDyncall(int a, int b);
        public native int testASinB(int a, int b);
    }

    public static class JNATest implements com.sun.jna.Library {
        static {
        	try {
        		com.sun.jna.Native.register(JNI.extractEmbeddedLibraryResource("test").toString());
        	} catch (Exception ex) {
        		throw new RuntimeException("Failed to initialize test JNA library", ex);
        	}
        }
        public static native int testAddJNA(int a, int b);
        public static native int testASinB(int a, int b);
    }
    public static native int testAddJNI(int a, int b);
    public static native double testASinB(int a, int b);
}*/
