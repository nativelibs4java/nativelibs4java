/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nativelibs4java.opencl;

import com.nativelibs4java.opencl.CLDevice.CLCacheType;
import com.nativelibs4java.opencl.library.OpenCLLibrary.cl_device_id;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.nativelibs4java.opencl.CLTestUtils.*;

/**
 *
 * @author ochafik
 */
public class InfoGettersTest {

	CLProgram createProgram() {
		CLProgram pg = createContext().createProgram("__kernel void f(__global int a) {}");
		try {
			pg.build();
		} catch (OpenCL4Java.CLBuildException ex) {
			assertFalse(ex.toString(), true);
		}
		return pg;
	}

	CLPlatform createPlatform() {
		return CLPlatform.listPlatforms()[0];
	}

	CLDevice createDevice() {
		return createPlatform().listAllDevices()[0];
	}

	CLContext createContext() {
		return CLContext.createContext(createDevice());
	}

	CLKernel createKernel() {
		return createProgram().createKernels()[0];
	}

	CLQueue createQueue() {
		CLContext c = createContext();
		CLDevice d = c.getDevices()[0];
		return d.createQueue(c);
	}

	@org.junit.Test
	public void CLProgramGetters() {
		testGetters(createProgram());
	}

	@org.junit.Test
	public void CLKernelGetters() {
		testGetters(createKernel());
	}

	@org.junit.Test
	public void CLMemGetters() {
		testGetters(createContext().createInput(10));
		testGetters(createContext().createOutput(10));
	}

	@org.junit.Test
	public void CLQueueGetters() {
		testGetters(createQueue());
	}

	@org.junit.Test
	public void CLDeviceGetters() {
		testGetters(CLPlatform.listPlatforms()[0].listAllDevices()[0]);
	}

	@org.junit.Test
	public void CLPlatformGetters() {
		testGetters(CLPlatform.listPlatforms()[0]);
	}

	@org.junit.Test
	public void CLContextGetters() {
		testGetters(CLContext.createContext(CLPlatform.listPlatforms()[0].listAllDevices()));
	}

	@org.junit.Test
	public void CLEventGetters() {
		//testGetters(CLEvent.listPlatforms()[0].listAllDevices()[0]);
	}
}
