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


import com.nativelibs4java.util.ValuedEnum;
import com.nativelibs4java.opencl.library.OpenGLContextUtils;
import com.nativelibs4java.util.EnumValue;
import com.nativelibs4java.util.EnumValues;
import com.ochafik.lang.jnaerator.runtime.NativeSize;
import com.ochafik.lang.jnaerator.runtime.NativeSizeByReference;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;
import com.sun.jna.*;
import com.sun.jna.ptr.*;
import java.nio.ByteOrder;
import java.util.*;
import static com.nativelibs4java.opencl.JavaCL.*;
import static com.nativelibs4java.opencl.CLException.*;
import static com.nativelibs4java.util.JNAUtils.*;

/**
 * OpenCL implementation entry point.
 * @see JavaCL#listPlatforms() 
 * @author Olivier Chafik
 */
public class CLPlatform extends CLAbstractEntity<cl_platform_id> {

    CLPlatform(cl_platform_id platform) {
        super(platform, true);
    }
    private static CLInfoGetter<cl_platform_id> infos = new CLInfoGetter<cl_platform_id>() {

        @Override
        protected int getInfo(cl_platform_id entity, int infoTypeEnum, NativeSize size, Pointer out, NativeSizeByReference sizeOut) {
            return CL.clGetPlatformInfo(entity, infoTypeEnum, size, out, sizeOut);
        }
    };

    @Override
    public String toString() {
        return getName() + " {vendor: " + getVendor() + ", version: " + getVersion() + ", profile: " + getProfile() + ", extensions: " + Arrays.toString(getExtensions()) + "}";
    }

    @Override
    protected void clear() {
    }

    /**
     * Lists all the devices of the platform
     * @param onlyAvailable if true, only returns devices that are available
     * @see CLPlatform#listDevices(java.util.EnumSet, boolean)
     */
    public CLDevice[] listAllDevices(boolean onlyAvailable) {
        return listDevices(EnumSet.allOf(CLDevice.Type.class), onlyAvailable);
    }

    /**
     * Lists all the GPU devices of the platform
     * @param onlyAvailable if true, only returns GPU devices that are available
     * @see CLPlatform#listDevices(java.util.EnumSet, boolean)
     */
    public CLDevice[] listGPUDevices(boolean onlyAvailable) {
        try {
            return listDevices(EnumSet.of(CLDevice.Type.GPU), onlyAvailable);
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
     * @see CLPlatform#listDevices(java.util.EnumSet, boolean)
     */
    public CLDevice[] listCPUDevices(boolean onlyAvailable) {
        try {
            return listDevices(EnumSet.of(CLDevice.Type.CPU), onlyAvailable);
        } catch (CLException ex) {
            if (ex.getCode() == CL_DEVICE_NOT_FOUND) {
                return new CLDevice[0];
            }
            throw new RuntimeException("Unexpected OpenCL error", ex);
        }
    }

    private CLDevice[] getDevices(cl_device_id[] ids, boolean onlyAvailable) {
        int nDevs = ids.length;
        CLDevice[] devices;
        if (onlyAvailable) {
            List<CLDevice> list = new ArrayList<CLDevice>(nDevs);
            for (int i = 0; i < nDevs; i++) {
                CLDevice device = new CLDevice(this, ids[i]);
                if (device.isAvailable()) {
                    list.add(device);
                }
            }
            devices = list.toArray(new CLDevice[list.size()]);
        } else {
            devices = new CLDevice[nDevs];
            for (int i = 0; i < nDevs; i++) {
                devices[i] = new CLDevice(this, ids[i]);
            }
        }
        return devices;
    }

    static long[] getContextProps(Map<ContextProperties, Object> contextProperties) {
        if (contextProperties == null)
            return null;
        final long[] properties = new long[contextProperties.size() * 2 + 2];
        int iProp = 0;
        for (Map.Entry<ContextProperties, Object> e : contextProperties.entrySet()) {
            //if (!(v instanceof Number)) throw new IllegalArgumentException("Invalid context property value for '" + e.getKey() + ": " + v);
            properties[iProp++] = e.getKey().value();
            Object v = e.getValue();
            if (v instanceof Number)
                properties[iProp++] = ((Number)v).longValue();
            else if (v instanceof Pointer)
                properties[iProp++] = PointerUtils.getAddress((Pointer)v);
            else
                throw new IllegalArgumentException("Cannot convert value " + v + " to a context property value !");
        }
        properties[iProp] = 0;
        return properties;
    }

    /**
     * Enums used to indicate how to choose the best CLDevice.
     */
    public enum DeviceFeature {
        /**
         * Prefer CPU devices (see @see CLDevice#getType())
         */
        CPU {
            Comparable extractValue(CLDevice device) {
                return device.getType().contains(CLDevice.Type.CPU) ? 1 : 0;
            }
        },
        /**
         * Prefer GPU devices (see @see CLDevice#getType())
         */
        GPU {
            Comparable extractValue(CLDevice device) {
                return device.getType().contains(CLDevice.Type.GPU) ? 1 : 0;
            }
        },
        /**
         * Prefer Accelerator devices (see @see CLDevice#getType())
         */
        Accelerator {
            Comparable extractValue(CLDevice device) {
                return device.getType().contains(CLDevice.Type.Accelerator) ? 1 : 0;
            }
        },
        /**
         * Prefer devices with the most compute units (see @see CLDevice#getMaxComputeUnits())
         */
        MaxComputeUnits {
            Comparable extractValue(CLDevice device) {
                return device.getMaxComputeUnits();
            }
        },
        /**
         * Prefer devices with the same byte ordering as the hosting platform (@see CLDevice#getKernelsDefaultByteOrder())
         */
        NativeEndianness {
            Comparable extractValue(CLDevice device) {
                return device.getKernelsDefaultByteOrder() == ByteOrder.nativeOrder() ? 1 : 0;
            }
        },
        /**
         * Prefer devices that support double-precision float computations (@see CLDevice#isDoubleSupported())
         */
        DoubleSupport {
            Comparable extractValue(CLDevice device) {
                return device.isDoubleSupported() ? 1 : 0;
            }
        },
        /**
         * Prefer devices that support images and with the most supported image formats (@see CLDevice#hasImageSupport())
         */
        ImageSupport {
            Comparable extractValue(CLDevice device) {
                return device.hasImageSupport() ? 1 : 0;
            }
        },
        /**
         * Prefer devices that support out of order queues (@see CLDevice#hasOutOfOrderQueueSupport())
         */
        OutOfOrderQueueSupport {
            Comparable extractValue(CLDevice device) {
                return device.hasOutOfOrderQueueSupport() ? 1 : 0;
            }
        },
        /**
         * Prefer devices with the greatest variety of supported image formats (@see CLContext#getSupportedImageFormats())
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
    public enum ContextProperties implements ValuedEnum {
    	//D3D10Device(CL_CONTEXT_D3D10_DEVICE_KHR), 
    	GLContext(CL_GL_CONTEXT_KHR),
		EGLDisplay(CL_EGL_DISPLAY_KHR),
		GLXDisplay(CL_GLX_DISPLAY_KHR),
		WGLHDC(CL_WGL_HDC_KHR),
        Platform(CL_CONTEXT_PLATFORM),
        CGLShareGroupApple(2684354),//CL_CONTEXT_PROPERTY_USE_CGL_SHAREGROUP_APPLE),
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

        if (Platform.isMac()) {
            NativeSize context = OpenGLContextUtils.INSTANCE.CGLGetCurrentContext();
            NativeSize shareGroup = OpenGLContextUtils.INSTANCE.CGLGetShareGroup(context);
            out.put(ContextProperties.GLContext, context.longValue());
            out.put(ContextProperties.CGLShareGroup, shareGroup.longValue());
        } else if (Platform.isWindows()) {
            NativeSize context = OpenGLContextUtils.INSTANCE.wglGetCurrentContext();
            NativeSize dc = OpenGLContextUtils.INSTANCE.wglGetCurrentDC();
            out.put(ContextProperties.GLContext, context.longValue());
            out.put(ContextProperties.WGLHDC, dc.longValue());
        } else if (Platform.isX11()) {
            NativeSize context = OpenGLContextUtils.INSTANCE.glXGetCurrentContext();
            NativeSize dc = OpenGLContextUtils.INSTANCE.glXGetCurrentDisplay();
            out.put(ContextProperties.GLContext, context.longValue());
            out.put(ContextProperties.GLXDisplay, dc.longValue());
        } else
            throw new UnsupportedOperationException("Current GL context retrieval not implemented on this platform !");
        
        //out.put(ContextProperties.Platform, platform.getEntity().getPointer());
        
        return out;
    }
    @Deprecated
    public CLContext createGLCompatibleContext(CLDevice... devices) {
        for (CLDevice device : devices)
            if (!device.isGLSharingSupported())
                throw new UnsupportedOperationException("Device " + device + " does not support CL/GL sharing.");
        
        return createContext(getGLContextProperties(this), devices);
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
        cl_device_id[] ids = new cl_device_id[nDevs];
        for (int i = 0; i < nDevs; i++) {
            ids[i] = devices[i].getEntity();
        }

        IntByReference errRef = new IntByReference();

        long[] props = getContextProps(contextProperties);
        Memory propsMem = toNSArray(props);
        NativeSizeByReference propsRef = new NativeSizeByReference();
        propsRef.setPointer(propsMem);
        cl_context context = CL.clCreateContext(propsRef, ids.length, ids, null/*errorCallback.getPointer()*/, null, errRef);
        error(errRef.getValue());
        return new CLContext(this, ids, context);
    }
    /*
    public static final clCreateContext_arg1_callback errorCallback = new clCreateContext_arg1_callback() {
		public void apply(Pointer<java.lang.Byte > errInfo, Pointer<? > private_info, @Ptr long cb, Pointer<? > user_data) {
			String log = errInfo.getCString();
			System.out.println("[JavaCL] " + log);
			throw new CLException(log);
		}
	};
	*/
    
    /**
     * List all the devices of the specified types, with only the ones declared as available if onlyAvailable is true.
     */
    @SuppressWarnings("deprecation")
    public CLDevice[] listDevices(EnumSet<CLDevice.Type> types, boolean onlyAvailable) {
        int flags = (int) CLDevice.Type.getValue(types);

        IntByReference pCount = new IntByReference();
        error(CL.clGetDeviceIDs(getEntity(), flags, 0, (PointerByReference) null, pCount));

        int nDevs = pCount.getValue();
        if (nDevs == 0) {
            return new CLDevice[0];
        }

        cl_device_id[] ids = new cl_device_id[nDevs];

        error(CL.clGetDeviceIDs(getEntity(), flags, nDevs, ids, pCount));
        return getDevices(ids, onlyAvailable);
    }

    /*
    public CLDevice[] listGLDevices(long openglContextId, boolean onlyAvailable) {
        
        IntByReference errRef = new IntByReference();
        long[] props = getContextProps(getGLContextProperties());
        Memory propsMem = toNSArray(props);
        NativeSizeByReference propsRef = new NativeSizeByReference();
        propsRef.setPointer(propsMem);
        
        NativeSizeByReference pCount = new NativeSizeByReference();
        error(CL.clGetGLContextInfoKHR(propsRef, CL_DEVICES_FOR_GL_CONTEXT_KHR, toNS(0), (Pointer) null, pCount));

        int nDevs = pCount.getValue().intValue();
        if (nDevs == 0)
            return new CLDevice[0];
        Memory idsMem = new Memory(nDevs * Pointer.SIZE);
        error(CL.clGetGLContextInfoKHR(propsRef, CL_DEVICES_FOR_GL_CONTEXT_KHR, toNS(nDevs), idsMem, pCount));
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
