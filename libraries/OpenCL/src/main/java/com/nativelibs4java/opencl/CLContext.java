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

public class CLContext extends CLEntity<cl_context> {

    protected final cl_device_id[] deviceIds;

    protected CLContext(cl_device_id[] deviceIds, cl_context context) {
        super(context);
        this.deviceIds = deviceIds;
    }

    public CLQueue createDefaultQueue() {
        return new CLDevice(deviceIds[0]).createQueue(this);
    }

    public cl_device_id[] getDeviceIds() {
        return deviceIds;
    }

    /**
     * Create a program with all the C source code content provided as argument.
     * @param srcs list of the content of source code for the program
     * @return a program that needs to be built
     */
    public CLProgram createProgram(String... srcs) {

        String[] source = new String[srcs.length];
        NativeLong[] lengths = new NativeLong[srcs.length];
        for (int i = 0; i < srcs.length; i++) {
            source[i] = srcs[i];
            lengths[i] = toNL(srcs[i].length());
        }
        IntBuffer errBuff = IntBuffer.wrap(new int[1]);
        cl_program program = CL.clCreateProgramWithSource(get(), srcs.length, source, lengths, errBuff);
        error(errBuff.get(0));
        return new CLProgram(this, program);
    }

    public static CLContext createContext(CLDevice... devices) {
        int nDevs = devices.length;
        cl_device_id[] ids = new cl_device_id[nDevs];
        for (int i = 0; i < nDevs; i++) {
            ids[i] = devices[i].get();
        }

        IntByReference errRef = new IntByReference();
        cl_context context = CL.clCreateContext(null, 1, ids, null, null, errRef);
        error(errRef.getValue());
        return new CLContext(ids, context);
    }

    //cl_queue queue;
    @Override
    protected void clear() {
        CL.clReleaseContext(get());
    }

    public CLMem createInput(Buffer buffer, boolean copy) {
        return createMem(buffer, -1, CL_MEM_READ_ONLY | (copy ? CL_MEM_COPY_HOST_PTR : CL_MEM_USE_HOST_PTR), true);
    }

    public CLMem createOutput(Buffer buffer) {
        return createMem(buffer, -1, CL_MEM_WRITE_ONLY | CL_MEM_USE_HOST_PTR, true);
    }

    public CLMem createInputOutput(Buffer buffer) {
        return createMem(buffer, -1, CL_MEM_READ_WRITE | CL_MEM_USE_HOST_PTR, true);
    }

    public CLMem createInput(int byteCount) {
        return createMem(null, byteCount, CL_MEM_READ_ONLY, false);
    }

    public CLMem createOutput(int byteCount) {
        return createMem(null, byteCount, CL_MEM_WRITE_ONLY, false);
    }

    public CLMem createInputOutput(int byteCount) {
        return createMem(null, byteCount, CL_MEM_READ_WRITE, false);
    }

    @SuppressWarnings("deprecation")
    protected CLMem createMem(final Buffer buffer, int byteCount, final int clMemFlags, final boolean retainBufferReference) {
        if (buffer != null) {
            byteCount = getSizeInBytes(buffer);
        } else if (retainBufferReference) {
            throw new IllegalArgumentException("Cannot retain reference to null pointer !");
        }

        if (byteCount <= 0) {
            throw new IllegalArgumentException("Buffer size must be greater than zero (asked for size " + byteCount + ")");
        }

        IntByReference errRef = new IntByReference();
        //IntBuffer errBuff = IntBuffer.wrap(new int[1]);
        cl_mem mem = CL.clCreateBuffer(
                get(),
                toNL(clMemFlags),
                toNL(byteCount),
                buffer == null ? null : Native.getDirectBufferPointer(buffer),
                errRef);
        error(errRef.getValue());

        return new CLMem(this, byteCount, mem) {
            /// keep a hard reference to the buffer

            public Buffer b = retainBufferReference ? buffer : null;

            @Override
            public String toString() {
                return "CLMem(flags = " + clMemFlags + (b == null ? "" : ", " + getSizeInBytes(b) + " bytes") + ")";
            }
        };
    }
}