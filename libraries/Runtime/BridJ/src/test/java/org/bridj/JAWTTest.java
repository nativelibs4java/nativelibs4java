package org.bridj;


import org.bridj.demangling.Demangler;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.Set;

import org.junit.Test;
import java.awt.*;
import org.bridj.jawt.*;
import java.io.*;

import static org.junit.Assert.*;

public class JAWTTest {
	
	@Test
	public void testWindowPeer() {
		System.out.println("HAHAH");
            File javaHome = new File(System.getProperty("java.home"));
            BridJ.addLibraryPath(new File(javaHome, "bin").toString());
            //if (Platform.isMacOSX()) {
            		System.out.println("ADD MAC LIB PATH");
            		BridJ.addLibraryPath(new File(javaHome, "../Libraries").toString());
		assertEquals(6 * Pointer.SIZE, BridJ.sizeOf(JAWT_DrawingSurface.class));
		assertEquals(4 * 4, BridJ.sizeOf(JAWT_Rectangle.class));
		//assertEquals(4 + 5 * Pointer.SIZE, BridJ.sizeOf(JAWT.class));
		//assertEquals(2 * 4 * 4 + 4 + Pointer.SIZE, BridJ.sizeOf(JAWT_DrawingSurfaceInfo.class));
		 
		
		Frame f = new Frame();
		f.pack();
		
		f.setVisible(true);
		long p = JAWTUtils.getNativePeerHandle(f);
		assertTrue(p != 0);
		f.setVisible(false);
	}
}
