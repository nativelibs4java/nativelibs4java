/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl;
import com.nativelibs4java.opencl.CLSampler.AddressingMode;
import com.nativelibs4java.opencl.CLSampler.FilterMode;
import com.nativelibs4java.opencl.library.OpenCLLibrary;
import com.nativelibs4java.opencl.library.cl_image_format;
import com.nativelibs4java.util.EnumValue;
import com.nativelibs4java.util.EnumValues;
import com.nativelibs4java.util.NIOUtils;
import com.ochafik.util.listenable.Pair;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;
import com.sun.jna.*;
import com.sun.jna.ptr.*;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.PixelGrabber;
import java.nio.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
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

	public CLImageFormat[] getSupportedImageFormats(CLMem.Flags flags, CLMem.ObjectType imageType) {
		IntByReference pCount = new IntByReference();
		int memFlags = (int)flags.getValue();
		int imTyp = (int)imageType.getValue();
		CL.clGetSupportedImageFormats(get(), memFlags, imTyp, 0, null, pCount);
		int n = pCount.getValue();
		if (n == 0)
			n = 1; // There HAS to be at least one format. the spec even says even more, but in fact on Mac OS X / CPU there's only one...
		cl_image_format[] formats = new cl_image_format().toArray(n);
		CL.clGetSupportedImageFormats(get(), memFlags, imTyp, n, formats[0], (IntByReference)null);
		CLImageFormat[] ret = new CLImageFormat[n];
		int i = 0;
		for (cl_image_format format : formats)
			if (format != null)
				ret[i] = new CLImageFormat(format);
		if (ret.length == 1 && ret[0] == null)
			return new CLImageFormat[0];
		return ret;
	}
	

			

	public CLSampler createSampler(boolean normalized_coords, AddressingMode addressing_mode, FilterMode filter_mode) {
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
	 * Create an input image2D from a TYPE_INT_ARGB buffered image
	 * @throws java.lang.IllegalArgumentException if the image is not of type BufferedImage.TYPE_INT_ARGB
	 */
	public CLImage2D createInput2D(BufferedImage image) {
		if (image.getType() != BufferedImage.TYPE_INT_ARGB)
			throw new IllegalArgumentException("The only image type supported is TYPE_INT_ARGB");
		
		DataBufferInt b = (DataBufferInt) image.getRaster().getDataBuffer();
		int[] data = b.getData();
		IntBuffer directData = NIOUtils.directInts(data.length);
		directData.put(IntBuffer.wrap(data));
		directData.rewind();

		return createInput2D(
			new CLImageFormat(CLImageFormat.ChannelOrder.ARGB, CLImageFormat.ChannelDataType.UNormInt8),
			image.getWidth(),
			image.getHeight(),
			0,
			directData,
			true
		);
	}
	public CLImage2D createInput2D(CLImageFormat format, long width, long height, long rowPitch, Buffer buffer, boolean copy) {
		if (buffer == null)
			throw new IllegalArgumentException("Null buffer given as image input !");
		return createImage2D(
			buffer,
			EnumSet.of(CLMem.Flags.ReadOnly, copy ? CLMem.Flags.CopyHostPtr : CLMem.Flags.UseHostPtr),
			format, width, height, rowPitch
		);
	}

	public CLImage2D createInput2D(CLImageFormat format, long width, long height) {
		return createImage2D(
			null,
			EnumSet.of(CLMem.Flags.ReadOnly),
			format, width, height, 0
		);
	}

	public CLImage2D createInputOutput2D(CLImageFormat format, long width, long height, long rowPitch, Buffer buffer, boolean copy) {
		if (buffer == null)
			throw new IllegalArgumentException("Null buffer given as image input !");
		return createImage2D(
			buffer,
			EnumSet.of(CLMem.Flags.ReadWrite, copy ? CLMem.Flags.CopyHostPtr : CLMem.Flags.UseHostPtr),
			format, width, height, rowPitch
		);
	}

	public CLImage2D createInputOutput2D(CLImageFormat format, long width, long height) {
		return createImage2D(
			null,
			EnumSet.of(CLMem.Flags.ReadWrite),
			format, width, height, 0
		);
	}

	public CLImage2D createOutput2D(CLImageFormat format, long width, long height) {
		return createImage2D(
			null,
			EnumSet.of(CLMem.Flags.WriteOnly),
			format, width, height, 0
		);
	}

	private CLImage2D createImage2D(Buffer buffer, EnumSet<CLMem.Flags> memFlags, CLImageFormat format, long width, long height, long rowPitch) {
		IntByReference pErr = new IntByReference();
		cl_mem mem = CL.clCreateImage2D(
			get(),
			CLMem.Flags.getValue(memFlags),
			format.to_cl_image_format(),
			toNL(width),
			toNL(height),
			toNL(rowPitch),
			buffer == null ? null : Native.getDirectBufferPointer(buffer),
			pErr
		);
		error(pErr.getValue());
		return new CLImage2D(this, mem, format);
	}

	public CLImage3D createInput3D(CLImageFormat format, long width, long height, long depth, long rowPitch, long slicePitch, Buffer buffer, boolean copy) {
		if (buffer == null)
			throw new IllegalArgumentException("Null buffer given as image input !");
		return createImage3D(
			buffer,
			EnumSet.of(CLMem.Flags.ReadOnly, copy ? CLMem.Flags.CopyHostPtr : CLMem.Flags.UseHostPtr),
			format, width, height, depth, rowPitch, slicePitch
		);
	}

	public CLImage3D createInput3D(CLImageFormat format, long width, long height, long depth) {
		return createImage3D(
			null,
			EnumSet.of(CLMem.Flags.ReadOnly),
			format, width, height, depth, 0, 0
		);
	}

	public CLImage3D createInputOutput3D(CLImageFormat format, long width, long height, long depth, long rowPitch, long slicePitch, Buffer buffer, boolean copy) {
		if (buffer == null)
			throw new IllegalArgumentException("Null buffer given as image input !");
		return createImage3D(
			buffer,
			EnumSet.of(CLMem.Flags.ReadWrite, copy ? CLMem.Flags.CopyHostPtr : CLMem.Flags.UseHostPtr),
			format, width, height, depth, rowPitch, slicePitch
		);
	}

	public CLImage3D createInputOutput3D(CLImageFormat format, long width, long height, long depth) {
		return createImage3D(
			null,
			EnumSet.of(CLMem.Flags.ReadWrite),
			format, width, height, depth, 0, 0
		);
	}

	public CLImage3D createOutput3D(CLImageFormat format, long width, long height, long depth) {
		return createImage3D(
			null,
			EnumSet.of(CLMem.Flags.WriteOnly),
			format, width, height, depth, 0, 0
		);
	}

	private CLImage3D createImage3D(Buffer buffer, EnumSet<CLMem.Flags> memFlags, CLImageFormat format, long width, long height, long depth, long rowPitch, long slicePitch) {
		IntByReference pErr = new IntByReference();
		cl_mem mem = CL.clCreateImage3D(
			get(),
			CLMem.Flags.getValue(memFlags),
			new cl_image_format((int)format.channelOrder.getValue(), (int)format.channelDataType.getValue()),
			toNL(width),
			toNL(height),
			toNL(depth),
			toNL(rowPitch),
			toNL(slicePitch),
			buffer == null ? null : Native.getDirectBufferPointer(buffer),
			pErr
		);
		error(pErr.getValue());
		return new CLImage3D(this, mem, format);
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