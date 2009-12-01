/*
	Copyright (c) 2009 Olivier Chafik (http://ochafik.free.fr/)
	
	This file is part of OpenCL4Java (http://code.google.com/p/nativelibs4java/wiki/OpenCL).
	
	OpenCL4Java is free software: you can redistribute it and/or modify
	it under the terms of the GNU Lesser General Public License as published by
	the Free Software Foundation, either version 2.1 of the License, or
	(at your option) any later version.
	
	OpenCL4Java is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU Lesser General Public License for more details.
	
	You should have received a copy of the GNU Lesser General Public License
	along with OpenCL4Java.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.nativelibs4java.opencl;

import com.nativelibs4java.opencl.library.OpenCLLibrary;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.*;

import com.ochafik.util.string.StringUtils;
import com.sun.jna.*;
import java.nio.*;
import static com.nativelibs4java.opencl.JavaCL.*;
import com.sun.jna.ptr.*;
import java.util.Arrays;
import static com.nativelibs4java.opencl.CLException.*;

/**
 * Entry point class for the OpenCL4Java Object-oriented wrappers around the OpenCL API.<br/>
 * @author Olivier Chafik
 */
public class JavaCL {

    static final OpenCLLibrary CL = OpenCLLibrary.INSTANCE;

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
        error(CL.clGetPlatformIDs(0, (cl_platform_id[])null, pCount));

        int nPlats = pCount.getValue();
        if (nPlats == 0)
            return new CLPlatform[0];

        cl_platform_id[] ids = new cl_platform_id[nPlats];

        error(CL.clGetPlatformIDs(nPlats, ids, null));
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
    public static CLContext createContext(Map<CLPlatform.ContextProperties, Number> contextProperties, CLDevice... devices) {
		return devices[0].getPlatform().createContext(contextProperties, devices);
    }

    @Deprecated
    public static CLContext createContext(CLDevice... devices) {
		return createContext(null, devices);
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
        List<CLDevice> devices = new ArrayList<CLDevice>();
		for (CLPlatform platform : listPlatforms())
			devices.addAll(Arrays.asList(platform.listAllDevices(true)));
        return CLPlatform.getBestDevice(CLPlatform.DeviceEvaluationStrategy.BiggestMaxComputeUnits, devices);
    }
	public static CLContext createBestContext() {
		CLDevice device = getBestDevice();
		return device.getPlatform().createContext(null, device);
	}

    public static CLContext createBestGLCompatibleContext(long glContextId, CLDevice... devices) {
		return devices[0].getPlatform().createGLCompatibleContext(glContextId, devices);
    }


}
