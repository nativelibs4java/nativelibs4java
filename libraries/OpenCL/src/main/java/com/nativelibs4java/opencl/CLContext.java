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

import com.nativelibs4java.opencl.CLSampler.AddressingMode;
import com.nativelibs4java.opencl.CLSampler.FilterMode;
import com.nativelibs4java.opencl.library.OpenCLLibrary;
import com.nativelibs4java.opencl.library.cl_image_format;
import com.nativelibs4java.util.EnumValue;
import com.nativelibs4java.util.EnumValues;
import com.nativelibs4java.util.NIOUtils;
import com.ochafik.lang.jnaerator.runtime.NativeSize;
import com.ochafik.lang.jnaerator.runtime.NativeSizeByReference;
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
		protected int getInfo(cl_context entity, int infoTypeEnum, NativeSize size, Pointer out, NativeSizeByReference sizeOut) {
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
	/**
	 * Create an out-of-order OpenCL queue on the first device of this context.<br/>
	 * Equivalent to calling <code>getDevices()[0].createQueue(context)</code>
	 * @return new OpenCL queue
	 */
	public CLQueue createDefaultOutOfOrderQueue() {
		return new CLDevice(platform, deviceIds[0]).createOutOfOrderQueue(this);
	}

	public CLQueue createDefaultProfilingQueue() {
		return new CLDevice(platform, deviceIds[0]).createProfilingQueue(this);
	}

	public CLImageFormat[] getSupportedImageFormats(CLBuffer.Flags flags, CLBuffer.ObjectType imageType) {
		IntByReference pCount = new IntByReference();
		int memFlags = (int) flags.getValue();
		int imTyp = (int) imageType.getValue();
		Memory memCount = new Memory(16);
		pCount.setPointer(memCount);
		CL.clGetSupportedImageFormats(get(), memFlags, imTyp, 0, null, pCount);
		cl_image_format ft = new cl_image_format();
		int sz = ft.size();
		int n = pCount.getValue();
		if (n == 0) {
			n = 30; // There HAS to be at least one format. the spec even says even more, but in fact on Mac OS X / CPU there's only one...
		}
		Memory mem = new Memory(n * sz);
		ft.use(mem);
		CL.clGetSupportedImageFormats(get(), memFlags, imTyp, n, ft, (IntByReference) null);
		List<CLImageFormat> ret = new ArrayList<CLImageFormat>(n);
		for (int i = 0; i < n; i++) {
			ft.use(mem, i * sz);
			ft.read();
			if (ft.image_channel_data_type == 0 && ft.image_channel_order == 0)
				break;

			ret.add(new CLImageFormat(ft));
		}
		return ret.toArray(new CLImageFormat[ret.size()]);
	}

	public CLSampler createSampler(boolean normalized_coords, AddressingMode addressing_mode, FilterMode filter_mode) {
		IntByReference pErr = new IntByReference();
		cl_sampler sampler = CL.clCreateSampler(get(), normalized_coords ? CL_TRUE : CL_FALSE, (int) addressing_mode.getValue(), (int) filter_mode.getValue(), pErr);
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
			int n = (int) (ptrs.getSize() / Native.POINTER_SIZE);
			deviceIds = new cl_device_id[n];
			for (int i = 0; i < n; i++) {
				deviceIds[i] = new cl_device_id(ptrs.getPointer(i * Native.POINTER_SIZE));
			}
		}
		CLDevice[] devices = new CLDevice[deviceIds.length];
		for (int i = devices.length; i-- != 0;) {
			devices[i] = new CLDevice(platform, deviceIds[i]);
		}
		return devices;
	}

	/**
	 * Create a program with all the C source code content provided as argument.
	 * @param srcs list of the content of source code for the program
	 * @return a program that needs to be built
	 */
	public CLProgram createProgram(String... srcs) {

		String[] source = new String[srcs.length];
		NativeSize[] lengths = new NativeSize[srcs.length];
		for (int i = 0; i < srcs.length; i++) {
			source[i] = srcs[i];
			lengths[i] = toNS(srcs[i].length());
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

	public CLByteBuffer createBufferFromGLBuffer(CLMem.Usage usage, int openGLBufferObject) {
		IntByReference pErr = new IntByReference();
		cl_mem mem = CL.clCreateFromGLBuffer(openGLBufferObject, pErr);
		error(pErr.getValue());
		return new CLByteBuffer(this, -1, mem, null);
	}
	public CLImage2D createImage2DFromGLRenderBuffer(CLMem.Usage usage, int openGLRenderBuffer) {
		IntByReference pErr = new IntByReference();
		cl_mem mem = CL.clCreateFromGLRenderbuffer(openGLRenderBuffer, pErr);
		error(pErr.getValue());
		return new CLImage2D(this, mem, null);
	}
	
	/**
	 * Creates an OpenCL 2D image object from an OpenGL 2D texture object, or a single face of an OpenGL cubemap texture object.
	 * @param usage
	 * @param textureTarget Must be one of GL_TEXTURE_2D, GL_TEXTURE_CUBE_MAP_POSITIVE_X, GL_TEXTURE_CUBE_MAP_POSITIVE_Y, GL_TEXTURE_CUBE_MAP_POSITIVE_Z, GL_TEXTURE_CUBE_MAP_NEGATIVE_X, GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, or GL_TEXTURE_RECTANGLE47. texture_target is used only to define the image type of texture. No reference to a bound GL texture object is made or implied by this parameter.
	 * @param mipLevel Mipmap level to be used (Implementations may return CL_INVALID_OPERATION for miplevel values > 0)
	 * @param texture Name of a GL 2D, cubemap or rectangle texture object. The texture object must be a complete texture as per OpenGL rules on texture completeness. The texture format and dimensions defined by OpenGL for the specified miplevel of the texture will be used to create the 2D image object. Only GL texture objects with an internal format that maps to appropriate image channel order and data type specified in tables 5.4 and 5.5 may be used to create a 2D image object.
	 * @return valid OpenCL image object if the image object is created successfully
	 * @throws CLException.InvalidMipLevel if miplevel is less than the value of levelbase (for OpenGL implementations) or zero (for OpenGL ES implementations); or greater than the value of q (for both OpenGL and OpenGL ES). levelbase and q are defined for the texture in section 3.8.10 (Texture Completeness) of the OpenGL 2.1 specification and section 3.7.10 of the OpenGL ES 2.0, or if miplevel is greather than zero and the OpenGL implementation does not support creating from non-zero mipmap levels.
	 * @throws CLException.InvalidGLObject if texture is not a GL texture object whose type matches texture_target, if the specified miplevel of texture is not defined, or if the width or height of the specified miplevel is zero.
	 * @throws CLException.InvalidImageFormatDescriptor if the OpenGL texture internal format does not map to a supported OpenCL image format.
	 */
	public CLImage2D createImage2DFromGLTexture2D(CLMem.Usage usage, GLTextureTarget textureTarget, int texture, int mipLevel) {
		IntByReference pErr = new IntByReference();
		cl_mem mem = CL.clCreateFromGLTexture2D(textureTarget.getValue(), mipLevel, texture, pErr);
		error(pErr.getValue());
		return new CLImage2D(this, mem, null);
	}

	private static final int 
		GL_TEXTURE_2D = 0x0DE1,
		GL_TEXTURE_3D = 0x806F,
		GL_TEXTURE_CUBE_MAP_POSITIVE_X = 0x8515,
		GL_TEXTURE_CUBE_MAP_NEGATIVE_X = 0x8516,
		GL_TEXTURE_CUBE_MAP_POSITIVE_Y = 0x8517,
		GL_TEXTURE_CUBE_MAP_NEGATIVE_Y = 0x8518,
		GL_TEXTURE_CUBE_MAP_POSITIVE_Z = 0x8519,
		GL_TEXTURE_CUBE_MAP_NEGATIVE_Z = 0x851A,
		GL_TEXTURE_RECTANGLE = 0x84F5;

	public CLPlatform getPlatform() {
		return platform;
	}
		

	public enum GLTextureTarget {
		
		@EnumValue(GL_TEXTURE_2D)						Texture2D,
		//@EnumValue(GL_TEXTURE_3D)						Texture3D,
		@EnumValue(GL_TEXTURE_CUBE_MAP_POSITIVE_X)		CubeMapPositiveX,
		@EnumValue(GL_TEXTURE_CUBE_MAP_NEGATIVE_X)		CubeMapNegativeX,
		@EnumValue(GL_TEXTURE_CUBE_MAP_POSITIVE_Y)		CubeMapPositiveY,
		@EnumValue(GL_TEXTURE_CUBE_MAP_NEGATIVE_Y)		CubeMapNegativeY,
		@EnumValue(GL_TEXTURE_CUBE_MAP_POSITIVE_Z)		CubeMapPositiveZ,
		@EnumValue(GL_TEXTURE_CUBE_MAP_NEGATIVE_Z)		CubeMapNegativeZ,
		@EnumValue(GL_TEXTURE_RECTANGLE)				Rectangle;

		public int getValue() { return (int)EnumValues.getValue(this); }
		public static GLTextureTarget getEnum(int v) { return EnumValues.getEnum(v, GLTextureTarget.class); }
	}
	
	/**
	 * Creates an OpenCL 3D image object from an OpenGL 3D texture object
	 * @param usage
	 * @param mipLevel Mipmap level to be used (Implementations may return CL_INVALID_OPERATION for miplevel values > 0)
	 * @param texture Name of a GL 3D texture object. The texture object must be a complete texture as per OpenGL rules on texture completeness. The texture format and dimensions defined by OpenGL for the specified miplevel of the texture will be used to create the 3D image object. Only GL texture objects with an internal format that maps to appropriate image channel order and data type specified in tables 5.4 and 5.5 can be used to create the 3D image object.
	 * @return valid OpenCL image object if the image object is created successfully
	 * @throws CLException.InvalidMipLevel if miplevel is less than the value of levelbase (for OpenGL implementations) or zero (for OpenGL ES implementations); or greater than the value of q (for both OpenGL and OpenGL ES). levelbase and q are defined for the texture in section 3.8.10 (Texture Completeness) of the OpenGL 2.1 specification and section 3.7.10 of the OpenGL ES 2.0, or if miplevel is greather than zero and the OpenGL implementation does not support creating from non-zero mipmap levels.
	 * @throws CLException.InvalidGLObject if texture is not a GL texture object whose type matches texture_target, if the specified miplevel of texture is not defined, or if the width or height of the specified miplevel is zero.
	 * @throws CLException.InvalidImageFormatDescriptor if the OpenGL texture internal format does not map to a supported OpenCL image format.
	 */
	public CLImage3D createImage3DFromGLTexture3D(CLMem.Usage usage, int texture, int mipLevel) {
		IntByReference pErr = new IntByReference();
		cl_mem mem = CL.clCreateFromGLTexture3D(GL_TEXTURE_3D, mipLevel, texture, pErr);
		error(pErr.getValue());
		return new CLImage3D(this, mem, null);
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
				new CLImageFormat(CLImageFormat.ChannelOrder.RGBA, CLImageFormat.ChannelDataType.UnsignedInt8),
				width, height,
				0,
				directData,
				true);
	}

	public CLImage2D createImage2D(CLMem.Usage usage, CLImageFormat format, long width, long height, long rowPitch, Buffer buffer, boolean copy) {
		long memFlags = usage.getIntFlags();
		if (buffer != null) {
			memFlags |= copy ? CL_MEM_COPY_HOST_PTR : CL_MEM_USE_HOST_PTR;
		}

		IntByReference pErr = new IntByReference();
		cl_mem mem = CL.clCreateImage2D(
				get(),
				memFlags,
				format.to_cl_image_format(),
				toNS(width),
				toNS(height),
				toNS(rowPitch),
				buffer == null ? null : Native.getDirectBufferPointer(buffer),
				pErr);
		error(pErr.getValue());
		return new CLImage2D(this, mem, format);
	}

	public CLImage2D createImage2D(CLMem.Usage usage, CLImageFormat format, long width, long height, long rowPitch) {
		return createImage2D(usage, format, width, height, rowPitch, null, false);
	}

	public CLImage2D createImage2D(CLMem.Usage usage, CLImageFormat format, long width, long height) {
		return createImage2D(usage, format, width, height, 0, null, false);
	}

	public CLImage3D createImage3D(CLMem.Usage usage, CLImageFormat format, long width, long height, long depth, long rowPitch, long slicePitch, Buffer buffer, boolean copy) {
		long memFlags = usage.getIntFlags();
		if (buffer != null) {
			memFlags |= copy ? CL_MEM_COPY_HOST_PTR : CL_MEM_USE_HOST_PTR;
		}

		IntByReference pErr = new IntByReference();
		cl_mem mem = CL.clCreateImage3D(
				get(),
				memFlags,
				format.to_cl_image_format(),
				toNS(width),
				toNS(height),
				toNS(depth),
				toNS(rowPitch),
				toNS(slicePitch),
				buffer == null ? null : Native.getDirectBufferPointer(buffer),
				pErr);
		error(pErr.getValue());
		return new CLImage3D(this, mem, format);
	}

	public CLImage3D createImage3D(CLMem.Usage usage, CLImageFormat format, long width, long height, long depth, long rowPitch, long slicePitch) {
		return createImage3D(usage, format, width, height, depth, rowPitch, slicePitch, null, false);
	}

	public CLImage3D createImage3D(CLMem.Usage usage, CLImageFormat format, long width, long height, long depth) {
		return createImage3D(usage, format, width, height, depth, 0, 0, null, false);
	}

	/**
	 * Create an input memory buffer based on existing data.<br/>
	 * If copy is true, the memory buffer created is a copy of the provided buffer. <br/>
	 * If copy is false, the memory buffer uses directly the provided buffer.<br/>
	 * Note that in the latter case, it is still necessary to enqueue map operations (due to cache mechanisms).
	 * @param buffer
	 * @param copy if true, the memory buffer created is a copy of the provided buffer. if false, the memory buffer uses directly the provided buffer.
	 * @return new memory buffer object
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
            if (!buffer.isDirect()) {
                if (!copy)
                    throw new UnsupportedOperationException("Cannot create an OpenCL buffer object out of a non-direct NIO buffer without copy.");
                buffer = NIOUtils.directCopy(buffer);
            }
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
				toNS(byteCount),
				buffer == null ? null : Native.getDirectBufferPointer(buffer),
				pErr);
		error(pErr.getValue());

		return new CLByteBuffer(this, byteCount, mem, buffer);
	}

	public boolean isDoubleSupported() {
		for (CLDevice device : getDevices())
			if (!device.isDoubleSupported())
				return false;
		return true;
	}
	public boolean isHalfSupported() {
		for (CLDevice device : getDevices())
			if (!device.isHalfSupported())
				return false;
		return true;
	}
	public boolean isByteAddressableStoreSupported() {
		for (CLDevice device : getDevices())
			if (!device.isByteAddressableStoreSupported())
				return false;
		return true;
	}


}
