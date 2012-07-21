package com.nativelibs4java.opencl;

import java.util.Map;
import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import org.bridj.Pointer;
import static org.bridj.Pointer.*;
import com.nativelibs4java.test.MiscTestUtils;

@SuppressWarnings("unchecked")
public class ByteOrderHackTest extends AbstractCommon {
	@Test
	public void test() {
		if (!ByteOrderHack.hackEnabled)
			return;
		for (CLPlatform platform : JavaCL.listPlatforms()) {
			for (CLDevice device : platform.listAllDevices(true)) {
				assertEquals(device.getByteOrder(), ByteOrderHack.checkByteOrderNeededForBuffers(device));
			}
		}
	}
}
