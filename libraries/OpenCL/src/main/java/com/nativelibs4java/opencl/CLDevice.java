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
import com.nativelibs4java.util.EnumValue;
import com.nativelibs4java.util.EnumValues;
import com.nativelibs4java.opencl.library.OpenCLLibrary;
import com.ochafik.lang.jnaerator.runtime.Size;
import com.ochafik.lang.jnaerator.runtime.SizeByReference;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;
import com.sun.jna.*;
import com.sun.jna.ptr.*;
import java.nio.*;
import static com.nativelibs4java.opencl.OpenCL4Java.*;
import static com.nativelibs4java.util.NIOUtils.*;
import java.util.*;
import static com.nativelibs4java.opencl.CLException.*;

/**
 * OpenCL device (CPU, GPU...).<br/>
 * Devices are retrieved from a CLPlatform
 * @see CLPlatform#listDevices(java.util.EnumSet, boolean) 
 */
public class CLDevice extends CLAbstractEntity<cl_device_id> {

	private static CLInfoGetter<cl_device_id> infos = new CLInfoGetter<cl_device_id>() {
		@Override
		protected int getInfo(cl_device_id entity, int infoTypeEnum, Size size, Pointer out, SizeByReference sizeOut) {
			return CL.clGetDeviceInfo(entity, infoTypeEnum, size, out, sizeOut);
		}
	};

	CLPlatform platform;

    CLDevice(CLPlatform platform, cl_device_id device) {
		super(device);
		this.platform = platform;
    }

	public CLPlatform getPlatform() {
		return platform;
	}
	
	@Override
	protected void clear() {}

	/** Bit values for CL_DEVICE_EXECUTION_CAPABILITIES */
	public enum ExecutionCapability {
		@EnumValue(CL_EXEC_KERNEL        ) Kernel,
		@EnumValue(CL_EXEC_NATIVE_KERNEL ) NativeKernel;

		public long getValue() { return EnumValues.getValue(this); }
		public static long getValue(EnumSet<ExecutionCapability> set) { return EnumValues.getValue(set); }
		public static EnumSet<ExecutionCapability> getEnumSet(long v) { return EnumValues.getEnumSet(v, ExecutionCapability.class); }
	}

	/**
	 * Describes the execution capabilities of the device.<br/>
	 * The mandated minimum capability is: Kernel.
	 */
	@InfoName("CL_DEVICE_EXECUTION_CAPABILITIES")
    public EnumSet<ExecutionCapability> getExecutionCapabilities() {
        return ExecutionCapability.getEnumSet(infos.getIntOrLong(get(), CL_DEVICE_EXECUTION_CAPABILITIES));
    }

	/** Bit values for CL_DEVICE_TYPE */
	public enum Type {
		@EnumValue(CL_DEVICE_TYPE_CPU         ) CPU,
		@EnumValue(CL_DEVICE_TYPE_GPU         ) GPU,
		@EnumValue(CL_DEVICE_TYPE_ACCELERATOR ) Accelerator,
		@EnumValue(CL_DEVICE_TYPE_DEFAULT     ) Default;

		public long getValue() { return EnumValues.getValue(this); }
		public static long getValue(EnumSet<Type> set) { return EnumValues.getValue(set); }
		public static EnumSet<Type> getEnumSet(long v) { return EnumValues.getEnumSet(v, Type.class); }
	}

	/**
	 * The OpenCL device type.
	 */
	@InfoName("CL_DEVICE_TYPE")
	public EnumSet<Type> getType() {
        return Type.getEnumSet(infos.getIntOrLong(get(), CL_DEVICE_TYPE));
    }

	/**
	 * A unique device vendor identifier. <br/>
	 * An example of a unique device identifier could be the PCIe ID.
	 */
	@InfoName("CL_DEVICE_VENDOR_ID")
	public int getVendorId() {
		return infos.getInt(get(), CL_DEVICE_VENDOR_ID);
	}

	/**
	 * The number of parallel compute cores on the OpenCL device. <br/>
	 * The minimum value is 1.
	 */
	@InfoName("CL_DEVICE_MAX_COMPUTE_UNITS")
	public int getMaxComputeUnits() {
		return infos.getInt(get(), CL_DEVICE_MAX_COMPUTE_UNITS);
	}

	/**
	 * Maximum dimensions that specify the global and local work-item IDs used by the data parallel execution model. <br/>
	 * (Refer to clEnqueueNDRangeKernel).
	 * <br/>The minimum value is 3.
	 */
	@InfoName("CL_DEVICE_MAX_WORK_ITEM_DIMENSIONS")
	public int getMaxWorkItemDimensions() {
		return infos.getInt(get(), CL_DEVICE_MAX_WORK_ITEM_DIMENSIONS);
	}

	/**
	 * Maximum number of work-items that can be specified in each dimension of the work-group to clEnqueueNDRangeKernel.
	 */
	@InfoName("CL_DEVICE_MAX_WORK_ITEM_SIZES")
	public long[] getMaxWorkItemSizes() {
		return infos.getSizes(get(), CL_DEVICE_MAX_WORK_ITEM_SIZES, getMaxWorkItemDimensions());
	}

	/**
	 * Maximum number of work-items in a work-group executing a kernel using the data parallel execution model.
	 * (Refer to clEnqueueNDRangeKernel). <br/>
	 * The minimum value is 1.
	 */
	@InfoName("CL_DEVICE_MAX_WORK_GROUP_SIZE")
	public long getMaxWorkGroupSize() {
		return infos.getIntOrLong(get(), CL_DEVICE_MAX_WORK_GROUP_SIZE);
	}

	/**
	 * Maximum configured clock frequency of the device in MHz.
	 */
	@InfoName("CL_DEVICE_MAX_CLOCK_FREQUENCY")
	public int getMaxClockFrequency() {
		return infos.getInt(get(), CL_DEVICE_MAX_CLOCK_FREQUENCY);
	}

	/**
	 * The default compute device address space size specified as an unsigned integer value in bits. Currently supported values are 32 or 64 bits.
	 */
	@InfoName("CL_DEVICE_ADDRESS_BITS")
	public int getAddressBits() {
		return infos.getInt(get(), CL_DEVICE_ADDRESS_BITS);
	}

	/**
	 * Max size of memory object allocation in bytes. The minimum value is max (1/4th of CL_DEVICE_GLOBAL_MEM_SIZE , 128*1024*1024)
	 */
	@InfoName("CL_DEVICE_MAX_MEM_ALLOC_SIZE")
	public long getMaxMemAllocSize() {
		return infos.getIntOrLong(get(), CL_DEVICE_MAX_MEM_ALLOC_SIZE);
	}

	/**
	 * Is CL_TRUE if images are supported by the OpenCL device and CL_FALSE otherwise.
	 */
	@InfoName("CL_DEVICE_IMAGE_SUPPORT")
	public boolean hasImageSupport() {
		return infos.getBool(get(), CL_DEVICE_IMAGE_SUPPORT);
	}

	/**
	 * Max number of simultaneous image objects that can be read by a kernel. <br/>
	 * The minimum value is 128 if CL_DEVICE_IMAGE_SUPPORT is CL_TRUE (@see hasImageSupport()).
	 */
	@InfoName("CL_DEVICE_MAX_READ_IMAGE_ARGS")
	public int getMaxReadImageArgs() {
		return infos.getInt(get(), CL_DEVICE_MAX_READ_IMAGE_ARGS);
	}

	/**
	 * Max number of simultaneous image objects that can be written to by a kernel. <br/>
	 * The minimum value is 8 if CL_DEVICE_IMAGE_SUPPORT is CL_TRUE (@see hasImageSupport()).
	 */
	@InfoName("CL_DEVICE_MAX_WRITE_IMAGE_ARGS")
	public int getMaxWriteImageArgs() {
		return infos.getInt(get(), CL_DEVICE_MAX_WRITE_IMAGE_ARGS);
	}


    @Override
    public String toString() {
        return getName();// + "{singleFPConfig: " + getSingleFPConfig() + "}";
    }

	/**
	 * Create an OpenCL execution queue on this device for the specified context.
	 * @param context context of the queue to create
	 * @return new OpenCL queue object
	 */
    @SuppressWarnings("deprecation")
    public CLQueue createQueue(EnumSet<QueueProperties> queueProperties, CLContext context) {
        IntByReference pErr = new IntByReference();
        cl_command_queue queue = CL.clCreateCommandQueue(context.get(), get(), QueueProperties.getValue(queueProperties), pErr);
        error(pErr.getValue());

        return new CLQueue(context, queue, this);
    }
	public CLQueue createQueue(CLContext context) {
		return createQueue(EnumSet.noneOf(QueueProperties.class), context);
	}
	public CLQueue createOutOfOrderQueue(CLContext context) {
		return createQueue(EnumSet.of(QueueProperties.OutOfOrderExecModeEnable), context);
	}

	/**
	 * Max width of 2D image in pixels. <br/>
	 * The minimum value is 8192 if CL_DEVICE_IMAGE_SUPPORT is CL_TRUE.
	 */
	@InfoName("CL_DEVICE_IMAGE2D_MAX_WIDTH")
	public long getImage2DMaxWidth() {
		return infos.getIntOrLong(get(), CL_DEVICE_IMAGE2D_MAX_WIDTH);
	}

	/**
	 * Max height of 2D image in pixels. <br/>
	 * The minimum value is 8192 if CL_DEVICE_IMAGE_SUPPORT is CL_TRUE.
	 */
	@InfoName("CL_DEVICE_IMAGE2D_MAX_HEIGHT")
	public long getImage2DMaxHeight() {
		return infos.getIntOrLong(get(), CL_DEVICE_IMAGE2D_MAX_HEIGHT);
	}

	/**
	 * Max width of 3D image in pixels. <br/>
	 * The minimum value is 2048 if CL_DEVICE_IMAGE_SUPPORT is CL_TRUE.
	 */
	@InfoName("CL_DEVICE_IMAGE3D_MAX_WIDTH")
	public long getImage3DMaxWidth() {
		return infos.getIntOrLong(get(), CL_DEVICE_IMAGE3D_MAX_WIDTH);
	}

	/**
	 * Max height of 3D image in pixels. <br/>
	 * The minimum value is 2048 if CL_DEVICE_IMAGE_SUPPORT is CL_TRUE.
	 */
	@InfoName("CL_DEVICE_IMAGE3D_MAX_HEIGHT")
	public long getImage3DMaxHeight() {
		return infos.getIntOrLong(get(), CL_DEVICE_IMAGE3D_MAX_HEIGHT);
	}

	/**
	 * Max depth of 3D image in pixels. <br/>
	 * The minimum value is 2048 if CL_DEVICE_IMAGE_SUPPORT is CL_TRUE.
	 */
	@InfoName("CL_DEVICE_IMAGE3D_MAX_DEPTH")
	public long getImage3DMaxDepth() {
		return infos.getIntOrLong(get(), CL_DEVICE_IMAGE3D_MAX_DEPTH);
	}

	/**
	 * Maximum number of samplers that can be used in a kernel. <br/>
	 * Refer to section 6.11.8 for a detailed description on samplers. <br/>
	 * The minimum value is 16 if CL_DEVICE_IMAGE_SUPPORT is CL_TRUE.
	 */
	@InfoName("CL_DEVICE_MAX_SAMPLERS")
	public int getMaxSamplers() {
		return infos.getInt(get(), CL_DEVICE_MAX_SAMPLERS);
	}

	/**
	 * Max size in bytes of the arguments that can be passed to a kernel. <br/>
	 * The minimum value is 256.
	 */
	@InfoName("CL_DEVICE_MAX_PARAMETER_SIZE")
	public long getMaxParameterSize() {
		return infos.getIntOrLong(get(), CL_DEVICE_MAX_PARAMETER_SIZE);
	}

	/**
	 * Describes the alignment in bits of the base address of any allocated memory object.
	 */
	@InfoName("CL_DEVICE_MEM_BASE_ADDR_ALIGN")
	public int getMemBaseAddrAlign() {
		return infos.getInt(get(), CL_DEVICE_MEM_BASE_ADDR_ALIGN);
	}

	/**
	 * The smallest alignment in bytes which can be used for any data type.
	 */
	@InfoName("CL_DEVICE_MIN_DATA_TYPE_ALIGN_SIZE")
	public int getMinDataTypeAlign() {
		return infos.getInt(get(), CL_DEVICE_MIN_DATA_TYPE_ALIGN_SIZE);
	}

	/** Bit values for CL_DEVICE_SINGLE_FP_CONFIG */
	public enum SingleFPConfig {
		/** denorms are supported                                  */ @EnumValue(CL_FP_DENORM			) Denorm			,
		/** INF and quiet NaNs are supported.                      */ @EnumValue(CL_FP_INF_NAN			) InfNaN			,
		/** round to nearest even rounding mode supported          */ @EnumValue(CL_FP_ROUND_TO_NEAREST	) RoundToNearest	,
		/** round to zero rounding mode supported                  */ @EnumValue(CL_FP_ROUND_TO_ZERO	) RoundToZero		,
		/** round to +ve and -ve infinity rounding modes supported */ @EnumValue(CL_FP_ROUND_TO_INF		) RoundToInf		,
		/** IEEE754-2008 fused multiply-add is supported.          */ @EnumValue(CL_FP_FMA				) FMA				;

		public long getValue() { return EnumValues.getValue(this); }
		public static long getValue(EnumSet<SingleFPConfig> set) { return EnumValues.getValue(set); }
		public static EnumSet<SingleFPConfig> getEnumSet(long v) { return EnumValues.getEnumSet(v, SingleFPConfig.class); }
	}

	/**
	 * Describes single precision floating- point capability of the device.<br/>
	 * The mandated minimum floating-point capability is: RoundToNearest and InfNaN.
	 */
	@InfoName("CL_DEVICE_SINGLE_FP_CONFIG")
	public EnumSet<SingleFPConfig> getSingleFPConfig() {
		return SingleFPConfig.getEnumSet(infos.getIntOrLong(get(), CL_DEVICE_SINGLE_FP_CONFIG));
	}

	/** Values for CL_DEVICE_GLOBAL_MEM_CACHE_TYPE */
	public enum GlobalMemCacheType {
		@EnumValue(CL_NONE             ) None          ,
		@EnumValue(CL_READ_ONLY_CACHE  ) ReadOnlyCache ,
		@EnumValue(CL_READ_WRITE_CACHE ) ReadWriteCache;
		
		public long getValue() { return EnumValues.getValue(this); }
		public static GlobalMemCacheType getEnum(long v) { return EnumValues.getEnum(v, GlobalMemCacheType.class); }
	}
	/**
	 * Type of global memory cache supported.
	 */
	@InfoName("CL_DEVICE_GLOBAL_MEM_CACHE_TYPE")
	public GlobalMemCacheType getGlobalMemCacheType() {
		return GlobalMemCacheType.getEnum(infos.getInt(get(), CL_DEVICE_GLOBAL_MEM_CACHE_TYPE));
	}


	/**
	 * Size of global memory cache line in bytes.
	 */
	@InfoName("CL_DEVICE_GLOBAL_MEM_CACHELINE_SIZE")
	public int getGlobalMemCachelineSize() {
		return infos.getInt(get(), CL_DEVICE_GLOBAL_MEM_CACHELINE_SIZE);
	}

	/**
	 * Size of global memory cache in bytes.
	 */
	@InfoName("CL_DEVICE_GLOBAL_MEM_CACHE_SIZE")
	public long getGlobalMemCacheSize() {
		return infos.getIntOrLong(get(), CL_DEVICE_GLOBAL_MEM_CACHE_SIZE);
	}

	/**
	 * Size of global device memory in bytes.
	 */
	@InfoName("CL_DEVICE_GLOBAL_MEM_SIZE")
	public long getGlobalMemSize() {
		return infos.getIntOrLong(get(), CL_DEVICE_GLOBAL_MEM_SIZE);
	}

	/**
	 * Max size in bytes of a constant buffer allocation. <br/>
	 * The minimum value is 64 KB.
	 */
	@InfoName("CL_DEVICE_MAX_CONSTANT_BUFFER_SIZE")
	public long getMaxConstantBufferSize() {
		return infos.getIntOrLong(get(), CL_DEVICE_MAX_CONSTANT_BUFFER_SIZE);
	}

	/**
	 * Max number of arguments declared with the __constant qualifier in a kernel. <br/>
	 * The minimum value is 8.
	 */
	@InfoName("CL_DEVICE_MAX_CONSTANT_ARGS")
	public int getMaxConstantArgs() {
		return infos.getInt(get(), CL_DEVICE_MAX_CONSTANT_ARGS);
	}

	/** Values for CL_DEVICE_LOCAL_MEM_TYPE */
	public enum LocalMemType {
		/** implying dedicated local memory storage such as SRAM */
		@EnumValue(CL_LOCAL  ) Local ,
		@EnumValue(CL_GLOBAL ) Global;

		public long getValue() { return EnumValues.getValue(this); }
		public static LocalMemType getEnum(long v) { return EnumValues.getEnum(v, LocalMemType.class); }
	}

	/**
	 * Type of local memory supported. <br/>
	 */
	@InfoName("CL_DEVICE_LOCAL_MEM_TYPE")
	public LocalMemType getLocalMemType() {
		return LocalMemType.getEnum(infos.getInt(get(), CL_DEVICE_LOCAL_MEM_TYPE));
	}
	
	/**
	 * Size of local memory arena in bytes. <br/>
	 * The minimum value is 16 KB.
	 */
	@InfoName("CL_DEVICE_LOCAL_MEM_SIZE")
	public long getLocalMemSize() {
		return infos.getIntOrLong(get(), CL_DEVICE_LOCAL_MEM_SIZE);
	}

	/**
	 * Is CL_TRUE if the device implements error correction for the memories, caches, registers etc. in the device. <br/>
	 * Is CL_FALSE if the device does not implement error correction. <br/>
	 * This can be a requirement for certain clients of OpenCL.
	 */
	@InfoName("CL_DEVICE_ERROR_CORRECTION_SUPPORT")
	public boolean hasErrorCorrectionSupport() {
		return infos.getBool(get(), CL_DEVICE_ERROR_CORRECTION_SUPPORT);
	}

	/**
	 * Describes the resolution of device timer. <br/>
	 * This is measured in nanoseconds.	<br/>
	 * Refer to section 5.9 for details.
	 */
	@InfoName("CL_DEVICE_PROFILING_TIMER_RESOLUTION")
	public long getProfilingTimerResolution() {
		return infos.getIntOrLong(get(), CL_DEVICE_PROFILING_TIMER_RESOLUTION);
	}

	/**
	 * Is CL_TRUE if the OpenCL device is a little endian device and CL_FALSE otherwise.
	 */
	@InfoName("CL_DEVICE_ENDIAN_LITTLE")
	public boolean isEndianLittle() {
		return infos.getBool(get(), CL_DEVICE_ENDIAN_LITTLE);
	}

	/**
	 * Is CL_TRUE if the device is available and CL_FALSE if the device is not available.
	 */
	@InfoName("CL_DEVICE_AVAILABLE")
	public boolean isAvailable() {
		return infos.getBool(get(), CL_DEVICE_AVAILABLE);
	}

	/**
	 * Is CL_FALSE if the implementation does not have a compiler available to compile the program source. <br/>
	 * Is CL_TRUE if the compiler is available.<br/>
	 * This can be CL_FALSE for the embededed platform profile only.
	 */
	@InfoName("CL_DEVICE_COMPILER_AVAILABLE")
	public boolean isCompilerAvailable() {
		return infos.getBool(get(), CL_DEVICE_COMPILER_AVAILABLE);
	}

	/**
	Device name string.
	 */
	@InfoName("CL_DEVICE_NAME")
	public String getName() {
		return infos.getString(get(), CL_DEVICE_NAME);
	}

	/**
	Vendor name string.
	 */
	@InfoName("CL_DEVICE_VENDOR")
	public String getVendor() {
		return infos.getString(get(), CL_DEVICE_VENDOR);
	}

	/**
	OpenCL software driver version string in the form major_number.minor_number.
	 */
	@InfoName("CL_DRIVER_VERSION")
	public String getDriverVersion() {
		return infos.getString(get(), CL_DRIVER_VERSION);
	}

	/**
	 * OpenCL profile string. <br/>
	 * Returns the profile name supported by the device. <br/>
	 * The profile name returned can be one of the following strings:
	 * <ul>
	 * <li>FULL_PROFILE if the device supports the OpenCL specification (functionality defined as part of the core specification and does not require any extensions to be supported).</li>
	 * <li>EMBEDDED_PROFILE if the device supports the OpenCL embedded profile.</li>
	 * </ul>
	 */
	@InfoName("CL_DEVICE_PROFILE")
	public String getProfile() {
		return infos.getString(get(), CL_DEVICE_PROFILE);
	}


	/**
	 * Preferred native vector width size for built-in scalar types that can be put into vectors. <br/>
	 * The vector width is defined as the number of scalar elements that can be stored in the vector. <br/>
	 * If the cl_khr_fp64 extension is not supported, CL_DEVICE_PREFERRED_VECTOR_WID TH_DOUBLE must return 0.
	 */
	@InfoName("CL_DEVICE_PREFERRED_VECTOR_WIDTH_CHAR")
	public int getPreferredVectorWidthChar() {
		return infos.getInt(get(), CL_DEVICE_PREFERRED_VECTOR_WIDTH_CHAR);
	}

	/**
	 * Preferred native vector width size for built-in scalar types that can be put into vectors. <br/>
	 * The vector width is defined as the number of scalar elements that can be stored in the vector. <br/>
	 * If the cl_khr_fp64 extension is not supported, CL_DEVICE_PREFERRED_VECTOR_WID TH_DOUBLE must return 0.
	 */
	@InfoName("CL_DEVICE_PREFERRED_VECTOR_WIDTH_SHORT")
	public int getPreferredVectorWidthShort() {
		return infos.getInt(get(), CL_DEVICE_PREFERRED_VECTOR_WIDTH_SHORT);
	}

	/**
	 * Preferred native vector width size for built-in scalar types that can be put into vectors. <br/>
	 * The vector width is defined as the number of scalar elements that can be stored in the vector. <br/>
	 * If the cl_khr_fp64 extension is not supported, CL_DEVICE_PREFERRED_VECTOR_WID TH_DOUBLE must return 0.
	 */
	@InfoName("CL_DEVICE_PREFERRED_VECTOR_WIDTH_INT")
	public int getPreferredVectorWidthInt() {
		return infos.getInt(get(), CL_DEVICE_PREFERRED_VECTOR_WIDTH_INT);
	}

	/**
	 * Preferred native vector width size for built-in scalar types that can be put into vectors. <br/>
	 * The vector width is defined as the number of scalar elements that can be stored in the vector. <br/>
	 * If the cl_khr_fp64 extension is not supported, CL_DEVICE_PREFERRED_VECTOR_WID TH_DOUBLE must return 0.
	 */
	@InfoName("CL_DEVICE_PREFERRED_VECTOR_WIDTH_LONG")
	public int getPreferredVectorWidthLong() {
		return infos.getInt(get(), CL_DEVICE_PREFERRED_VECTOR_WIDTH_LONG);
	}

	/**
	 * Preferred native vector width size for built-in scalar types that can be put into vectors. <br/>
	 * The vector width is defined as the number of scalar elements that can be stored in the vector. <br/>
	 * If the cl_khr_fp64 extension is not supported, CL_DEVICE_PREFERRED_VECTOR_WID TH_DOUBLE must return 0.
	 */
	@InfoName("CL_DEVICE_PREFERRED_VECTOR_WIDTH_FLOAT")
	public int getPreferredVectorWidthFloat() {
		return infos.getInt(get(), CL_DEVICE_PREFERRED_VECTOR_WIDTH_FLOAT);
	}

	/**
	 * Preferred native vector width size for built-in scalar types that can be put into vectors. <br/>
	 * The vector width is defined as the number of scalar elements that can be stored in the vector. <br/>
	 * If the cl_khr_fp64 extension is not supported, CL_DEVICE_PREFERRED_VECTOR_WID TH_DOUBLE must return 0.
	 */
	@InfoName("CL_DEVICE_PREFERRED_VECTOR_WIDTH_DOUBLE")
	public int getPreferredVectorWidthDouble() {
		return infos.getInt(get(), CL_DEVICE_PREFERRED_VECTOR_WIDTH_DOUBLE);
	}
	
	/**
	 * OpenCL version string. <br/>
	 * Returns the OpenCL version supported by the device.<br/>
	 * This version string has the following format:
	 * <code>
	 * OpenCL&lt;space&gt;&lt;major_version.min or_version&gt;&lt;space&gt;&lt;vendor-specific information>
	 * </code>
	 * The major_version.minor_version value returned will be 1.0.
	 */
	@InfoName("CL_DEVICE_VERSION")
	public String getVersion() {
		return infos.getString(get(), CL_DEVICE_VERSION);
	}

	/**
	Returns a space separated list of extension names (the extension names themselves do not contain any spaces). The list of extension names returned currently can include one or more of
	 */
	@InfoName("CL_DEVICE_EXTENSIONS")
	public String[] getExtensions() {
		if (extensions == null)
			extensions = infos.getString(get(), CL_DEVICE_EXTENSIONS).split("\\s+");
		return extensions;
	}
	private String[] extensions;


	boolean hasExtension(String name) {
		name = name.trim();
		for (String x : getExtensions())
			if (name.equals(x.trim()))
				return true;
		return false;
	}

	@InfoName("cl_khr_fp64")
	public boolean isDoubleSupported() {
		return hasExtension("cl_khr_fp16");
	}
	@InfoName("cl_khr_fp16")
	public boolean isHalfSupported() {
		return hasExtension("cl_khr_fp64");
	}
	@InfoName("cl_khr_byte_addressable_store")
	public boolean isByteAddressableStoreSupported() {
		return hasExtension("cl_khr_byte_addressable_store");
	}

	/** Bit values for CL_DEVICE_QUEUE_PROPERTIES */
	public enum QueueProperties {
		@EnumValue(CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE) OutOfOrderExecModeEnable,
		@EnumValue(CL_QUEUE_PROFILING_ENABLE             ) ProfilingEnable		   ;
		
		public long getValue() { return EnumValues.getValue(this); }
		public static long getValue(EnumSet<QueueProperties> set) { return EnumValues.getValue(set); }
		public static EnumSet<QueueProperties> getEnumSet(long v) { return EnumValues.getEnumSet(v, QueueProperties.class); }
	}

	/**
	 * Describes the command-queue properties supported by the device.<br/>
	 * These properties are described in table 5.1.<br/>
	 * The mandated minimum capability is: ProfilingEnable.
	 */
	@InfoName("CL_DEVICE_QUEUE_PROPERTIES")
	public EnumSet<QueueProperties> getQueueProperties() {
		return QueueProperties.getEnumSet(infos.getIntOrLong(get(), CL_DEVICE_QUEUE_PROPERTIES));
	}
}