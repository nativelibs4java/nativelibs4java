/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl;

import org.junit.Before;
import org.junit.BeforeClass;

import com.nativelibs4java.test.MiscTestUtils;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 *
 * @author ochafik
 */
@RunWith(Parameterized.class)
public abstract class AbstractCommon {

	CLPlatform platform;
	CLContext context;
	CLQueue queue;
	CLDevice device;
	CLImageFormat[] formatsRead2D, formatsRead3D, formatsWrite2D, formatsWrite3D, formatsReadWrite2D, formatsReadWrite3D;
	/*
    @BeforeClass
    public static void setup() {
        MiscTestUtils.protectJNI();
    }
    */
    
    static boolean listedPlatforms;
    
    AbstractCommon(CLDevice device) {
        this.device = device;
        platform = device.getPlatform();
        context = platform.createContext(null, device);
        queue = context.createDefaultQueue();
		device = context.getDevices()[0];
		formatsRead2D = context.getSupportedImageFormats(CLMem.Flags.ReadOnly, CLMem.ObjectType.Image2D);
		formatsWrite2D = context.getSupportedImageFormats(CLMem.Flags.WriteOnly, CLMem.ObjectType.Image2D);
		formatsRead3D = context.getSupportedImageFormats(CLMem.Flags.ReadOnly, CLMem.ObjectType.Image3D);
		formatsWrite3D = context.getSupportedImageFormats(CLMem.Flags.WriteOnly, CLMem.ObjectType.Image3D);
		formatsReadWrite2D = context.getSupportedImageFormats(CLMem.Flags.ReadWrite, CLMem.ObjectType.Image2D);
		formatsReadWrite3D = context.getSupportedImageFormats(CLMem.Flags.ReadWrite, CLMem.ObjectType.Image3D);
    }
    
    @Parameterized.Parameters
    public static List<Object[]> getDeviceParameters() {
        List<Object[]> ret = new ArrayList<Object[]>();
        for (CLPlatform platform : JavaCL.listPlatforms())
            for (CLDevice device : platform.listAllDevices(true))
                ret.add(new Object[] { device });
        return ret;
    }
    /*
    @After
    public void cleanup() {
        queue.finish();
        queue.release();
        context.release();
        device.release();
        platform.release();
    }    
    */
}