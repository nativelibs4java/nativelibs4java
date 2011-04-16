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
	
	public static native void crashIllegalAccess();
	public static native void throwCPPException(Pointer<Byte> message);
	
	@Test(expected=RuntimeException.class)
	public void testCrashIllegalAccess() {
		crashIllegalAccess();
	}
	
	@Test(expected=RuntimeException.class)
	public void testThrowCPPException() {
		//throwCPPException(pointerToCString("Whatever"));
	}
}

