/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nativelibs4java.opencl.proxy;

import com.nativelibs4java.opencl.library.IOpenCLImplementation;
import static com.nativelibs4java.opencl.library.IOpenCLImplementation.*;
import com.nativelibs4java.opencl.library.OpenCLLibrary;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import org.bridj.BridJ;
import org.bridj.Pointer;
import static org.bridj.Pointer.*;
import static com.nativelibs4java.opencl.proxy.PointerUtils.*;
import org.bridj.SizeT;
import org.bridj.StructObject;
import org.bridj.ann.Field;
import org.bridj.ann.Ptr;

/**
 *
 * @author ochafik
 */
public class ProxiedOpenCLImplementation implements IOpenCLImplementation {
    
    private static Pointer<?> icdDispatchTable;
    public static void setIcdDispatchTable(long icdDispatchTablePeer) {
        ProxiedOpenCLImplementation.icdDispatchTable = pointerToAddress(icdDispatchTablePeer);
    }

    private static ProxiedOpenCLImplementation instance;
    public static synchronized IOpenCLImplementation getInstance() {
        if (instance == null) {
            List<IOpenCLImplementation> platforms = new ArrayList<IOpenCLImplementation>();
            for (Iterator<IOpenCLImplementation> it = ServiceLoader.load(IOpenCLImplementation.class).iterator(); it.hasNext();) {
                IOpenCLImplementation platform = it.next();
                platforms.add(platform);
            }
            instance = new ProxiedOpenCLImplementation(platforms);
        }
        return instance;
    }
    
    protected static class IcdEntity extends StructObject {
        @Field(0)
        public Pointer<?> icdDispatchTable = ProxiedOpenCLImplementation.icdDispatchTable;
        @Field(1)
        public int platformIndex;
        
        public IcdEntity() {
            super();
        }
        public IcdEntity(Pointer<? extends IcdEntity> peer, Object... targs) {
            super(peer);
        }
    }
    protected static class PlatformId extends IcdEntity {
        
        public PlatformId() {
            super();
        }
        public PlatformId(Pointer<? extends PlatformId> peer, Object... targs) {
            super(peer);
        }
        
    }
    
    private final List<IOpenCLImplementation> platforms;
    private final List<PlatformId> platformIds;

    public ProxiedOpenCLImplementation(List<IOpenCLImplementation> platforms) {
        this.platforms = new ArrayList<IOpenCLImplementation>(platforms);
        
        List<PlatformId> platformIds = new ArrayList<PlatformId>();
        for (IOpenCLImplementation implementation : this.platforms) {
            PlatformId platformId = new PlatformId();
            platformId.platformIndex = platforms.size();
            BridJ.writeToNative(platformId);

            platforms.add(implementation);
            platformIds.add(platformId);
        }
        this.platformIds = platformIds;
    }

    
    protected IOpenCLImplementation getImplementation(long icdEntityPeer) {
        Pointer<IcdEntity> icdEntityPtr = getPointer(icdEntityPeer, IcdEntity.class);
        IcdEntity icdEntity = icdEntityPtr.get();
        if (!icdDispatchTable.equals(icdEntity.icdDispatchTable))
            throw new IllegalArgumentException("Not an ICD entity, or different ICD dispatch table: " + icdEntityPeer);
        IOpenCLImplementation implementation = platforms.get(icdEntity.platformIndex);
        return implementation;
    }
    
    @Override
    public int clGetPlatformIDs(int num_entries, @Ptr long platforms, @Ptr long num_platforms) {
        System.out.println("Called clGetPlatformIDs");
        if ((platforms == 0 || num_entries == 0) && num_platforms == 0) {
            setSizeT(num_platforms, platformIds.size());
        } else if (platforms != 0 && num_entries != 0) {
            int numWrote = 0;
            for (int i = 0; i < num_entries; i++) {
                setPointerAtIndex(platforms, i, platformIds.get(i));
                numWrote++;
            }
            if (num_platforms != 0) {
                setSizeT(num_platforms, numWrote);
            }
        } else
            return CL_INVALID_VALUE;
        return CL_SUCCESS;
    }
    
    @Override
    public int clIcdGetPlatformIDsKHR(int num_entries, @Ptr long platforms, @Ptr long num_platforms) {
        // TODO
        return clGetPlatformIDs(num_entries, platforms, num_platforms);
    }

    

    
    @Override
    public int clGetDeviceIDs(long cl_platform_id1, long cl_device_type1, int cl_uint1, long cl_device_idPtr1, long cl_uintPtr1) {
        return getImplementation(cl_platform_id1).clGetDeviceIDs(cl_platform_id1, cl_device_type1, cl_uint1, cl_device_idPtr1, cl_uintPtr1);
    }

    @Override
    public int clGetDeviceInfo(long cl_device_id1, int cl_device_info1, long size_t1, long voidPtr1, long size_tPtr1) {
        return getImplementation(cl_device_id1).clGetDeviceInfo(cl_device_id1, cl_device_info1, size_t1, voidPtr1, size_tPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clGetPlatformInfo(long cl_platform_id1, int cl_platform_info1, long size_t1, long voidPtr1, long size_tPtr1) {
        return getImplementation(cl_platform_id1).clGetPlatformInfo(cl_platform_id1, cl_platform_info1, size_t1, voidPtr1, size_tPtr1);
    }

    @Override
    public int clBuildProgram(long cl_program1, int cl_uint1, long cl_device_idPtr1, long charPtr1, long arg1, long voidPtr1) {
        return getImplementation(cl_program1).clBuildProgram(cl_program1, cl_uint1, cl_device_idPtr1, charPtr1, arg1, voidPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clCompileProgram(cl_program cl_program1, int cl_uint1, Pointer<cl_device_id> cl_device_idPtr1, Pointer<Byte> charPtr1, int cl_uint2, Pointer<cl_program> cl_programPtr1, Pointer<Pointer<Byte>> charPtrPtr1, Pointer<OpenCLLibrary.clCompileProgram_arg1_callback> arg1, Pointer<?> voidPtr1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long clCreateBuffer(long cl_context1, long cl_mem_flags1, long size_t1, long voidPtr1, long cl_intPtr1) {
        return getImplementation(cl_context1).clCreateBuffer(cl_context1, cl_mem_flags1, size_t1, voidPtr1, cl_intPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public long clCreateCommandQueue(long cl_context1, long cl_device_id1, long cl_command_queue_properties1, long cl_intPtr1) {
        return getImplementation(cl_context1).clCreateCommandQueue(cl_context1, cl_device_id1, cl_command_queue_properties1, cl_intPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public long clCreateContext(long cl_context_propertiesPtr1, int cl_uint1, long cl_device_idPtr1, long arg1, long voidPtr1, long cl_intPtr1) {
        // TODO! 
        throw new UnsupportedOperationException();
//        return getImplementation(cl_context_propertiesPtr1).clCreateContext(cl_context_propertiesPtr1, cl_uint1, cl_device_idPtr1, arg1, voidPtr1, cl_intPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public cl_context clCreateContextFromType(Pointer<Pointer<Integer>> cl_context_propertiesPtr1, long cl_device_type1, Pointer<OpenCLLibrary.clCreateContextFromType_arg1_callback> arg1, Pointer<?> voidPtr1, Pointer<Integer> cl_intPtr1) {
        // TODO! 
        throw new UnsupportedOperationException();
//        return getImplementation(cl_program1).clCreateContextFromType(cl_context_propertiesPtr1, cl_device_type1, arg1, voidPtr1, cl_intPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public cl_event clCreateEventFromGLsyncKHR(cl_context cl_context1, cl_GLsync cl_GLsync1, Pointer<Integer> cl_intPtr1) {
        throw new UnsupportedOperationException();
//        return getImplementation(cl_program1).clCreateEventFromGLsyncKHR(cl_context1, cl_GLsync1, cl_intPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public long clCreateFromGLBuffer(long cl_context1, long cl_mem_flags1, int cl_GLuint1, long intPtr1) {
        return getImplementation(cl_context1).clCreateFromGLBuffer(cl_context1, cl_mem_flags1, cl_GLuint1, intPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public long clCreateFromGLRenderbuffer(long cl_context1, long cl_mem_flags1, int cl_GLuint1, long cl_intPtr1) {
        return getImplementation(cl_context1).clCreateFromGLRenderbuffer(cl_context1, cl_mem_flags1, cl_GLuint1, cl_intPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public long clCreateFromGLTexture(long cl_context1, long cl_mem_flags1, int cl_GLenum1, int cl_GLint1, int cl_GLuint1, long cl_intPtr1) {
        return getImplementation(cl_context1).clCreateFromGLTexture(cl_context1, cl_mem_flags1, cl_GLenum1, cl_GLint1, cl_GLuint1, cl_intPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public long clCreateFromGLTexture2D(long cl_context1, long cl_mem_flags1, int cl_GLenum1, int cl_GLint1, int cl_GLuint1, long cl_intPtr1) {
        return getImplementation(cl_context1).clCreateFromGLTexture2D(cl_context1, cl_mem_flags1, cl_GLenum1, cl_GLint1, cl_GLuint1, cl_intPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public long clCreateFromGLTexture3D(long cl_context1, long cl_mem_flags1, int cl_GLenum1, int cl_GLint1, int cl_GLuint1, long cl_intPtr1) {
        return getImplementation(cl_context1).clCreateFromGLTexture3D(cl_context1, cl_mem_flags1, cl_GLenum1, cl_GLint1, cl_GLuint1, cl_intPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public long clCreateImage(long cl_context1, long cl_mem_flags1, long cl_image_formatPtr1, long cl_image_descPtr1, long voidPtr1, long cl_intPtr1) {
        return getImplementation(cl_context1).clCreateImage(cl_context1, cl_mem_flags1, cl_image_formatPtr1, cl_image_descPtr1, voidPtr1, cl_intPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public long clCreateImage2D(long cl_context1, long cl_mem_flags1, long cl_image_formatPtr1, long size_t1, long size_t2, long size_t3, long voidPtr1, long cl_intPtr1) {
        return getImplementation(cl_context1).clCreateImage2D(cl_context1, cl_mem_flags1, cl_image_formatPtr1, size_t1, size_t2, size_t3, voidPtr1, cl_intPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public long clCreateImage3D(long cl_context1, long cl_mem_flags1, long cl_image_formatPtr1, long size_t1, long size_t2, long size_t3, long size_t4, long size_t5, long voidPtr1, long cl_intPtr1) {
        return getImplementation(cl_context1).clCreateImage3D(cl_context1, cl_mem_flags1, cl_image_formatPtr1, size_t1, size_t2, size_t3, size_t4, size_t5, voidPtr1, cl_intPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public long clCreateKernel(long cl_program1, long charPtr1, long cl_intPtr1) {
        return getImplementation(cl_program1).clCreateKernel(cl_program1, charPtr1, cl_intPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clCreateKernelsInProgram(long cl_program1, int cl_uint1, long cl_kernelPtr1, long cl_uintPtr1) {
        return getImplementation(cl_program1).clCreateKernelsInProgram(cl_program1, cl_uint1, cl_kernelPtr1, cl_uintPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public long clCreateProgramWithBinary(long cl_context1, int cl_uint1, long cl_device_idPtr1, long size_tPtr1, long charPtrPtr1, long cl_intPtr1, long cl_intPtr2) {
        return getImplementation(cl_context1).clCreateProgramWithBinary(cl_context1, cl_uint1, cl_device_idPtr1, size_tPtr1, charPtrPtr1, cl_intPtr1, cl_intPtr2); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public cl_program clCreateProgramWithBuiltInKernels(cl_context cl_context1, int cl_uint1, Pointer<cl_device_id> cl_device_idPtr1, Pointer<Byte> charPtr1, Pointer<Integer> cl_intPtr1) {
        
        throw new UnsupportedOperationException();
//        return getImplementation(cl_program1).clCreateProgramWithBuiltInKernels(cl_context1, cl_uint1, cl_device_idPtr1, charPtr1, cl_intPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public long clCreateProgramWithSource(long cl_context1, int cl_uint1, long charPtrPtr1, long size_tPtr1, long cl_intPtr1) {
        return getImplementation(cl_context1).clCreateProgramWithSource(cl_context1, cl_uint1, charPtrPtr1, size_tPtr1, cl_intPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public long clCreateSampler(long cl_context1, int cl_bool1, int cl_addressing_mode1, int cl_filter_mode1, long cl_intPtr1) {
        return getImplementation(cl_context1).clCreateSampler(cl_context1, cl_bool1, cl_addressing_mode1, cl_filter_mode1, cl_intPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public long clCreateSubBuffer(long cl_mem1, long cl_mem_flags1, int cl_buffer_create_type1, long voidPtr1, long cl_intPtr1) {
        return getImplementation(cl_mem1).clCreateSubBuffer(cl_mem1, cl_mem_flags1, cl_buffer_create_type1, voidPtr1, cl_intPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clCreateSubDevices(cl_device_id cl_device_id1, Pointer<Pointer<Integer>> cl_device_partition_propertyPtr1, int cl_uint1, Pointer<cl_device_id> cl_device_idPtr1, Pointer<Integer> cl_uintPtr1) {
        
        throw new UnsupportedOperationException();
//        return getImplementation(cl_program1).clCreateSubDevices(cl_device_id1, cl_device_partition_propertyPtr1, cl_uint1, cl_device_idPtr1, cl_uintPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clCreateSubDevicesEXT(cl_device_id cl_device_id1, Pointer<Long> cl_device_partition_property_extPtr1, int cl_uint1, Pointer<cl_device_id> cl_device_idPtr1, Pointer<Integer> cl_uintPtr1) {
        
        throw new UnsupportedOperationException();
//        return getImplementation(cl_program1).clCreateSubDevicesEXT(cl_device_id1, cl_device_partition_property_extPtr1, cl_uint1, cl_device_idPtr1, cl_uintPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public long clCreateUserEvent(long cl_context1, long cl_intPtr1) {
        return getImplementation(cl_context1).clCreateUserEvent(cl_context1, cl_intPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clEnqueueAcquireGLObjects(long cl_command_queue1, int cl_uint1, long cl_memPtr1, int cl_uint2, long cl_eventPtr1, long cl_eventPtr2) {
        return getImplementation(cl_command_queue1).clEnqueueAcquireGLObjects(cl_command_queue1, cl_uint1, cl_memPtr1, cl_uint2, cl_eventPtr1, cl_eventPtr2); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clEnqueueBarrier(long cl_command_queue1) {
        return getImplementation(cl_command_queue1).clEnqueueBarrier(cl_command_queue1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clEnqueueBarrierWithWaitList(cl_command_queue cl_command_queue1, int cl_uint1, Pointer<cl_event> cl_eventPtr1, Pointer<cl_event> cl_eventPtr2) {
        
        throw new UnsupportedOperationException();
//        return getImplementation(cl_program1).clEnqueueBarrierWithWaitList(cl_command_queue1, cl_uint1, cl_eventPtr1, cl_eventPtr2); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clEnqueueCopyBuffer(long cl_command_queue1, long cl_mem1, long cl_mem2, long size_t1, long size_t2, long size_t3, int cl_uint1, long cl_eventPtr1, long cl_eventPtr2) {
        return getImplementation(cl_command_queue1).clEnqueueCopyBuffer(cl_command_queue1, cl_mem1, cl_mem2, size_t1, size_t2, size_t3, cl_uint1, cl_eventPtr1, cl_eventPtr2); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clEnqueueCopyBufferRect(cl_command_queue cl_command_queue1, cl_mem cl_mem1, cl_mem cl_mem2, Pointer<SizeT> size_tPtr1, Pointer<SizeT> size_tPtr2, Pointer<SizeT> size_tPtr3, long size_t1, long size_t2, long size_t3, long size_t4, int cl_uint1, Pointer<cl_event> cl_eventPtr1, Pointer<cl_event> cl_eventPtr2) {
        
        throw new UnsupportedOperationException();
//        return getImplementation(cl_program1).clEnqueueCopyBufferRect(cl_command_queue1, cl_mem1, cl_mem2, size_tPtr1, size_tPtr2, size_tPtr3, size_t1, size_t2, size_t3, size_t4, cl_uint1, cl_eventPtr1, cl_eventPtr2); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clEnqueueCopyBufferToImage(cl_command_queue cl_command_queue1, cl_mem cl_mem1, cl_mem cl_mem2, long size_t1, Pointer<SizeT> size_tPtr1, Pointer<SizeT> size_tPtr2, int cl_uint1, Pointer<cl_event> cl_eventPtr1, Pointer<cl_event> cl_eventPtr2) {
        
        throw new UnsupportedOperationException();
//        return getImplementation(cl_program1).clEnqueueCopyBufferToImage(cl_command_queue1, cl_mem1, cl_mem2, size_t1, size_tPtr1, size_tPtr2, cl_uint1, cl_eventPtr1, cl_eventPtr2); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clEnqueueCopyImage(cl_command_queue cl_command_queue1, cl_mem cl_mem1, cl_mem cl_mem2, Pointer<SizeT> size_tPtr1, Pointer<SizeT> size_tPtr2, Pointer<SizeT> size_tPtr3, int cl_uint1, Pointer<cl_event> cl_eventPtr1, Pointer<cl_event> cl_eventPtr2) {
        
        throw new UnsupportedOperationException();
//        return getImplementation(cl_program1).clEnqueueCopyImage(cl_command_queue1, cl_mem1, cl_mem2, size_tPtr1, size_tPtr2, size_tPtr3, cl_uint1, cl_eventPtr1, cl_eventPtr2); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clEnqueueCopyImageToBuffer(cl_command_queue cl_command_queue1, cl_mem cl_mem1, cl_mem cl_mem2, Pointer<SizeT> size_tPtr1, Pointer<SizeT> size_tPtr2, long size_t1, int cl_uint1, Pointer<cl_event> cl_eventPtr1, Pointer<cl_event> cl_eventPtr2) {
        
        throw new UnsupportedOperationException();
//        return getImplementation(cl_program1).clEnqueueCopyImageToBuffer(cl_command_queue1, cl_mem1, cl_mem2, size_tPtr1, size_tPtr2, size_t1, cl_uint1, cl_eventPtr1, cl_eventPtr2); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clEnqueueFillBuffer(cl_command_queue cl_command_queue1, cl_mem cl_mem1, Pointer<?> voidPtr1, long size_t1, long size_t2, long size_t3, int cl_uint1, Pointer<cl_event> cl_eventPtr1, Pointer<cl_event> cl_eventPtr2) {
        
        throw new UnsupportedOperationException();
//        return getImplementation(cl_program1).clEnqueueFillBuffer(cl_command_queue1, cl_mem1, voidPtr1, size_t1, size_t2, size_t3, cl_uint1, cl_eventPtr1, cl_eventPtr2); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clEnqueueFillImage(cl_command_queue cl_command_queue1, cl_mem cl_mem1, Pointer<?> voidPtr1, Pointer<SizeT> size_tPtr1, Pointer<SizeT> size_tPtr2, int cl_uint1, Pointer<cl_event> cl_eventPtr1, Pointer<cl_event> cl_eventPtr2) {
        
        throw new UnsupportedOperationException();
//        return getImplementation(cl_program1).clEnqueueFillImage(cl_command_queue1, cl_mem1, voidPtr1, size_tPtr1, size_tPtr2, cl_uint1, cl_eventPtr1, cl_eventPtr2); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public long clEnqueueMapBuffer(long cl_command_queue1, long cl_mem1, int cl_bool1, long cl_map_flags1, long size_t1, long size_t2, int cl_uint1, long cl_eventPtr1, long cl_eventPtr2, long cl_intPtr1) {
        return getImplementation(cl_command_queue1).clEnqueueMapBuffer(cl_command_queue1, cl_mem1, cl_bool1, cl_map_flags1, size_t1, size_t2, cl_uint1, cl_eventPtr1, cl_eventPtr2, cl_intPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public long clEnqueueMapImage(long cl_command_queue1, long cl_mem1, int cl_bool1, long cl_map_flags1, long size_tPtr1, long size_tPtr2, long size_tPtr3, long size_tPtr4, int cl_uint1, long cl_eventPtr1, long cl_eventPtr2, long cl_intPtr1) {
        return getImplementation(cl_command_queue1).clEnqueueMapImage(cl_command_queue1, cl_mem1, cl_bool1, cl_map_flags1, size_tPtr1, size_tPtr2, size_tPtr3, size_tPtr4, cl_uint1, cl_eventPtr1, cl_eventPtr2, cl_intPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clEnqueueMarker(long cl_command_queue1, long cl_eventPtr1) {
        return getImplementation(cl_command_queue1).clEnqueueMarker(cl_command_queue1, cl_eventPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clEnqueueMarkerWithWaitList(cl_command_queue cl_command_queue1, int cl_uint1, Pointer<cl_event> cl_eventPtr1, Pointer<cl_event> cl_eventPtr2) {
        
        throw new UnsupportedOperationException();
//        return getImplementation(cl_program1).clEnqueueMarkerWithWaitList(cl_command_queue1, cl_uint1, cl_eventPtr1, cl_eventPtr2); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clEnqueueMigrateMemObjects(cl_command_queue cl_command_queue1, int cl_uint1, Pointer<cl_mem> cl_memPtr1, long cl_mem_migration_flags1, int cl_uint2, Pointer<cl_event> cl_eventPtr1, Pointer<cl_event> cl_eventPtr2) {
        
        throw new UnsupportedOperationException();
//        return getImplementation(cl_program1).clEnqueueMigrateMemObjects(cl_command_queue1, cl_uint1, cl_memPtr1, cl_mem_migration_flags1, cl_uint2, cl_eventPtr1, cl_eventPtr2); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clEnqueueNDRangeKernel(long cl_command_queue1, long cl_kernel1, int cl_uint1, long size_tPtr1, long size_tPtr2, long size_tPtr3, int cl_uint2, long cl_eventPtr1, long cl_eventPtr2) {
        
        throw new UnsupportedOperationException();
//        return getImplementation(cl_program1).clEnqueueNDRangeKernel(cl_command_queue1, cl_kernel1, cl_uint1, size_tPtr1, size_tPtr2, size_tPtr3, cl_uint2, cl_eventPtr1, cl_eventPtr2); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clEnqueueNativeKernel(cl_command_queue cl_command_queue1, Pointer<OpenCLLibrary.clEnqueueNativeKernel_arg1_callback> arg1, Pointer<?> voidPtr1, long size_t1, int cl_uint1, Pointer<cl_mem> cl_memPtr1, Pointer<Pointer<?>> voidPtrPtr1, int cl_uint2, Pointer<cl_event> cl_eventPtr1, Pointer<cl_event> cl_eventPtr2) {
        
        throw new UnsupportedOperationException();
//        return getImplementation(cl_program1).clEnqueueNativeKernel(cl_command_queue1, arg1, voidPtr1, size_t1, cl_uint1, cl_memPtr1, voidPtrPtr1, cl_uint2, cl_eventPtr1, cl_eventPtr2); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clEnqueueReadBuffer(long cl_command_queue1, long cl_mem1, int cl_bool1, long size_t1, long size_t2, long voidPtr1, int cl_uint1, long cl_eventPtr1, long cl_eventPtr2) {
        return getImplementation(cl_command_queue1).clEnqueueReadBuffer(cl_command_queue1, cl_mem1, cl_bool1, size_t1, size_t2, voidPtr1, cl_uint1, cl_eventPtr1, cl_eventPtr2); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clEnqueueReadBufferRect(cl_command_queue cl_command_queue1, cl_mem cl_mem1, int cl_bool1, Pointer<SizeT> size_tPtr1, Pointer<SizeT> size_tPtr2, Pointer<SizeT> size_tPtr3, long size_t1, long size_t2, long size_t3, long size_t4, Pointer<?> voidPtr1, int cl_uint1, Pointer<cl_event> cl_eventPtr1, Pointer<cl_event> cl_eventPtr2) {
        
        throw new UnsupportedOperationException();
//        return getImplementation(cl_program1).clEnqueueReadBufferRect(cl_command_queue1, cl_mem1, cl_bool1, size_tPtr1, size_tPtr2, size_tPtr3, size_t1, size_t2, size_t3, size_t4, voidPtr1, cl_uint1, cl_eventPtr1, cl_eventPtr2); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clEnqueueReadImage(long cl_command_queue1, long cl_mem1, int cl_bool1, long size_tPtr1, long size_tPtr2, long size_t1, long size_t2, long voidPtr1, int cl_uint1, long cl_eventPtr1, long cl_eventPtr2) {
        return getImplementation(cl_command_queue1).clEnqueueReadImage(cl_command_queue1, cl_mem1, cl_bool1, size_tPtr1, size_tPtr2, size_t1, size_t2, voidPtr1, cl_uint1, cl_eventPtr1, cl_eventPtr2); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clEnqueueReleaseGLObjects(long cl_command_queue1, int cl_uint1, long cl_memPtr1, int cl_uint2, long cl_eventPtr1, long cl_eventPtr2) {
        return getImplementation(cl_command_queue1).clEnqueueReleaseGLObjects(cl_command_queue1, cl_uint1, cl_memPtr1, cl_uint2, cl_eventPtr1, cl_eventPtr2); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clEnqueueTask(long cl_command_queue1, long cl_kernel1, int cl_uint1, long cl_eventPtr1, long cl_eventPtr2) {
        return getImplementation(cl_command_queue1).clEnqueueTask(cl_command_queue1, cl_kernel1, cl_uint1, cl_eventPtr1, cl_eventPtr2); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clEnqueueUnmapMemObject(long cl_command_queue1, long cl_mem1, long voidPtr1, int cl_uint1, long cl_eventPtr1, long cl_eventPtr2) {
        return getImplementation(cl_command_queue1).clEnqueueUnmapMemObject(cl_command_queue1, cl_mem1, voidPtr1, cl_uint1, cl_eventPtr1, cl_eventPtr2); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clEnqueueWaitForEvents(long cl_command_queue1, int cl_uint1, long cl_eventPtr1) {
        return getImplementation(cl_command_queue1).clEnqueueWaitForEvents(cl_command_queue1, cl_uint1, cl_eventPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clEnqueueWriteBuffer(long cl_command_queue1, long cl_mem1, int cl_bool1, long size_t1, long size_t2, long voidPtr1, int cl_uint1, long cl_eventPtr1, long cl_eventPtr2) {
        return getImplementation(cl_command_queue1).clEnqueueWriteBuffer(cl_command_queue1, cl_mem1, cl_bool1, size_t1, size_t2, voidPtr1, cl_uint1, cl_eventPtr1, cl_eventPtr2); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clEnqueueWriteBufferRect(cl_command_queue cl_command_queue1, cl_mem cl_mem1, int cl_bool1, Pointer<SizeT> size_tPtr1, Pointer<SizeT> size_tPtr2, Pointer<SizeT> size_tPtr3, long size_t1, long size_t2, long size_t3, long size_t4, Pointer<?> voidPtr1, int cl_uint1, Pointer<cl_event> cl_eventPtr1, Pointer<cl_event> cl_eventPtr2) {
        return getImplementation(getPeer(cl_command_queue1)).clEnqueueWriteBufferRect(cl_command_queue1, cl_mem1, cl_bool1, size_tPtr1, size_tPtr2, size_tPtr3, size_t1, size_t2, size_t3, size_t4, voidPtr1, cl_uint1, cl_eventPtr1, cl_eventPtr2); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clEnqueueWriteImage(long cl_command_queue1, long cl_mem1, int cl_bool1, long size_tPtr1, long size_tPtr2, long size_t1, long size_t2, long voidPtr1, int cl_uint1, long cl_eventPtr1, long cl_eventPtr2) {
        return getImplementation(cl_command_queue1).clEnqueueWriteImage(cl_command_queue1, cl_mem1, cl_bool1, size_tPtr1, size_tPtr2, size_t1, size_t2, voidPtr1, cl_uint1, cl_eventPtr1, cl_eventPtr2); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clFinish(long cl_command_queue1) {
        return getImplementation(cl_command_queue1).clFinish(cl_command_queue1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clFlush(long cl_command_queue1) {
        return getImplementation(cl_command_queue1).clFlush(cl_command_queue1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clGetCommandQueueInfo(long cl_command_queue1, int cl_command_queue_info1, long size_t1, long voidPtr1, long size_tPtr1) {
        return getImplementation(cl_command_queue1).clGetCommandQueueInfo(cl_command_queue1, cl_command_queue_info1, size_t1, voidPtr1, size_tPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clGetContextInfo(long cl_context1, int cl_context_info1, long size_t1, long voidPtr1, long size_tPtr1) {
        return getImplementation(cl_context1).clGetContextInfo(cl_context1, cl_context_info1, size_t1, voidPtr1, size_tPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clGetEventInfo(long cl_event1, int cl_event_info1, long size_t1, long voidPtr1, long size_tPtr1) {
        return getImplementation(cl_event1).clGetEventInfo(cl_event1, cl_event_info1, size_t1, voidPtr1, size_tPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clGetEventProfilingInfo(long cl_event1, int cl_profiling_info1, long size_t1, long voidPtr1, long size_tPtr1) {
        return getImplementation(cl_event1).clGetEventProfilingInfo(cl_event1, cl_profiling_info1, size_t1, voidPtr1, size_tPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Pointer<?> clGetExtensionFunctionAddress(Pointer<Byte> charPtr1) {
        // TODO
        return null;
    }

    @Override
    public Pointer<?> clGetExtensionFunctionAddressForPlatform(cl_platform_id cl_platform_id1, Pointer<Byte> charPtr1) {
        return getImplementation(getPeer(cl_platform_id1)).clGetExtensionFunctionAddressForPlatform(cl_platform_id1, charPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clGetGLContextInfoAPPLE(long cl_context1, long voidPtr1, int cl_gl_platform_info1, long size_t1, long voidPtr2, long size_tPtr1) {
        return getImplementation(cl_context1).clGetGLContextInfoAPPLE(cl_context1, voidPtr1, cl_gl_platform_info1, size_t1, voidPtr2, size_tPtr1);
    }

    @Override
    public int clGetGLContextInfoKHR(long cl_context_propertiesPtr1, int cl_gl_context_info1, long size_t1, long voidPtr1, long size_tPtr1) {
        // TODO
        throw new UnsupportedOperationException();
//        return getImplementation(cl_program1).clGetGLContextInfoKHR(cl_context_propertiesPtr1, cl_gl_context_info1, size_t1, voidPtr1, size_tPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clGetGLObjectInfo(long cl_mem1, long cl_gl_object_typePtr1, long cl_GLuintPtr1) {
        return getImplementation(cl_mem1).clGetGLObjectInfo(cl_mem1, cl_gl_object_typePtr1, cl_GLuintPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clGetGLTextureInfo(long cl_mem1, int cl_gl_texture_info1, long size_t1, long voidPtr1, long size_tPtr1) {
        return getImplementation(cl_mem1).clGetGLTextureInfo(cl_mem1, cl_gl_texture_info1, size_t1, voidPtr1, size_tPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clGetImageInfo(long cl_mem1, int cl_image_info1, long size_t1, long voidPtr1, long size_tPtr1) {
        return getImplementation(cl_mem1).clGetImageInfo(cl_mem1, cl_image_info1, size_t1, voidPtr1, size_tPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clGetKernelArgInfo(long cl_kernel1, int cl_uint1, int cl_kernel_arg_info1, long size_t1, long voidPtr1, long size_tPtr1) {
        return getImplementation(cl_kernel1).clGetKernelArgInfo(cl_kernel1, cl_uint1, cl_kernel_arg_info1, size_t1, voidPtr1, size_tPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clGetKernelInfo(long cl_kernel1, int cl_kernel_info1, long size_t1, long voidPtr1, long size_tPtr1) {
        return getImplementation(cl_kernel1).clGetKernelInfo(cl_kernel1, cl_kernel_info1, size_t1, voidPtr1, size_tPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clGetKernelWorkGroupInfo(long cl_kernel1, long cl_device_id1, int cl_kernel_work_group_info1, long size_t1, long voidPtr1, long size_tPtr1) {
        return getImplementation(cl_kernel1).clGetKernelWorkGroupInfo(cl_kernel1, cl_device_id1, cl_kernel_work_group_info1, size_t1, voidPtr1, size_tPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clGetMemObjectInfo(long cl_mem1, int cl_mem_info1, long size_t1, long voidPtr1, long size_tPtr1) {
        return getImplementation(cl_mem1).clGetMemObjectInfo(cl_mem1, cl_mem_info1, size_t1, voidPtr1, size_tPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clGetProgramBuildInfo(long cl_program1, long cl_device_id1, int cl_program_build_info1, long size_t1, long voidPtr1, long size_tPtr1) {
        return getImplementation(cl_program1).clGetProgramBuildInfo(cl_program1, cl_device_id1, cl_program_build_info1, size_t1, voidPtr1, size_tPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clGetProgramInfo(long cl_program1, int cl_program_info1, long size_t1, long voidPtr1, long size_tPtr1) {
        return getImplementation(cl_program1).clGetProgramInfo(cl_program1, cl_program_info1, size_t1, voidPtr1, size_tPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clGetSamplerInfo(long cl_sampler1, int cl_sampler_info1, long size_t1, long voidPtr1, long size_tPtr1) {
        return getImplementation(cl_sampler1).clGetSamplerInfo(cl_sampler1, cl_sampler_info1, size_t1, voidPtr1, size_tPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clGetSupportedImageFormats(long cl_context1, long cl_mem_flags1, int cl_mem_object_type1, int cl_uint1, long cl_image_formatPtr1, long cl_uintPtr1) {
        return getImplementation(cl_context1).clGetSupportedImageFormats(cl_context1, cl_mem_flags1, cl_mem_object_type1, cl_uint1, cl_image_formatPtr1, cl_uintPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public cl_program clLinkProgram(cl_context cl_context1, int cl_uint1, Pointer<cl_device_id> cl_device_idPtr1, Pointer<Byte> charPtr1, int cl_uint2, Pointer<cl_program> cl_programPtr1, Pointer<OpenCLLibrary.clLinkProgram_arg1_callback> arg1, Pointer<?> voidPtr1, Pointer<Integer> cl_intPtr1) {
        return getImplementation(getPeer(cl_context1)).clLinkProgram(cl_context1, cl_uint1, cl_device_idPtr1, charPtr1, cl_uint2, cl_programPtr1, arg1, voidPtr1, cl_intPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void clLogMessagesToStderrAPPLE(Pointer<Byte> charPtr1, Pointer<?> voidPtr1, long size_t1, Pointer<?> voidPtr2) {
        throw new UnsupportedOperationException();
//        getImplementation(cl_program1).clLogMessagesToStderrAPPLE(charPtr1, voidPtr1, size_t1, voidPtr2); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void clLogMessagesToStdoutAPPLE(Pointer<Byte> charPtr1, Pointer<?> voidPtr1, long size_t1, Pointer<?> voidPtr2) {
        throw new UnsupportedOperationException();
//        getImplementation(cl_program1).clLogMessagesToStdoutAPPLE(charPtr1, voidPtr1, size_t1, voidPtr2); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void clLogMessagesToSystemLogAPPLE(Pointer<Byte> charPtr1, Pointer<?> voidPtr1, long size_t1, Pointer<?> voidPtr2) {
        throw new UnsupportedOperationException();
//        getImplementation(cl_program1).clLogMessagesToSystemLogAPPLE(charPtr1, voidPtr1, size_t1, voidPtr2); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clReleaseCommandQueue(long cl_command_queue1) {
        return getImplementation(cl_command_queue1).clReleaseCommandQueue(cl_command_queue1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clReleaseContext(long cl_context1) {
        return getImplementation(cl_context1).clReleaseContext(cl_context1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clReleaseDevice(long cl_device_id1) {
        return getImplementation(cl_device_id1).clReleaseDevice(cl_device_id1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clReleaseDeviceEXT(long cl_device_id1) {
        return getImplementation(cl_device_id1).clReleaseDeviceEXT(cl_device_id1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clReleaseEvent(long cl_event1) {
        return getImplementation(cl_event1).clReleaseEvent(cl_event1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clReleaseKernel(long cl_kernel1) {
        return getImplementation(cl_kernel1).clReleaseKernel(cl_kernel1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clReleaseMemObject(long cl_mem1) {
        return getImplementation(cl_mem1).clReleaseMemObject(cl_mem1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clReleaseProgram(long cl_program1) {
        return getImplementation(cl_program1).clReleaseProgram(cl_program1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clReleaseSampler(long cl_sampler1) {
        return getImplementation(cl_sampler1).clReleaseSampler(cl_sampler1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clRetainCommandQueue(long cl_command_queue1) {
        return getImplementation(cl_command_queue1).clRetainCommandQueue(cl_command_queue1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clRetainContext(long cl_context1) {
        return getImplementation(cl_context1).clRetainContext(cl_context1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clRetainDevice(long cl_device_id1) {
        return getImplementation(cl_device_id1).clRetainDevice(cl_device_id1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clRetainDeviceEXT(long cl_device_id1) {
        return getImplementation(cl_device_id1).clRetainDeviceEXT(cl_device_id1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clRetainEvent(long cl_event1) {
        return getImplementation(cl_event1).clRetainEvent(cl_event1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clRetainKernel(long cl_kernel1) {
        return getImplementation(cl_kernel1).clRetainKernel(cl_kernel1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clRetainMemObject(long cl_mem1) {
        return getImplementation(cl_mem1).clRetainMemObject(cl_mem1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clRetainProgram(long cl_program1) {
        return getImplementation(cl_program1).clRetainProgram(cl_program1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clRetainSampler(long cl_sampler1) {
        return getImplementation(cl_sampler1).clRetainSampler(cl_sampler1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clSetCommandQueueProperty(long cl_command_queue1, long cl_command_queue_properties1, int cl_bool1, long cl_command_queue_propertiesPtr1) {
        return getImplementation(cl_command_queue1).clSetCommandQueueProperty(cl_command_queue1, cl_command_queue_properties1, cl_bool1, cl_command_queue_propertiesPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clSetEventCallback(long cl_event1, int cl_int1, long arg1, long voidPtr1) {
        return getImplementation(cl_event1).clSetEventCallback(cl_event1, cl_int1, arg1, voidPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clSetKernelArg(long cl_kernel1, int cl_uint1, long size_t1, long voidPtr1) {
        return getImplementation(cl_kernel1).clSetKernelArg(cl_kernel1, cl_uint1, size_t1, voidPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clSetMemObjectDestructorAPPLE(cl_mem cl_mem1, Pointer<OpenCLLibrary.clSetMemObjectDestructorAPPLE_arg1_callback> arg1, Pointer<?> voidPtr1) {
        return getImplementation(getPeer(cl_mem1)).clSetMemObjectDestructorAPPLE(cl_mem1, arg1, voidPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clSetMemObjectDestructorCallback(long cl_mem1, long arg1, long voidPtr1) {
        return getImplementation(cl_mem1).clSetMemObjectDestructorCallback(cl_mem1, arg1, voidPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clSetPrintfCallback(cl_context cl_context1, Pointer<OpenCLLibrary.clSetPrintfCallback_arg1_callback> arg1, Pointer<?> voidPtr1) {
        return getImplementation(getPeer(cl_context1)).clSetPrintfCallback(cl_context1, arg1, voidPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clSetUserEventStatus(long cl_event1, int cl_int1) {
        return getImplementation(cl_event1).clSetUserEventStatus(cl_event1, cl_int1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clUnloadCompiler() {
        for (IOpenCLImplementation implementation : platforms) {
            int res = implementation.clUnloadCompiler();
            if (res != CL_SUCCESS)
                return res;
        }
        return CL_SUCCESS;
    }

    @Override
    public int clUnloadPlatformCompiler(long cl_platform_id1) {
        return getImplementation(cl_platform_id1).clUnloadPlatformCompiler(cl_platform_id1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clWaitForEvents(int cl_uint1, long cl_eventPtr1) {
        // TODO
        throw new UnsupportedOperationException();
//        return getImplementation(cl_program1).clWaitForEvents(cl_uint1, cl_eventPtr1); //To change body of generated methods, choose Tools | Templates.
    }
    
}
