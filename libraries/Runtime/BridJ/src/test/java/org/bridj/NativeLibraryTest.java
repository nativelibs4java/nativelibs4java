package org.bridj;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import org.bridj.objc.*;
import org.bridj.ann.Field;
import org.bridj.ann.Library;
import org.bridj.ann.Runtime;
import org.bridj.ann.Array;
import org.bridj.ann.Ptr;
import org.bridj.cpp.com.*;
import static org.bridj.Pointer.*;
import static org.bridj.BridJ.*;
import static org.bridj.objc.FoundationLibrary.*;
import java.util.*;
import java.io.*;

@Library("Foundation")
@Runtime(ObjectiveCRuntime.class)
public class NativeLibraryTest {
	
	static File tempDir() throws IOException {
		File f = File.createTempFile("bridj", "natlibtest");
		f.delete();
		f.mkdir();
		f.deleteOnExit();
		return f;
	}
	static void touch(File dir, String name) throws IOException {
		File f = new File(dir, name);
		f.deleteOnExit();
		new FileOutputStream(f).close();
	}
	
	@Test
	public void testVersionedLibrary() throws IOException {
		File d = new File(".");
		String[] files = new String[] { 
			"libc.so.0.1",
			"libc.so.1",
			"libc.so.13.0",
			"libc.so.2.1",
			"libc.so.1.0.0"
		};
		
		assertEquals("libc.so.13.0", BridJ.findFileWithGreaterVersion(d, files, "libc.so").getName());
	}
}
