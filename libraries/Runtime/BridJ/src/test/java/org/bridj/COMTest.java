package org.bridj;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bridj.CPPTest.Ctest;
import org.bridj.CPPTest.Ctest2;
import org.junit.Test;

import org.bridj.ann.*;
import static org.junit.Assert.*;
import org.bridj.cpp.com.COMRuntime;
import org.bridj.cpp.com.IUnknown;
import org.bridj.cpp.com.VARIANT;
import org.bridj.cpp.com.DECIMAL;
import org.bridj.cpp.com.shell.IShellFolder;
import org.bridj.cpp.com.shell.IShellWindows;

public class COMTest {

	static boolean hasCOM = Platform.isWindows();// || !Platform.is64Bits();
	

	@Test
	public void shellFolder() {
		if (!hasCOM)
            return;
        try {
            IShellWindows win = COMRuntime.newInstance(IShellWindows.class);
            assertNotNull(win);
            IUnknown iu = win.QueryInterface(IUnknown.class);
            assertNotNull(iu);
            win = iu.QueryInterface(IShellWindows.class);
            assertNotNull(win);
            win.Release();
            
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(COMTest.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
	}
	
	static class SomeUnknown extends IUnknown {
		//public SomeUnknown() {}
		@Override
		public int QueryInterface(Pointer<Byte> riid, Pointer<Pointer<IUnknown>> ppvObject) {
			return 0;
		}
		int refs;
		@Override
		public synchronized int AddRef() { return ++refs; } 
		
		@Override
		public synchronized int Release() { return --refs; }
    }

    @Test
    public void testSomeUnknownInstantiation() {
    		new SomeUnknown();
    }
}
