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
import com.ochafik.lang.jnaerator.runtime.NativeSize;
import com.ochafik.lang.jnaerator.runtime.NativeSizeByReference;
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
 * @see CLProgram#createKernel(java.lang.String, java.lang.Object[])
 * @see CLProgram#createKernels() 
 * @author Olivier Chafik
 */
public class CLKernel extends CLAbstractEntity<cl_kernel> {

    protected final CLProgram program;
    protected String name;
	private static CLInfoGetter<cl_kernel> infos = new CLInfoGetter<cl_kernel>() {
		@Override
		protected int getInfo(cl_kernel entity, int infoTypeEnum, NativeSize size, Pointer out, NativeSizeByReference sizeOut) {
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
		} else if (arg instanceof NativeSize) {
            setArg(i, (NativeSize) arg);
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
		error(CL.clSetKernelArg(get(), argIndex, toNS(localArgByteLength), null));
	}
	
    public void setArg(int i, NativeLong arg) {
        error(CL.clSetKernelArg(get(), i, toNS(NativeLong.SIZE), new NativeLongByReference(arg).getPointer()));
//			error(CL.clSetKernelArg(get(), i, OpenCL4Java.toNL(Native.LONG_SIZE), new IntByReference(128).getPointer()));
//			error(CL.clSetKernelArg(get(), i, toNL(Native.LONG_SIZE), new IntByReference(arg.intValue()).getPointer()));
    }

    public void setArg(int i, NativeSize arg) {
        error(CL.clSetKernelArg(get(), i, toNS(NativeSize.SIZE), new NativeSizeByReference(arg).getPointer()));
    }

    public void setArg(int i, int arg) {
        error(CL.clSetKernelArg(get(), i, toNS(4), new IntByReference(arg).getPointer()));
    }

    public void setArg(int i, long arg) {
        error(CL.clSetKernelArg(get(), i, toNS(8), new LongByReference(arg).getPointer()));
    }

    public void setArg(int i, short arg) {
        error(CL.clSetKernelArg(get(), i, toNS(2), new ShortByReference(arg).getPointer()));
    }

    public void setArg(int i, byte arg) {
        error(CL.clSetKernelArg(get(), i, toNS(1), new ByteByReference(arg).getPointer()));
    }

    public void setArg(int i, float arg) {
        error(CL.clSetKernelArg(get(), i, toNS(4), new FloatByReference(arg).getPointer()));
    }

    public void setArg(int i, double arg) {
        error(CL.clSetKernelArg(get(), i, toNS(8), new DoubleByReference(arg).getPointer()));
    }

    public void setArg(int index, CLMem mem) {
        error(CL.clSetKernelArg(get(), index, toNS(Pointer.SIZE), new PointerByReference(mem.get().getPointer()).getPointer()));
    }

    public void setArg(int index, CLSampler sampler) {
        error(CL.clSetKernelArg(get(), index, toNS(Pointer.SIZE), new PointerByReference(sampler.get().getPointer()).getPointer()));
    }

    @Override
    protected void clear() {
        error(CL.clReleaseKernel(get()));
    }

	private static final NativeSize[] oneNL = new NativeSize[] {new NativeSize(1)};
	/**
	 * Enqueues a command to execute a kernel on a device. <br>
	 * The kernel is executed using a single work-item.
	 * @param queue
	 * @param eventsToWaitFor Events that need to complete before this particular command can be executed.
	 * @return Event object that identifies this command and can be used to query or queue a wait for the command to complete.
	 */
	public CLEvent enqueueTask(CLQueue queue, CLEvent... eventsToWaitFor) {
		cl_event[] eventOut = new cl_event[1];
        error(CL.clEnqueueNDRangeKernel(queue.get(), get(), 1, null, oneNL, oneNL, eventsToWaitFor.length, CLEvent.to_cl_event_array(eventsToWaitFor), eventOut));
		return CLEvent.createEvent(eventOut[0]);
	}

    /**
	 * Enqueues a command to execute a kernel on a device.
	 * @param globalWorkSizes Each element describes the number of global work-items in a dimension that will execute the kernel function. The total number of global work-items is computed as globalWorkSizes[0] * ... * globalWorkSizes[globalWorkSizes.length Ð 1].
	 * @param localWorkSizes Each element describes the number of work-items that make up a work-group (also referred to as the size of the work-group) that will execute the kernel specified by kernel. The total number of work-items in a work-group is computed as localWorkSizes[0] * ... * localWorkSizes[localWorkSizes.length Ð 1]. The total number of work-items in the work-group must be less than or equal to the CL_DEVICE_MAX_WORK_GROUP_SIZE value specified in table 4.3 and the number of work- items specified in localWorkSizes[0], ... localWorkSizes[localWorkSizes.length Ð 1] must be less than or equal to the corresponding values specified by CLDevice.getMaxWorkItemSizes()[dimensionIndex].	The explicitly specified localWorkSize will be used to determine how to break the global work-items specified by global_work_size into appropriate work-group instances. If localWorkSize is specified, the values specified in globalWorkSize[dimensionIndex] must be evenly divisible by the corresponding values specified in localWorkSize[dimensionIndex].
	 * @param queue This kernel will be queued for execution on the device associated with that queue.
	 * @param eventsToWaitFor Events that need to complete before this particular command can be executed.
	 * @return Event object that identifies this command and can be used to query or queue a wait for the command to complete.
	 */
	public CLEvent enqueueNDRange(CLQueue queue /*, int[] globalOffsets*/, int[] globalWorkSizes, int[] localWorkSizes, CLEvent... eventsToWaitFor) {
        int nDims = globalWorkSizes.length;
        if (localWorkSizes != null && localWorkSizes.length != nDims) {
            throw new IllegalArgumentException("Global and local sizes must have same dimensions, given " + globalWorkSizes.length + " vs. " + localWorkSizes.length);
        }
		cl_event[] eventOut = new cl_event[1];
        error(CL.clEnqueueNDRangeKernel(queue.get(), get(), nDims, null/*toNL(globalOffsets)*/, toNS(globalWorkSizes), toNS(localWorkSizes), eventsToWaitFor.length, CLEvent.to_cl_event_array(eventsToWaitFor), eventOut));
		return CLEvent.createEvent(eventOut[0]);
    }
	
	/**
	 * Return the number of arguments to kernel.
	 */
	@InfoName("CL_KERNEL_NUM_ARGS")
	public int getNumArgs() {
		return infos.getInt(get(), CL_KERNEL_NUM_ARGS);
    }

	/**
	 * Return the kernel function name.
	 */
	@InfoName("CL_KERNEL_FUNCTION_NAME")
    public String getFunctionName() {
		if (name == null)
			name = infos.getString(get(), CL_KERNEL_FUNCTION_NAME);
		return name;
    }

	
}