#parse("main/Header.vm")
package com.nativelibs4java.opencl;

import com.nativelibs4java.opencl.library.OpenGLContextUtils;
import com.nativelibs4java.util.EnumValue;
import com.nativelibs4java.util.EnumValues;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;
import static com.nativelibs4java.opencl.library.IOpenCLLibrary.*;

import org.bridj.*;
import org.bridj.ann.*;
import static org.bridj.Pointer.*;

import java.nio.ByteOrder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.*;

import static com.nativelibs4java.opencl.JavaCL.*;
import static com.nativelibs4java.opencl.CLException.*;

/**
 * OpenCL implementation entry point.
 * see {@link JavaCL#listPlatforms() } 
 * @author Olivier Chafik
 */
public class CLPlatform extends CLAbstractEntity {

    CLPlatform(long platform) {
        super(platform, true);
    }
    
    #declareInfosGetter("infos", "CL.clGetPlatformInfo")

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

    private CLDevice[] getDevices(Pointer<SizeT> ids, boolean onlyAvailable) {
        int nDevs = (int)ids.getValidElements();
        CLDevice[] devices;
        if (onlyAvailable) {
            List<CLDevice> list = new ArrayList<CLDevice>(nDevs);
            for (int i = 0; i < nDevs; i++) {
                CLDevice device = new CLDevice(this, ids.getSizeTAtIndex(i));
                if (device.isAvailable()) {
                    list.add(device);
                }
            }
            devices = list.toArray(new CLDevice[list.size()]);
        } else {
            devices = new CLDevice[nDevs];
            for (int i = 0; i < nDevs; i++) {
                devices[i] = new CLDevice(this, ids.getSizeTAtIndex(i));
            }
        }
        return devices;
    }

    long[] getContextProps(Map<ContextProperties, Object> contextProperties) {
        int nContextProperties = contextProperties == null ? 0 : contextProperties.size();
        final long[] properties = new long[(nContextProperties + 1) * 2 + 1];
        properties[0] = CL_CONTEXT_PLATFORM;
        properties[1] = getEntity();
        int iProp = 2;
        if (nContextProperties != 0) {
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
         * Prefer devices with the same byte ordering as the hosting platform (see {@link CLDevice#getByteOrder() })
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
        return getBestDevice(Arrays.asList(DeviceFeature.MaxComputeUnits), Arrays.asList(listAllDevices(true)));
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
            out.put(ContextProperties.Platform, platform.getEntity());
        } else if (Platform.isUnix()) {
            Pointer<?> context = OpenGLContextUtils.glXGetCurrentContext();
            Pointer<?> dc = OpenGLContextUtils.glXGetCurrentDisplay();
            out.put(ContextProperties.GLContext, context.getPeer());
            out.put(ContextProperties.GLXDisplay, dc.getPeer());
            out.put(ContextProperties.Platform, platform.getEntity());
        } else
            throw new UnsupportedOperationException("Current GL context retrieval not implemented on this platform !");
        
        //out.put(ContextProperties.Platform, platform.getEntity().getPointer());
        
        return out;
    }
    /**
#documentCallsFunction("clCreateContext")
	 */
    @Deprecated
    public CLContext createGLCompatibleContext(CLDevice... devices) {
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
#documentCallsFunction("clCreateContext")
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
        Pointer<SizeT> ids = allocateSizeTs(nDevs);
        for (int i = 0; i < nDevs; i++) {
            ids.setSizeTAtIndex(i, devices[i].getEntity());
        }

        #declareReusablePtrsAndPErr()

        long[] props = getContextProps(contextProperties);
        Pointer<SizeT> propsRef = props == null ? null : pointerToSizeTs(props);
        //System.out.println("ERROR CALLBACK " + Long.toHexString(errCb.getPeer()));
        long context = CL.clCreateContext(getPeer(propsRef), nDevs, getPeer(ids), 0, 0, getPeer(pErr));
        #checkPErr();
        return new CLContext(this, ids, context);
    }

    /**
#documentCallsFunction("clGetDeviceIDs")
     * List all the devices of the specified types, with only the ones declared as available if onlyAvailable is true.
     */
    @SuppressWarnings("deprecation")
    public CLDevice[] listDevices(CLDevice.Type type, boolean onlyAvailable) {
        Pointer<Integer> pCount = allocateInt();
		error(CL.clGetDeviceIDs(getEntity(), type.value(), 0, 0, getPeer(pCount)));

        int nDevs = pCount.getInt();
        if (nDevs <= 0) {
            return new CLDevice[0];
        }

        Pointer<SizeT> ids = allocateSizeTs(nDevs);

        error(CL.clGetDeviceIDs(getEntity(), type.value(), nDevs, getPeer(ids), 0));
        return getDevices(ids, onlyAvailable);
    }

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

    private double versionValue = Double.NaN;
    private static final Pattern VERSION_PATTERN = Pattern.compile("OpenCL (\\d+\\.\\d+)\\b.*");
    double getVersionValue() {
    	if (Double.isNaN(versionValue)) {
    		String versionString = getVersion();
    		Matcher matcher = VERSION_PATTERN.matcher(versionString);
    		if (matcher.matches()) {
    			String str = matcher.group(1);
    			versionValue = Double.parseDouble(str);
    		} else {
    			log(Level.SEVERE, "Failed to parse OpenCL version: '" + versionString + "'");
    		}
    	}
    	return versionValue;
    }
    void requireMinVersionValue(String feature, double minValue) {
    	requireMinVersionValue(feature, minValue, Double.NaN);
    }
    void requireMinVersionValue(String feature, double minValue, double deprecationValue) {
    	double value = getVersionValue();
    	if (value < minValue) {
    		throw new CLVersionException(feature + " requires OpenCL version " + minValue +
    			" (detected version is " + value + ")");
    	} else if (!Double.isNaN(deprecationValue) && (value < deprecationValue)) {
    		// The above test will work fine (i.e. will fail) with NaN.
    		log(Level.WARNING, feature + " is deprecated from OpenCL version " + deprecationValue +
    			" (detected version is " + value + ")");
    	}
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

    /**
     * Allows the implementation to release the resources allocated by the OpenCL compiler for this platform.
     */
    public void unloadPlatformCompiler() {
    	if (getVersionValue() < 1.2) {
			requireMinVersionValue("clUnloadCompiler", 1.1, 1.2);
			error(CL.clUnloadCompiler());
		} else {
			requireMinVersionValue("clUnloadPlatformCompiler", 1.2);
			error(CL.clUnloadPlatformCompiler(getEntity()));
		}
    }

}
