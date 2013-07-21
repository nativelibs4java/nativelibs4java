#parse("main/Header.vm")
package com.nativelibs4java.opencl;

import static com.nativelibs4java.opencl.CLException.error;
import com.nativelibs4java.opencl.CLPlatform.DeviceFeature;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.*;

import com.nativelibs4java.opencl.library.OpenCLLibrary;
import com.nativelibs4java.opencl.library.IOpenCLImplementation.cl_platform_id;
import org.bridj.*;
import org.bridj.ann.Ptr;

import org.bridj.util.ProcessUtils;
import org.bridj.util.StringUtils;
import static org.bridj.Pointer.*;

/**
 * Entry point class for the OpenCL4Java Object-oriented wrappers around the OpenCL API.<br/>
 * @author Olivier Chafik
 */
public class JavaCL {

	static final boolean debug = "true".equals(System.getProperty("javacl.debug")) || "1".equals(System.getenv("JAVACL_DEBUG"));
    static final boolean verbose = debug || "true".equals(System.getProperty("javacl.verbose")) || "1".equals(System.getenv("JAVACL_VERBOSE"));
    static final int minLogLevel = Level.WARNING.intValue();
    
    static final String JAVACL_DEBUG_COMPILER_FLAGS_PROP = "JAVACL_DEBUG_COMPILER_FLAGS";
    static List<String> DEBUG_COMPILER_FLAGS;
    
	static boolean shouldLog(Level level) {
        return verbose || level.intValue() >= minLogLevel;
    }
	static boolean log(Level level, String message, Throwable ex) {
        if (!shouldLog(level))
            return true;
		Logger.getLogger(JavaCL.class.getSimpleName()).log(level, message, ex);
        return true;
	}
	
	static boolean log(Level level, String message) {
		log(level, message, null);
		return true;
	}

	private static int getPlatformIDs(int count, Pointer<cl_platform_id> out, Pointer<Integer> pCount) {
		try {
			return CL.clIcdGetPlatformIDsKHR(count, getPeer(out), getPeer(pCount));
		} catch (Throwable th) {
			return CL.clGetPlatformIDs(count, getPeer(out), getPeer(pCount));
		}
	}
	
	@org.bridj.ann.Library("OpenCLProbe") 
	@org.bridj.ann.Convention(org.bridj.ann.Convention.Style.StdCall)
	public static class OpenCLProbeLibrary {
		@org.bridj.ann.Optional
		public native static synchronized int clGetPlatformIDs(int cl_uint1, Pointer<cl_platform_id > cl_platform_idPtr1, Pointer<Integer > cl_uintPtr1);
		@org.bridj.ann.Optional
		public native static synchronized int clIcdGetPlatformIDsKHR(int cl_uint1, Pointer<cl_platform_id > cl_platform_idPtr1, Pointer<Integer > cl_uintPtr1);
		@org.bridj.ann.Optional
		public native static int clGetPlatformInfo(@Ptr long cl_platform_id1, int cl_platform_info1, @Ptr long size_t1, @Ptr long voidPtr1, @Ptr long size_tPtr1);
	
        #declareInfosGetter("infos", "clGetPlatformInfo")
        
        private static int getPlatformIDs(int count, Pointer<cl_platform_id> out, Pointer<Integer> pCount) {
            try {
                return clIcdGetPlatformIDsKHR(count, out, pCount);
            } catch (Throwable th) {
                return clGetPlatformIDs(count, out, pCount);
            }
        }
        private static Pointer<cl_platform_id> getPlatformIDs() {
            Pointer<Integer> pCount = allocateInt();
            error(getPlatformIDs(0, null, pCount));

            int nPlats = pCount.getInt();
            if (nPlats == 0)
                return null;

            Pointer<cl_platform_id> ids = allocateTypedPointers(cl_platform_id.class, nPlats);
            error(getPlatformIDs(nPlats, ids, null));
            return ids;
        }
        public static boolean hasOpenCL1_0() {
            Pointer<cl_platform_id> ids = getPlatformIDs();
            if (ids == null)
                return false;
            
            for (cl_platform_id id : ids)
                if (isOpenCL1_0(id))
                    return true;
            return false;
        }
        public static boolean isOpenCL1_0(cl_platform_id platform) {
            String version = infos.getString(getPeer(platform), OpenCLLibrary.CL_PLATFORM_VERSION);
            return version.matches("OpenCL 1\\.0.*");
        }
		public static boolean isValid() {
			try {
                Pointer<cl_platform_id> ids = getPlatformIDs();
                return ids != null;
			} catch (Throwable th) {
                return false;
			}
		}
	}	

    static final OpenCLLibrary CL;
	static {
		if (Platform.isLinux()) {
			String amdAppBase = "/opt/AMDAPP/lib";
			BridJ.addLibraryPath(amdAppBase + "/" + (Platform.is64Bits() ? "x86_64" : "x86"));
			BridJ.addLibraryPath(amdAppBase);
		}
        	boolean needsAdditionalSynchronization = false;
		{
			OpenCLProbeLibrary probe = null;
			try {
				try {
					BridJ.setNativeLibraryActualName("OpenCLProbe", "OpenCL");
					BridJ.register();
				} catch (Throwable th) {}
				
				probe = new OpenCLProbeLibrary();
				
				if (!probe.isValid()) {
					BridJ.unregister(OpenCLProbeLibrary.class);
					//BridJ.setNativeLibraryActualName("OpenCLProbe", "OpenCL");
                   			String alt;
					if (Platform.is64Bits() && (BridJ.getNativeLibraryFile(alt = "atiocl64") != null || BridJ.getNativeLibraryFile(alt = "amdocl64") != null) ||
						BridJ.getNativeLibraryFile(alt = "atiocl32") != null ||
						BridJ.getNativeLibraryFile(alt = "atiocl") != null ||
						BridJ.getNativeLibraryFile(alt = "amdocl32") != null ||
						BridJ.getNativeLibraryFile(alt = "amdocl") != null) 
					{
						log(Level.INFO, "Hacking around ATI's weird driver bugs (using atiocl/amdocl library instead of OpenCL)", null); 
						BridJ.setNativeLibraryActualName("OpenCL", alt);
					}
                    BridJ.register(OpenCLProbeLibrary.class);
				}
                
                
                if (probe.hasOpenCL1_0()) {
                    needsAdditionalSynchronization = true;
                    log(Level.WARNING, "At least one OpenCL platform uses OpenCL 1.0, which is not thread-safe: will use (slower) synchronized low-level bindings.");
                }
			} finally {
				if (probe != null)
					BridJ.unregister(OpenCLProbeLibrary.class);
				probe = null;
			}
		}
		
        if (debug) {
            String debugArgs = System.getenv(JAVACL_DEBUG_COMPILER_FLAGS_PROP);
            if (debugArgs != null)
                DEBUG_COMPILER_FLAGS = Arrays.asList(debugArgs.split(" "));
            else if (Platform.isMacOSX())
                DEBUG_COMPILER_FLAGS = Arrays.asList("-g");
            else
                DEBUG_COMPILER_FLAGS = Arrays.asList("-O0", "-g");
            
            int pid = ProcessUtils.getCurrentProcessId();
            log(Level.INFO, "Debug mode enabled with compiler flags \"" + StringUtils.implode(DEBUG_COMPILER_FLAGS, " ") + "\" (can be overridden with env. var. JAVACL_DEBUG_COMPILER_FLAGS_PROP)");
            log(Level.INFO, "You can debug your kernels with GDB using one of the following commands :\n"
                    + "\tsudo gdb --tui --pid=" + pid + "\n"
                    + "\tsudo ddd --debugger \"gdb --pid=" + pid + "\"\n"
                    + "More info here :\n"
                    + "\thttp://code.google.com/p/javacl/wiki/DebuggingKernels");
            
            
        }
        Class<? extends OpenCLLibrary> libraryClass = OpenCLLibrary.class;
        if (needsAdditionalSynchronization) {
            try {
                libraryClass = BridJ.subclassWithSynchronizedNativeMethods(libraryClass);
            } catch (Throwable ex) {
                throw new RuntimeException("Failed to create a synchronized version of the OpenCL API bindings: " + ex, ex);
            }
        }
        BridJ.register(libraryClass);
        try {
            CL = libraryClass.newInstance();
        } catch (Throwable ex) {
            throw new RuntimeException("Failed to instantiate library " + libraryClass.getName() + ": " + ex, ex);
        }
	}
	
    /**
     * List the OpenCL implementations that contain at least one GPU device.
     */
    public static CLPlatform[] listGPUPoweredPlatforms() {
        CLPlatform[] platforms = listPlatforms();
        List<CLPlatform> out = new ArrayList<CLPlatform>(platforms.length);
        for (CLPlatform platform : platforms) {
            if (platform.listGPUDevices(true).length > 0)
                out.add(platform);
        }
        return out.toArray(new CLPlatform[out.size()]);
    }
	/**
	 * Lists all available OpenCL implementations.
	 */
    public static CLPlatform[] listPlatforms() {
        Pointer<Integer> pCount = allocateInt();
        error(getPlatformIDs(0, null, pCount));

        int nPlats = pCount.getInt();
        if (nPlats == 0)
            return new CLPlatform[0];

        Pointer<cl_platform_id> ids = allocateTypedPointers(cl_platform_id.class, nPlats);

        error(getPlatformIDs(nPlats, ids, null));
        CLPlatform[] platforms = new CLPlatform[nPlats];

        for (int i = 0; i < nPlats; i++) {
            platforms[i] = new CLPlatform(ids.getSizeTAtIndex(i));
        }
        return platforms;
    }

	/**
	 * Creates an OpenCL context formed of the provided devices.<br/>
	 * It is generally not a good idea to create a context with more than one device,
	 * because much data is shared between all the devices in the same context.
	 * @param devices devices that are to form the new context
	 * @return new OpenCL context
	 */
    public static CLContext createContext(Map<CLPlatform.ContextProperties, Object> contextProperties, CLDevice... devices) {
		return devices[0].getPlatform().createContext(contextProperties, devices);
    }

    /**
	 * Allows the implementation to release the resources allocated by the OpenCL compiler. <br/>
	 * This is a hint from the application and does not guarantee that the compiler will not be used in the future or that the compiler will actually be unloaded by the implementation. <br/>
	 * Calls to Program.build() after unloadCompiler() will reload the compiler, if necessary, to build the appropriate program executable.
	 */
	public static void unloadCompiler() {
		error(CL.clUnloadCompiler());
	}

	/**
	 * Returns the "best" OpenCL device (currently, the one that has the largest amount of compute units).<br>
	 * For more control on what is to be considered a better device, please use the {@link JavaCL#getBestDevice(CLPlatform.DeviceFeature[]) } variant.<br>
	 * This is currently equivalent to <code>getBestDevice(MaxComputeUnits)</code>
	 */
    public static CLDevice getBestDevice() {
        return getBestDevice(CLPlatform.DeviceFeature.MaxComputeUnits);
    }
	/**
	 * Returns the "best" OpenCL device based on the comparison of the provided prioritized device feature.<br>
	 * The returned device does not necessarily exhibit the features listed in preferredFeatures, but it has the best ordered composition of them.<br>
	 * For instance on a system with a GPU and a CPU device, <code>JavaCL.getBestDevice(CPU, MaxComputeUnits)</code> will return the CPU device, but on another system with two GPUs and no CPU device it will return the GPU that has the most compute units.
	 */
    public static CLDevice getBestDevice(CLPlatform.DeviceFeature... preferredFeatures) {
        List<CLDevice> devices = new ArrayList<CLDevice>();
		for (CLPlatform platform : listPlatforms())
			devices.addAll(Arrays.asList(platform.listAllDevices(true)));
        return CLPlatform.getBestDevice(Arrays.asList(preferredFeatures), devices);
    }
    /**
     * Creates an OpenCL context with the "best" device (see {@link JavaCL#getBestDevice() })
     */
	public static CLContext createBestContext() {
        return createBestContext(DeviceFeature.MaxComputeUnits);
	}

    /**
     * Creates an OpenCL context with the "best" device based on the comparison of the provided 
	 * prioritized device feature (see {@link JavaCL#getBestDevice(CLPlatform.DeviceFeature...) })
     */
	public static CLContext createBestContext(CLPlatform.DeviceFeature... preferredFeatures) {
		CLDevice device = getBestDevice(preferredFeatures);
		return device.getPlatform().createContext(null, device);
	}

    /**
     * Creates an OpenCL context able to share entities with the current OpenGL context.
     * @throws RuntimeException if JavaCL is unable to create an OpenGL-shared OpenCL context.
     */
	public static CLContext createContextFromCurrentGL() {
        RuntimeException first = null;
        for (CLPlatform platform : listPlatforms()) {
            try {
                CLContext ctx = platform.createContextFromCurrentGL();
                if (ctx != null)
                    return ctx;
            } catch (RuntimeException ex) {
                if (first == null)
                    first = ex;
                
            }
        }
        throw new RuntimeException("Failed to create an OpenCL context based on the current OpenGL context", first);
    }
    
    static File userJavaCLDir = new File(new File(System.getProperty("user.home")), ".javacl");
    static File userCacheDir = new File(userJavaCLDir, "cache");
    
    static synchronized File createTempFile(String prefix, String suffix, String category) {
    		File dir = new File(userJavaCLDir, category);
    		dir.mkdirs();
    		try {
    			return File.createTempFile(prefix, suffix, dir);
    		} catch (IOException ex) {
    			throw new RuntimeException("Failed to create a temporary directory for category '" + category + "' in " + userJavaCLDir + ": " + ex.getMessage(), ex);
    		}
    }
    static synchronized File createTempDirectory(String prefix, String suffix, String category) {
    		File file = createTempFile(prefix, suffix, category);
		file.delete();
		file.mkdir();
    		return file;
    }
}
