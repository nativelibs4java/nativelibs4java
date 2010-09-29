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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.nativelibs4java.opencl.library.OpenCLLibrary;
import com.nativelibs4java.opencl.library.OpenCLLibrary.cl_platform_id;
import org.bridj.*;
import static org.bridj.Pointer.*;

/**
 * Entry point class for the OpenCL4Java Object-oriented wrappers around the OpenCL API.<br/>
 * @author Olivier Chafik
 */
public class JavaCL {

    static final OpenCLLibrary CL = new OpenCLLibrary();

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
        Pointer<Integer> pCount = allocateInt();
        error(CL.clGetPlatformIDs(0, null, pCount));

        int nPlats = pCount.get();
        if (nPlats == 0)
            return new CLPlatform[0];

        Pointer<cl_platform_id> ids = allocateTypedPointers(cl_platform_id.class, nPlats);

        error(CL.clGetPlatformIDs(nPlats, ids, null));
        CLPlatform[] platforms = new CLPlatform[nPlats];

        for (int i = 0; i < nPlats; i++) {
            platforms[i] = new CLPlatform(ids.get(i));
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

    public static CLDevice getBestDevice() {
        return getBestDevice(CLPlatform.DeviceEvaluationStrategy.BiggestMaxComputeUnits);
    }
	public static CLDevice getBestDevice(CLPlatform.DeviceEvaluationStrategy strategy) {
        List<CLDevice> devices = new ArrayList<CLDevice>();
		for (CLPlatform platform : listPlatforms())
			devices.addAll(Arrays.asList(platform.listAllDevices(true)));
        return CLPlatform.getBestDevice(strategy, devices);
    }
	public static CLContext createBestContext() {
        return createBestContext(CLPlatform.DeviceEvaluationStrategy.BiggestMaxComputeUnits);
	}

    public static CLContext createBestContext(CLPlatform.DeviceEvaluationStrategy strategy) {
		CLDevice device = getBestDevice(strategy);
		return device.getPlatform().createContext(null, device);
	}

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
