package org.bridj;

import org.junit.Test;

import org.bridj.ann.Library;


///http://www.codesourcery.com/public/cxx-abi/cxx-vtable-ex.html

@Library("test")
public class ExceptionsTest {
	static {
		BridJ.register();
	}
	
	public static native void crashIllegalAccess() throws RuntimeException;
	public static native void throwMyExceptionByValue(Pointer<Byte> message) throws RuntimeException;
	public static native void throwNewMyException(Pointer<Byte> message) throws RuntimeException;
	public static native void throwInt(int value) throws RuntimeException;
	
	void throwExpectedIfNotSupported() {
		if (!BridJ.exceptionsSupported)
			throw new RuntimeException("Not supported");
	}
	@Test(expected=RuntimeException.class)
	public void testCrashIllegalAccess() {
		throwExpectedIfNotSupported();
		
		try {
			//crashIllegalAccess();
		} catch (RuntimeException ex) {
			ex.printStackTrace();
			throw ex;
		}
	}
	
	
	//@Test(expected=RuntimeException.class)
	//public void testThrowCPPException() {
		//throwMyExceptionByValue(pointerToCString("Whatever"));
	//}
}

