package org.bridj;

import static org.bridj.Platform.*;
import org.junit.*;
import static org.junit.Assert.*;

public class PlatformTest {
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
		
		assertEquals(name.machine, Platform.getArch());
	}
}
