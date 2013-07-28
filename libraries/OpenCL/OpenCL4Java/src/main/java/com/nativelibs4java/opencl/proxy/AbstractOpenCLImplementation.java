package com.nativelibs4java.opencl.proxy;

import com.nativelibs4java.opencl.library.IOpenCLLibrary;
import com.nativelibs4java.opencl.library.OpenCLLibrary;
import org.bridj.Pointer;
import org.bridj.SizeT;

public abstract class AbstractOpenCLImplementation implements IOpenCLLibrary {

    public int clBuildProgram(long cl_program1, int cl_uint1, long cl_device_idPtr1, long charPtr1, long arg1, long voidPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clCompileProgram(cl_program cl_program1, int cl_uint1, Pointer<cl_device_id> cl_device_idPtr1, Pointer<Byte> charPtr1, int cl_uint2, Pointer<cl_program> cl_programPtr1, Pointer<Pointer<Byte>> charPtrPtr1, Pointer<OpenCLLibrary.clCompileProgram_arg1_callback> arg1, Pointer<?> voidPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public long clCreateBuffer(long cl_context1, long cl_mem_flags1, long size_t1, long voidPtr1, long cl_intPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public long clCreateCommandQueue(long cl_context1, long cl_device_id1, long cl_command_queue_properties1, long cl_intPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public long clCreateContext(long cl_context_propertiesPtr1, int cl_uint1, long cl_device_idPtr1, long arg1, long voidPtr1, long cl_intPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public cl_context clCreateContextFromType(Pointer<Pointer<Integer>> cl_context_propertiesPtr1, long cl_device_type1, Pointer<OpenCLLibrary.clCreateContextFromType_arg1_callback> arg1, Pointer<?> voidPtr1, Pointer<Integer> cl_intPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public cl_event clCreateEventFromGLsyncKHR(cl_context cl_context1, cl_GLsync cl_GLsync1, Pointer<Integer> cl_intPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public long clCreateFromGLBuffer(long cl_context1, long cl_mem_flags1, int cl_GLuint1, long intPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public long clCreateFromGLRenderbuffer(long cl_context1, long cl_mem_flags1, int cl_GLuint1, long cl_intPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public long clCreateFromGLTexture(long cl_context1, long cl_mem_flags1, int cl_GLenum1, int cl_GLint1, int cl_GLuint1, long cl_intPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public long clCreateFromGLTexture2D(long cl_context1, long cl_mem_flags1, int cl_GLenum1, int cl_GLint1, int cl_GLuint1, long cl_intPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public long clCreateFromGLTexture3D(long cl_context1, long cl_mem_flags1, int cl_GLenum1, int cl_GLint1, int cl_GLuint1, long cl_intPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public long clCreateImage(long cl_context1, long cl_mem_flags1, long cl_image_formatPtr1, long cl_image_descPtr1, long voidPtr1, long cl_intPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public long clCreateImage2D(long cl_context1, long cl_mem_flags1, long cl_image_formatPtr1, long size_t1, long size_t2, long size_t3, long voidPtr1, long cl_intPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public long clCreateImage3D(long cl_context1, long cl_mem_flags1, long cl_image_formatPtr1, long size_t1, long size_t2, long size_t3, long size_t4, long size_t5, long voidPtr1, long cl_intPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public long clCreateKernel(long cl_program1, long charPtr1, long cl_intPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clCreateKernelsInProgram(long cl_program1, int cl_uint1, long cl_kernelPtr1, long cl_uintPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public long clCreateProgramWithBinary(long cl_context1, int cl_uint1, long cl_device_idPtr1, long size_tPtr1, long charPtrPtr1, long cl_intPtr1, long cl_intPtr2) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public cl_program clCreateProgramWithBuiltInKernels(cl_context cl_context1, int cl_uint1, Pointer<cl_device_id> cl_device_idPtr1, Pointer<Byte> charPtr1, Pointer<Integer> cl_intPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public long clCreateProgramWithSource(long cl_context1, int cl_uint1, long charPtrPtr1, long size_tPtr1, long cl_intPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public long clCreateSampler(long cl_context1, int cl_bool1, int cl_addressing_mode1, int cl_filter_mode1, long cl_intPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public long clCreateSubBuffer(long cl_mem1, long cl_mem_flags1, int cl_buffer_create_type1, long voidPtr1, long cl_intPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clCreateSubDevices(cl_device_id cl_device_id1, Pointer<Pointer<Integer>> cl_device_partition_propertyPtr1, int cl_uint1, Pointer<cl_device_id> cl_device_idPtr1, Pointer<Integer> cl_uintPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clCreateSubDevicesEXT(cl_device_id cl_device_id1, Pointer<Long> cl_device_partition_property_extPtr1, int cl_uint1, Pointer<cl_device_id> cl_device_idPtr1, Pointer<Integer> cl_uintPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public long clCreateUserEvent(long cl_context1, long cl_intPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clEnqueueAcquireGLObjects(long cl_command_queue1, int cl_uint1, long cl_memPtr1, int cl_uint2, long cl_eventPtr1, long cl_eventPtr2) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clEnqueueBarrier(long cl_command_queue1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clEnqueueBarrierWithWaitList(cl_command_queue cl_command_queue1, int cl_uint1, Pointer<cl_event> cl_eventPtr1, Pointer<cl_event> cl_eventPtr2) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clEnqueueCopyBuffer(long cl_command_queue1, long cl_mem1, long cl_mem2, long size_t1, long size_t2, long size_t3, int cl_uint1, long cl_eventPtr1, long cl_eventPtr2) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clEnqueueCopyBufferRect(cl_command_queue cl_command_queue1, cl_mem cl_mem1, cl_mem cl_mem2, Pointer<SizeT> size_tPtr1, Pointer<SizeT> size_tPtr2, Pointer<SizeT> size_tPtr3, long size_t1, long size_t2, long size_t3, long size_t4, int cl_uint1, Pointer<cl_event> cl_eventPtr1, Pointer<cl_event> cl_eventPtr2) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clEnqueueCopyBufferToImage(cl_command_queue cl_command_queue1, cl_mem cl_mem1, cl_mem cl_mem2, long size_t1, Pointer<SizeT> size_tPtr1, Pointer<SizeT> size_tPtr2, int cl_uint1, Pointer<cl_event> cl_eventPtr1, Pointer<cl_event> cl_eventPtr2) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clEnqueueCopyImage(cl_command_queue cl_command_queue1, cl_mem cl_mem1, cl_mem cl_mem2, Pointer<SizeT> size_tPtr1, Pointer<SizeT> size_tPtr2, Pointer<SizeT> size_tPtr3, int cl_uint1, Pointer<cl_event> cl_eventPtr1, Pointer<cl_event> cl_eventPtr2) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clEnqueueCopyImageToBuffer(cl_command_queue cl_command_queue1, cl_mem cl_mem1, cl_mem cl_mem2, Pointer<SizeT> size_tPtr1, Pointer<SizeT> size_tPtr2, long size_t1, int cl_uint1, Pointer<cl_event> cl_eventPtr1, Pointer<cl_event> cl_eventPtr2) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clEnqueueFillBuffer(cl_command_queue cl_command_queue1, cl_mem cl_mem1, Pointer<?> voidPtr1, long size_t1, long size_t2, long size_t3, int cl_uint1, Pointer<cl_event> cl_eventPtr1, Pointer<cl_event> cl_eventPtr2) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clEnqueueFillImage(cl_command_queue cl_command_queue1, cl_mem cl_mem1, Pointer<?> voidPtr1, Pointer<SizeT> size_tPtr1, Pointer<SizeT> size_tPtr2, int cl_uint1, Pointer<cl_event> cl_eventPtr1, Pointer<cl_event> cl_eventPtr2) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public long clEnqueueMapBuffer(long cl_command_queue1, long cl_mem1, int cl_bool1, long cl_map_flags1, long size_t1, long size_t2, int cl_uint1, long cl_eventPtr1, long cl_eventPtr2, long cl_intPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public long clEnqueueMapImage(long cl_command_queue1, long cl_mem1, int cl_bool1, long cl_map_flags1, long size_tPtr1, long size_tPtr2, long size_tPtr3, long size_tPtr4, int cl_uint1, long cl_eventPtr1, long cl_eventPtr2, long cl_intPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clEnqueueMarker(long cl_command_queue1, long cl_eventPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clEnqueueMarkerWithWaitList(cl_command_queue cl_command_queue1, int cl_uint1, Pointer<cl_event> cl_eventPtr1, Pointer<cl_event> cl_eventPtr2) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clEnqueueMigrateMemObjects(cl_command_queue cl_command_queue1, int cl_uint1, Pointer<cl_mem> cl_memPtr1, long cl_mem_migration_flags1, int cl_uint2, Pointer<cl_event> cl_eventPtr1, Pointer<cl_event> cl_eventPtr2) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clEnqueueNDRangeKernel(long cl_command_queue1, long cl_kernel1, int cl_uint1, long size_tPtr1, long size_tPtr2, long size_tPtr3, int cl_uint2, long cl_eventPtr1, long cl_eventPtr2) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clEnqueueNativeKernel(cl_command_queue cl_command_queue1, Pointer<OpenCLLibrary.clEnqueueNativeKernel_arg1_callback> arg1, Pointer<?> voidPtr1, long size_t1, int cl_uint1, Pointer<cl_mem> cl_memPtr1, Pointer<Pointer<?>> voidPtrPtr1, int cl_uint2, Pointer<cl_event> cl_eventPtr1, Pointer<cl_event> cl_eventPtr2) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clEnqueueReadBuffer(long cl_command_queue1, long cl_mem1, int cl_bool1, long size_t1, long size_t2, long voidPtr1, int cl_uint1, long cl_eventPtr1, long cl_eventPtr2) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clEnqueueReadBufferRect(cl_command_queue cl_command_queue1, cl_mem cl_mem1, int cl_bool1, Pointer<SizeT> size_tPtr1, Pointer<SizeT> size_tPtr2, Pointer<SizeT> size_tPtr3, long size_t1, long size_t2, long size_t3, long size_t4, Pointer<?> voidPtr1, int cl_uint1, Pointer<cl_event> cl_eventPtr1, Pointer<cl_event> cl_eventPtr2) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clEnqueueReadImage(long cl_command_queue1, long cl_mem1, int cl_bool1, long size_tPtr1, long size_tPtr2, long size_t1, long size_t2, long voidPtr1, int cl_uint1, long cl_eventPtr1, long cl_eventPtr2) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clEnqueueReleaseGLObjects(long cl_command_queue1, int cl_uint1, long cl_memPtr1, int cl_uint2, long cl_eventPtr1, long cl_eventPtr2) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clEnqueueTask(long cl_command_queue1, long cl_kernel1, int cl_uint1, long cl_eventPtr1, long cl_eventPtr2) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clEnqueueUnmapMemObject(long cl_command_queue1, long cl_mem1, long voidPtr1, int cl_uint1, long cl_eventPtr1, long cl_eventPtr2) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clEnqueueWaitForEvents(long cl_command_queue1, int cl_uint1, long cl_eventPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clEnqueueWriteBuffer(long cl_command_queue1, long cl_mem1, int cl_bool1, long size_t1, long size_t2, long voidPtr1, int cl_uint1, long cl_eventPtr1, long cl_eventPtr2) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clEnqueueWriteBufferRect(cl_command_queue cl_command_queue1, cl_mem cl_mem1, int cl_bool1, Pointer<SizeT> size_tPtr1, Pointer<SizeT> size_tPtr2, Pointer<SizeT> size_tPtr3, long size_t1, long size_t2, long size_t3, long size_t4, Pointer<?> voidPtr1, int cl_uint1, Pointer<cl_event> cl_eventPtr1, Pointer<cl_event> cl_eventPtr2) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clEnqueueWriteImage(long cl_command_queue1, long cl_mem1, int cl_bool1, long size_tPtr1, long size_tPtr2, long size_t1, long size_t2, long voidPtr1, int cl_uint1, long cl_eventPtr1, long cl_eventPtr2) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clFinish(long cl_command_queue1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clFlush(long cl_command_queue1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clGetCommandQueueInfo(long cl_command_queue1, int cl_command_queue_info1, long size_t1, long voidPtr1, long size_tPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clGetContextInfo(long cl_context1, int cl_context_info1, long size_t1, long voidPtr1, long size_tPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clGetDeviceIDs(long cl_platform_id1, long cl_device_type1, int cl_uint1, long cl_device_idPtr1, long cl_uintPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clGetDeviceInfo(long cl_device_id1, int cl_device_info1, long size_t1, long voidPtr1, long size_tPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clGetEventInfo(long cl_event1, int cl_event_info1, long size_t1, long voidPtr1, long size_tPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clGetEventProfilingInfo(long cl_event1, int cl_profiling_info1, long size_t1, long voidPtr1, long size_tPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public Pointer<?> clGetExtensionFunctionAddress(Pointer<Byte> charPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public Pointer<?> clGetExtensionFunctionAddressForPlatform(cl_platform_id cl_platform_id1, Pointer<Byte> charPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clGetGLContextInfoAPPLE(long cl_context1, long voidPtr1, int cl_gl_platform_info1, long size_t1, long voidPtr2, long size_tPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clGetGLContextInfoKHR(long cl_context_propertiesPtr1, int cl_gl_context_info1, long size_t1, long voidPtr1, long size_tPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clGetGLObjectInfo(long cl_mem1, long cl_gl_object_typePtr1, long cl_GLuintPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clGetGLTextureInfo(long cl_mem1, int cl_gl_texture_info1, long size_t1, long voidPtr1, long size_tPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clGetImageInfo(long cl_mem1, int cl_image_info1, long size_t1, long voidPtr1, long size_tPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clGetKernelArgInfo(long cl_kernel1, int cl_uint1, int cl_kernel_arg_info1, long size_t1, long voidPtr1, long size_tPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clGetKernelInfo(long cl_kernel1, int cl_kernel_info1, long size_t1, long voidPtr1, long size_tPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clGetKernelWorkGroupInfo(long cl_kernel1, long cl_device_id1, int cl_kernel_work_group_info1, long size_t1, long voidPtr1, long size_tPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clGetMemObjectInfo(long cl_mem1, int cl_mem_info1, long size_t1, long voidPtr1, long size_tPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clGetPlatformIDs(int cl_uint1, long cl_platform_idPtr1, long cl_uintPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clGetPlatformInfo(long cl_platform_id1, int cl_platform_info1, long size_t1, long voidPtr1, long size_tPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clGetProgramBuildInfo(long cl_program1, long cl_device_id1, int cl_program_build_info1, long size_t1, long voidPtr1, long size_tPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clGetProgramInfo(long cl_program1, int cl_program_info1, long size_t1, long voidPtr1, long size_tPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clGetSamplerInfo(long cl_sampler1, int cl_sampler_info1, long size_t1, long voidPtr1, long size_tPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clGetSupportedImageFormats(long cl_context1, long cl_mem_flags1, int cl_mem_object_type1, int cl_uint1, long cl_image_formatPtr1, long cl_uintPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clIcdGetPlatformIDsKHR(int cl_uint1, long cl_platform_idPtr1, long cl_uintPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public cl_program clLinkProgram(cl_context cl_context1, int cl_uint1, Pointer<cl_device_id> cl_device_idPtr1, Pointer<Byte> charPtr1, int cl_uint2, Pointer<cl_program> cl_programPtr1, Pointer<OpenCLLibrary.clLinkProgram_arg1_callback> arg1, Pointer<?> voidPtr1, Pointer<Integer> cl_intPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void clLogMessagesToStderrAPPLE(Pointer<Byte> charPtr1, Pointer<?> voidPtr1, long size_t1, Pointer<?> voidPtr2) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void clLogMessagesToStdoutAPPLE(Pointer<Byte> charPtr1, Pointer<?> voidPtr1, long size_t1, Pointer<?> voidPtr2) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void clLogMessagesToSystemLogAPPLE(Pointer<Byte> charPtr1, Pointer<?> voidPtr1, long size_t1, Pointer<?> voidPtr2) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clReleaseCommandQueue(long cl_command_queue1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clReleaseContext(long cl_context1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clReleaseDevice(long cl_device_id1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clReleaseDeviceEXT(long cl_device_id1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clReleaseEvent(long cl_event1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clReleaseKernel(long cl_kernel1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clReleaseMemObject(long cl_mem1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clReleaseProgram(long cl_program1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clReleaseSampler(long cl_sampler1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clRetainCommandQueue(long cl_command_queue1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clRetainContext(long cl_context1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clRetainDevice(long cl_device_id1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clRetainDeviceEXT(long cl_device_id1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clRetainEvent(long cl_event1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clRetainKernel(long cl_kernel1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clRetainMemObject(long cl_mem1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clRetainProgram(long cl_program1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clRetainSampler(long cl_sampler1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clSetCommandQueueProperty(long cl_command_queue1, long cl_command_queue_properties1, int cl_bool1, long cl_command_queue_propertiesPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clSetEventCallback(long cl_event1, int cl_int1, long arg1, long voidPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clSetKernelArg(long cl_kernel1, int cl_uint1, long size_t1, long voidPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clSetMemObjectDestructorAPPLE(cl_mem cl_mem1, Pointer<OpenCLLibrary.clSetMemObjectDestructorAPPLE_arg1_callback> arg1, Pointer<?> voidPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clSetMemObjectDestructorCallback(long cl_mem1, long arg1, long voidPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clSetPrintfCallback(cl_context cl_context1, Pointer<OpenCLLibrary.clSetPrintfCallback_arg1_callback> arg1, Pointer<?> voidPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clSetUserEventStatus(long cl_event1, int cl_int1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clUnloadCompiler() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clUnloadPlatformCompiler(long cl_platform_id1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int clWaitForEvents(int cl_uint1, long cl_eventPtr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
