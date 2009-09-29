/*

*/

package com.nativelibs4java.opencl;
import com.nativelibs4java.opencl.library.OpenCLLibrary;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;
import com.sun.jna.*;
import com.sun.jna.ptr.*;
import java.nio.*;
import static com.nativelibs4java.opencl.OpenCL4Java.*;

/**
 * OpenCL device (CPU, GPU...).<br/>
 * Devices are retrieved from a CLPlatform (@see CLPlatform.listAllDevices())
 */
public class CLDevice {

    final cl_device_id device;
	static CLInfoGetter<cl_device_id> infos = new CLInfoGetter<cl_device_id>() {
		@Override
		protected int getInfo(cl_device_id entity, int infoTypeEnum, NativeLong size, Pointer out, NativeLongByReference sizeOut) {
			return CL.clGetDeviceInfo(entity, infoTypeEnum, size, out, sizeOut);
		}
	};

    CLDevice(cl_device_id device) {
        this.device = device;
    }

	/**
	 * CL_DEVICE_EXECUTION_CAPABILITIES<br/>
	 * Describes the execution capabilities of the device. This is a bit-field that describes one or more of the following values:
	 * <ul>
	 * <li>CL_EXEC_KERNEL The OpenCL device can execute OpenCL kernels.</li>
	 * <li>CL_EXEC_NATIVE_KERNEL The OpenCL device can execute native kernels.</li>
	 * </ul>
	 * The mandated minimum capability is: CL_EXEC_KERNEL.
	 */
    public int getExecutionCapabilities() {
        return infos.getInt(get(), CL_DEVICE_EXECUTION_CAPABILITIES);
    }
	/**
	 * CL_DEVICE_TYPE<br/>
	 * The OpenCL device type.
	 * @return CL_DEVICE_TYPE_CPU, CL_DEVICE_TYPE_GPU, CL_DEVICE_TYPE_ACCELERATOR, CL_DEVICE_TYPE_DEFAULT or a
combination of the above.
	 */
	public int getType() {
        return infos.getInt(get(), CL_DEVICE_TYPE);
    }

	/**
	 * CL_DEVICE_VENDOR_ID<br/>
	 * A unique device vendor identifier. An example of a unique device identifier could be the PCIe ID.
	 */
	public int getVendorId() {
		return infos.getInt(get(), CL_DEVICE_VENDOR_ID);
	}

	/**
	 * CL_DEVICE_MAX_COMPUTE_UNITS<br/>
	 * The number of parallel compute cores on the OpenCL device. The minimum value is 1.
	 */
	public int getMaxComputeUnits() {
		return infos.getInt(get(), CL_DEVICE_MAX_COMPUTE_UNITS);
	}

	/**
	 * CL_DEVICE_MAX_WORK_ITEM_DIMENSIONS<br/>
	 * Maximum dimensions that specify the global and local work-item IDs used by the data parallel execution model. (Refer to clEnqueueNDRangeKernel). The minimum value is 3.
	 */
	public int getMaxWorkItemDimensions() {
		return infos.getInt(get(), CL_DEVICE_MAX_WORK_ITEM_DIMENSIONS);
	}

	/**
	 * CL_DEVICE_MAX_WORK_GROUP_SIZE<br/>
	 * Maximum number of work-items in a work-group executing a kernel using the data parallel execution model.
	 * (Refer to clEnqueueNDRangeKernel). The minimum value is 1.
	 */
	public long getMaxWorkGroupSize() {
		return infos.getNativeLong(get(), CL_DEVICE_MAX_WORK_GROUP_SIZE).longValue();
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
		return infos.getNativeLong(get(), CL_DEVICE_MAX_MEM_ALLOC_SIZE).longValue();
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
	 * Max number of simultaneous image objects that can be read by a kernel. The minimum value is 128 if CL_DEVICE_IMAGE_SUPPORT is CL_TRUE (@see hasImageSupport()).
	 */
	public int getMaxReadImageArgs() {
		return infos.getInt(get(), CL_DEVICE_MAX_READ_IMAGE_ARGS);
	}

	/**
	 * CL_DEVICE_MAX_WRITE_IMAGE_ARGS<br/>
	 * Max number of simultaneous image objects that can be written to by a kernel. The minimum value is 8 if CL_DEVICE_IMAGE_SUPPORT is CL_TRUE (@see hasImageSupport()).
	 */
	public int getMaxWriteImageArgs() {
		return infos.getInt(get(), CL_DEVICE_MAX_WRITE_IMAGE_ARGS);
	}


    @Override
    public String toString() {
        return getName();
    }

    @SuppressWarnings("deprecation")
    public CLQueue createQueue(CLContext context) {
        IntByReference errRef = new IntByReference();
        cl_command_queue queue = CL.clCreateCommandQueue(
                context.get(),
                device,
                0,
                errRef);
        error(errRef.getValue());

        return new CLQueue(context, queue);
    }

    public cl_device_id get() {
        return device;
    }


	/**
	 * CL_DEVICE_IMAGE2D_MAX_WIDTH<br/>
	Max width of 2D image in pixels. The minimum value is 8192 if CL_DEVICE_IMAGE_SUPPORT is CL_TRUE.
	 */
	public long getImage2DMaxWidth() {
		return infos.getNativeLong(get(), CL_DEVICE_IMAGE2D_MAX_WIDTH).longValue();
	}

	/**
	 * CL_DEVICE_IMAGE2D_MAX_HEIGHT<br/>
	Max height of 2D image in pixels. The
	Last Revision Date: 5/16/09	Page 34
	minimum value is 8192 if CL_DEVICE_IMAGE_SUPPORT is CL_TRUE.
	 */
	public long getImage2DMaxHeight() {
		return infos.getNativeLong(get(), CL_DEVICE_IMAGE2D_MAX_HEIGHT).longValue();
	}

	/**
	 * CL_DEVICE_IMAGE3D_MAX_WIDTH<br/>
	Max width of 3D image in pixels. The minimum value is 2048 if CL_DEVICE_IMAGE_SUPPORT is CL_TRUE.
	 */
	public long getImage3DMaxWidth() {
		return infos.getNativeLong(get(), CL_DEVICE_IMAGE3D_MAX_WIDTH).longValue();
	}

	/**
	 * CL_DEVICE_IMAGE3D_MAX_HEIGHT<br/>
	Max height of 3D image in pixels. The minimum value is 2048 if CL_DEVICE_IMAGE_SUPPORT is CL_TRUE.
	 */
	public long getImage3DMaxHeight() {
		return infos.getNativeLong(get(), CL_DEVICE_IMAGE3D_MAX_HEIGHT).longValue();
	}

	/**
	 * CL_DEVICE_IMAGE3D_MAX_DEPTH<br/>
	Max depth of 3D image in pixels. The minimum value is 2048 if CL_DEVICE_IMAGE_SUPPORT is CL_TRUE.
	 */
	public long getImage3DMaxDepth() {
		return infos.getNativeLong(get(), CL_DEVICE_IMAGE3D_MAX_DEPTH).longValue();
	}

	/**
	 * CL_DEVICE_MAX_SAMPLERS<br/>
	Maximum number of samplers that can be used in a kernel. Refer to section 6.11.8 for a detailed description on samplers. The minimum value is 16 if CL_DEVICE_IMAGE_SUPPORT is CL_TRUE.
	 */
	public int getMaxSamplers() {
		return infos.getInt(get(), CL_DEVICE_MAX_SAMPLERS);
	}

	/**
	 * CL_DEVICE_MAX_PARAMETER_SIZE<br/>
	Max size in bytes of the arguments that can be passed to a kernel. The minimum value is 256.
	 */
	public long getMaxParameterSize() {
		return infos.getNativeLong(get(), CL_DEVICE_MAX_PARAMETER_SIZE).longValue();
	}

	/**
	 * CL_DEVICE_MEM_BASE_ADDR_ALIGN<br/>
	Describes the alignment in bits of the base address of any allocated memory object.
	 */
	public int getMemBaseAddrAlign() {
		return infos.getInt(get(), CL_DEVICE_MEM_BASE_ADDR_ALIGN);
	}

	/**
	 * CL_DEVICE_MIN_DATA_TYPE_ALIGN_SIZE<br/>
	The smallest alignment in bytes which can be used for any data type.
	 */
	public int getMinDataTypeAlign() {
		return infos.getInt(get(), CL_DEVICE_MIN_DATA_TYPE_ALIGN_SIZE);
	}

	/**
	 * CL_DEVICE_SINGLE_FP_CONFIG<br/>
	 * Describes single precision floating- point capability of the device. This is a bit-field that describes one or more of the following values:
	 * <ul>
	 * <li>CL_FP_DENORM denorms are supported</li>
	 * <li>CL_FP_INF_NAN INF and quiet NaNs are supported.</li>
	 * <li>CL_FP_ROUND_TO_NEAREST round to nearest even rounding mode supported</li>
	 * <li>CL_FP_ROUND_TO_ZERO round to zero rounding mode supported</li>
	 * <li>CL_FP_ROUND_TO_INF round to +ve and -ve infinity rounding modes supported</li>
	 * <li>CL_FP_FMA IEEE754-2008 fused multiply-add is supported.</li>
	 * </ul>
	 * The mandated minimum floating-point capability is: CL_FP_ROUND_TO_NEAREST | CL_FP_INF_NAN.
	 */
	public int getSingleFPConfig() {
		return infos.getInt(get(), CL_DEVICE_SINGLE_FP_CONFIG);
	}

	/**
	 * CL_DEVICE_GLOBAL_MEM_CACHE_TYPE<br/>
	Type of global memory cache
	supported. Valid values are: CL_NONE, CL_READ_ONLY_CACHE and CL_READ_WRITE_CACHE.
	 */
	public int getGlobalMemCacheType() {
		return infos.getInt(get(), CL_DEVICE_GLOBAL_MEM_CACHE_TYPE);
	}


	/**
	 * CL_DEVICE_GLOBAL_MEM_CACHELINE_SIZE<br/>
	Size of global memory cache line in bytes.
	 */
	public int getGlobalMemCachelineSize() {
		return infos.getInt(get(), CL_DEVICE_GLOBAL_MEM_CACHELINE_SIZE);
	}

	/**
	 * CL_DEVICE_GLOBAL_MEM_CACHE_SIZE<br/>
	Size of global memory cache in bytes.
	 */
	public long getGlobalMemCacheSize() {
		return infos.getNativeLong(get(), CL_DEVICE_GLOBAL_MEM_CACHE_SIZE).longValue();
	}

	/**
	 * CL_DEVICE_GLOBAL_MEM_SIZE<br/>
	Size of global device memory in bytes.
	 */
	public long getDEVICE_GLOBAL_MEM_SIZE() {
		return infos.getNativeLong(get(), CL_DEVICE_GLOBAL_MEM_SIZE).longValue();
	}

	/**
	 * CL_DEVICE_MAX_CONSTANT_BUFFER_SIZE<br/>
	Max size in bytes of a constant buffer allocation. The minimum value is 64 KB.
	 */
	public long getDEVICE_MAX_CONSTANT_BUFFER_SIZE() {
		return infos.getNativeLong(get(), CL_DEVICE_MAX_CONSTANT_BUFFER_SIZE).longValue();
	}

	/**
	 * CL_DEVICE_MAX_CONSTANT_ARGS<br/>
	Max number of arguments declared with the __constant qualifier in a kernel. The minimum value is 8.
	 */
	public int getMaxConstantArgs() {
		return infos.getInt(get(), CL_DEVICE_MAX_CONSTANT_ARGS);
	}

	/**
	 * CL_DEVICE_LOCAL_MEM_TYPE<br/>
	Type of local memory supported. This can be set to CL_LOCAL implying dedicated local memory storage such as SRAM, or CL_GLOBAL.
	 */
	public int getLocalMemType() {
		return infos.getInt(get(), CL_DEVICE_LOCAL_MEM_TYPE);
	}

	/**
	 * CL_DEVICE_LOCAL_MEM_SIZE<br/>
	Size of local memory arena in bytes. The minimum value is 16 KB.
	 */
	public long getLocalMemSize() {
		return infos.getNativeLong(get(), CL_DEVICE_LOCAL_MEM_SIZE).longValue();
	}

	/**
	 * CL_DEVICE_ERROR_CORRECTION_SUPPORT<br/>
	Is CL_TRUE if the device implements error correction for the memories, caches, registers etc. in the device. Is CL_FALSE if the device does not implement error correction. This can be a requirement for certain clients of OpenCL.
	 */
	public boolean hasErrorCorrectionSupport() {
		return infos.getBool(get(), CL_DEVICE_ERROR_CORRECTION_SUPPORT);
	}

	/**
	 * CL_DEVICE_PROFILING_TIMER_RESOLUTION<br/>
	Describes the resolution of device timer. This is measured in nanoseconds.	Refer to section 5.9 for
	Last Revision Date: 5/16/09	Page 36
	details.
	 */
	public long getProfilingTimerResolution() {
		return infos.getNativeLong(get(), CL_DEVICE_PROFILING_TIMER_RESOLUTION).longValue();
	}

	/**
	 * CL_DEVICE_ENDIAN_LITTLE<br/>
	Is CL_TRUE if the OpenCL device is a little endian device and CL_FALSE otherwise.
	 */
	public boolean isEndianLittle() {
		return infos.getBool(get(), CL_DEVICE_ENDIAN_LITTLE);
	}

	/**
	 * CL_DEVICE_AVAILABLE<br/>
	Is CL_TRUE if the device is available and CL_FALSE if the device is not available.
	 */
	public boolean isAvailable() {
		return infos.getBool(get(), CL_DEVICE_AVAILABLE);
	}

	/**
	 * CL_DEVICE_COMPILER_AVAILABLE<br/>
	Is CL_FALSE if the implementation does not have a compiler available to compile the program source. Is CL_TRUE if the compiler is available.
	This can be CL_FALSE for the embededed platform profile only.
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
	 * OpenCL profile string. Returns the profile name supported by the device. The profile name returned can be one of the following strings:
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
	OpenCL version string. Returns the OpenCL version supported by the device. This version string has the following format:
	OpenCL<space><major_version.min or_version><space><vendor-specific information>
	The major_version.minor_version value returned will be 1.0.
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


	/**
	 * CL_DEVICE_QUEUE_PROPERTIES<br/>
	Describes the command-queue properties supported by the device. This is a bit-field that describes one or more of the following values:
	CL_QUEUE_OUT_OF_ORDER_EXEC_ MODE_ENABLE
	CL_QUEUE_PROFILING_ENABLE
	These properties are described in table 5.1.
	Last Revision Date: 5/16/09	Page 37
	The mandated minimum capability is: CL_QUEUE_PROFILING_ENABLE.
	 */
	public int getQueueProperties() {
		return infos.getInt(get(), CL_DEVICE_QUEUE_PROPERTIES);
	}

}