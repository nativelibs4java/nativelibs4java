/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl;

import org.junit.Before;
import org.junit.BeforeClass;

import com.nativelibs4java.test.MiscTestUtils;

/**
 *
 * @author ochafik
 */
public abstract class AbstractCommon {

	CLPlatform platform;
	CLContext context;
	CLQueue queue;
	CLDevice device;
	CLImageFormat[] formatsRead2D, formatsRead3D, formatsWrite2D, formatsWrite3D, formatsReadWrite2D, formatsReadWrite3D;

    @BeforeClass
    public static void setup() {
        MiscTestUtils.protectJNI();
    }
    
    static String chosenPlatformName = System.getProperty("javacl.test.platform", System.getenv("JAVACL_TEST_PLATFORM"));
    static boolean listedPlatforms;
    
    @Before
    public void setUp() {
    	CLPlatform chosenPlatform = null;
    	for (CLPlatform platform : JavaCL.listPlatforms()) {
    		if (!listedPlatforms)
    			System.out.println("Platform Detected : \"" + platform.getName() + "\"");
    		if (chosenPlatformName != null && platform.getName().contains(chosenPlatformName)) {
    			chosenPlatform = platform;
    		}
    	}
    	listedPlatforms = true;
    	if (chosenPlatform != null) {
    		platform = chosenPlatform;
    		context = platform.createContext(null, platform.getBestDevice());
		} else {
			context = JavaCL.createBestContext();
			platform = context.getPlatform();
		}
		queue = context.createDefaultQueue();
		device = context.getDevices()[0];
		formatsRead2D = context.getSupportedImageFormats(CLMem.Flags.ReadOnly, CLMem.ObjectType.Image2D);
		formatsWrite2D = context.getSupportedImageFormats(CLMem.Flags.WriteOnly, CLMem.ObjectType.Image2D);
		formatsRead3D = context.getSupportedImageFormats(CLMem.Flags.ReadOnly, CLMem.ObjectType.Image3D);
		formatsWrite3D = context.getSupportedImageFormats(CLMem.Flags.WriteOnly, CLMem.ObjectType.Image3D);
		formatsReadWrite2D = context.getSupportedImageFormats(CLMem.Flags.ReadWrite, CLMem.ObjectType.Image2D);
		formatsReadWrite3D = context.getSupportedImageFormats(CLMem.Flags.ReadWrite, CLMem.ObjectType.Image3D);
    }


}