/*
 * JavaCL - Java API and utilities for OpenCL
 * http://javacl.googlecode.com/
 *
 * Copyright (c) 2009-2010, Olivier Chafik (http://ochafik.free.fr/)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Olivier Chafik nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY OLIVIER CHAFIK AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.nativelibs4java.opencl;

import static com.nativelibs4java.opencl.CLException.error;
import com.nativelibs4java.opencl.CLPlatform.DeviceFeature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.*;

import com.nativelibs4java.opencl.library.OpenCLLibrary;
import com.nativelibs4java.opencl.library.OpenCLLibrary.cl_platform_id;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.Native;

/**
 * Entry point class for the OpenCL4Java Object-oriented wrappers around the OpenCL API.<br/>
 * @author Olivier Chafik
 */
public class JavaCL {

	static final boolean verbose = "true".equals(System.getProperty("javacl.verbose")) || "1".equals(System.getenv("JAVACL_VERBOSE"));
    static final int minLogLevel = Level.WARNING.intValue();
	static boolean shouldLog(Level level) {
        return verbose || level.intValue() >= minLogLevel;
    }
	static boolean log(Level level, String message, Throwable ex) {
        if (!shouldLog(level))
            return true;
		Logger.getLogger(JavaCL.class.getName()).log(level, message, ex);
        return true;
	}
	
	static boolean log(Level level, String message) {
		log(level, message, null);
		return true;
	}

	private static int getPlatformIDs(OpenCLLibrary lib, int count, cl_platform_id[] out, IntByReference pCount) {
		try {
			return lib.clIcdGetPlatformIDsKHR(count, out, pCount);
		} catch (Throwable th) {
			return lib.clGetPlatformIDs(count, out, pCount);
		}
	}
    static final OpenCLLibrary CL;
	static {
		OpenCLLibrary lib = (OpenCLLibrary)Native.loadLibrary("OpenCL", OpenCLLibrary.class);
		//if (Platform.isWindows())
		try {
			IntByReference pCount = new IntByReference();
			int err = getPlatformIDs(lib, 0, null, pCount);
			if (err != OpenCLLibrary.CL_SUCCESS) {
				OpenCLLibrary atiLib = (OpenCLLibrary)Native.loadLibrary("atiocl", OpenCLLibrary.class);
				if (atiLib != null) {
					err = getPlatformIDs(atiLib, 0, null, pCount);
					if (err == OpenCLLibrary.CL_SUCCESS) {
						System.out.println("[JavaCL] Hacking around ATI's weird driver bugs (using atiocl library instead of OpenCL)"); 
						lib = atiLib;
					}
				}
			}
		} catch (Throwable th) {}
		
		CL = (OpenCLLibrary)Native.synchronizedLibrary(lib);
	}

    /**
     * List the OpenCL implementations that contain at least one GPU device.
     */
    public static CLPlatform[] listGPUPoweredPlatforms() {
        CLPlatform[] platforms = listPlatforms();
        List<CLPlatform> out = new ArrayList<CLPlatform>(platforms.length);
        for (CLPlatform platform : platforms) {
            if (platform.listGPUDevices(true).length > 0)
                out.add(platform);
        }
        return out.toArray(new CLPlatform[out.size()]);
    }
	/**
	 * Lists all available OpenCL implementations.
	 */
    public static CLPlatform[] listPlatforms() {
        IntByReference pCount = new IntByReference();
        error(getPlatformIDs(CL, 0, null, pCount));

        int nPlats = pCount.getValue();
        if (nPlats == 0)
            return new CLPlatform[0];

        cl_platform_id[] ids = new cl_platform_id[nPlats];

        error(getPlatformIDs(CL, nPlats, ids, null));
        CLPlatform[] platforms = new CLPlatform[nPlats];

        for (int i = 0; i < nPlats; i++) {
            platforms[i] = new CLPlatform(ids[i]);
        }
        return platforms;
    }

	/**
	 * Creates an OpenCL context formed of the provided devices.<br/>
	 * It is generally not a good idea to create a context with more than one device,
	 * because much data is shared between all the devices in the same context.
	 * @param devices devices that are to form the new context
	 * @return new OpenCL context
	 */
    public static CLContext createContext(Map<CLPlatform.ContextProperties, Object> contextProperties, CLDevice... devices) {
		return devices[0].getPlatform().createContext(contextProperties, devices);
    }

    /**
	 * Allows the implementation to release the resources allocated by the OpenCL compiler. <br/>
	 * This is a hint from the application and does not guarantee that the compiler will not be used in the future or that the compiler will actually be unloaded by the implementation. <br/>
	 * Calls to Program.build() after unloadCompiler() will reload the compiler, if necessary, to build the appropriate program executable.
	 */
	public static void unloadCompiler() {
		error(CL.clUnloadCompiler());
	}

	/**
	 * Returns the "best" OpenCL device (currently, the one that has the largest amount of compute units).<br>
	 * For more control on what is to be considered a better device, please use the @see JavaCL#getBestDevice(CLPlatform.DeviceFeature[]) variant.<br>
	 * This is currently equivalent to <code>getBestDevice(MaxComputeUnits)</code>
	 */
    public static CLDevice getBestDevice() {
        return getBestDevice(CLPlatform.DeviceFeature.MaxComputeUnits);
    }
	/**
	 * Returns the "best" OpenCL device based on the comparison of the provided prioritized device feature.<br>
	 * The returned device does not necessarily exhibit the features listed in preferredFeatures, but it has the best ordered composition of them.<br>
	 * For instance on a system with a GPU and a CPU device, <code>JavaCL.getBestDevice(CPU, MaxComputeUnits)</code> will return the CPU device, but on another system with two GPUs and no CPU device it will return the GPU that has the most compute units.
	 */
    public static CLDevice getBestDevice(CLPlatform.DeviceFeature... preferredFeatures) {
        List<CLDevice> devices = new ArrayList<CLDevice>();
		for (CLPlatform platform : listPlatforms())
			devices.addAll(Arrays.asList(platform.listAllDevices(true)));
        return CLPlatform.getBestDevice(Arrays.asList(preferredFeatures), devices);
    }
    /**
     * Creates an OpenCL context with the "best" device (see @see JavaCL#getBestDevice())
     */
	public static CLContext createBestContext() {
        return createBestContext(DeviceFeature.MaxComputeUnits);
	}

    /**
     * Creates an OpenCL context with the "best" device based on the comparison of the provided prioritized device feature (see @see JavaCL#getBestDevice(CLPlatform.DeviceFeature))
     */
	public static CLContext createBestContext(CLPlatform.DeviceFeature... preferredFeatures) {
		CLDevice device = getBestDevice(preferredFeatures);
		return device.getPlatform().createContext(null, device);
	}

    /**
     * Creates an OpenCL context able to share entities with the current OpenGL context.
     * @throws RuntimeException if JavaCL is unable to create an OpenGL-shared OpenCL context.
     */
	public static CLContext createContextFromCurrentGL() {
        RuntimeException first = null;
        for (CLPlatform platform : listPlatforms()) {
            try {
                CLContext ctx = platform.createContextFromCurrentGL();
                if (ctx != null)
                    return ctx;
            } catch (RuntimeException ex) {
                if (first == null)
                    first = ex;
                
            }
        }
        throw new RuntimeException("Failed to create an OpenCL context based on the current OpenGL context", first);
    }
}
