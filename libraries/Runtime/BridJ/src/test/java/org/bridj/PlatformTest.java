package org.bridj;

import static org.bridj.Platform.*;
import org.junit.*;
import static org.junit.Assert.*;
import java.io.*;

public class PlatformTest {
	/*
	@Test
	public void test_uname() {
		if (!isUnix())
			return;
		
		utsname name = uname();
		System.out.println("uname = " + name);
		assertNotNull(name);
		assertNotNull(name.sysname);
		assertNotNull(name.nodename);
		assertNotNull(name.release);
		assertNotNull(name.version);
		assertNotNull(name.machine);
		
		assertEquals(name.machine, Platform.getMachine());
	}
	*/
	
	@Test
	public void testMachine() throws Exception {
		Process p = Runtime.getRuntime().exec(new String[] { "uname", "-m" });
		BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String s = r.readLine().trim();
		assertTrue(s.length() > 0);
		assertEquals(s, Platform.getMachine());
	}
}
