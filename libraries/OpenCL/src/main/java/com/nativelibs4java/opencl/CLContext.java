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
import static com.nativelibs4java.util.ImageUtils.*;


/**
 * OpenCL context.<br/>
 * An OpenCL context is created with one or more devices.<br/>
 * Contexts are used by the OpenCL runtime for managing objects such as command-queues, memory, program and kernel objects and for executing kernels on one or more devices specified in the context.
 * @author Olivier Chafik
 */
public class CLContext extends CLAbstractEntity<cl_context> {

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

	public CLImageFormat[] getSupportedImageFormats(CLBuffer.Flags flags, CLBuffer.ObjectType imageType) {
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
	 * Create an ARGB input 2D image with the content provided
	 * @param allowUnoptimizingDirectRead Some images expose their internal data for direct read, leading to performance increase during the creation of the OpenCL image. However, direct access to the image data disables some Java2D optimizations for this image, leading to degraded performance in subsequent uses with AWT/Swing.
	 */
	public CLImage2D createImage2D(CLMem.Usage usage, Image image, boolean allowUnoptimizingDirectRead) {
		int width = image.getWidth(null), height = image.getHeight(null);
		int[] data = getImageIntPixels(image, 0, 0, width, height, allowUnoptimizingDirectRead);
		IntBuffer directData = NIOUtils.directInts(data.length);
		directData.put(IntBuffer.wrap(data));
		directData.rewind();

		return createImage2D(
			usage,
			new CLImageFormat(CLImageFormat.ChannelOrder.ARGB, CLImageFormat.ChannelDataType.UNormInt8),
			width, height,
			0,
			directData,
			true
		);
	}
	public CLImage2D createImage2D(CLMem.Usage usage, CLImageFormat format, long width, long height, long rowPitch, Buffer buffer, boolean copy) {
		long memFlags = usage.getIntFlags();
		if (buffer != null)
			memFlags |= copy ? CL_MEM_COPY_HOST_PTR : CL_MEM_USE_HOST_PTR;

		IntByReference pErr = new IntByReference();
		cl_mem mem = CL.clCreateImage2D(
			get(),
			memFlags,
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
	public CLImage2D createImage2D(CLMem.Usage usage, CLImageFormat format, long width, long height, long rowPitch, long slicePitch) {
		return createImage2D(usage, format, width, height, rowPitch, null, false);
	}



	public CLImage3D createImage3D(CLMem.Usage usage, CLImageFormat format, long width, long height, long depth, long rowPitch, long slicePitch, Buffer buffer, boolean copy) {
		long memFlags = usage.getIntFlags();
		if (buffer != null)
			memFlags |= copy ? CL_MEM_COPY_HOST_PTR : CL_MEM_USE_HOST_PTR;

		IntByReference pErr = new IntByReference();
		cl_mem mem = CL.clCreateImage3D(
			get(),
			memFlags,
			format.to_cl_image_format(),
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
	public CLImage3D createImage3D(CLMem.Usage usage, CLImageFormat format, long width, long height, long depth, long rowPitch, long slicePitch) {
		return createImage3D(usage, format, width, height, depth, rowPitch, slicePitch, null, false);
	}

	/**
	 * Create an input memory buffer based on existing data.<br/>
	 * If copy is true, the memory buffer created is a copy of the provided buffer. <br/>
	 * If copy is false, the memory buffer uses directly the provided buffer.<br/>
	 * Note that in the latter case, it is still necessary to enqueue map operations (due to cache mechanisms).
	 * @param buffer
	 * @param copy if true, the memory buffer created is a copy of the provided buffer. if false, the memory buffer uses directly the provided buffer.
	 * @return
	 * @deprecated Use createXXXBuffer instead
	 */
    @Deprecated
    public CLByteBuffer createInput(Buffer buffer, boolean copy) {
		return createByteBuffer(CLMem.Usage.Input, buffer, copy);
    }

    /**
	 * Create an output memory buffer based on existing data.<br/>
	 * If copy is true, the memory buffer created is a copy of the provided buffer. <br/>
	 * If copy is false, the memory buffer uses directly the provided buffer.<br/>
	 * Note that in the latter case, it is still necessary to enqueue map operations (due to cache mechanisms).
	 * @param buffer
	 * @param copy if true, the memory buffer created is a copy of the provided buffer. if false, the memory buffer uses directly the provided buffer.
	 * @return new memory buffer object
	 * @deprecated Use createXXXBuffer instead
	 */
	@Deprecated
    public CLByteBuffer createOutput(Buffer buffer) {
		return createByteBuffer(CLMem.Usage.Output, buffer, true);
    }

    /**
	 * Create a memory buffer that can be used both as input and as output, based on existing data.<br/>
	 * If copy is true, the memory buffer created is a copy of the provided buffer. <br/>
	 * If copy is false, the memory buffer uses directly the provided buffer.<br/>
	 * Note that in the latter case, it is still necessary to enqueue map operations (due to cache mechanisms).
	 * @param buffer
	 * @param copy if true, the memory buffer created is a copy of the provided buffer. if false, the memory buffer uses directly the provided buffer.
	 * @return new memory buffer object
	 * @deprecated Use createXXXBuffer instead
	 */
	@Deprecated
    public CLByteBuffer createInputOutput(Buffer buffer) {
		return createByteBuffer(CLMem.Usage.InputOutput, buffer, true);
    }

	
    /**
	 * Create an input memory buffer of the specified size.
	 * @param byteCount size in bytes of the memory buffer to create
	 * @return new memory buffer object
	 * @deprecated Use createXXXBuffer instead
	 */
	@Deprecated
    public CLByteBuffer createInput(long byteCount) {
		return createByteBuffer(CLMem.Usage.Input, byteCount);
    }

    /**
	 * Create an output memory buffer of the specified size.
	 * @param byteCount size in bytes of the memory buffer to create
	 * @return new memory buffer object
	 * @deprecated Use createXXXBuffer instead
	 */
	@Deprecated
    public CLByteBuffer createOutput(long byteCount) {
		return createByteBuffer(CLMem.Usage.Output, byteCount);
    }

    /**
	 * Create a memory buffer that can be used both as input and as output of the specified size.
	 * @param byteCount size in bytes of the memory buffer to create
	 * @return new memory buffer object
	 * @deprecated Use createXXXBuffer instead
	 */
	@Deprecated
    public CLByteBuffer createInputOutput(int byteCount) {
		return createByteBuffer(CLMem.Usage.InputOutput, byteCount);
    }

	public CLIntBuffer createIntBuffer(CLMem.Usage kind, IntBuffer buffer, boolean copy) {
		return createByteBuffer(kind, buffer, copy).asCLIntBuffer();
	}
	public CLIntBuffer createIntBuffer(CLMem.Usage kind, long count) {
		return createByteBuffer(kind, count * 4).asCLIntBuffer();
	}

	public CLLongBuffer createLongBuffer(CLMem.Usage kind, LongBuffer buffer, boolean copy) {
		return createByteBuffer(kind, buffer, copy).asCLLongBuffer();
	}
	public CLLongBuffer createLongBuffer(CLMem.Usage kind, long count) {
		return createByteBuffer(kind, count * 8).asCLLongBuffer();
	}

	public CLShortBuffer createShortBuffer(CLMem.Usage kind, ShortBuffer buffer, boolean copy) {
		return createByteBuffer(kind, buffer, copy).asCLShortBuffer();
	}
	public CLShortBuffer createShortBuffer(CLMem.Usage kind, long count) {
		return createByteBuffer(kind, count * 2).asCLShortBuffer();
	}

	public CLCharBuffer createCharBuffer(CLMem.Usage kind, CharBuffer buffer, boolean copy) {
		return createByteBuffer(kind, buffer, copy).asCLCharBuffer();
	}
	public CLCharBuffer createCharBuffer(CLMem.Usage kind, long count) {
		return createByteBuffer(kind, count * 2).asCLCharBuffer();
	}

	public CLFloatBuffer createFloatBuffer(CLMem.Usage kind, FloatBuffer buffer, boolean copy) {
		return createByteBuffer(kind, buffer, copy).asCLFloatBuffer();
	}
	public CLFloatBuffer createFloatBuffer(CLMem.Usage kind, long count) {
		return createByteBuffer(kind, count * 4).asCLFloatBuffer();
	}

	public CLDoubleBuffer createDoubleBuffer(CLMem.Usage kind, DoubleBuffer buffer, boolean copy) {
		return createByteBuffer(kind, buffer, copy).asCLDoubleBuffer();
	}
	public CLDoubleBuffer createDoubleBuffer(CLMem.Usage kind, long count) {
		return createByteBuffer(kind, count * 8).asCLDoubleBuffer();
	}

	public CLByteBuffer createByteBuffer(CLMem.Usage kind, long count) {
		return createBuffer(null, count, kind.getIntFlags(), false);
	}
	public CLByteBuffer createByteBuffer(CLMem.Usage kind, Buffer buffer, boolean copy) {
		return createBuffer(buffer, -1, kind.getIntFlags() | (copy ? CL_MEM_COPY_HOST_PTR : CL_MEM_USE_HOST_PTR), copy);
	}
	
	@SuppressWarnings("deprecation")
    private CLByteBuffer createBuffer(final Buffer buffer, long byteCount, final int CLBufferFlags, final boolean retainBufferReference) {
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
                CLBufferFlags,
                toNL(byteCount),
                buffer == null ? null : Native.getDirectBufferPointer(buffer),
                pErr);
        error(pErr.getValue());

        return new CLByteBuffer(this, byteCount, mem, buffer);
    }
}