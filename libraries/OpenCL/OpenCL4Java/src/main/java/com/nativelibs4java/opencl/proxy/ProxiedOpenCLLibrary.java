/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nativelibs4java.opencl.proxy;

import com.nativelibs4java.opencl.library.IOpenCLLibrary;
import static com.nativelibs4java.opencl.library.IOpenCLLibrary.*;
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
public class ProxiedOpenCLLibrary extends AbstractOpenCLLibrary {
    private static Pointer<?> icdDispatchTable;
    public static void setIcdDispatchTable(long icdDispatchTablePeer) {
        ProxiedOpenCLLibrary.icdDispatchTable = pointerToAddress(icdDispatchTablePeer);
    }

    private static ProxiedOpenCLLibrary instance;
    public static synchronized IOpenCLLibrary getInstance() {
        if (instance == null) {
            List<IOpenCLLibrary> platforms = new ArrayList<IOpenCLLibrary>();
            for (Iterator<IOpenCLLibrary> it = ServiceLoader.load(IOpenCLLibrary.class).iterator(); it.hasNext();) {
                IOpenCLLibrary platform = it.next();
                platforms.add(platform);
            }
            instance = new ProxiedOpenCLLibrary(platforms);
        }
        return instance;
    }
    
    protected static class IcdEntity extends StructObject {
        @Field(0)
        public Pointer<?> icdDispatchTable = ProxiedOpenCLLibrary.icdDispatchTable;
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
    
    private final List<IOpenCLLibrary> platforms;
    private final List<PlatformId> platformIds;

    public ProxiedOpenCLLibrary(List<IOpenCLLibrary> platforms) {
        this.platforms = new ArrayList<IOpenCLLibrary>(platforms);
        
        List<PlatformId> platformIds = new ArrayList<PlatformId>();
        for (IOpenCLLibrary implementation : this.platforms) {
            PlatformId platformId = new PlatformId();
            platformId.platformIndex = platforms.size();
            BridJ.writeToNative(platformId);

            platforms.add(implementation);
            platformIds.add(platformId);
        }
        this.platformIds = platformIds;
    }

    
    protected IOpenCLLibrary getImplementation(long icdEntityPeer) {
        Pointer<IcdEntity> icdEntityPtr = getPointer(icdEntityPeer, IcdEntity.class);
        IcdEntity icdEntity = icdEntityPtr.get();
        if (!icdDispatchTable.equals(icdEntity.icdDispatchTable))
            throw new IllegalArgumentException("Not an ICD entity, or different ICD dispatch table: " + icdEntityPeer);
        IOpenCLLibrary implementation = platforms.get(icdEntity.platformIndex);
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
    public int clGetDeviceIDs(long cl_platform_id1, long cl_device_type1, int cl_uint1, long cl_device_idPtr1, long cl_uintPtr1) {
        IOpenCLLibrary implementation = getImplementation(cl_platform_id1);
        return implementation.clGetDeviceIDs(cl_platform_id1, cl_device_type1, cl_uint1, cl_device_idPtr1, cl_uintPtr1);
    }

    @Override
    public int clGetDeviceInfo(long cl_device_id1, int cl_device_info1, long size_t1, long voidPtr1, long size_tPtr1) {
        IOpenCLLibrary implementation = getImplementation(cl_device_id1);
        return implementation.clGetDeviceInfo(cl_device_id1, cl_device_info1, size_t1, voidPtr1, size_tPtr1); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clGetPlatformInfo(long cl_platform_id1, int cl_platform_info1, long size_t1, long voidPtr1, long size_tPtr1) {
        IOpenCLLibrary implementation = getImplementation(cl_platform_id1);
        return implementation.clGetPlatformInfo(cl_platform_id1, cl_platform_info1, size_t1, voidPtr1, size_tPtr1);
    }
    
    
    
}
