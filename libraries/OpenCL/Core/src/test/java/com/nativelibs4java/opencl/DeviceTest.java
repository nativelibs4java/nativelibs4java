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

import org.junit.*;

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
    @Ignore
    @Test
    public void testSplitEqually() {
        int computeUnits = device.getMaxComputeUnits();
        System.out.println("computeUnits = " + computeUnits);
        int subComputeUnits = 1;//computeUnits / 2;
        
        CLDevice[] subDevices = device.createSubDevicesEqually(subComputeUnits);
        for (CLDevice subDevice : subDevices) {
            assertEquals(subComputeUnits, subDevice.getMaxComputeUnits());
            checkParent(device, subDevice);
        }
    }
    @Ignore
    @Test
    public void testSplitByCounts() {
        long[] counts = new long[] { 2, 4, 8 };
        CLDevice[] subDevices = device.createSubDevicesByCounts(counts);
        assertEquals(counts.length, subDevices.length);
        int i = 0;
        for (CLDevice subDevice : subDevices) {
        	long count = counts[i];
            assertEquals(count, subDevice.getMaxComputeUnits());
            checkParent(device, subDevice);
            i++;
        }
    }
    @Ignore
    @Test
    public void testSplitByAffinity() {
        CLDevice[] subDevices = device.createSubDevicesByAffinity(CLDevice.AffinityDomain.NextPartitionable);
        assertTrue(subDevices.length > 1);
        for (CLDevice subDevice : subDevices) {
            checkParent(device, subDevice);
        }
    }

    private void checkParent(CLDevice parent, CLDevice child) {
        assertSame(device, child.getParent());
        // Force a get info CL_DEVICE_PARENT_DEVICE.
        assertSame(device, new CLDevice(platform, null, child.getEntity(), false).getParent());
    }

}
