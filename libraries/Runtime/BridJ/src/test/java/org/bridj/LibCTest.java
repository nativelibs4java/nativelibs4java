package org.bridj;

import java.io.FileNotFoundException;
import org.bridj.ann.*; // annotations such as Library...

import static org.bridj.Pointer.*;
import java.util.Collections;
import java.util.Iterator;
import org.junit.*;
import static org.junit.Assert.*;

@Library("c")
@org.bridj.ann.Runtime(CRuntime.class)
public class LibCTest {
	static {
		if (Platform.isWindows())
			BridJ.setNativeLibraryActualName("c", "msvcrt");
		if ("1".equals(System.getenv("JNA")))
			com.sun.jna.Native.register("c");
		else
			BridJ.register();
	}
	public static native void sprintf(Pointer<Byte> dest, Pointer<Byte> format, Object... values);
	public static native double fabs(double x);
	public static native int abs(int x);
	public static native int getpid();
	
	public static native long strtoul(Pointer<Byte> str, Pointer<Pointer<Byte>> endptr, int base) throws LastError;
	
	@Test
	public void testFabs() {
		assertEquals(10.0, fabs(-10.0), 0.000001);
	}
	@Test
	public void testErrno() throws FileNotFoundException {
		if (!Platform.isUnix())
			return;
		
		assertNotNull(BridJ.getNativeLibrary("c").getSymbolPointer("errno"));
	}
	
	public void testNoLastError() {
        long v = strtoul(pointerToCString("1010"), null, 10);
        assertEquals(1010, v);
	}
	
	@Test(expected = LastError.class)
	public void testLastError() {
        strtoul(pointerToCString("18446744073709551616"), null, 10);
	}
	
	@Test
	public void testAbs() {
		assertEquals(10, abs(-10));
	}
}
	

