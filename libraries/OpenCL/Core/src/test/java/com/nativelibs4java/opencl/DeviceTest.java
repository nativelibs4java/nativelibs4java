/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl;

import static com.nativelibs4java.util.NIOUtils.directBuffer;
import static com.nativelibs4java.util.NIOUtils.get;
import static com.nativelibs4java.util.NIOUtils.put;
import static org.junit.Assert.*;

import java.nio.*;

import org.junit.BeforeClass;
import org.junit.Test;

import com.nativelibs4java.test.MiscTestUtils;
import com.nativelibs4java.util.NIOUtils;
import org.bridj.*;
import java.nio.ByteOrder;
import static org.bridj.Pointer.*;
import java.nio.ByteOrder;
import java.util.List;
import org.junit.runners.Parameterized;

/**
 *
 * @author ochafik
 */
public class DeviceTest extends AbstractCommon {
    public DeviceTest(CLDevice device) {
        super(device);
    }
    
    @Parameterized.Parameters
    public static List<Object[]> getDeviceParameters() {
        return AbstractCommon.getDeviceParameters();
    }
    @Test
    public void testSplitEqually() {
        int computeUnits = device.getMaxComputeUnits();
        int subComputeUnits = 1;//computeUnits / 2;
        
        CLDevice[] subDevices = device.createSubDevicesEqually(subComputeUnits);
        for (CLDevice subDevice : subDevices) {
            assertEquals(subComputeUnits, subDevice.getMaxComputeUnits());
        }
    }

}
