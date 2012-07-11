package com.nativelibs4java.opencl;
import static com.nativelibs4java.opencl.JavaCL.*;

import java.util.*;

import static org.junit.Assert.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class CLPlatformTest {
	final CLPlatform platform;
	
	public CLPlatformTest(CLPlatform platform) {
		this.platform = platform;
	}
	@Test
	public void ensureHasDevices() {
		CLDevice[] devices = platform.listAllDevices(false);
		assertTrue("No device in platform " + platform, devices.length > 0);
	}
	@Parameters
	public static List<Object[]> readParameters() {
		List<Object[]> data = new ArrayList<Object[]>();
		
		for (CLPlatform platform : JavaCL.listPlatforms()) {
			data.add(new Object[] { platform });	
		}
		
		return data;
	}
}
