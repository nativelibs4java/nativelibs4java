/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;
import com.sun.jna.*;
import com.sun.jna.ptr.*;
import java.nio.*;
import static com.nativelibs4java.opencl.OpenCL4Java.*;

/**
 * OpenCL implementation entry point.
 * @author Olivier Chafik
 */
public class CLPlatform extends CLEntity<cl_platform_id> {

    CLPlatform(cl_platform_id platform) {
        super(platform);
    }

	static CLInfoGetter<cl_platform_id> infos = new CLInfoGetter<cl_platform_id>() {
		@Override
		protected int getInfo(cl_platform_id entity, int infoTypeEnum, NativeLong size, Pointer out, NativeLongByReference sizeOut) {
			return CL.clGetPlatformInfo(entity, infoTypeEnum, size, out, sizeOut);
		}
	};
	
    @Override
    public String toString() {
        return getName() + " {vendor: " + getVendor() + ", version: " + getVersion() + ", profile: " + getProfile() + ", extensions: " + getExtensions() +"}";
    }

    @Override
    protected void clear() {
        //CL.clReleasePlatform(get());
    }


    private String getInfoString(int infoName) {
        NativeLongByReference pLen = new NativeLongByReference();
        error(CL.clGetPlatformInfo(get(), infoName, toNL(0), null, pLen));

        Memory buffer = new Memory(pLen.getValue().intValue() + 1);
        error(CL.clGetPlatformInfo(get(), infoName, pLen.getValue(), buffer, null));

        return buffer.getString(0);
    }
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


    public CLDevice[] listAllDevices() {
            return listDevices(true, true);
    }

    public CLDevice[] listGPUDevices() {
            try {
                    return listDevices(true, false);
            } catch (CLException ex) {
            if (ex.getCode() == CL_DEVICE_NOT_FOUND)
                return new CLDevice[0];
            throw new RuntimeException("Unexpected OpenCL error", ex);
        }
    }

    public CLDevice[] listCPUDevices() {
        try {
            return listDevices(false, true);
        } catch (CLException ex) {
            if (ex.getCode() == CL_DEVICE_NOT_FOUND)
                return new CLDevice[0];
            throw new RuntimeException("Unexpected OpenCL error", ex);
        }
    }

    @SuppressWarnings("deprecation")
    protected CLDevice[] listDevices(boolean gpu, boolean cpu) {
        int flags = (gpu ? CL_DEVICE_TYPE_GPU : 0) | (cpu ? CL_DEVICE_TYPE_CPU : 0);

        IntByReference pCount = new IntByReference();
        error(CL.clGetDeviceIDs(
            get(),
            flags,
            0,
            (PointerByReference) null,
            pCount
        ));

        int nDevs = pCount.getValue();
        if (nDevs == 0)
            return new CLDevice[0];

        cl_device_id[] ids = new cl_device_id[nDevs];

        error(CL.clGetDeviceIDs(get(), flags, nDevs, ids, pCount));
        CLDevice[] devices = new CLDevice[nDevs];

        for (int i = 0; i < nDevs; i++) {
            devices[i] = new CLDevice(ids[i]);
        }
        return devices;
    }


	/**
	 * CL_PLATFORM_PROFILE<br/>
	OpenCL profile string. Returns the profile name supported by the implementation. The profile name returned can be one of the following strings:
	 * <ul>
	 * <li>FULL_PROFILE if the implementation supports the OpenCL specification (functionality defined as part of the core specification and does not require any extensions to be supported).</li>
	 * <li>EMBEDDED_PROFILE if the implementation supports the OpenCL embedded profile. The embedded profile is defined to be a subset for each version of OpenCL. The embedded profile for OpenCL 1.0 is described in section 10.</li>
	 * </ul>
	 */
	public String getProfile() {
		return infos.getString(get(), CL_PLATFORM_PROFILE);
	}

	/**
	 * CL_PLATFORM_VERSION<br/>
	OpenCL version string. Returns the OpenCL version supported by the implementation. This version string has the following format:
	OpenCL<space><major_version.min or_version><space><platform- specific information>
	Last Revision Date: 5/16/09	Page 30
	The major_version.minor_version value returned will be 1.0.
	 */
	public String getVersion() {
		return infos.getString(get(), CL_PLATFORM_VERSION);
	}

	/**
	 * CL_PLATFORM_NAME<br/>
	Platform name string.
	 */
	public String getName() {
		return infos.getString(get(), CL_PLATFORM_NAME);
	}

	/**
	 * CL_PLATFORM_VENDOR<br/>
	Platform vendor string.
	 */
	public String getVendor() {
		return infos.getString(get(), CL_PLATFORM_VENDOR);
	}

	/**
	 * CL_PLATFORM_EXTENSIONS<br/>
	Returns a space separated list of extension names (the extension names themselves do not contain any spaces) supported by the platform. Extensions defined here must be supported by all devices associated with this platform.
	 */
	public String getExtensions() {
		return infos.getString(get(), CL_PLATFORM_EXTENSIONS);
	}

}
