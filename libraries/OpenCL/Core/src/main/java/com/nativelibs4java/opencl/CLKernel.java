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
import static com.nativelibs4java.opencl.CLException.error;
import static com.nativelibs4java.opencl.JavaCL.CL;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;
import static com.nativelibs4java.util.JNAUtils.toNS;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.Map;

import com.nativelibs4java.opencl.library.OpenCLLibrary.cl_device_id;
import com.nativelibs4java.opencl.library.OpenCLLibrary.cl_event;
import com.nativelibs4java.opencl.library.OpenCLLibrary.cl_kernel;
import com.nativelibs4java.util.NIOUtils;
import com.ochafik.lang.jnaerator.runtime.NativeSize;
import com.ochafik.lang.jnaerator.runtime.NativeSizeByReference;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.DoubleByReference;
import com.sun.jna.ptr.FloatByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.ptr.ShortByReference;

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

    private volatile CLInfoGetter<cl_device_id> kernelInfos;
    protected synchronized CLInfoGetter<cl_device_id> getKernelInfos() {
        if (kernelInfos == null)
            kernelInfos = new CLInfoGetter<cl_device_id>() {

                @Override
                protected int getInfo(cl_device_id entity, int infoTypeEnum, NativeSize size, Pointer out, NativeSizeByReference sizeOut) {
                    return CL.clGetKernelWorkGroupInfo(getEntity(), entity, infoTypeEnum, size, out, sizeOut);
                }
            };
        return kernelInfos;
    }

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

    /**
     * Returns the preferred multiple of work- group size for launch. <br/>
     * This is a performance hint. <br/>
     * Specifying a work- group size that is not a multiple of the value returned by this query as the value of the local work size argument to clEnqueueNDRangeKernel will not fail to enqueue the kernel for execution unless the work-group size specified is larger than the device maximum.
     */
    public Map<CLDevice, Long> getPreferredWorkGroupSizeMultiple() {
    	CLDevice[] devices = program.getDevices();
        Map<CLDevice, Long> ret = new HashMap<CLDevice, Long>(devices.length);
        for (CLDevice device : devices)
            ret.put(device, getKernelInfos().getIntOrLong(device.getEntity(), CL_KERNEL_PREFERRED_WORK_GROUP_SIZE_MULTIPLE));
        return ret;
    }
    
    /**
     * This provides a mechanism for the application to query the maximum work-group size that can be used to execute a kernel on a specific device given by device. <br/>
     * The OpenCL implementation uses the resource requirements of the kernel (register usage etc.) to determine what this work- group size should be.
     * @see CL_KERNEL_WORK_GROUP_SIZE
     */
    public Map<CLDevice, Long> getWorkGroupSize() {
        CLDevice[] devices = program.getDevices();
        Map<CLDevice, Long> ret = new HashMap<CLDevice, Long>(devices.length);
        for (CLDevice device : devices)
            ret.put(device, getKernelInfos().getIntOrLong(device.getEntity(), CL_KERNEL_WORK_GROUP_SIZE));
        return ret;
    }

    /**
     * Returns the work-group size specified by the __attribute__((reqd_work_gr oup_size(X, Y, Z))) qualifier.<br/>
     * Refer to section 6.7.2.<br/>
     * If the work-group size is not specified using the above attribute qualifier (0, 0, 0) is returned.
     * @see CL_KERNEL_COMPILE_WORK_GROUP_SIZE
     * @return for each CLDevice, array of 3 longs
     */
    public Map<CLDevice, long[]> getCompileWorkGroupSize() {
        CLDevice[] devices = program.getDevices();
        Map<CLDevice, long[]> ret = new HashMap<CLDevice, long[]>(devices.length);
        for (CLDevice device : devices)
            ret.put(device, getKernelInfos().getNativeSizes(device.getEntity(), CL_KERNEL_COMPILE_WORK_GROUP_SIZE, 3));
        return ret;
    }
    
    /**
     * Returns the amount of local memory in bytes being used by a kernel. <br/>
     * This includes local memory that may be needed by an implementation to execute the kernel, variables declared inside the kernel with the __local address qualifier and local memory to be allocated for arguments to the kernel declared as pointers with the __local address qualifier and whose size is specified with clSetKernelArg.<br/>
     * If the local memory size, for any pointer argument to the kernel declared with the __local address qualifier, is not specified, its size is assumed to be 0.
     * @see CL_KERNEL_LOCAL_MEM_SIZE
     */
    public Map<CLDevice, Long> getLocalMemSize() {
        CLDevice[] devices = program.getDevices();
        Map<CLDevice, Long> ret = new HashMap<CLDevice, Long>(devices.length);
        for (CLDevice device : devices)
            ret.put(device, getKernelInfos().getIntOrLong(device.getEntity(), CL_KERNEL_LOCAL_MEM_SIZE));
        return ret;
    }
    
    public void setArgs(Object... args) {
        for (int i = 0; i < args.length; i++) {
            setObjectArg(i, args[i]);
        }
    }

    public static class LocalSize {
        long size;
        public LocalSize(long size) {
            this.size = size;
        }
    }
    private static final NativeSize zeroNS = toNS(0);
    public static final Object NULL_POINTER_KERNEL_ARGUMENT = new Object() {};
    public void setObjectArg(int iArg, Object arg) {

        if (arg == null)
            throw new IllegalArgumentException("Null arguments are not accepted. Please use CLKernel.NULL_POINTER_KERNEL_ARGUMENT instead.");

        if (arg == NULL_POINTER_KERNEL_ARGUMENT) {
            setArg(iArg, (NativeSize)zeroNS);
        //} else if (arg instanceof NativeLong) {
        //    setArg(iArg, (NativeLong) arg);
        } else if (arg instanceof NativeSize) {
            setArg(iArg, (NativeSize) arg);
        } else if (arg instanceof CLMem) {
            setArg(iArg, (CLMem) arg);
        } else if (arg instanceof CLEvent) {
            setArg(iArg, (CLEvent) arg);
        } else if (arg instanceof CLSampler) {
            setArg(iArg, (CLSampler) arg);
        } else if (arg instanceof Integer) {
            setArg(iArg, (Integer) arg);
        } else if (arg instanceof LocalSize) {
            setArg(iArg, (LocalSize)arg);
        } else if (arg instanceof Long) {
            setArg(iArg, (Long) arg);
        } else if (arg instanceof Short) {
            setArg(iArg, (Short) arg);
        } else if (arg instanceof Byte) {
            setArg(iArg, (Byte) arg);
        } else if (arg instanceof Float) {
            setArg(iArg, (Float) arg);
        } else if (arg instanceof Double) {
            setArg(iArg, (Double) arg);
		} else if (arg instanceof Boolean) {
            setArg(iArg, (byte)(Boolean.TRUE.equals(arg) ? 1 : 0));
		} else if (arg instanceof Buffer) {
            setArg(iArg, (Buffer) arg);
		} else if (arg instanceof int[]) {
			setArg(iArg, IntBuffer.wrap((int[])arg));
        } else if (arg instanceof long[]) {
			setArg(iArg, LongBuffer.wrap((long[])arg));
        } else if (arg instanceof short[]) {
			setArg(iArg, ShortBuffer.wrap((short[])arg));
        } else if (arg instanceof double[]) {
			setArg(iArg, DoubleBuffer.wrap((double[])arg));
        } else if (arg instanceof float[]) {
			setArg(iArg, FloatBuffer.wrap((float[])arg));
        } else if (arg instanceof byte[]) {
			setArg(iArg, ByteBuffer.wrap((byte[])arg));
        } else if (arg instanceof boolean[]) {
            boolean[] bools = (boolean[])arg;
            byte[] bytes = new byte[bools.length];
            for (int iValue = 0, n = bools.length; iValue < n; iValue++)
                bytes[iValue] = (byte)(bools[iValue] ? 1 : 0);
			setArg(iArg, ByteBuffer.wrap(bytes));
        } else {
			throw new IllegalArgumentException("Cannot handle kernel arguments of type " + arg.getClass().getName() + ". Use CLKernel.get() and OpenCL4Java directly.");
        }
    }

    public void setArg(int i, LocalSize arg) {
        setLocalArg(i, arg.size);
    }
    public void setLocalArg(int argIndex, long localArgByteLength) {
        error(CL.clSetKernelArg(getEntity(), argIndex, toNS(localArgByteLength), null));
    }

    //public void setArg(int i, NativeLong arg) {
    //    error(CL.clSetKernelArg(getEntity(), i, toNS(NativeLong.SIZE), new NativeLongByReference(arg).getPointer()));
//			error(CL.clSetKernelArg(get(), i, OpenCL4Java.toNL(Native.LONG_SIZE), new IntByReference(128).getPointer()));
//			error(CL.clSetKernelArg(get(), i, toNL(Native.LONG_SIZE), new IntByReference(arg.intValue()).getPointer()));
    //}

    public void setArg(int i, float[] arg) {
        setArg(i, FloatBuffer.wrap(arg));
    }
    public void setArg(int i, int[] arg) {
        setArg(i, IntBuffer.wrap(arg));
    }
    public void setArg(int i, double[] arg) {
        setArg(i, DoubleBuffer.wrap(arg));
    }
    public void setArg(int i, long[] arg) {
        setArg(i, LongBuffer.wrap(arg));
    }
    public void setArg(int i, short[] arg) {
        setArg(i, ShortBuffer.wrap(arg));
    }
    public void setArg(int i, byte[] arg) {
        setArg(i, ByteBuffer.wrap(arg));
    }
    public void setArg(int i, char[] arg) {
        setArg(i, CharBuffer.wrap(arg));
    }
    public void setArg(int i, Buffer arg) {
		if (!arg.isDirect())
			arg = NIOUtils.directCopy(arg, getProgram().getContext().getByteOrder());
		long size = NIOUtils.getSizeInBytes(arg);
        error(CL.clSetKernelArg(getEntity(), i, toNS(size), Native.getDirectBufferPointer(arg)));
    }

    public void setArg(int i, NativeSize arg) {
        switch (getProgram().getContext().getAddressBits()) {
            case 32:
                error(CL.clSetKernelArg(getEntity(), i, toNS(4), new IntByReference(arg.intValue()).getPointer()));
                break;
            case 64:
                error(CL.clSetKernelArg(getEntity(), i, toNS(8), new LongByReference(arg.longValue()).getPointer()));
                break;
            default:
                error(CL.clSetKernelArg(getEntity(), i, toNS(NativeSize.SIZE), new NativeSizeByReference(arg).getPointer()));
                break;
        }
    }

    public void setArg(int i, int arg) {
        error(CL.clSetKernelArg(getEntity(), i, toNS(4), new IntByReference(arg).getPointer()));
    }

    public void setArg(int i, long arg) {
        error(CL.clSetKernelArg(getEntity(), i, toNS(8), new LongByReference(arg).getPointer()));
    }

    public void setArg(int i, short arg) {
        error(CL.clSetKernelArg(getEntity(), i, toNS(2), new ShortByReference(arg).getPointer()));
    }

    public void setArg(int i, byte arg) {
        error(CL.clSetKernelArg(getEntity(), i, toNS(1), new ByteByReference(arg).getPointer()));
    }

    public void setArg(int i, float arg) {
        error(CL.clSetKernelArg(getEntity(), i, toNS(4), new FloatByReference(arg).getPointer()));
    }

    public void setArg(int i, double arg) {
        error(CL.clSetKernelArg(getEntity(), i, toNS(8), new DoubleByReference(arg).getPointer()));
    }

    public void setArg(int index, CLMem mem) {
        error(CL.clSetKernelArg(getEntity(), index, toNS(Pointer.SIZE), new PointerByReference(mem.getEntity().getPointer()).getPointer()));
    }

    public void setArg(int index, CLEvent event) {
        error(CL.clSetKernelArg(getEntity(), index, toNS(Pointer.SIZE), new PointerByReference(event.getEntity().getPointer()).getPointer()));
    }

    public void setArg(int index, CLSampler sampler) {
        error(CL.clSetKernelArg(getEntity(), index, toNS(Pointer.SIZE), new PointerByReference(sampler.getEntity().getPointer()).getPointer()));
    }

    @Override
    protected void clear() {
        error(CL.clReleaseKernel(getEntity()));
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
        cl_event[] eventOut = CLEvent.new_event_out(eventsToWaitFor);
        cl_event[] evts = CLEvent.to_cl_event_array(eventsToWaitFor);
        error(CL.clEnqueueNDRangeKernel(queue.getEntity(), getEntity(), 1, null, oneNL, oneNL, evts == null ? 0 : evts.length, evts, eventOut));
        return CLEvent.createEvent(queue, eventOut);
    }

    /**
     * Enqueues a command to execute a kernel on a device.
     * @see enqueueNDRange(CLQueue, int[], int[], int[], CLEvent...)
     * @param globalWorkSizes Each element describes the number of global work-items in a dimension that will execute the kernel function. The total number of global work-items is computed as globalWorkSizes[0] * ... * globalWorkSizes[globalWorkSizes.length - 1].
     * @param localWorkSizes Each element describes the number of work-items that make up a work-group (also referred to as the size of the work-group) that will execute the kernel specified by kernel. The total number of work-items in a work-group is computed as localWorkSizes[0] * ... * localWorkSizes[localWorkSizes.length - 1]. The total number of work-items in the work-group must be less than or equal to the CL_DEVICE_MAX_WORK_GROUP_SIZE value specified in table 4.3 and the number of work- items specified in localWorkSizes[0], ... localWorkSizes[localWorkSizes.length - 1] must be less than or equal to the corresponding values specified by CLDevice.getMaxWorkItemSizes()[dimensionIndex].	The explicitly specified localWorkSize will be used to determine how to break the global work-items specified by global_work_size into appropriate work-group instances. If localWorkSize is specified, the values specified in globalWorkSize[dimensionIndex] must be evenly divisible by the corresponding values specified in localWorkSize[dimensionIndex].
     * @param queue This kernel will be queued for execution on the device associated with that queue.
     * @param eventsToWaitFor Events that need to complete before this particular command can be executed.
     * @return Event object that identifies this command and can be used to query or queue a wait for the command to complete.
     */
    public CLEvent enqueueNDRange(CLQueue queue /*, int[] globalOffsets*/, int[] globalWorkSizes, int[] localWorkSizes, CLEvent... eventsToWaitFor) {
    	return enqueueNDRange(queue, null, globalWorkSizes, localWorkSizes, eventsToWaitFor);
    }
    
    /**
     * Enqueues a command to execute a kernel on a device.
     * @param globalOffsets Must be null in OpenCL 1.0.
     * @param globalWorkSizes Each element describes the number of global work-items in a dimension that will execute the kernel function. The total number of global work-items is computed as globalWorkSizes[0] * ... * globalWorkSizes[globalWorkSizes.length - 1].
     * @param localWorkSizes Each element describes the number of work-items that make up a work-group (also referred to as the size of the work-group) that will execute the kernel specified by kernel. The total number of work-items in a work-group is computed as localWorkSizes[0] * ... * localWorkSizes[localWorkSizes.length - 1]. The total number of work-items in the work-group must be less than or equal to the CL_DEVICE_MAX_WORK_GROUP_SIZE value specified in table 4.3 and the number of work- items specified in localWorkSizes[0], ... localWorkSizes[localWorkSizes.length - 1] must be less than or equal to the corresponding values specified by CLDevice.getMaxWorkItemSizes()[dimensionIndex].	The explicitly specified localWorkSize will be used to determine how to break the global work-items specified by global_work_size into appropriate work-group instances. If localWorkSize is specified, the values specified in globalWorkSize[dimensionIndex] must be evenly divisible by the corresponding values specified in localWorkSize[dimensionIndex].
     * @param queue This kernel will be queued for execution on the device associated with that queue.
     * @param eventsToWaitFor Events that need to complete before this particular command can be executed.
     * @return Event object that identifies this command and can be used to query or queue a wait for the command to complete.
     */
    public CLEvent enqueueNDRange(CLQueue queue, int[] globalOffsets, int[] globalWorkSizes, int[] localWorkSizes, CLEvent... eventsToWaitFor) {
        int nDims = globalWorkSizes.length;
        if (localWorkSizes != null && localWorkSizes.length != nDims) {
            throw new IllegalArgumentException("Global and local sizes must have same dimensions, given " + globalWorkSizes.length + " vs. " + localWorkSizes.length);
        }
        cl_event[] eventOut = CLEvent.new_event_out(eventsToWaitFor);
        cl_event[] evts = CLEvent.to_cl_event_array(eventsToWaitFor);
        error(CL.clEnqueueNDRangeKernel(queue.getEntity(), getEntity(), nDims, toNS(globalOffsets), toNS(globalWorkSizes), toNS(localWorkSizes), evts == null ? 0 : evts.length, evts, eventOut));
        return CLEvent.createEvent(queue, eventOut);
    }
	
	/**
	 * Return the number of arguments to kernel.
	 */
    @InfoName("CL_KERNEL_NUM_ARGS")
    public int getNumArgs() {
        return infos.getInt(getEntity(), CL_KERNEL_NUM_ARGS);
    }

    /**
     * Return the kernel function name.
     */
    @InfoName("CL_KERNEL_FUNCTION_NAME")
    public String getFunctionName() {
        if (name == null)
            name = infos.getString(getEntity(), CL_KERNEL_FUNCTION_NAME);
        return name;
    }

	
}