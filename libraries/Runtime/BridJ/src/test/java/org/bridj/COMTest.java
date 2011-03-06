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
import org.bridj.cpp.com.shell.IShellFolder;
import org.bridj.cpp.com.shell.IShellWindows;

@Library("test")
public class COMTest {

	boolean hasCOM = Platform.isWindows();// || !Platform.is64Bits();
	static {
		BridJ.register();
	}
	
	public static native @Ptr long sizeOfVARIANT();

	@Test
	public void variantSize() {
        long s = BridJ.sizeOf(VARIANT.class);
		if (hasCOM)
            assertEquals(sizeOfVARIANT(), s);
        else
            assertEquals(24, s);
	}


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
}
