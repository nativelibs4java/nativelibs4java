#parse("main/Header.vm")
package com.nativelibs4java.opencl;


import com.nativelibs4java.opencl.library.OpenGLContextUtils;
import com.nativelibs4java.util.EnumValue;
import com.nativelibs4java.util.EnumValues;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;

import org.bridj.*;
import org.bridj.ann.*;
import static org.bridj.Pointer.*;

import java.nio.ByteOrder;
import java.util.*;
import static com.nativelibs4java.opencl.JavaCL.*;
import static com.nativelibs4java.opencl.CLException.*;

/**
 * OpenCL implementation entry point.
 * see {@link JavaCL#listPlatforms() } 
 * @author Olivier Chafik
 */
public class CLPlatform extends CLAbstractEntity<cl_platform_id> {

    CLPlatform(cl_platform_id platform) {
        super(platform, true);
    }
    
    @Override
    protected cl_platform_id createEntityPointer(long peer) {
    	return new cl_platform_id(peer);
    }
    
    private static CLInfoGetter<cl_platform_id> infos = new CLInfoGetter<cl_platform_id>() {

        @Override
        protected int getInfo(cl_platform_id entity, int infoTypeEnum, long size, Pointer out, Pointer<SizeT> sizeOut) {
            return CL.clGetPlatformInfo(entity, infoTypeEnum, size, out, sizeOut);
        }
    };

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }
    StringBuilder toString(StringBuilder out) {
        out.
        		append(getName()). 
        		append(" {vendor: ").append(getVendor()).
        		append(", version: ").append(getVersion()).
        		append(", profile: ").append(getProfile()).
        		append(", extensions: ").append(Arrays.toString(getExtensions())).
        		append("}");
        	return out;
    }

    @Override
    protected void clear() {
    }

    /**
     * Lists all the devices of the platform
     * @param onlyAvailable if true, only returns devices that are available
     * see {@link CLPlatform#listDevices(CLDevice.Type, boolean) }
     */
    public CLDevice[] listAllDevices(boolean onlyAvailable) {
        return listDevices(CLDevice.Type.All, onlyAvailable);
    }

    /**
     * Lists all the GPU devices of the platform
     * @param onlyAvailable if true, only returns GPU devices that are available
     * see {@link CLPlatform#listDevices(CLDevice.Type, boolean) }
     */
    public CLDevice[] listGPUDevices(boolean onlyAvailable) {
        try {
            return listDevices(CLDevice.Type.GPU, onlyAvailable);
        } catch (CLException ex) {
            if (ex.getCode() == CL_DEVICE_NOT_FOUND) {
                return new CLDevice[0];
            }
            throw new RuntimeException("Unexpected OpenCL error", ex);
        }
    }

    /**
     * Lists all the CPU devices of the platform
     * @param onlyAvailable if true, only returns CPU devices that are available
     * see {@link CLPlatform#listDevices(CLDevice.Type, boolean) }
     */
    public CLDevice[] listCPUDevices(boolean onlyAvailable) {
        try {
            return listDevices(CLDevice.Type.CPU, onlyAvailable);
        } catch (CLException ex) {
            if (ex.getCode() == CL_DEVICE_NOT_FOUND) {
                return new CLDevice[0];
            }
            throw new RuntimeException("Unexpected OpenCL error", ex);
        }
    }

    private CLDevice[] getDevices(Pointer<cl_device_id> ids, boolean onlyAvailable) {
        int nDevs = (int)ids.getValidElements();
        CLDevice[] devices;
        if (onlyAvailable) {
            List<CLDevice> list = new ArrayList<CLDevice>(nDevs);
            for (int i = 0; i < nDevs; i++) {
                CLDevice device = new CLDevice(this, ids.get(i));
                if (device.isAvailable()) {
                    list.add(device);
                }
            }
            devices = list.toArray(new CLDevice[list.size()]);
        } else {
            devices = new CLDevice[nDevs];
            for (int i = 0; i < nDevs; i++) {
                devices[i] = new CLDevice(this, ids.get(i));
            }
        }
        return devices;
    }

    static long[] getContextProps(Map<ContextProperties, Object> contextProperties) {
        if (contextProperties == null)
            return null;
        final long[] properties = new long[contextProperties.size() * 2 + 1];
        int iProp = 0;
        for (Map.Entry<ContextProperties, Object> e : contextProperties.entrySet()) {
            //if (!(v instanceof Number)) throw new IllegalArgumentException("Invalid context property value for '" + e.getKey() + ": " + v);
            properties[iProp++] = e.getKey().value();
            Object v = e.getValue();
            if (v instanceof Number)
                properties[iProp++] = ((Number)v).longValue();
            else if (v instanceof Pointer)
                properties[iProp++] = ((Pointer)v).getPeer();
            else
                throw new IllegalArgumentException("Cannot convert value " + v + " to a context property value !");
        }
        //properties[iProp] = 0;
        return properties;
    }

    /**
     * Enums used to indicate how to choose the best CLDevice.
     */
    public enum DeviceFeature {
        /**
         * Prefer CPU devices (see {@link CLDevice#getType() })
         */
        CPU {
            Comparable extractValue(CLDevice device) {
                return device.getType().contains(CLDevice.Type.CPU) ? 1 : 0;
            }
        },
        /**
         * Prefer GPU devices (see {@link CLDevice#getType() })
         */
        GPU {
            Comparable extractValue(CLDevice device) {
                return device.getType().contains(CLDevice.Type.GPU) ? 1 : 0;
            }
        },
        /**
         * Prefer Accelerator devices (see {@link CLDevice#getType() })
         */
        Accelerator {
            Comparable extractValue(CLDevice device) {
                return device.getType().contains(CLDevice.Type.Accelerator) ? 1 : 0;
            }
        },
        /**
         * Prefer devices with the most compute units (see {@link CLDevice#getMaxComputeUnits() })
         */
        MaxComputeUnits {
            Comparable extractValue(CLDevice device) {
                return device.getMaxComputeUnits();
            }
        },
        /**
         * Prefer devices with the same byte ordering as the hosting platform (see {@link CLDevice#getKernelsDefaultByteOrder() })
         */
        NativeEndianness {
            Comparable extractValue(CLDevice device) {
                return device.getKernelsDefaultByteOrder() == ByteOrder.nativeOrder() ? 1 : 0;
            }
        },
        /**
         * Prefer devices that support double-precision float computations (see {@link CLDevice#isDoubleSupported() })
         */
        DoubleSupport {
            Comparable extractValue(CLDevice device) {
                return device.isDoubleSupported() ? 1 : 0;
            }
        },
        /**
         * Prefer devices that support images and with the most supported image formats (see {@link CLDevice#hasImageSupport() })
         */
        ImageSupport {
            Comparable extractValue(CLDevice device) {
                return device.hasImageSupport() ? 1 : 0;
            }
        },
        /**
         * Prefer devices that support out of order queues (see {@link CLDevice#hasOutOfOrderQueueSupport() })
         */
        OutOfOrderQueueSupport {
            Comparable extractValue(CLDevice device) {
                return device.hasOutOfOrderQueueSupport() ? 1 : 0;
            }
        },
        /**
         * Prefer devices with the greatest variety of supported image formats (see {@link CLContext#getSupportedImageFormats(CLMem.Flags, CLMem.ObjectType) })
         */
        MostImageFormats {
            Comparable extractValue(CLDevice device) {
                if (!device.hasImageSupport())
                    return 0;
                // TODO: fix that ugly hack ?
                CLContext context = JavaCL.createContext(null, device);
                try {
                    return (Integer)context.getSupportedImageFormats(CLMem.Flags.ReadWrite, CLMem.ObjectType.Image2D).length;
                } finally {
                    context.release();
                }
            }
        };

        Comparable extractValue(CLDevice device) {
            throw new RuntimeException();
        }
    }

    public static class DeviceComparator implements Comparator<CLDevice> {

        private final List<DeviceFeature> evals;
        public DeviceComparator(List<DeviceFeature> evals) {
            this.evals = evals;
        }

        @Override
        public int compare(CLDevice a, CLDevice b) {
            for (DeviceFeature eval : evals) {
                if (eval == null)
                    continue;
                
                Comparable va = eval.extractValue(a), vb = eval.extractValue(b);
                int c = va.compareTo(vb);
                if (c != 0)
                    return c;
            }
            return 0;
        }
        
    }
	public static CLDevice getBestDevice(List<DeviceFeature> evals, Collection<CLDevice> devices) {
        List<CLDevice> list = new ArrayList<CLDevice>(devices);
        Collections.sort(list, new DeviceComparator(evals));
        return !list.isEmpty() ? list.get(list.size() - 1) : null;
    }

    public CLDevice getBestDevice() {
        return getBestDevice(Arrays.asList(DeviceFeature.MaxComputeUnits), Arrays.asList(listGPUDevices(true)));
    }

    /** Bit values for CL_CONTEXT_PROPERTIES */
    public enum ContextProperties implements com.nativelibs4java.util.ValuedEnum {
    	//D3D10Device(CL_CONTEXT_D3D10_DEVICE_KHR), 
    	GLContext(CL_GL_CONTEXT_KHR),
		EGLDisplay(CL_EGL_DISPLAY_KHR),
		GLXDisplay(CL_GLX_DISPLAY_KHR),
		WGLHDC(CL_WGL_HDC_KHR),
        Platform(CL_CONTEXT_PLATFORM),
        CGLShareGroupApple(CL_CONTEXT_PROPERTY_USE_CGL_SHAREGROUP_APPLE),
		CGLShareGroup(CL_CGL_SHAREGROUP_KHR);

		ContextProperties(long value) { this.value = value; }
		long value;
		@Override
		public long value() { return value; }
		
        public static long getValue(EnumSet<ContextProperties> set) {
            return EnumValues.getValue(set);
        }

        public static EnumSet<ContextProperties> getEnumSet(long v) {
            return EnumValues.getEnumSet(v, ContextProperties.class);
        }
    }

    public CLContext createContextFromCurrentGL() {
        return createGLCompatibleContext(listAllDevices(true));
    }

    static Map<ContextProperties, Object> getGLContextProperties(CLPlatform platform) {
        Map<ContextProperties, Object> out = new LinkedHashMap<ContextProperties, Object>();

        if (Platform.isMacOSX()) {
            Pointer<?> context = OpenGLContextUtils.CGLGetCurrentContext();
            Pointer<?> shareGroup = OpenGLContextUtils.CGLGetShareGroup(context);
            out.put(ContextProperties.CGLShareGroupApple, shareGroup.getPeer());
        } else if (Platform.isWindows()) {
            Pointer<?> context = OpenGLContextUtils.wglGetCurrentContext();
            Pointer<?> dc = OpenGLContextUtils.wglGetCurrentDC();
            out.put(ContextProperties.GLContext, context.getPeer());
            out.put(ContextProperties.WGLHDC, dc.getPeer());
            out.put(ContextProperties.Platform, platform.getEntityPeer());
        } else if (Platform.isUnix()) {
            Pointer<?> context = OpenGLContextUtils.glXGetCurrentContext();
            Pointer<?> dc = OpenGLContextUtils.glXGetCurrentDisplay();
            out.put(ContextProperties.GLContext, context.getPeer());
            out.put(ContextProperties.GLXDisplay, dc.getPeer());
            out.put(ContextProperties.Platform, platform.getEntityPeer());
        } else
            throw new UnsupportedOperationException("Current GL context retrieval not implemented on this platform !");
        
        //out.put(ContextProperties.Platform, platform.getEntity().getPointer());
        
        return out;
    }
    @Deprecated
    public CLContext createGLCompatibleContext(CLDevice... devices) {
        try {
            return createContext(getGLContextProperties(this), devices);
        } catch (Throwable th) {}
        
        for (CLDevice device : devices) {
            if (!device.isGLSharingSupported())
                continue;
            
            try {
                return createContext(getGLContextProperties(this), device);
            } catch (Throwable th) {}
        }
        throw new UnsupportedOperationException("Failed to create an OpenGL-sharing-enabled OpenCL context out of devices " + Arrays.asList(devices));
    }

    /**
     * Creates an OpenCL context formed of the provided devices.<br/>
     * It is generally not a good idea to create a context with more than one device,
     * because much data is shared between all the devices in the same context.
     * @param devices devices that are to form the new context
     * @return new OpenCL context
     */
    public CLContext createContext(Map<ContextProperties, Object> contextProperties, CLDevice... devices) {
        int nDevs = devices.length;
        if (nDevs == 0) {
            throw new IllegalArgumentException("Cannot create a context with no associated device !");
        }
        Pointer<cl_device_id> ids = allocateTypedPointers(cl_device_id.class, nDevs);
        for (int i = 0; i < nDevs; i++) {
            ids.set(i, devices[i].getEntity());
        }

        #declareReusablePtrsAndPErr()

        long[] props = getContextProps(contextProperties);
        Pointer<SizeT> propsRef = props == null ? null : pointerToSizeTs(props);
        //Pointer<clCreateContext_arg1_callback> errCb = null;//pointerTo(errorCallback);
        //System.out.println("ERROR CALLBACK " + Long.toHexString(errCb.getPeer()));
        cl_context context = CL.clCreateContext((Pointer)propsRef, nDevs, ids, null, null, pErr);
        #checkPErr();
        return new CLContext(this, ids, context);
    }
    /*
    public static final clCreateContext_arg1_callback errorCallback = new clCreateContext_arg1_callback() {
		public void apply(Pointer<java.lang.Byte > errInfo, Pointer<? > private_info, @Ptr long cb, Pointer<? > user_data) {
			//new RuntimeException().printStackTrace();
			String log = errInfo.getCString();
			System.out.println("[JavaCL] " + log);
			throw new CLException(log, -1);
		}
	};*/

    /**
     * List all the devices of the specified types, with only the ones declared as available if onlyAvailable is true.
     */
    @SuppressWarnings("deprecation")
    public CLDevice[] listDevices(CLDevice.Type type, boolean onlyAvailable) {
        Pointer<Integer> pCount = allocateInt();
		error(CL.clGetDeviceIDs(getEntity(), type.value(), 0, null, pCount));

        int nDevs = pCount.getInt();
        if (nDevs <= 0) {
            return new CLDevice[0];
        }

        Pointer<cl_device_id> ids = allocateTypedPointers(cl_device_id.class, nDevs);

        error(CL.clGetDeviceIDs(getEntity(), type.value(), nDevs, ids, pCount));
        return getDevices(ids, onlyAvailable);
    }

    /*
    public CLDevice[] listGLDevices(long openglContextId, boolean onlyAvailable) {
        
        Pointer<Integer> errRef = allocateInt();
        long[] props = getContextProps(getGLContextProperties());
        Memory propsMem = toNSArray(props);
        Pointer<SizeT> propsRef = allocateSizeT();
        propsRef.setPointer(propsMem);
        
        Pointer<SizeT> pCount = allocateSizeT();
        error(CL.clGetGLContextInfoKHR(propsRef, CL_DEVICES_FOR_GL_CONTEXT_KHR, 0, (Pointer) null, pCount));

        int nDevs = pCount.getValue().intValue();
        if (nDevs == 0)
            return new CLDevice[0];
        Memory idsMem = new Memory(nDevs * Pointer.SIZE);
        error(CL.clGetGLContextInfoKHR(propsRef, CL_DEVICES_FOR_GL_CONTEXT_KHR, nDevs, idsMem, pCount));
        cl_device_id[] ids = new cl_device_id[nDevs];
        for (int i = 0; i < nDevs; i++)
            ids[i] = new cl_device_id(idsMem.getPointer(i * Pointer.SIZE));
        return getDevices(ids, onlyAvailable);
    }*/

    /**
     * OpenCL profile string. Returns the profile name supported by the implementation. The profile name returned can be one of the following strings:
     * <ul>
     * <li>FULL_PROFILE if the implementation supports the OpenCL specification (functionality defined as part of the core specification and does not require any extensions to be supported).</li>
     * <li>EMBEDDED_PROFILE if the implementation supports the OpenCL embedded profile. The embedded profile is defined to be a subset for each version of OpenCL. The embedded profile for OpenCL 1.0 is described in section 10.</li>
     * </ul>
     */
    @InfoName("CL_PLATFORM_PROFILE")
    public String getProfile() {
        return infos.getString(getEntity(), CL_PLATFORM_PROFILE);
    }

    /**
    OpenCL version string. Returns the OpenCL version supported by the implementation. This version string has the following format:
    OpenCL<space><major_version.min or_version><space><platform- specific information>
    Last Revision Date: 5/16/09	Page 30
    The major_version.minor_version value returned will be 1.0.
     */
    @InfoName("CL_PLATFORM_VERSION")
    public String getVersion() {
        return infos.getString(getEntity(), CL_PLATFORM_VERSION);
    }

    /**
     * Platform name string.
     */
    @InfoName("CL_PLATFORM_NAME")
    public String getName() {
        return infos.getString(getEntity(), CL_PLATFORM_NAME);
    }

    /**
     * Platform vendor string.
     */
    @InfoName("CL_PLATFORM_VENDOR")
    public String getVendor() {
        return infos.getString(getEntity(), CL_PLATFORM_VENDOR);
    }

    /**
     * Returns a list of extension names <br/>
     * Extensions defined here must be supported by all devices associated with this platform.
     */
    @InfoName("CL_PLATFORM_EXTENSIONS")
    public String[] getExtensions() {
        if (extensions == null) {
            extensions = infos.getString(getEntity(), CL_PLATFORM_EXTENSIONS).split("\\s+");
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

    @InfoName("cl_nv_device_attribute_query")
    public boolean isNVDeviceAttributeQuerySupported() {
        return hasExtension("cl_nv_device_attribute_query");
    }

    @InfoName("cl_nv_compiler_options")
    public boolean isNVCompilerOptionsSupported() {
        return hasExtension("cl_nv_compiler_options");
    }

    @InfoName("cl_khr_byte_addressable_store")
    public boolean isByteAddressableStoreSupported() {
        return hasExtension("cl_khr_byte_addressable_store");
    }

    @InfoName("cl_khr_gl_sharing")
    public boolean isGLSharingSupported() {
        return hasExtension("cl_khr_gl_sharing") || hasExtension("cl_APPLE_gl_sharing");
    }

}
