package org.bridj;


import org.bridj.demangling.Demangler;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.Set;

import org.junit.Test;
import java.awt.*;
import org.bridj.jawt.*;

import static org.junit.Assert.*;

public class JAWTTest {
	
	@Test
	public void testWindowPeer() {
		Frame f = new Frame();
		f.pack();
		
		f.setVisible(true);
		long p = JAWTUtils.getNativePeerHandle(f);
		assertTrue(p != 0);
	}
}
