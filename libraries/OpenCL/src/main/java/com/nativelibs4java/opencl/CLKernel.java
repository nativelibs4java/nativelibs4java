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
import static com.nativelibs4java.opencl.CLException.*;
import static com.nativelibs4java.util.JNAUtils.*;

/**
 * OpenCL kernel.<br/>
 * A kernel is a function declared in a program. <br/>
 * A kernel is identified by the __kernel qualifier applied to any function in a program. <br/>
 * A kernel object encapsulates the specific __kernel function declared in a program and the argument values to be used when executing this __kernel function.</br>
 * </br>
 * Kernels can be queued for execution in a CLQueue (see enqueueTask and enqueueNDRange)
 * @author Olivier Chafik
 */
public class CLKernel extends CLEntity<cl_kernel> {

    protected final CLProgram program;
    protected String name;
	private static CLInfoGetter<cl_kernel> infos = new CLInfoGetter<cl_kernel>() {
		@Override
		protected int getInfo(cl_kernel entity, int infoTypeEnum, NativeLong size, Pointer out, NativeLongByReference sizeOut) {
			return CL.clGetKernelInfo(entity, infoTypeEnum, size, out, sizeOut);
		}
	};

    CLKernel(CLProgram program, String name, cl_kernel entity) {
        super(entity);
        this.program = program;
        this.name = name;
    }
    public CLProgram getProgram() {
        return program;
    }

	public String toString() {
		return getFunctionName() + " {args: " + getNumArgs() + "}";//, workGroupSize = " + getWorkGroupSize() + ", localMemSize = " + getLocalMemSize() + "}";
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
        } else if (arg instanceof CLSampler) {
            setArg(i, (CLSampler) arg);
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

	public void setLocalArg(int argIndex, long localArgByteLength) {
		error(CL.clSetKernelArg(get(), argIndex, toNL(localArgByteLength), null));
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
        error(CL.clSetKernelArg(get(), index, toNL(Pointer.SIZE), new PointerByReference(mem.get().getPointer()).getPointer()));
    }

    public void setArg(int index, CLSampler sampler) {
        error(CL.clSetKernelArg(get(), index, toNL(Pointer.SIZE), new PointerByReference(sampler.get().getPointer()).getPointer()));
    }

    @Override
    protected void clear() {
        error(CL.clReleaseKernel(get()));
    }

	private static final NativeLong[] oneNL = new NativeLong[] {new NativeLong(1)};
	public CLEvent enqueueTask(CLQueue queue, CLEvent... eventsToWaitFor) {
		cl_event[] eventOut = new cl_event[1];
        error(CL.clEnqueueNDRangeKernel(queue.get(), get(), 1, null, oneNL, oneNL, eventsToWaitFor.length, CLEvent.to_cl_event_array(eventsToWaitFor), eventOut));
		return CLEvent.createEvent(eventOut[0]);
	}

    public CLEvent enqueueNDRange(CLQueue queue /*, int[] globalOffsets*/, int[] globalSizes, int[] localSizes, CLEvent... eventsToWaitFor) {
        int nDims = globalSizes.length;
        if (localSizes != null && localSizes.length != nDims) {
            throw new IllegalArgumentException("Global and local sizes must have same dimensions, given " + globalSizes.length + " vs. " + localSizes.length);
        }
		cl_event[] eventOut = new cl_event[1];
        error(CL.clEnqueueNDRangeKernel(queue.get(), get(), nDims, null/*toNL(globalOffsets)*/, toNL(globalSizes), toNL(localSizes), eventsToWaitFor.length, CLEvent.to_cl_event_array(eventsToWaitFor), eventOut));
		return CLEvent.createEvent(eventOut[0]);
    }
	
	/**
	 * CL_KERNEL_NUM_ARGS<br/>
	 * Return the number of arguments to kernel.
	 */
	public int getNumArgs() {
		return infos.getInt(get(), CL_KERNEL_NUM_ARGS);
    }

	/**
	 * CL_KERNEL_FUNCTION_NAME<br/>
	 * Return the kernel function name.
	 */
    public String getFunctionName() {
		if (name == null)
			name = infos.getString(get(), CL_KERNEL_FUNCTION_NAME);
		return name;
    }

	
}