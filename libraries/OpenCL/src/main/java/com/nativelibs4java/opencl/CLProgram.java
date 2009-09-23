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

public class CLProgram extends CLEntity<cl_program> {

    protected final CLContext context;

    CLProgram(CLContext context, cl_program entity) {
        super(entity);
        this.context = context;
    }
    public CLContext getContext() {
        return context;
    }
    public CLProgram build() throws CLBuildException {

        int err = CL.clBuildProgram(get(), 0, null/*context.getDeviceIds()*/, (String) null, null, null);
        if (err != CL_SUCCESS) {
            NativeLongByReference len = new NativeLongByReference();
            int bufLen = 2048;
            Memory buffer = new Memory(bufLen);

            HashSet<String> errs = new HashSet<String>();
            for (cl_device_id device_id : context.getDeviceIds()) {
                error(CL.clGetProgramBuildInfo(get(), device_id, CL_PROGRAM_BUILD_LOG, toNL(bufLen), buffer, len));
                String s = buffer.getString(0);
                errs.add(s);
            }
            throw new CLBuildException(errorString(err), errs);
        }
        return this;
    }

    @Override
    protected void clear() {
        CL.clReleaseProgram(get());
    }

    public CLKernel createKernel(String name, Object... args) {
        IntBuffer errBuff = IntBuffer.wrap(new int[1]);
        cl_kernel kernel = CL.clCreateKernel(get(), name, errBuff);
        error(errBuff.get(0));

        CLKernel kn = new CLKernel(this, name, kernel);
        kn.setArgs(args);
        return kn;
    }
}