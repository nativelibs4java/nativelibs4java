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


import com.nativelibs4java.opencl.library.OpenGLApple;
import com.nativelibs4java.util.EnumValue;
import com.nativelibs4java.util.EnumValues;
import com.ochafik.lang.jnaerator.runtime.NativeSize;
import com.ochafik.lang.jnaerator.runtime.NativeSizeByReference;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;
import com.sun.jna.*;
import com.sun.jna.ptr.*;
import java.util.*;
import static com.nativelibs4java.opencl.JavaCL.*;
import static com.nativelibs4java.opencl.CLException.*;
import static com.nativelibs4java.util.JNAUtils.*;

/**
 * OpenCL implementation entry point.
 * @see OpenCL4Java#listPlatforms() 
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

    private long[] getContextProps(Map<ContextProperties, Number> contextProperties) {
        if (contextProperties == null)
            return null;
        final long[] properties = new long[contextProperties.size() * 2 + 1];
        int iProp = 0;
        for (Map.Entry<ContextProperties, Number> e : contextProperties.entrySet()) {
            //if (!(v instanceof Number)) throw new IllegalArgumentException("Invalid context property value for '" + e.getKey() + ": " + v);
            properties[iProp++] = e.getKey().getValue();
            properties[iProp++] = e.getValue().longValue();
        }
        properties[iProp] = 0;
        return properties;
    }

    public enum DeviceEvaluationStrategy {

        BiggestMaxComputeUnits
    }

	static CLDevice getBestDevice(DeviceEvaluationStrategy eval, Iterable<CLDevice> devices) {

        CLDevice bestDevice = null;
        for (CLDevice device : devices) {
            if (bestDevice == null) {
                bestDevice = device;
            } else {
                switch (eval) {
                    case BiggestMaxComputeUnits:
                        if (bestDevice.getMaxComputeUnits() < device.getMaxComputeUnits()) {
                            bestDevice = device;
                        }
                        break;
                }
            }
        }
        return bestDevice;
    }

    public CLDevice getBestDevice() {
        return getBestDevice(DeviceEvaluationStrategy.BiggestMaxComputeUnits, Arrays.asList(listGPUDevices(true)));
    }

    /** Bit values for CL_CONTEXT_PROPERTIES */
    public enum ContextProperties {

        @EnumValue(CL_GL_CONTEXT_KHR        ) GLContext 	   ,
		@EnumValue(CL_EGL_DISPLAY_KHR       ) EGLDisplay	   ,
		@EnumValue(CL_GLX_DISPLAY_KHR       ) GLXDisplay	   ,
		@EnumValue(CL_WGL_HDC_KHR           ) WGLHDC		   ,
		@EnumValue(CL_CGL_SHAREGROUP_KHR	) CGLShareGroup    ;

        public long getValue() {
            return EnumValues.getValue(this);
        }

        public static long getValue(EnumSet<ContextProperties> set) {
            return EnumValues.getValue(set);
        }

        public static EnumSet<ContextProperties> getEnumSet(long v) {
            return EnumValues.getEnumSet(v, ContextProperties.class);
        }
    }

    public static ContextProperties getGLContextPropertyKey() {
        if (Platform.isMac())
            return ContextProperties.GLContext;//.CGLShareGroup;
        else if (Platform.isWindows())
            return ContextProperties.WGLHDC;
        else if (Platform.isX11())
            return ContextProperties.GLXDisplay;
        else
            return ContextProperties.EGLDisplay;
    }
    static Map<ContextProperties, Number> getGLContextProperty(long glContextId, Map<ContextProperties, Number> out) {
        ContextProperties key = getGLContextPropertyKey();
        if (out == null)
            return Collections.singletonMap(key, (Number)glContextId);
        else
        {
            out.put(key, glContextId);
            return out;
        }
    }
    @Deprecated
    public CLContext createGLCompatibleContext(long glContextId, CLDevice... devices) {
        for (CLDevice device : devices)
            if (!device.isGLSharingSupported())
                throw new UnsupportedOperationException("Device " + device + " does not support CL/GL sharing.");
        
    	return createContext(getGLContextProperty(glContextId, null), devices);
    }

    /**
     * Creates an OpenCL context formed of the provided devices.<br/>
     * It is generally not a good idea to create a context with more than one device,
     * because much data is shared between all the devices in the same context.
     * @param devices devices that are to form the new context
     * @return new OpenCL context
     */
    public CLContext createContext(Map<ContextProperties, Number> contextProperties, CLDevice... devices) {
        int nDevs = devices.length;
        if (nDevs == 0) {
            throw new IllegalArgumentException("Cannot create a context with no associated device !");
        }
        cl_device_id[] ids = new cl_device_id[nDevs];
        for (int i = 0; i < nDevs; i++) {
            ids[i] = devices[i].get();
        }

        IntByReference errRef = new IntByReference();

        long[] props = getContextProps(contextProperties);
        Memory propsMem = toNSArray(props);
        NativeSizeByReference propsRef = new NativeSizeByReference();
        propsRef.setPointer(propsMem);
        cl_context context = CL.clCreateContext(propsRef, ids.length, ids, null, null, errRef);
        error(errRef.getValue());
        return new CLContext(this, ids, context);
    }

    /**
     * List all the devices of the specified types, with only the ones declared as available if onlyAvailable is true.
     */
    @SuppressWarnings("deprecation")
    public CLDevice[] listDevices(EnumSet<CLDevice.Type> types, boolean onlyAvailable) {
        int flags = (int) CLDevice.Type.getValue(types);

        IntByReference pCount = new IntByReference();
        error(CL.clGetDeviceIDs(get(), flags, 0, (PointerByReference) null, pCount));

        int nDevs = pCount.getValue();
        if (nDevs == 0) {
            return new CLDevice[0];
        }

        cl_device_id[] ids = new cl_device_id[nDevs];

        error(CL.clGetDeviceIDs(get(), flags, nDevs, ids, pCount));
        return getDevices(ids, onlyAvailable);
    }

    public long getCurrentGLContext() {
        if (Platform.isMac()) {
            return OpenGLApple.INSTANCE.CGLGetShareGroup(OpenGLApple.INSTANCE.CGLGetCurrentContext()).longValue();
        }
        throw new UnsupportedOperationException("Current GL context retrieval not implemented on this platform !");
    }
    public CLContext createContextFromCurrentGL() {
        return createBestGLCompatibleContext(getCurrentGLContext(), listAllDevices(true));
    }

    @Deprecated
    public CLDevice currentGLDevice() {
        IntByReference errRef = new IntByReference();
        int openglContextId = 0;
        long[] props = getContextProps(getGLContextProperty(openglContextId, null));
        Memory propsMem = toNSArray(props);
        NativeSizeByReference propsRef = new NativeSizeByReference();
        propsRef.setPointer(propsMem);
        
        NativeSizeByReference pCount = new NativeSizeByReference();
        NativeSizeByReference pLen = new NativeSizeByReference();
        Memory mem = new Memory(Pointer.SIZE);
        if (Platform.isMac())
            error(CL.clGetGLContextInfoAPPLE(Pointer.createConstant(CL_CURRENT_DEVICE_FOR_GL_CONTEXT_KHR), toNS(Pointer.SIZE), mem, pCount));
        else
            error(CL.clGetGLContextInfoKHR(propsRef, CL_CURRENT_DEVICE_FOR_GL_CONTEXT_KHR, toNS(Pointer.SIZE), mem, pCount));

        if (pCount.getValue().intValue() != Pointer.SIZE)
            throw new RuntimeException("Not a device : len = " + pCount.getValue().intValue());

        Pointer p = mem.getPointer(0);
        if (p.equals(Pointer.NULL))
            return null;
        return new CLDevice(this, new cl_device_id(p));
    }
    public CLDevice[] listGLDevices(long openglContextId, boolean onlyAvailable) {
        
        IntByReference errRef = new IntByReference();
        long[] props = getContextProps(getGLContextProperty(openglContextId, null));
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
        return infos.getString(get(), CL_PLATFORM_PROFILE);
    }

    /**
    OpenCL version string. Returns the OpenCL version supported by the implementation. This version string has the following format:
    OpenCL<space><major_version.min or_version><space><platform- specific information>
    Last Revision Date: 5/16/09	Page 30
    The major_version.minor_version value returned will be 1.0.
     */
    @InfoName("CL_PLATFORM_VERSION")
    public String getVersion() {
        return infos.getString(get(), CL_PLATFORM_VERSION);
    }

    /**
     * Platform name string.
     */
    @InfoName("CL_PLATFORM_NAME")
    public String getName() {
        return infos.getString(get(), CL_PLATFORM_NAME);
    }

    /**
     * Platform vendor string.
     */
    @InfoName("CL_PLATFORM_VENDOR")
    public String getVendor() {
        return infos.getString(get(), CL_PLATFORM_VENDOR);
    }

    /**
     * Returns a list of extension names <br/>
     * Extensions defined here must be supported by all devices associated with this platform.
     */
    @InfoName("CL_PLATFORM_EXTENSIONS")
    public String[] getExtensions() {
        if (extensions == null) {
            extensions = infos.getString(get(), CL_PLATFORM_EXTENSIONS).split("\\s+");
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
        return hasExtension("cl_khr_gl_sharing");
    }

}
