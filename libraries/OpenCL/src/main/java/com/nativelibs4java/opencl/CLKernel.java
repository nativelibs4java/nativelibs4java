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

public class CLKernel extends CLEntity<cl_kernel> {

    protected final CLProgram program;
    protected final String name;

    CLKernel(CLProgram program, String name, cl_kernel entity) {
        super(entity);
        this.program = program;
        this.name = name;
    }
    public CLProgram getProgram() {
        return program;
    }
    public String getName() {
        return name;
    }

    public void setArgs(Object... args) {
        for (int i = 0; i < args.length; i++) {
            setObjectArg(i, args[i]);
        }
    }

    public void setObjectArg(int i, Object arg) {

        if (arg instanceof NativeLong) {
            setArg(i, (NativeLong) arg);
        } else if (arg instanceof CLMem) {
            setArg(i, (CLMem) arg);
        } else if (arg instanceof Integer) {
            setArg(i, (Integer) arg);
        } else if (arg instanceof Long) {
            setArg(i, (Long) arg);
        } else if (arg instanceof Short) {
            setArg(i, (Short) arg);
        } else if (arg instanceof Byte) {
            setArg(i, (Byte) arg);
        } else if (arg instanceof Float) {
            setArg(i, (Float) arg);
        } else if (arg instanceof Double) {
            setArg(i, (Double) arg);
        } else {
            throw new IllegalArgumentException("Cannot handle kernel arguments of type " + arg.getClass().getName() + ". Use CLKernel.get() and OpenCL4Java directly.");
        }
    }

    public void setArg(int i, NativeLong arg) {
        error(CL.clSetKernelArg(get(), i, toNL(Native.LONG_SIZE), new NativeLongByReference(arg).getPointer()));
//			error(CL.clSetKernelArg(get(), i, OpenCL4Java.toNL(Native.LONG_SIZE), new IntByReference(128).getPointer()));
//			error(CL.clSetKernelArg(get(), i, toNL(Native.LONG_SIZE), new IntByReference(arg.intValue()).getPointer()));
    }

    public void setArg(int i, int arg) {
        error(CL.clSetKernelArg(get(), i, toNL(4), new IntByReference(arg).getPointer()));
    }

    public void setArg(int i, long arg) {
        error(CL.clSetKernelArg(get(), i, toNL(8), new LongByReference(arg).getPointer()));
    }

    public void setArg(int i, short arg) {
        error(CL.clSetKernelArg(get(), i, toNL(2), new ShortByReference(arg).getPointer()));
    }

    public void setArg(int i, byte arg) {
        error(CL.clSetKernelArg(get(), i, toNL(1), new ByteByReference(arg).getPointer()));
    }

    public void setArg(int i, float arg) {
        error(CL.clSetKernelArg(get(), i, toNL(4), new FloatByReference(arg).getPointer()));
    }

    public void setArg(int i, double arg) {
        error(CL.clSetKernelArg(get(), i, toNL(8), new DoubleByReference(arg).getPointer()));
    }

    public void setArg(int index, CLMem mem) {
//			new PointerByReference(input.getPointer()).getPointer())
        error(CL.clSetKernelArg(get(), index, toNL(Pointer.SIZE), new PointerByReference(mem.getPointer()).getPointer()));
    }

    @Override
    protected void clear() {
        CL.clReleaseKernel(get());
    }

    /// TODO: Get the maximum work-group size with CL.clGetKernelWorkGroupInfo(CL_KERNEL_WORK_GROUP_SIZE)
    public void enqueueNDRange(CLQueue queue, int[] globalSizes, int[] localSizes) {
        int nDims = globalSizes.length;
        if (localSizes.length != nDims) {
            throw new IllegalArgumentException("Global and local sizes must have same dimensions, given " + globalSizes.length + " vs. " + localSizes.length);
        }
        NativeLong[] globalSizesNL = new NativeLong[nDims], localSizesNL = new NativeLong[nDims];
        for (int i = 0; i < nDims; i++) {
            globalSizesNL[i] = toNL(globalSizes[i]);
            localSizesNL[i] = toNL(localSizes[i]);
        }
        error(CL.clEnqueueNDRangeKernel(queue.get(), get(), 1, null, globalSizesNL, localSizesNL, 0, null, null));
    }
}