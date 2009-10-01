/*

*/

package com.nativelibs4java.opencl;
import com.nativelibs4java.util.EnumValue;
import com.nativelibs4java.util.EnumValues;
import com.nativelibs4java.opencl.library.OpenCLLibrary;
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
 * Devices are retrieved from a CLPlatform (@see CLPlatform.listAllDevices())
 */
public class CLDevice extends CLEntity<cl_device_id> {

	static CLInfoGetter<cl_device_id> infos = new CLInfoGetter<cl_device_id>() {
		@Override
		protected int getInfo(cl_device_id entity, int infoTypeEnum, NativeLong size, Pointer out, NativeLongByReference sizeOut) {
			return CL.clGetDeviceInfo(entity, infoTypeEnum, size, out, sizeOut);
		}
	};

    CLDevice(cl_device_id device) {
		super(device);
    }

	@Override
	protected void clear() {}

	/** Bit values for CL_DEVICE_EXECUTION_CAPABILITIES */
	public enum CLExecutionCapability {
		@EnumValue(CL_EXEC_KERNEL        ) Kernel,
		@EnumValue(CL_EXEC_NATIVE_KERNEL ) NativeKernel;

		public long getValue() { return EnumValues.getValue(this); }
		public static long getValue(EnumSet<CLExecutionCapability> set) { return EnumValues.getValue(set); }
		public static EnumSet<CLExecutionCapability> getEnumSet(long v) { return EnumValues.getEnumSet(v, CLExecutionCapability.class); }
	}

	/**
	 * CL_DEVICE_EXECUTION_CAPABILITIES<br/>
	 * Describes the execution capabilities of the device.<br/>
	 * The mandated minimum capability is: Kernel.
	 */
    public EnumSet<CLExecutionCapability> getExecutionCapabilities() {
        return CLExecutionCapability.getEnumSet(infos.getNativeLong(get(), CL_DEVICE_EXECUTION_CAPABILITIES));
    }

	/** Bit values for CL_DEVICE_TYPE */
	public enum CLDeviceType {    
		@EnumValue(CL_DEVICE_TYPE_CPU         ) CPU,
		@EnumValue(CL_DEVICE_TYPE_GPU         ) GPU,
		@EnumValue(CL_DEVICE_TYPE_ACCELERATOR ) Accelerator,
		@EnumValue(CL_DEVICE_TYPE_DEFAULT     ) Default;

		public long getValue() { return EnumValues.getValue(this); }
		public static long getValue(EnumSet<CLDeviceType> set) { return EnumValues.getValue(set); }
		public static EnumSet<CLDeviceType> getEnumSet(long v) { return EnumValues.getEnumSet(v, CLDeviceType.class); }
	}

	/**
	 * CL_DEVICE_TYPE<br/>
	 * The OpenCL device type.
	 */
	public EnumSet<CLDeviceType> getType() {
        return CLDeviceType.getEnumSet(infos.getNativeLong(get(), CL_DEVICE_TYPE));
    }

	/**
	 * CL_DEVICE_VENDOR_ID<br/>
	 * A unique device vendor identifier. <br/>
	 * An example of a unique device identifier could be the PCIe ID.
	 */
	public int getVendorId() {
		return infos.getInt(get(), CL_DEVICE_VENDOR_ID);
	}

	/**
	 * CL_DEVICE_MAX_COMPUTE_UNITS<br/>
	 * The number of parallel compute cores on the OpenCL device. <br/>
	 * The minimum value is 1.
	 */
	public int getMaxComputeUnits() {
		return infos.getInt(get(), CL_DEVICE_MAX_COMPUTE_UNITS);
	}

	/**
	 * CL_DEVICE_MAX_WORK_ITEM_DIMENSIONS<br/>
	 * Maximum dimensions that specify the global and local work-item IDs used by the data parallel execution model. <br/>
	 * (Refer to clEnqueueNDRangeKernel).
	 * <br/>The minimum value is 3.
	 */
	public int getMaxWorkItemDimensions() {
		return infos.getInt(get(), CL_DEVICE_MAX_WORK_ITEM_DIMENSIONS);
	}

	/**
	 * CL_DEVICE_MAX_WORK_ITEM_SIZES<br/>
	 * Maximum number of work-items that can be specified in each dimension of the work-group to clEnqueueNDRangeKernel.
	 */
	public long[] getMaxWorkItemSizes() {
		return infos.getNativeLongs(get(), CL_DEVICE_MAX_WORK_ITEM_SIZES, getMaxWorkItemDimensions());
	}

	/**
	 * CL_DEVICE_MAX_WORK_GROUP_SIZE<br/>
	 * Maximum number of work-items in a work-group executing a kernel using the data parallel execution model.
	 * (Refer to clEnqueueNDRangeKernel). <br/>
	 * The minimum value is 1.
	 */
	public long getMaxWorkGroupSize() {
		return infos.getNativeLong(get(), CL_DEVICE_MAX_WORK_GROUP_SIZE);
	}

	/**
	 * CL_DEVICE_MAX_CLOCK_FREQUENCY<br/>
	 * Maximum configured clock frequency of the device in MHz.
	 */
	public int getMaxClockFrequency() {
		return infos.getInt(get(), CL_DEVICE_MAX_CLOCK_FREQUENCY);
	}

	/**
	 * CL_DEVICE_ADDRESS_BITS
	 * The default compute device address space size specified as an unsigned integer value in bits. Currently supported values are 32 or 64 bits.
	 */
	public int getAddressBits() {
		return infos.getInt(get(), CL_DEVICE_ADDRESS_BITS);
	}

	/**
	 * CL_DEVICE_MAX_MEM_ALLOC_SIZE
	 * Max size of memory object allocation in bytes. The minimum value is max (1/4th of CL_DEVICE_GLOBAL_MEM_SIZE , 128*1024*1024)
	 */
	public long getMaxMemAllocSize() {
		return infos.getNativeLong(get(), CL_DEVICE_MAX_MEM_ALLOC_SIZE);
	}

	/**
	 * CL_DEVICE_IMAGE_SUPPORT<br/>
	 * Is CL_TRUE if images are supported by the OpenCL device and CL_FALSE otherwise.
	 */
	public boolean hasImageSupport() {
		return infos.getBool(get(), CL_DEVICE_IMAGE_SUPPORT);
	}

	/**
	 * CL_DEVICE_MAX_READ_IMAGE_ARGS<br/>
	 * Max number of simultaneous image objects that can be read by a kernel. <br/>
	 * The minimum value is 128 if CL_DEVICE_IMAGE_SUPPORT is CL_TRUE (@see hasImageSupport()).
	 */
	public int getMaxReadImageArgs() {
		return infos.getInt(get(), CL_DEVICE_MAX_READ_IMAGE_ARGS);
	}

	/**
	 * CL_DEVICE_MAX_WRITE_IMAGE_ARGS<br/>
	 * Max number of simultaneous image objects that can be written to by a kernel. <br/>
	 * The minimum value is 8 if CL_DEVICE_IMAGE_SUPPORT is CL_TRUE (@see hasImageSupport()).
	 */
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
    public CLQueue createQueue(CLContext context) {
        IntByReference pErr = new IntByReference();
        cl_command_queue queue = CL.clCreateCommandQueue(context.get(), get(), 0, pErr);
        error(pErr.getValue());

        return new CLQueue(context, queue);
    }

	/**
	 * CL_DEVICE_IMAGE2D_MAX_WIDTH<br/>
	 * Max width of 2D image in pixels. <br/>
	 * The minimum value is 8192 if CL_DEVICE_IMAGE_SUPPORT is CL_TRUE.
	 */
	public long getImage2DMaxWidth() {
		return infos.getNativeLong(get(), CL_DEVICE_IMAGE2D_MAX_WIDTH);
	}

	/**
	 * CL_DEVICE_IMAGE2D_MAX_HEIGHT<br/>
	 * Max height of 2D image in pixels. <br/>
	 * The minimum value is 8192 if CL_DEVICE_IMAGE_SUPPORT is CL_TRUE.
	 */
	public long getImage2DMaxHeight() {
		return infos.getNativeLong(get(), CL_DEVICE_IMAGE2D_MAX_HEIGHT);
	}

	/**
	 * CL_DEVICE_IMAGE3D_MAX_WIDTH<br/>
	 * Max width of 3D image in pixels. <br/>
	 * The minimum value is 2048 if CL_DEVICE_IMAGE_SUPPORT is CL_TRUE.
	 */
	public long getImage3DMaxWidth() {
		return infos.getNativeLong(get(), CL_DEVICE_IMAGE3D_MAX_WIDTH);
	}

	/**
	 * CL_DEVICE_IMAGE3D_MAX_HEIGHT<br/>
	 * Max height of 3D image in pixels. <br/>
	 * The minimum value is 2048 if CL_DEVICE_IMAGE_SUPPORT is CL_TRUE.
	 */
	public long getImage3DMaxHeight() {
		return infos.getNativeLong(get(), CL_DEVICE_IMAGE3D_MAX_HEIGHT);
	}

	/**
	 * CL_DEVICE_IMAGE3D_MAX_DEPTH<br/>
	 * Max depth of 3D image in pixels. <br/>
	 * The minimum value is 2048 if CL_DEVICE_IMAGE_SUPPORT is CL_TRUE.
	 */
	public long getImage3DMaxDepth() {
		return infos.getNativeLong(get(), CL_DEVICE_IMAGE3D_MAX_DEPTH);
	}

	/**
	 * CL_DEVICE_MAX_SAMPLERS<br/>
	 * Maximum number of samplers that can be used in a kernel. <br/>
	 * Refer to section 6.11.8 for a detailed description on samplers. <br/>
	 * The minimum value is 16 if CL_DEVICE_IMAGE_SUPPORT is CL_TRUE.
	 */
	public int getMaxSamplers() {
		return infos.getInt(get(), CL_DEVICE_MAX_SAMPLERS);
	}

	/**
	 * CL_DEVICE_MAX_PARAMETER_SIZE<br/>
	 * Max size in bytes of the arguments that can be passed to a kernel. <br/>
	 * The minimum value is 256.
	 */
	public long getMaxParameterSize() {
		return infos.getNativeLong(get(), CL_DEVICE_MAX_PARAMETER_SIZE);
	}

	/**
	 * CL_DEVICE_MEM_BASE_ADDR_ALIGN<br/>
	 * Describes the alignment in bits of the base address of any allocated memory object.
	 */
	public int getMemBaseAddrAlign() {
		return infos.getInt(get(), CL_DEVICE_MEM_BASE_ADDR_ALIGN);
	}

	/**
	 * CL_DEVICE_MIN_DATA_TYPE_ALIGN_SIZE<br/>
	 * The smallest alignment in bytes which can be used for any data type.
	 */
	public int getMinDataTypeAlign() {
		return infos.getInt(get(), CL_DEVICE_MIN_DATA_TYPE_ALIGN_SIZE);
	}

	/** Bit values for CL_DEVICE_SINGLE_FP_CONFIG */
	public enum CLDeviceSingleFP {
		/** denorms are supported                                  */ @EnumValue(CL_FP_DENORM			) Denorm			,
		/** INF and quiet NaNs are supported.                      */ @EnumValue(CL_FP_INF_NAN			) InfNaN			,
		/** round to nearest even rounding mode supported          */ @EnumValue(CL_FP_ROUND_TO_NEAREST	) RoundToNearest	,
		/** round to zero rounding mode supported                  */ @EnumValue(CL_FP_ROUND_TO_ZERO	) RoundToZero		,
		/** round to +ve and -ve infinity rounding modes supported */ @EnumValue(CL_FP_ROUND_TO_INF		) RoundToInf		,
		/** IEEE754-2008 fused multiply-add is supported.          */ @EnumValue(CL_FP_FMA				) FMA				;

		public long getValue() { return EnumValues.getValue(this); }
		public static long getValue(EnumSet<CLDeviceSingleFP> set) { return EnumValues.getValue(set); }
		public static EnumSet<CLDeviceSingleFP> getEnumSet(long v) { return EnumValues.getEnumSet(v, CLDeviceSingleFP.class); }
	}

	/**
	 * CL_DEVICE_SINGLE_FP_CONFIG<br/>
	 * Describes single precision floating- point capability of the device.<br/>
	 * The mandated minimum floating-point capability is: RoundToNearest and InfNaN.
	 */
	public EnumSet<CLDeviceSingleFP> getSingleFPConfig() {
		return CLDeviceSingleFP.getEnumSet(infos.getNativeLong(get(), CL_DEVICE_SINGLE_FP_CONFIG));
	}

	/** Values for CL_DEVICE_GLOBAL_MEM_CACHE_TYPE */
	public enum CLCacheType {
		@EnumValue(CL_NONE             ) None          ,
		@EnumValue(CL_READ_ONLY_CACHE  ) ReadOnlyCache ,
		@EnumValue(CL_READ_WRITE_CACHE ) ReadWriteCache;
		
		public long getValue() { return EnumValues.getValue(this); }
		public static CLCacheType getEnum(long v) { return EnumValues.getEnum(v, CLCacheType.class); }
	}
	/**
	 * CL_DEVICE_GLOBAL_MEM_CACHE_TYPE<br/>
	 * Type of global memory cache supported.
	 */
	public CLCacheType getGlobalMemCacheType() {
		return CLCacheType.getEnum(infos.getInt(get(), CL_DEVICE_GLOBAL_MEM_CACHE_TYPE));
	}


	/**
	 * CL_DEVICE_GLOBAL_MEM_CACHELINE_SIZE<br/>
	 * Size of global memory cache line in bytes.
	 */
	public int getGlobalMemCachelineSize() {
		return infos.getInt(get(), CL_DEVICE_GLOBAL_MEM_CACHELINE_SIZE);
	}

	/**
	 * CL_DEVICE_GLOBAL_MEM_CACHE_SIZE<br/>
	 * Size of global memory cache in bytes.
	 */
	public long getGlobalMemCacheSize() {
		return infos.getNativeLong(get(), CL_DEVICE_GLOBAL_MEM_CACHE_SIZE);
	}

	/**
	 * CL_DEVICE_GLOBAL_MEM_SIZE<br/>
	 * Size of global device memory in bytes.
	 */
	public long getGlobalMemSize() {
		return infos.getNativeLong(get(), CL_DEVICE_GLOBAL_MEM_SIZE);
	}

	/**
	 * CL_DEVICE_MAX_CONSTANT_BUFFER_SIZE<br/>
	 * Max size in bytes of a constant buffer allocation. <br/>
	 * The minimum value is 64 KB.
	 */
	public long getMaxConstantBufferSize() {
		return infos.getNativeLong(get(), CL_DEVICE_MAX_CONSTANT_BUFFER_SIZE);
	}

	/**
	 * CL_DEVICE_MAX_CONSTANT_ARGS<br/>
	 * Max number of arguments declared with the __constant qualifier in a kernel. <br/>
	 * The minimum value is 8.
	 */
	public int getMaxConstantArgs() {
		return infos.getInt(get(), CL_DEVICE_MAX_CONSTANT_ARGS);
	}

	/** Values for CL_DEVICE_LOCAL_MEM_TYPE */
	public enum CLMemType {
		/** implying dedicated local memory storage such as SRAM */
		@EnumValue(CL_LOCAL  ) Local ,
		@EnumValue(CL_GLOBAL ) Global;

		public long getValue() { return EnumValues.getValue(this); }
		public static CLMemType getEnum(long v) { return EnumValues.getEnum(v, CLMemType.class); }
	}

	/**
	 * CL_DEVICE_LOCAL_MEM_TYPE<br/>
	 * Type of local memory supported. <br/>
	 */
	public CLMemType getLocalMemType() {
		return CLMemType.getEnum(infos.getInt(get(), CL_DEVICE_LOCAL_MEM_TYPE));
	}
	
	/**
	 * CL_DEVICE_LOCAL_MEM_SIZE<br/>
	 * Size of local memory arena in bytes. <br/>
	 * The minimum value is 16 KB.
	 */
	public long getLocalMemSize() {
		return infos.getNativeLong(get(), CL_DEVICE_LOCAL_MEM_SIZE);
	}

	/**
	 * CL_DEVICE_ERROR_CORRECTION_SUPPORT<br/>
	 * Is CL_TRUE if the device implements error correction for the memories, caches, registers etc. in the device. <br/>
	 * Is CL_FALSE if the device does not implement error correction. <br/>
	 * This can be a requirement for certain clients of OpenCL.
	 */
	public boolean hasErrorCorrectionSupport() {
		return infos.getBool(get(), CL_DEVICE_ERROR_CORRECTION_SUPPORT);
	}

	/**
	 * CL_DEVICE_PROFILING_TIMER_RESOLUTION<br/>
	 * Describes the resolution of device timer. <br/>
	 * This is measured in nanoseconds.	<br/>
	 * Refer to section 5.9 for details.
	 */
	public long getProfilingTimerResolution() {
		return infos.getNativeLong(get(), CL_DEVICE_PROFILING_TIMER_RESOLUTION);
	}

	/**
	 * CL_DEVICE_ENDIAN_LITTLE<br/>
	 * Is CL_TRUE if the OpenCL device is a little endian device and CL_FALSE otherwise.
	 */
	public boolean isEndianLittle() {
		return infos.getBool(get(), CL_DEVICE_ENDIAN_LITTLE);
	}

	/**
	 * CL_DEVICE_AVAILABLE<br/>
	 * Is CL_TRUE if the device is available and CL_FALSE if the device is not available.
	 */
	public boolean isAvailable() {
		return infos.getBool(get(), CL_DEVICE_AVAILABLE);
	}

	/**
	 * CL_DEVICE_COMPILER_AVAILABLE<br/>
	 * Is CL_FALSE if the implementation does not have a compiler available to compile the program source. <br/>
	 * Is CL_TRUE if the compiler is available.<br/>
	 * This can be CL_FALSE for the embededed platform profile only.
	 */
	public boolean isCompilerAvailable() {
		return infos.getBool(get(), CL_DEVICE_COMPILER_AVAILABLE);
	}

	/**
	 * CL_DEVICE_NAME<br/>
	Device name string.
	 */
	public String getName() {
		return infos.getString(get(), CL_DEVICE_NAME);
	}

	/**
	 * CL_DEVICE_VENDOR<br/>
	Vendor name string.
	 */
	public String getVendor() {
		return infos.getString(get(), CL_DEVICE_VENDOR);
	}

	/**
	 * CL_DRIVER_VERSION<br/>
	OpenCL software driver version string in the form major_number.minor_number.
	 */
	public String getDriverVersion() {
		return infos.getString(get(), CL_DRIVER_VERSION);
	}

	/**
	 * CL_DEVICE_PROFILE<br/>
	 * OpenCL profile string. <br/>
	 * Returns the profile name supported by the device. <br/>
	 * The profile name returned can be one of the following strings:
	 * <ul>
	 * <li>FULL_PROFILE if the device supports the OpenCL specification (functionality defined as part of the core specification and does not require any extensions to be supported).</li>
	 * <li>EMBEDDED_PROFILE if the device supports the OpenCL embedded profile.</li>
	 * </ul>
	 */
	public String getProfile() {
		return infos.getString(get(), CL_DEVICE_PROFILE);
	}

	/**
	 * CL_DEVICE_VERSION<br/>
	 * OpenCL version string. <br/>
	 * Returns the OpenCL version supported by the device.<br/>
	 * This version string has the following format:
	 * <code>
	 * OpenCL&lt;space&gt;&lt;major_version.min or_version&gt;&lt;space&gt;&lt;vendor-specific information>
	 * </code>
	 * The major_version.minor_version value returned will be 1.0.
	 */
	public String getVersion() {
		return infos.getString(get(), CL_DEVICE_VERSION);
	}

	/**
	 * CL_DEVICE_EXTENSIONS<br/>
	Returns a space separated list of extension names (the extension names themselves do not contain any spaces). The list of extension names returned currently can include one or more of
	 */
	public String getExtensions() {
		return infos.getString(get(), CL_DEVICE_EXTENSIONS);
	}

	/** Bit values for CL_DEVICE_QUEUE_PROPERTIES */
	public enum CLQueueProperty {
		@EnumValue(CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE) OutOfOrderExecModeEnable,
		@EnumValue(CL_QUEUE_PROFILING_ENABLE             ) ProfilingEnable		   ;
		
		public long getValue() { return EnumValues.getValue(this); }
		public static long getValue(EnumSet<CLQueueProperty> set) { return EnumValues.getValue(set); }
		public static EnumSet<CLQueueProperty> getEnumSet(long v) { return EnumValues.getEnumSet(v, CLQueueProperty.class); }
	}

	/**
	 * CL_DEVICE_QUEUE_PROPERTIES<br/>
	 * Describes the command-queue properties supported by the device.<br/>
	 * These properties are described in table 5.1.<br/>
	 * The mandated minimum capability is: ProfilingEnable.
	 */
	public EnumSet<CLQueueProperty> getQueueProperties() {
		return CLQueueProperty.getEnumSet(infos.getNativeLong(get(), CL_DEVICE_QUEUE_PROPERTIES));
	}

}