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
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;
import com.sun.jna.*;
import com.sun.jna.ptr.*;
import java.nio.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import static com.nativelibs4java.opencl.OpenCL4Java.*;
import static com.nativelibs4java.opencl.CLException.*;
import static com.nativelibs4java.util.JNAUtils.*;

/**
 * OpenCL implementation entry point.
 * @see OpenCL4Java#listPlatforms() 
 * @author Olivier Chafik
 */
public class CLPlatform extends CLAbstractEntity<cl_platform_id> {

    CLPlatform(cl_platform_id platform) {
        super(platform, true);
    }

	private static CLInfoGetter<cl_platform_id> infos = new CLInfoGetter<cl_platform_id>() {
		@Override
		protected int getInfo(cl_platform_id entity, int infoTypeEnum, NativeLong size, Pointer out, NativeLongByReference sizeOut) {
			return CL.clGetPlatformInfo(entity, infoTypeEnum, size, out, sizeOut);
		}
	};
	
    @Override
    public String toString() {
        return getName() + " {vendor: " + getVendor() + ", version: " + getVersion() + ", profile: " + getProfile() + ", extensions: " + Arrays.toString(getExtensions()) +"}";
    }

    @Override
    protected void clear() {}

	/**
	 * Lists all the devices of the platform
	 * @param onlyAvailable if true, only returns devices that are available
	 * @see CLPlatform#listDevices(java.util.EnumSet, boolean)
	 */
    public CLDevice[] listAllDevices(boolean onlyAvailable) {
            return listDevices(EnumSet.allOf(CLDevice.Type.class), onlyAvailable);
    }

    /**
	 * Lists all the GPU devices of the platform
	 * @param onlyAvailable if true, only returns GPU devices that are available
	 * @see CLPlatform#listDevices(java.util.EnumSet, boolean)
	 */
    public CLDevice[] listGPUDevices(boolean onlyAvailable) {
            try {
                    return listDevices(EnumSet.of(CLDevice.Type.GPU), onlyAvailable);
            } catch (CLException ex) {
            if (ex.getCode() == CL_DEVICE_NOT_FOUND)
                return new CLDevice[0];
            throw new RuntimeException("Unexpected OpenCL error", ex);
        }
    }

    /**
	 * Lists all the CPU devices of the platform
	 * @param onlyAvailable if true, only returns CPU devices that are available
	 * @see CLPlatform#listDevices(java.util.EnumSet, boolean)
	 */
    public CLDevice[] listCPUDevices(boolean onlyAvailable) {
        try {
            return listDevices(EnumSet.of(CLDevice.Type.CPU), onlyAvailable);
        } catch (CLException ex) {
            if (ex.getCode() == CL_DEVICE_NOT_FOUND)
                return new CLDevice[0];
            throw new RuntimeException("Unexpected OpenCL error", ex);
        }
    }

	/**
	 * Creates an OpenCL context formed of the provided devices.<br/>
	 * It is generally not a good idea to create a context with more than one device,
	 * because much data is shared between all the devices in the same context.
	 * @param devices devices that are to form the new context
	 * @return new OpenCL context
	 */
    public CLContext createContext(CLDevice... devices) {
        int nDevs = devices.length;
		if (nDevs == 0)
			throw new IllegalArgumentException("Cannot create a context with no associated device !");
        cl_device_id[] ids = new cl_device_id[nDevs];
        for (int i = 0; i < nDevs; i++) {
            ids[i] = devices[i].get();
        }

        IntByReference errRef = new IntByReference();
		/*Memory properties = new Memory(3 * Native.POINTER_SIZE);
		IntByReference pPlatKey = new IntByReference(CL_CONTEXT_PLATFORM);
		PointerByReference pPlatVal = new PointerByReference(get().getPointer());
		properties.setPointer(0, pPlatKey.getPointer());
		properties.setPointer(Native.POINTER_SIZE, pPlatVal.getPointer());
        properties.setPointer(2 * Native.POINTER_SIZE, Pointer.NULL);*/
        cl_context context = CL.clCreateContext(null, ids.length, ids, null, null, errRef);
        error(errRef.getValue());
        return new CLContext(this, ids, context);
    }

	/**
	 * List all the devices of the specified types, with only the ones declared as available if onlyAvailable is true.
	 */
    @SuppressWarnings("deprecation")
    public CLDevice[] listDevices(EnumSet<CLDevice.Type> types, boolean onlyAvailable) {
        int flags = (int)CLDevice.Type.getValue(types);

        IntByReference pCount = new IntByReference();
        error(CL.clGetDeviceIDs(get(), flags, 0, (PointerByReference) null, pCount ));

        int nDevs = pCount.getValue();
        if (nDevs == 0)
            return new CLDevice[0];

        cl_device_id[] ids = new cl_device_id[nDevs];

        error(CL.clGetDeviceIDs(get(), flags, nDevs, ids, pCount));
		CLDevice[] devices;
		if (onlyAvailable) {
			List<CLDevice> list = new ArrayList<CLDevice>(nDevs);
			for (int i = 0; i < nDevs; i++) {
				CLDevice device = new CLDevice(this, ids[i]);
				if (device.isAvailable())
					list.add(device);
			}
			devices = list.toArray(new CLDevice[list.size()]);
		} else {
			devices = new CLDevice[nDevs];
			for (int i = 0; i < nDevs; i++)
				devices[i] = new CLDevice(this, ids[i]);
		}
        return devices;
    }


	/**
	 * OpenCL profile string. Returns the profile name supported by the implementation. The profile name returned can be one of the following strings:
	 * <ul>
	 * <li>FULL_PROFILE if the implementation supports the OpenCL specification (functionality defined as part of the core specification and does not require any extensions to be supported).</li>
	 * <li>EMBEDDED_PROFILE if the implementation supports the OpenCL embedded profile. The embedded profile is defined to be a subset for each version of OpenCL. The embedded profile for OpenCL 1.0 is described in section 10.</li>
	 * </ul>
	 */
	@InfoName("CL_PLATFORM_PROFILE")
	public String getProfile() {
		return infos.getString(get(), CL_PLATFORM_PROFILE);
	}

	/**
	OpenCL version string. Returns the OpenCL version supported by the implementation. This version string has the following format:
	OpenCL<space><major_version.min or_version><space><platform- specific information>
	Last Revision Date: 5/16/09	Page 30
	The major_version.minor_version value returned will be 1.0.
	 */
	@InfoName("CL_PLATFORM_VERSION")
	public String getVersion() {
		return infos.getString(get(), CL_PLATFORM_VERSION);
	}

	/**
	 * Platform name string.
	 */
	@InfoName("CL_PLATFORM_NAME")
	public String getName() {
		return infos.getString(get(), CL_PLATFORM_NAME);
	}

	/**
	 * Platform vendor string.
	 */
	@InfoName("CL_PLATFORM_VENDOR")
	public String getVendor() {
		return infos.getString(get(), CL_PLATFORM_VENDOR);
	}

	/**
	 * Returns a list of extension names <br/>
	 * Extensions defined here must be supported by all devices associated with this platform.
	 */
	@InfoName("CL_PLATFORM_EXTENSIONS")
	public String[] getExtensions() {
		return infos.getString(get(), CL_PLATFORM_EXTENSIONS).split("\\s+");
	}
}
