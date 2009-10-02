/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl;
import com.nativelibs4java.opencl.CLSampler.CLAddressingMode;
import com.nativelibs4java.opencl.CLSampler.CLFilterMode;
import com.nativelibs4java.opencl.library.OpenCLLibrary;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;
import com.sun.jna.*;
import com.sun.jna.ptr.*;
import java.nio.*;
import static com.nativelibs4java.opencl.OpenCL4Java.*;
import static com.nativelibs4java.opencl.CLException.*;
import static com.nativelibs4java.util.JNAUtils.*;
import static com.nativelibs4java.util.NIOUtils.*;


/**
 * OpenCL context.<br/>
 * An OpenCL context is created with one or more devices.<br/>
 * Contexts are used by the OpenCL runtime for managing objects such as command-queues, memory, program and kernel objects and for executing kernels on one or more devices specified in the context.
 * @author Olivier Chafik
 */
public class CLContext extends CLEntity<cl_context> {

    private static CLInfoGetter<cl_context> infos = new CLInfoGetter<cl_context>() {
		@Override
		protected int getInfo(cl_context entity, int infoTypeEnum, NativeLong size, Pointer out, NativeLongByReference sizeOut) {
			return CL.clGetContextInfo(entity, infoTypeEnum, size, out, sizeOut);
		}
	};

	CLPlatform platform;

    protected cl_device_id[] deviceIds;

    CLContext(CLPlatform platform, cl_device_id[] deviceIds, cl_context context) {
        super(context);
		this.platform = platform;
        this.deviceIds = deviceIds;
    }

	/**
	 * Create an OpenCL queue on the first device of this context.<br/>
	 * Equivalent to calling <code>getDevices()[0].createQueue(context)</code>
	 * @return new OpenCL queue
	 */
    public CLQueue createDefaultQueue() {
        return new CLDevice(platform, deviceIds[0]).createQueue(this);
    }


	public CLSampler createSampler(boolean normalized_coords, CLAddressingMode addressing_mode, CLFilterMode filter_mode) {
		IntByReference pErr = new IntByReference();
		cl_sampler sampler = CL.clCreateSampler(get(), normalized_coords ? CL_TRUE : CL_FALSE, (int)addressing_mode.getValue(), (int)filter_mode.getValue(), pErr);
		error(pErr.getValue());
		return new CLSampler(sampler);
	}

	/**
	 * Lists the devices of this context
	 * @return array of the devices that form this context
	 */
	public synchronized CLDevice[] getDevices() {
		if (deviceIds == null) {
			Memory ptrs = infos.getMemory(get(), CL_CONTEXT_DEVICES);
			int n = (int)(ptrs.getSize() / Native.POINTER_SIZE);
			deviceIds = new cl_device_id[n];
			for (int i = 0; i < n; i++)
				deviceIds[i] = new cl_device_id(ptrs.getPointer(i * Native.POINTER_SIZE));
		}
		CLDevice[] devices = new CLDevice[deviceIds.length];
		for (int i = devices.length; i-- != 0;)
			devices[i] = new CLDevice(platform, deviceIds[i]);
		return devices;
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

	/**
	 * @see OpenCL4Java#createContext(com.nativelibs4java.opencl.CLDevice[])
	 * @deprecated Use same method in CLPlatform instead
	 */
    public static CLContext createContext(CLDevice... devices) {
		return OpenCL4Java.listPlatforms()[0].createContext(devices);
    }

    //cl_queue queue;
    @Override
    protected void clear() {
        error(CL.clReleaseContext(get()));
    }

	/**
	 * Create an input memory buffer based on existing data.<br/>
	 * If copy is true, the memory buffer created is a copy of the provided buffer. <br/>
	 * If copy is false, the memory buffer uses directly the provided buffer.<br/>
	 * Note that in the latter case, it is still necessary to enqueue map operations (due to cache mechanisms).
	 * @param buffer
	 * @param copy if true, the memory buffer created is a copy of the provided buffer. if false, the memory buffer uses directly the provided buffer.
	 * @return
	 */
    public CLMem createInput(Buffer buffer, boolean copy) {
        return createMem(buffer, -1, CL_MEM_READ_ONLY | (copy ? CL_MEM_COPY_HOST_PTR : CL_MEM_USE_HOST_PTR), true);
    }

    /**
	 * Create an output memory buffer based on existing data.<br/>
	 * If copy is true, the memory buffer created is a copy of the provided buffer. <br/>
	 * If copy is false, the memory buffer uses directly the provided buffer.<br/>
	 * Note that in the latter case, it is still necessary to enqueue map operations (due to cache mechanisms).
	 * @param buffer
	 * @param copy if true, the memory buffer created is a copy of the provided buffer. if false, the memory buffer uses directly the provided buffer.
	 * @return new memory buffer object
	 */
    public CLMem createOutput(Buffer buffer) {
        return createMem(buffer, -1, CL_MEM_WRITE_ONLY | CL_MEM_USE_HOST_PTR, true);
    }

    /**
	 * Create a memory buffer that can be used both as input and as output, based on existing data.<br/>
	 * If copy is true, the memory buffer created is a copy of the provided buffer. <br/>
	 * If copy is false, the memory buffer uses directly the provided buffer.<br/>
	 * Note that in the latter case, it is still necessary to enqueue map operations (due to cache mechanisms).
	 * @param buffer
	 * @param copy if true, the memory buffer created is a copy of the provided buffer. if false, the memory buffer uses directly the provided buffer.
	 * @return new memory buffer object
	 */
    public CLMem createInputOutput(Buffer buffer) {
        return createMem(buffer, -1, CL_MEM_READ_WRITE | CL_MEM_USE_HOST_PTR, true);
    }

    /**
	 * Create an input memory buffer of the specified size.
	 * @param byteCount size in bytes of the memory buffer to create
	 * @return new memory buffer object
	 */
    public CLMem createInput(int byteCount) {
        return createMem(null, byteCount, CL_MEM_READ_ONLY, false);
    }

    /**
	 * Create an output memory buffer of the specified size.
	 * @param byteCount size in bytes of the memory buffer to create
	 * @return new memory buffer object
	 */
    public CLMem createOutput(int byteCount) {
        return createMem(null, byteCount, CL_MEM_WRITE_ONLY, false);
    }

    /**
	 * Create a memory buffer that can be used both as input and as output of the specified size.
	 * @param byteCount size in bytes of the memory buffer to create
	 * @return new memory buffer object
	 */
    public CLMem createInputOutput(int byteCount) {
        return createMem(null, byteCount, CL_MEM_READ_WRITE, false);
    }

    @SuppressWarnings("deprecation")
    private CLMem createMem(final Buffer buffer, int byteCount, final int clMemFlags, final boolean retainBufferReference) {
        if (buffer != null) {
            byteCount = getSizeInBytes(buffer);
        } else if (retainBufferReference) {
            throw new IllegalArgumentException("Cannot retain reference to null pointer !");
        }

        if (byteCount <= 0) {
            throw new IllegalArgumentException("Buffer size must be greater than zero (asked for size " + byteCount + ")");
        }

        IntByReference pErr = new IntByReference();
        //IntBuffer errBuff = IntBuffer.wrap(new int[1]);
        cl_mem mem = CL.clCreateBuffer(
                get(),
                clMemFlags,
                toNL(byteCount),
                buffer == null ? null : Native.getDirectBufferPointer(buffer),
                pErr);
        error(pErr.getValue());

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