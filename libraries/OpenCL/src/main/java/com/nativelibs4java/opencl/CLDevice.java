/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
*/

package com.nativelibs4java.opencl;
import com.nativelibs4java.opencl.library.OpenCLLibrary;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;
import com.sun.jna.*;
import com.sun.jna.ptr.*;
import java.nio.*;
import static com.nativelibs4java.opencl.OpenCL4Java.*;

/// OpenCL device (CPU core, GPU...)
public class CLDevice {

    final cl_device_id device;

    CLDevice(cl_device_id device) {
        this.device = device;
    }
    String name;

    public String getName() {
        if (name == null) {
            name = getInfoString(CL_DEVICE_NAME);
        }

        return name;
    }

    public String getExecutionCapabilities() {
        return getInfoString(CL_DEVICE_EXECUTION_CAPABILITIES);
    }

    private String getInfoString(int infoName) {
        NativeLongByReference pLen = new NativeLongByReference();
        error(CL.clGetDeviceInfo(get(), infoName, toNL(0), null, pLen));

        Memory buffer = new Memory(pLen.getValue().intValue() + 1);
        error(CL.clGetDeviceInfo(get(), infoName, pLen.getValue(), buffer, null));

        return buffer.getString(0);
    }

    @Override
    public String toString() {
        return getName();
    }

    @SuppressWarnings("deprecation")
    public CLQueue createQueue(CLContext context) {
        IntByReference errRef = new IntByReference();
        cl_command_queue queue = CL.clCreateCommandQueue(context.get(), device, 0, errRef);
        error(errRef.getValue());

        return new CLQueue(context, queue);
    }

    public cl_device_id get() {
        return device;
    }

    public static CLDevice[] listAllDevices() {
            return listDevices(true, true);
    }

    public static CLDevice[] listGPUDevices() {
            try {
                    return listDevices(true, false);
            } catch (CLException ex) {
            if (ex.getCode() == CL_DEVICE_NOT_FOUND)
                return new CLDevice[0];
            throw new RuntimeException("Unexpected OpenCL error", ex);
        }
    }

    public static CLDevice[] listCPUDevices() {
        try {
            return listDevices(false, true);
        } catch (CLException ex) {
            if (ex.getCode() == CL_DEVICE_NOT_FOUND)
                return new CLDevice[0];
            throw new RuntimeException("Unexpected OpenCL error", ex);
        }
    }

    @SuppressWarnings("deprecation")
    protected static CLDevice[] listDevices(boolean gpu, boolean cpu) {
        int flags = (gpu ? CL_DEVICE_TYPE_GPU : 0) | (cpu ? CL_DEVICE_TYPE_CPU : 0);

        IntByReference pCount = new IntByReference();
        error(CL.clGetDeviceIDs(null, flags, 0, (PointerByReference) null, pCount));

        int nDevs = pCount.getValue();
        if (nDevs == 0)
            return new CLDevice[0];

        cl_device_id[] ids = new cl_device_id[nDevs];

        error(CL.clGetDeviceIDs(null, flags, nDevs, ids, pCount));
        CLDevice[] devices = new CLDevice[nDevs];

        for (int i = 0; i < nDevs; i++) {
            devices[i] = new CLDevice(ids[i]);
        }
        return devices;
    }
}