package com.nativelibs4java.opencl;

import java.util.Map;
import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import org.bridj.Pointer;
import static org.bridj.Pointer.*;
import com.nativelibs4java.test.MiscTestUtils;
import java.util.List;
import org.junit.runners.Parameterized;

@SuppressWarnings("unchecked")
public class ByteOrderHackTest extends AbstractCommon {
    public ByteOrderHackTest(CLDevice device) {
        super(device);
    }
    
    @Parameterized.Parameters
    public static List<Object[]> getDeviceParameters() {
        return AbstractCommon.getDeviceParameters();
    }
    
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
