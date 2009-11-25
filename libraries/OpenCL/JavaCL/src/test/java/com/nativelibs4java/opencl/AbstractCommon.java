/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl;

import com.nativelibs4java.util.ImageUtils;
import java.awt.image.BufferedImage;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

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

    @Before
    public void setUp() {
		context = OpenCL4Java.createBestContext();
		platform = context.getPlatform();
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