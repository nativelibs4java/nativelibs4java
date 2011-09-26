package org.bridj.platform.idletime;
import static org.bridj.Platform.*;
import org.junit.*;
import static org.junit.Assert.*;
import java.io.*;

public class IdleTimeUtilsTest {
	
	@Test
	public void testIdleTime() {
		long time = IdleTimeUtils.getInstance().getIdleTimeMillis();
		assertTrue(time > 0);
		//println("Idle time = " + time);
	}
}
