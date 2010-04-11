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


import com.nativelibs4java.opencl.library.OpenGLContextUtils;
import com.nativelibs4java.util.EnumValue;
import com.nativelibs4java.util.EnumValues;
import com.ochafik.lang.jnaerator.runtime.NativeSize;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;
import com.bridj.*;

import java.nio.ByteOrder;
import java.util.*;
import static com.nativelibs4java.opencl.JavaCL.*;
import static com.nativelibs4java.opencl.CLException.*;
import static com.nativelibs4java.util.JNAUtils.*;
import static com.bridj.Pointer.*;

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
        protected int getInfo(cl_platform_id entity, int infoTypeEnum, long size, Pointer out, Pointer<SizeT> sizeOut) {
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

    private CLDevice[] getDevices(Pointer<cl_device_id> ids, boolean onlyAvailable) {
        int nDevs = (int)ids.getRemainingElements();
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

    static long[] getContextProps(Map<ContextProperties, Number> contextProperties) {
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

        BiggestMaxComputeUnits,
        BiggestMaxComputeUnitsWithNativeEndianness
    }

	static CLDevice getBestDevice(DeviceEvaluationStrategy eval, Iterable<CLDevice> devices) {

        CLDevice bestDevice = null;
        for (CLDevice device : devices) {
            if (bestDevice == null) {
                bestDevice = device;
            } else {
                switch (eval) {
                    case BiggestMaxComputeUnitsWithNativeEndianness:
                        if (bestDevice.getKernelsDefaultByteOrder() != ByteOrder.nativeOrder() && device.getKernelsDefaultByteOrder() == ByteOrder.nativeOrder()) {
                            bestDevice = device;
                            break;
                        }
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

    public CLContext createContextFromCurrentGL() {
        return createGLCompatibleContext(listAllDevices(true));
    }

    static Map<ContextProperties, Number> getGLContextProperties() {
        Map<ContextProperties, Number> out = new LinkedHashMap<ContextProperties, Number>();

        if (JNI.isMacOSX()) {
            Pointer<?> context = OpenGLContextUtils.CGLGetCurrentContext();
            Pointer<?> shareGroup = OpenGLContextUtils.CGLGetShareGroup(context);
            out.put(ContextProperties.GLContext, context.getPeer());
            out.put(ContextProperties.CGLShareGroup, shareGroup.getPeer());
        } else if (JNI.isWindows()) {
            Pointer<?> context = OpenGLContextUtils.wglGetCurrentContext();
            Pointer<?> dc = OpenGLContextUtils.wglGetCurrentDC();
            out.put(ContextProperties.GLContext, context.getPeer());
            out.put(ContextProperties.WGLHDC, dc.getPeer());
        } else if (JNI.isUnix()) {
            Pointer<?> context = OpenGLContextUtils.glXGetCurrentContext();
            Pointer<?> dc = OpenGLContextUtils.glXGetCurrentDisplay();
            out.put(ContextProperties.GLContext, context.getPeer());
            out.put(ContextProperties.GLXDisplay, dc.getPeer());
        } else
            throw new UnsupportedOperationException("Current GL context retrieval not implemented on this platform !");
        
        return out;
    }
    @Deprecated
    public CLContext createGLCompatibleContext(CLDevice... devices) {
        for (CLDevice device : devices)
            if (!device.isGLSharingSupported())
                throw new UnsupportedOperationException("Device " + device + " does not support CL/GL sharing.");
        
    	return createContext(getGLContextProperties(), devices);
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
        Pointer<cl_device_id> ids = allocateTypedPointers(cl_device_id.class, nDevs);
        for (int i = 0; i < nDevs; i++) {
            ids.set(i, devices[i].getEntity());
        }

        Pointer<Integer> errRef = allocateInt();

        long[] props = getContextProps(contextProperties);
        Pointer<SizeT> propsRef = props == null ? null : pointerToSizeTs(props);
        cl_context context = CL.clCreateContext(propsRef, nDevs, ids, null, null, errRef);
        error(errRef.get());
        return new CLContext(this, ids, context);
    }

    /**
     * List all the devices of the specified types, with only the ones declared as available if onlyAvailable is true.
     */
    @SuppressWarnings("deprecation")
    public CLDevice[] listDevices(EnumSet<CLDevice.Type> types, boolean onlyAvailable) {
        int flags = (int) CLDevice.Type.getValue(types);

        Pointer<Integer> pCount = allocateInt();
        error(CL.clGetDeviceIDs(getEntity(), flags, 0, null, pCount));

        int nDevs = pCount.get();
        if (nDevs <= 0) {
            return new CLDevice[0];
        }

        Pointer<cl_device_id> ids = allocateTypedPointers(cl_device_id.class, nDevs);

        error(CL.clGetDeviceIDs(getEntity(), flags, nDevs, ids, pCount));
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
