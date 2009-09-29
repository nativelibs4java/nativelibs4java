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
 * Entry point of OpenCL4Java : gives access to OpenCL implementations available on the system.
 * @author Olivier Chafik
 */
public class CLPlatform extends CLEntity<cl_platform_id> {

    CLPlatform(cl_platform_id platform) {
        super(platform);
    }


    String name, version;

    public String getName() {
        if (name == null)
            name = getInfoString(CL_PLATFORM_PROFILE);
        return name;
    }
    public String getVersion() {
        if (version == null)
            version = getInfoString(CL_PLATFORM_VERSION);
        return version;
    }


    @Override
    public String toString() {
        return getName() + "(" + getVersion() + ")";
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
}
