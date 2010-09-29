/*
 * JavaCL - Java API and utilities for OpenCL
 * http://javacl.googlecode.com/
 *
 * Copyright (c) 2009-2010, Olivier Chafik (http://ochafik.free.fr/)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Olivier Chafik nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY OLIVIER CHAFIK AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.nativelibs4java.opencl;

import com.nativelibs4java.util.EnumValue;
import com.nativelibs4java.util.EnumValues;
import com.nativelibs4java.opencl.library.OpenCLLibrary;
import com.nativelibs4java.util.IOUtils;
import com.nativelibs4java.util.NIOUtils;

import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;
import org.bridj.*;
import static org.bridj.Pointer.*;

import java.io.IOException;
import java.nio.*;
import static com.nativelibs4java.opencl.JavaCL.*;
import static com.nativelibs4java.util.NIOUtils.*;
import java.util.*;
import static com.nativelibs4java.opencl.CLException.*;
import org.bridj.ValuedEnum;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * OpenCL device (CPU, GPU...).<br/>
 * Devices are retrieved from a CLPlatform
 * @see CLPlatform#listDevices(java.util.EnumSet, boolean)
 * @see CLPlatform#listAllDevices(boolean)
 * @see CLPlatform#listCPUDevices(boolean)
 * @see CLPlatform#listGPUDevices(boolean)
 */
@SuppressWarnings("unused")
public class CLDevice extends CLAbstractEntity<cl_device_id> {

    private static CLInfoGetter<cl_device_id> infos = new CLInfoGetter<cl_device_id>() {

        @Override
        protected int getInfo(cl_device_id entity, int infoTypeEnum, long size, Pointer out, Pointer<SizeT> sizeOut) {
            return CL.clGetDeviceInfo(entity, infoTypeEnum, size, out, sizeOut);
        }
    };
    volatile CLPlatform platform;

    CLDevice(CLPlatform platform, cl_device_id device) {
        super(device);
        this.platform = platform;
    }

    public synchronized CLPlatform getPlatform() {
        if (platform == null) {
            Pointer pplat = infos.getPointer(getEntity(), CL_DEVICE_PLATFORM);
            platform = new CLPlatform(pplat == null ? null : new cl_platform_id(pplat));
        }
        return platform;
    }

    @Override
    protected void clear() {
    }

    public String createSignature() {
        return getName() + "|" + getVendor() + "|" + getDriverVersion() + "|" + getProfile();
    }
    public static Map<String, List<CLDevice>> getDevicesBySignature(List<CLDevice> devices) {
        Map<String, List<CLDevice>> ret = new HashMap<String, List<CLDevice>>();
        for (CLDevice device : devices) {
            String signature = device.createSignature();
            List<CLDevice> list = ret.get(signature);
            if (list == null)
                ret.put(signature, list = new ArrayList<CLDevice>());
            list.add(device);
        }
        return ret;
    }


    public ByteOrder getByteOrder() {
        return isEndianLittle() ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
    }

    ByteOrder kernelsDefaultByteOrder;
    public synchronized ByteOrder getKernelsDefaultByteOrder() {
        if (kernelsDefaultByteOrder == null) {
            kernelsDefaultByteOrder = ByteOrder.nativeOrder();
            /*
            CLPlatform platform = getPlatform();
            if (platform != null && platform.getVendor().toLowerCase().contains("nvidia"))
                kernelsDefaultByteOrder = getByteOrder();
            else {
                CLContext context = JavaCL.createContext((Map)null, this);
                CLQueue queue = context.createDefaultQueue();
                try {
                    int n = 16;

                    CLIntBuffer inputMatchResult = context.createIntBuffer(CLMem.Usage.Output, n);
                    float testValue = 12;
                    CLFloatBuffer inPtr = context.createFloatBuffer(CLMem.Usage.Input, n);
                    inPtr.write(queue, FloatBuffer.wrap(new float[] { testValue }), true);
                    CLProgram program =
                        context.createProgram(IOUtils.readText(CLDevice.class.getResourceAsStream("EndiannessTest.cl")))
                        .defineMacro("TEST_VALUE", testValue)
                        .build();

                    CLKernel test = program.createKernel("testEndianness", inPtr, inPtr, inPtr, inputMatchResult);
                    test.enqueueNDRange(queue, new int[] { n }, new int[] { 1 });
                    queue.finish();

                    IntBuffer b = NIOUtils.directInts(n, getByteOrder());
                    inputMatchResult.read(queue, b, true);
                    switch (b.get(0)) {
                        case 1: // device endianness
                            kernelsDefaultByteOrder = getByteOrder();
                            break;
                        case 2: // host endianness
                            kernelsDefaultByteOrder = ByteOrder.nativeOrder();
                            break;
                        default:
                            throw new RuntimeException("Default kernel argument endianness of this device couldn't be guessed out.");
                    }

                } catch (CLBuildException ex) {
                    throw new RuntimeException("Default kernel argument endianness of this device couldn't be guessed out.", ex);
                } catch (IOException ex) {
                    throw new RuntimeException("Couldn't find internal resources needed to guess this device's kernel argument endianness.", ex);
                } finally {
                    queue.finish();
                    System.gc();
                    queue.release();
                    context.release();
                }
            }//*/
        }
        return kernelsDefaultByteOrder;
    }

    /** Bit values for CL_DEVICE_EXECUTION_CAPABILITIES */
    public enum ExecutionCapability implements com.nativelibs4java.util.ValuedEnum {

        Kernel(CL_EXEC_KERNEL),
        NativeKernel(CL_EXEC_NATIVE_KERNEL);

        ExecutionCapability(long value) { this.value = value; }
        long value;
        @Override
        public long value() { return value; }
        public static long getValue(EnumSet<ExecutionCapability> set) {
            return EnumValues.getValue(set);
        }

        public static EnumSet<ExecutionCapability> getEnumSet(long v) {
            return EnumValues.getEnumSet(v, ExecutionCapability.class);
        }
    }

    /**
     * Describes the execution capabilities of the device.<br/>
     * The mandated minimum capability is: Kernel.
     */
    @InfoName("CL_DEVICE_EXECUTION_CAPABILITIES")
    public EnumSet<ExecutionCapability> getExecutionCapabilities() {
        return ExecutionCapability.getEnumSet(infos.getIntOrLong(getEntity(), CL_DEVICE_EXECUTION_CAPABILITIES));
    }

    /** Bit values for CL_DEVICE_TYPE */
    public enum Type implements com.nativelibs4java.util.ValuedEnum {

        CPU(CL_DEVICE_TYPE_CPU),
        GPU(CL_DEVICE_TYPE_GPU),
        Accelerator(CL_DEVICE_TYPE_ACCELERATOR),
        Default(CL_DEVICE_TYPE_DEFAULT);

        Type(long value) { this.value = value; }
        long value;
        @Override
		public long value() { return value; }
        
        public static long getValue(EnumSet<Type> set) {
            return EnumValues.getValue(set);
        }

        public static EnumSet<Type> getEnumSet(long v) {
            return EnumValues.getEnumSet(v, Type.class);
        }
    }

    /**
     * The OpenCL device type.
     */
    @InfoName("CL_DEVICE_TYPE")
    public EnumSet<Type> getType() {
        return Type.getEnumSet(infos.getIntOrLong(getEntity(), CL_DEVICE_TYPE));
    }

    /**
     * A unique device vendor identifier. <br/>
     * An example of a unique device identifier could be the PCIe ID.
     */
    @InfoName("CL_DEVICE_VENDOR_ID")
    public int getVendorId() {
        return infos.getInt(getEntity(), CL_DEVICE_VENDOR_ID);
    }

    /**
     * The number of parallel compute cores on the OpenCL device. <br/>
     * The minimum value is 1.
     */
    @InfoName("CL_DEVICE_MAX_COMPUTE_UNITS")
    public int getMaxComputeUnits() {
        return infos.getInt(getEntity(), CL_DEVICE_MAX_COMPUTE_UNITS);
    }

    /**
     * Maximum dimensions that specify the global and local work-item IDs used by the data parallel execution model. <br/>
     * (Refer to clEnqueueNDRangeKernel).
     * <br/>The minimum value is 3.
     */
    @InfoName("CL_DEVICE_MAX_WORK_ITEM_DIMENSIONS")
    public int getMaxWorkItemDimensions() {
        return infos.getInt(getEntity(), CL_DEVICE_MAX_WORK_ITEM_DIMENSIONS);
    }

    /**
     * Maximum number of work-items that can be specified in each dimension of the work-group to clEnqueueNDRangeKernel.
     */
    @InfoName("CL_DEVICE_MAX_WORK_ITEM_SIZES")
    public long[] getMaxWorkItemSizes() {
        long sizes[] = infos.getNativeSizes(getEntity(), CL_DEVICE_MAX_WORK_ITEM_SIZES, getMaxWorkItemDimensions());
        for (int i = 0, n = sizes.length; i < n; i++) {
            long size = sizes[i];
            if ((size & 0xffffffff00000000L) == 0xcccccccc00000000L)
                sizes[i] = size & 0xffffffffL;
        }
        return sizes;
    }

    /**
     * Maximum number of work-items in a work-group executing a kernel using the data parallel execution model.
     * (Refer to clEnqueueNDRangeKernel). <br/>
     * The minimum value is 1.
     */
    @InfoName("CL_DEVICE_MAX_WORK_GROUP_SIZE")
    public long getMaxWorkGroupSize() {
        return infos.getIntOrLong(getEntity(), CL_DEVICE_MAX_WORK_GROUP_SIZE);
    }

    /**
     * Maximum configured clock frequency of the device in MHz.
     */
    @InfoName("CL_DEVICE_MAX_CLOCK_FREQUENCY")
    public int getMaxClockFrequency() {
        return infos.getInt(getEntity(), CL_DEVICE_MAX_CLOCK_FREQUENCY);
    }

    /**
     * The default compute device address space size specified as an unsigned integer value in bits. Currently supported values are 32 or 64 bits..<br>
     * Size of size_t type in OpenCL kernels can be obtained with getAddressBits() / 8.
     */
    @InfoName("CL_DEVICE_ADDRESS_BITS")
    public int getAddressBits() {
        return infos.getInt(getEntity(), CL_DEVICE_ADDRESS_BITS);
    }

    /**
     * Max size of memory object allocation in bytes. The minimum value is max (1/4th of CL_DEVICE_GLOBAL_MEM_SIZE , 128*1024*1024)
     */
    @InfoName("CL_DEVICE_MAX_MEM_ALLOC_SIZE")
    public long getMaxMemAllocSize() {
        return infos.getIntOrLong(getEntity(), CL_DEVICE_MAX_MEM_ALLOC_SIZE);
    }

    /**
     * Is CL_TRUE if images are supported by the OpenCL device and CL_FALSE otherwise.
     */
    @InfoName("CL_DEVICE_IMAGE_SUPPORT")
    public boolean hasImageSupport() {
        return infos.getBool(getEntity(), CL_DEVICE_IMAGE_SUPPORT);
    }

    /**
     * Max number of simultaneous image objects that can be read by a kernel. <br/>
     * The minimum value is 128 if CL_DEVICE_IMAGE_SUPPORT is CL_TRUE (@see hasImageSupport()).
     */
    @InfoName("CL_DEVICE_MAX_READ_IMAGE_ARGS")
    public int getMaxReadImageArgs() {
        return infos.getInt(getEntity(), CL_DEVICE_MAX_READ_IMAGE_ARGS);
    }

    /**
     * Max number of simultaneous image objects that can be written to by a kernel. <br/>
     * The minimum value is 8 if CL_DEVICE_IMAGE_SUPPORT is CL_TRUE (@see hasImageSupport()).
     */
    @InfoName("CL_DEVICE_MAX_WRITE_IMAGE_ARGS")
    public int getMaxWriteImageArgs() {
        return infos.getInt(getEntity(), CL_DEVICE_MAX_WRITE_IMAGE_ARGS);
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
    public CLQueue createQueue(CLContext context, QueueProperties... queueProperties) {
        Pointer<Integer> pErr = allocateInt();
        long flags = 0;
        for (QueueProperties prop : queueProperties)
            flags |= prop.value();
        cl_command_queue queue = CL.clCreateCommandQueue(context.getEntity(), getEntity(), flags, pErr);
        error(pErr.get());

        return new CLQueue(context, queue, this);
    }

    @Deprecated
    public CLQueue createQueue(EnumSet<QueueProperties> queueProperties, CLContext context) {
        Pointer<Integer> pErr = allocateInt();
        cl_command_queue queue = CL.clCreateCommandQueue(context.getEntity(), getEntity(), QueueProperties.getValue(queueProperties), pErr);
        error(pErr.get());

        return new CLQueue(context, queue, this);
    }

    public CLQueue createOutOfOrderQueue(CLContext context) {
        return createQueue(EnumSet.of(QueueProperties.OutOfOrderExecModeEnable), context);
    }

    public CLQueue createProfilingQueue(CLContext context) {
        return createQueue(EnumSet.of(QueueProperties.ProfilingEnable), context);
    }

    /**
     * Max width of 2D image in pixels. <br/>
     * The minimum value is 8192 if CL_DEVICE_IMAGE_SUPPORT is CL_TRUE.
     */
    @InfoName("CL_DEVICE_IMAGE2D_MAX_WIDTH")
    public long getImage2DMaxWidth() {
        return infos.getIntOrLong(getEntity(), CL_DEVICE_IMAGE2D_MAX_WIDTH);
    }

    /**
     * Max height of 2D image in pixels. <br/>
     * The minimum value is 8192 if CL_DEVICE_IMAGE_SUPPORT is CL_TRUE.
     */
    @InfoName("CL_DEVICE_IMAGE2D_MAX_HEIGHT")
    public long getImage2DMaxHeight() {
        return infos.getIntOrLong(getEntity(), CL_DEVICE_IMAGE2D_MAX_HEIGHT);
    }

    /**
     * Max width of 3D image in pixels. <br/>
     * The minimum value is 2048 if CL_DEVICE_IMAGE_SUPPORT is CL_TRUE.
     */
    @InfoName("CL_DEVICE_IMAGE3D_MAX_WIDTH")
    public long getImage3DMaxWidth() {
        return infos.getIntOrLong(getEntity(), CL_DEVICE_IMAGE3D_MAX_WIDTH);
    }

    /**
     * Max height of 3D image in pixels. <br/>
     * The minimum value is 2048 if CL_DEVICE_IMAGE_SUPPORT is CL_TRUE.
     */
    @InfoName("CL_DEVICE_IMAGE3D_MAX_HEIGHT")
    public long getImage3DMaxHeight() {
        return infos.getIntOrLong(getEntity(), CL_DEVICE_IMAGE3D_MAX_HEIGHT);
    }

    /**
     * Max depth of 3D image in pixels. <br/>
     * The minimum value is 2048 if CL_DEVICE_IMAGE_SUPPORT is CL_TRUE.
     */
    @InfoName("CL_DEVICE_IMAGE3D_MAX_DEPTH")
    public long getImage3DMaxDepth() {
        return infos.getIntOrLong(getEntity(), CL_DEVICE_IMAGE3D_MAX_DEPTH);
    }

    /**
     * Maximum number of samplers that can be used in a kernel. <br/>
     * Refer to section 6.11.8 for a detailed description on samplers. <br/>
     * The minimum value is 16 if CL_DEVICE_IMAGE_SUPPORT is CL_TRUE.
     */
    @InfoName("CL_DEVICE_MAX_SAMPLERS")
    public int getMaxSamplers() {
        return infos.getInt(getEntity(), CL_DEVICE_MAX_SAMPLERS);
    }

    /**
     * Max size in bytes of the arguments that can be passed to a kernel. <br/>
     * The minimum value is 256.
     */
    @InfoName("CL_DEVICE_MAX_PARAMETER_SIZE")
    public long getMaxParameterSize() {
        return infos.getIntOrLong(getEntity(), CL_DEVICE_MAX_PARAMETER_SIZE);
    }

    /**
     * Describes the alignment in bits of the base address of any allocated memory object.
     */
    @InfoName("CL_DEVICE_MEM_BASE_ADDR_ALIGN")
    public int getMemBaseAddrAlign() {
        return infos.getInt(getEntity(), CL_DEVICE_MEM_BASE_ADDR_ALIGN);
    }

    /**
     * The smallest alignment in bytes which can be used for any data type.
     */
    @InfoName("CL_DEVICE_MIN_DATA_TYPE_ALIGN_SIZE")
    public int getMinDataTypeAlign() {
        return infos.getInt(getEntity(), CL_DEVICE_MIN_DATA_TYPE_ALIGN_SIZE);
    }

    /** Bit values for CL_DEVICE_SINGLE_FP_CONFIG */
    public enum SingleFPConfig implements com.nativelibs4java.util.ValuedEnum {

        /** denorms are supported                                  */
        Denorm(CL_FP_DENORM),
        /** INF and quiet NaNs are supported.                      */
        InfNaN(CL_FP_INF_NAN),
        /** round to nearest even rounding mode supported          */
        RoundToNearest(CL_FP_ROUND_TO_NEAREST),
        /** round to zero rounding mode supported                  */
        RoundToZero(CL_FP_ROUND_TO_ZERO),
        /** round to +ve and -ve infinity rounding modes supported */
        RoundToInf(CL_FP_ROUND_TO_INF),
        /** IEEE754-2008 fused multiply-add is supported.          */
        FMA(CL_FP_FMA);

        SingleFPConfig(long value) { this.value = value; }
        long value;
        @Override
		public long value() { return value; }
        

        public static long getValue(EnumSet<SingleFPConfig> set) {
            return EnumValues.getValue(set);
        }

        public static EnumSet<SingleFPConfig> getEnumSet(long v) {
            return EnumValues.getEnumSet(v, SingleFPConfig.class);
        }
    }

    /**
     * Describes single precision floating- point capability of the device.<br/>
     * The mandated minimum floating-point capability is: RoundToNearest and InfNaN.
     */
    @InfoName("CL_DEVICE_SINGLE_FP_CONFIG")
    public EnumSet<SingleFPConfig> getSingleFPConfig() {
        return SingleFPConfig.getEnumSet(infos.getIntOrLong(getEntity(), CL_DEVICE_SINGLE_FP_CONFIG));
    }

    /** Values for CL_DEVICE_GLOBAL_MEM_CACHE_TYPE */
    public enum GlobalMemCacheType implements com.nativelibs4java.util.ValuedEnum {

        None(CL_NONE),
        ReadOnlyCache(CL_READ_ONLY_CACHE),
        ReadWriteCache(CL_READ_WRITE_CACHE);

        GlobalMemCacheType(long value) { this.value = value; }
        long value;
        @Override
		public long value() { return value; }
        
        public static GlobalMemCacheType getEnum(long v) {
            return EnumValues.getEnum(v, GlobalMemCacheType.class);
        }
    }

    /**
     * Type of global memory cache supported.
     */
    @InfoName("CL_DEVICE_GLOBAL_MEM_CACHE_TYPE")
    public GlobalMemCacheType getGlobalMemCacheType() {
        return GlobalMemCacheType.getEnum(infos.getInt(getEntity(), CL_DEVICE_GLOBAL_MEM_CACHE_TYPE));
    }

    /**
     * Size of global memory cache line in bytes.
     */
    @InfoName("CL_DEVICE_GLOBAL_MEM_CACHELINE_SIZE")
    public int getGlobalMemCachelineSize() {
        return infos.getInt(getEntity(), CL_DEVICE_GLOBAL_MEM_CACHELINE_SIZE);
    }

    /**
     * Size of global memory cache in bytes.
     */
    @InfoName("CL_DEVICE_GLOBAL_MEM_CACHE_SIZE")
    public long getGlobalMemCacheSize() {
        return infos.getIntOrLong(getEntity(), CL_DEVICE_GLOBAL_MEM_CACHE_SIZE);
    }

    /**
     * Size of global device memory in bytes.
     */
    @InfoName("CL_DEVICE_GLOBAL_MEM_SIZE")
    public long getGlobalMemSize() {
        return infos.getIntOrLong(getEntity(), CL_DEVICE_GLOBAL_MEM_SIZE);
    }

    /**
     * Max size in bytes of a constant buffer allocation. <br/>
     * The minimum value is 64 KB.
     */
    @InfoName("CL_DEVICE_MAX_CONSTANT_BUFFER_SIZE")
    public long getMaxConstantBufferSize() {
        return infos.getIntOrLong(getEntity(), CL_DEVICE_MAX_CONSTANT_BUFFER_SIZE);
    }

    /**
     * Max number of arguments declared with the __constant qualifier in a kernel. <br/>
     * The minimum value is 8.
     */
    @InfoName("CL_DEVICE_MAX_CONSTANT_ARGS")
    public int getMaxConstantArgs() {
        return infos.getInt(getEntity(), CL_DEVICE_MAX_CONSTANT_ARGS);
    }

    /** Values for CL_DEVICE_LOCAL_MEM_TYPE */
    public enum LocalMemType implements com.nativelibs4java.util.ValuedEnum {

        /** implying dedicated local memory storage such as SRAM */
        Local(CL_LOCAL),
        Global(CL_GLOBAL);

        LocalMemType(long value) { this.value = value; }
        long value;
        @Override
		public long value() { return value; }
        
        public static LocalMemType getEnum(long v) {
            return EnumValues.getEnum(v, LocalMemType.class);
        }
    }

    /**
     * Type of local memory supported. <br/>
     */
    @InfoName("CL_DEVICE_LOCAL_MEM_TYPE")
    public LocalMemType getLocalMemType() {
        return LocalMemType.getEnum(infos.getInt(getEntity(), CL_DEVICE_LOCAL_MEM_TYPE));
    }

    /**
     * Size of local memory arena in bytes. <br/>
     * The minimum value is 16 KB.
     */
    @InfoName("CL_DEVICE_LOCAL_MEM_SIZE")
    public long getLocalMemSize() {
        return infos.getIntOrLong(getEntity(), CL_DEVICE_LOCAL_MEM_SIZE);
    }

    /**
     * Is CL_TRUE if the device implements error correction for the memories, caches, registers etc. in the device. <br/>
     * Is CL_FALSE if the device does not implement error correction. <br/>
     * This can be a requirement for certain clients of OpenCL.
     */
    @InfoName("CL_DEVICE_ERROR_CORRECTION_SUPPORT")
    public boolean hasErrorCorrectionSupport() {
        return infos.getBool(getEntity(), CL_DEVICE_ERROR_CORRECTION_SUPPORT);
    }

    /**
     * Describes the resolution of device timer. <br/>
     * This is measured in nanoseconds.	<br/>
     * Refer to section 5.9 for details.
     */
    @InfoName("CL_DEVICE_PROFILING_TIMER_RESOLUTION")
    public long getProfilingTimerResolution() {
        return infos.getIntOrLong(getEntity(), CL_DEVICE_PROFILING_TIMER_RESOLUTION);
    }

    /**
     * Is CL_TRUE if the OpenCL device is a little endian device and CL_FALSE otherwise.
     */
    @InfoName("CL_DEVICE_ENDIAN_LITTLE")
    public boolean isEndianLittle() {
        return infos.getBool(getEntity(), CL_DEVICE_ENDIAN_LITTLE);
    }

    /**
     * Is CL_TRUE if the device is available and CL_FALSE if the device is not available.
     */
    @InfoName("CL_DEVICE_AVAILABLE")
    public boolean isAvailable() {
        return infos.getBool(getEntity(), CL_DEVICE_AVAILABLE);
    }

    /**
     * Is CL_FALSE if the implementation does not have a compiler available to compile the program source. <br/>
     * Is CL_TRUE if the compiler is available.<br/>
     * This can be CL_FALSE for the embededed platform profile only.
     */
    @InfoName("CL_DEVICE_COMPILER_AVAILABLE")
    public boolean isCompilerAvailable() {
        return infos.getBool(getEntity(), CL_DEVICE_COMPILER_AVAILABLE);
    }

    /**
    Device name string.
     */
    @InfoName("CL_DEVICE_NAME")
    public String getName() {
        return infos.getString(getEntity(), CL_DEVICE_NAME);
    }

    /**
     * OpenCL C version string. <br/>
     * Returns the highest OpenCL C version supported by the compiler for this device. <br/>
     * This version string has the following format:<br/>
     *  OpenCL&lt;space&gt;C&lt;space&gt;&lt;major_version.minor_version&gt;&lt;space&gt;&lt;vendor-specific information&gt;<br/>
     *  The major_version.minor_version value returned must be 1.1 if CL_DEVICE_VERSION is OpenCL 1.1.<br/>
     *  The major_version.minor_version value returned can be 1.0 or 1.1 if CL_DEVICE_VERSION is OpenCL 1.0. <br/>
     *  If OpenCL C 1.1 is returned, this implies that the language feature set defined in section 6 of the OpenCL 1.1 specification is supported by the OpenCL 1.0 device.
     *  @since OpenCL 1.1
     */
    @InfoName("CL_DEVICE_OPENCL_C_VERSION")
    public String getOpenCLVersion() {
    	try {
    		return infos.getString(getEntity(), CL_DEVICE_OPENCL_C_VERSION);
    	} catch (Throwable th) {
    		// TODO throw if supposed to handle OpenCL 1.1
    		return "OpenCL C 1.0";
    	}
    }
    
    /**
    Vendor name string.
     */
    @InfoName("CL_DEVICE_VENDOR")
    public String getVendor() {
        return infos.getString(getEntity(), CL_DEVICE_VENDOR);
    }

    /**
    OpenCL software driver version string in the form major_number.minor_number.
     */
    @InfoName("CL_DRIVER_VERSION")
    public String getDriverVersion() {
        return infos.getString(getEntity(), CL_DRIVER_VERSION);
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
        return infos.getString(getEntity(), CL_DEVICE_PROFILE);
    }

    /**
     * Whether the device and the host have a unified memory subsystem.
     * @since OpenCL 1.1
     */
    @InfoName("CL_DEVICE_HOST_UNIFIED_MEMORY")
    public boolean isHostUnifiedMemory() {
    	try {
    		return infos.getBool(getEntity(), CL_DEVICE_HOST_UNIFIED_MEMORY);
    	} catch (Throwable th) {
    		// TODO throw if supposed to handle OpenCL 1.1
    		return false;
    	}
    }
    
    /**
     * Preferred native vector width size for built-in scalar types that can be put into vectors. <br/>
     * The vector width is defined as the number of scalar elements that can be stored in the vector. <br/>
     * If the cl_khr_fp64 extension is not supported, CL_DEVICE_PREFERRED_VECTOR_WID TH_DOUBLE must return 0.
     */
    @InfoName("CL_DEVICE_PREFERRED_VECTOR_WIDTH_CHAR")
    public int getPreferredVectorWidthChar() {
        return infos.getInt(getEntity(), CL_DEVICE_PREFERRED_VECTOR_WIDTH_CHAR);
    }

    /**
     * Preferred native vector width size for built-in scalar types that can be put into vectors. <br/>
     * The vector width is defined as the number of scalar elements that can be stored in the vector. <br/>
     * If the cl_khr_fp64 extension is not supported, CL_DEVICE_PREFERRED_VECTOR_WID TH_DOUBLE must return 0.
     */
    @InfoName("CL_DEVICE_PREFERRED_VECTOR_WIDTH_SHORT")
    public int getPreferredVectorWidthShort() {
        return infos.getInt(getEntity(), CL_DEVICE_PREFERRED_VECTOR_WIDTH_SHORT);
    }

    /**
     * Preferred native vector width size for built-in scalar types that can be put into vectors. <br/>
     * The vector width is defined as the number of scalar elements that can be stored in the vector. <br/>
     * If the cl_khr_fp64 extension is not supported, CL_DEVICE_PREFERRED_VECTOR_WID TH_DOUBLE must return 0.
     */
    @InfoName("CL_DEVICE_PREFERRED_VECTOR_WIDTH_INT")
    public int getPreferredVectorWidthInt() {
        return infos.getInt(getEntity(), CL_DEVICE_PREFERRED_VECTOR_WIDTH_INT);
    }

    /**
     * Preferred native vector width size for built-in scalar types that can be put into vectors. <br/>
     * The vector width is defined as the number of scalar elements that can be stored in the vector. <br/>
     * If the cl_khr_fp64 extension is not supported, CL_DEVICE_PREFERRED_VECTOR_WID TH_DOUBLE must return 0.
     */
    @InfoName("CL_DEVICE_PREFERRED_VECTOR_WIDTH_LONG")
    public int getPreferredVectorWidthLong() {
        return infos.getInt(getEntity(), CL_DEVICE_PREFERRED_VECTOR_WIDTH_LONG);
    }

    /**
     * Preferred native vector width size for built-in scalar types that can be put into vectors. <br/>
     * The vector width is defined as the number of scalar elements that can be stored in the vector. <br/>
     * If the cl_khr_fp64 extension is not supported, CL_DEVICE_PREFERRED_VECTOR_WID TH_DOUBLE must return 0.
     */
    @InfoName("CL_DEVICE_PREFERRED_VECTOR_WIDTH_FLOAT")
    public int getPreferredVectorWidthFloat() {
        return infos.getInt(getEntity(), CL_DEVICE_PREFERRED_VECTOR_WIDTH_FLOAT);
    }

    /**
     * Preferred native vector width size for built-in scalar types that can be put into vectors. <br/>
     * The vector width is defined as the number of scalar elements that can be stored in the vector. <br/>
     * If the cl_khr_fp64 extension is not supported, CL_DEVICE_PREFERRED_VECTOR_WID TH_DOUBLE must return 0.
     */
    @InfoName("CL_DEVICE_PREFERRED_VECTOR_WIDTH_DOUBLE")
    public int getPreferredVectorWidthDouble() {
        return infos.getInt(getEntity(), CL_DEVICE_PREFERRED_VECTOR_WIDTH_DOUBLE);
    }
    
    /**
     * Returns the native ISA vector width. <br/>
     * The vector width is defined as the number of scalar elements that can be stored in the vector. <br/>
     * If the cl_khr_fp64 extension is not supported, CL_DEVICE_NATIVE_VECTOR_WID TH_DOUBLE must return 0.
     */
    @InfoName("CL_DEVICE_NATIVE_VECTOR_WIDTH_CHAR")
    public int getNativeVectorWidthChar() {
        return infos.getOptionalFeatureInt(getEntity(), CL_DEVICE_NATIVE_VECTOR_WIDTH_CHAR);
    }

    /**
     * Returns the native ISA vector width. <br/>
     * The vector width is defined as the number of scalar elements that can be stored in the vector. <br/>
     * If the cl_khr_fp64 extension is not supported, CL_DEVICE_NATIVE_VECTOR_WID TH_DOUBLE must return 0.
     */
    @InfoName("CL_DEVICE_NATIVE_VECTOR_WIDTH_SHORT")
    public int getNativeVectorWidthShort() {
        return infos.getOptionalFeatureInt(getEntity(), CL_DEVICE_NATIVE_VECTOR_WIDTH_SHORT);
    }

    /**
     * Returns the native ISA vector width. <br/>
     * The vector width is defined as the number of scalar elements that can be stored in the vector. <br/>
     * If the cl_khr_fp64 extension is not supported, CL_DEVICE_NATIVE_VECTOR_WID TH_DOUBLE must return 0.
     */
    @InfoName("CL_DEVICE_NATIVE_VECTOR_WIDTH_INT")
    public int getNativeVectorWidthInt() {
        return infos.getOptionalFeatureInt(getEntity(), CL_DEVICE_NATIVE_VECTOR_WIDTH_INT);
    }

    /**
     * Returns the native ISA vector width. <br/>
     * The vector width is defined as the number of scalar elements that can be stored in the vector. <br/>
     * If the cl_khr_fp64 extension is not supported, CL_DEVICE_NATIVE_VECTOR_WID TH_DOUBLE must return 0.
     */
    @InfoName("CL_DEVICE_NATIVE_VECTOR_WIDTH_LONG")
    public int getNativeVectorWidthLong() {
        return infos.getOptionalFeatureInt(getEntity(), CL_DEVICE_NATIVE_VECTOR_WIDTH_LONG);
    }

    /**
     * Returns the native ISA vector width. <br/>
     * The vector width is defined as the number of scalar elements that can be stored in the vector. <br/>
     * If the cl_khr_fp64 extension is not supported, CL_DEVICE_NATIVE_VECTOR_WID TH_DOUBLE must return 0.
     */
    @InfoName("CL_DEVICE_NATIVE_VECTOR_WIDTH_FLOAT")
    public int getNativeVectorWidthFloat() {
        return infos.getOptionalFeatureInt(getEntity(), CL_DEVICE_NATIVE_VECTOR_WIDTH_FLOAT);
    }

    /**
     * Returns the native ISA vector width. <br/>
     * The vector width is defined as the number of scalar elements that can be stored in the vector. <br/>
     * If the cl_khr_fp64 extension is not supported, CL_DEVICE_NATIVE_VECTOR_WID TH_DOUBLE must return 0.
     */
    @InfoName("CL_DEVICE_NATIVE_VECTOR_WIDTH_DOUBLE")
    public int getNativeVectorWidthDouble() {
        return infos.getOptionalFeatureInt(getEntity(), CL_DEVICE_NATIVE_VECTOR_WIDTH_DOUBLE);
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
        return infos.getString(getEntity(), CL_DEVICE_VERSION);
    }

    /**
    Returns a space separated list of extension names (the extension names themselves do not contain any spaces). The list of extension names returned currently can include one or more of
     */
    @InfoName("CL_DEVICE_EXTENSIONS")
    public String[] getExtensions() {
        if (extensions == null) {
            extensions = infos.getString(getEntity(), CL_DEVICE_EXTENSIONS).split("\\s+");
        }
        return extensions;
    }
    private String[] extensions;

    boolean hasExtension(String name) {
        name = name.trim();
        for (String x : getExtensions()) {
            if (name.equals(x.trim())) {
                return true;
            }
        }
        return false;
    }

    public boolean isDoubleSupported() {
        return hasExtension("cl_khr_fp64") || hasExtension("cl_amd_fp64");
    }

    public boolean isHalfSupported() {
        return hasExtension("cl_khr_fp16");
    }

    public boolean isByteAddressableStoreSupported() {
        return hasExtension("cl_khr_byte_addressable_store");
    }

    public boolean isGLSharingSupported() {
        return hasExtension("cl_khr_gl_sharing") || hasExtension("cl_APPLE_gl_sharing");
    }
	
	
    public boolean isGlobalInt32BaseAtomicsSupported() {
        return hasExtension("cl_khr_global_int32_base_atomics");
    }
    public boolean isGlobalInt32ExtendedAtomicsSupported() {
        return hasExtension("cl_khr_global_int32_extended_atomics");
    }
    public boolean isLocalInt32BaseAtomicsSupported() {
        return hasExtension("cl_khr_local_int32_base_atomics");
    }
    public boolean isLocalInt32ExtendedAtomicsSupported() {
        return hasExtension("cl_khr_local_int32_extended_atomics");
    }

    /** Bit values for CL_DEVICE_QUEUE_PROPERTIES */
    public enum QueueProperties implements com.nativelibs4java.util.ValuedEnum {

        OutOfOrderExecModeEnable(CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE),
        ProfilingEnable(CL_QUEUE_PROFILING_ENABLE);

        QueueProperties(long value) { this.value = value; }
        long value;
        @Override
		public long value() { return value; }
        
        public static long getValue(EnumSet<QueueProperties> set) {
            return EnumValues.getValue(set);
        }

        public static EnumSet<QueueProperties> getEnumSet(long v) {
            return EnumValues.getEnumSet(v, QueueProperties.class);
        }
    }

    /**
     * Describes the command-queue properties supported by the device.<br/>
     * These properties are described in table 5.1.<br/>
     * The mandated minimum capability is: ProfilingEnable.
     */
    @InfoName("CL_DEVICE_QUEUE_PROPERTIES")
    public EnumSet<QueueProperties> getQueueProperties() {
        return QueueProperties.getEnumSet(infos.getIntOrLong(getEntity(), CL_DEVICE_QUEUE_PROPERTIES));
    }
}
