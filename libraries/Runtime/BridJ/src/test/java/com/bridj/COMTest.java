package com.bridj;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;

import com.bridj.ann.*;
import static org.junit.Assert.*;
import com.bridj.cpp.com.COMRuntime;
import com.bridj.cpp.com.IUnknown;
import com.bridj.cpp.com.shell.IShellFolder;
import com.bridj.cpp.com.shell.IShellWindows;

public class COMTest {

	boolean skip = !JNI.isWindows();
	
	@Test
	public void shellFolder() {
		if (skip) return;
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
