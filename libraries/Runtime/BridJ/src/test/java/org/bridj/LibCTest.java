package org.bridj;

import org.bridj.ann.*; // annotations such as Library...


import java.util.Collections;
import java.util.Iterator;
import org.junit.*;
import static org.junit.Assert.*;

@Library("c")
@org.bridj.ann.Runtime(CRuntime.class)
public class LibCTest {
	static {
		if (Platform.isWindows())
			BridJ.setNativeLibraryActualName("c", "msvc");
		if ("1".equals(System.getenv("JNA")))
			com.sun.jna.Native.register("c");
		else
			BridJ.register();
	}
	public static native double fabs(double x);
	public static native int abs(int x);
	public static native int getpid();
	
	@Test
	public void testFabs() {
		assertEquals(10.0, fabs(-10.0), 0.000001);
	}
	@Test
	public void testAbs() {
		assertEquals(10, abs(-10));
	}
}
	

