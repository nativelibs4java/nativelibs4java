package org.bridj;

import com.sun.jna.Memory;
import java.nio.ByteBuffer;
import org.junit.Test;
import static org.junit.Assert.*;

import org.bridj.ann.Field;
import org.bridj.ann.Library;
import org.bridj.ann.Array;
import org.bridj.ann.Ptr;
import org.bridj.cpp.com.*;
import static org.bridj.Pointer.*;
import static org.bridj.BridJ.*;

import javolution.io.*;

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

