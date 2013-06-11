#parse("main/Header.vm")
package com.nativelibs4java.opencl;
import java.util.Iterator;
import java.util.Arrays;
import com.nativelibs4java.util.Pair;
import static com.nativelibs4java.opencl.CLException.error;
import static com.nativelibs4java.opencl.CLException.errorString;
import static com.nativelibs4java.opencl.CLException.failedForLackOfMemory;
import static com.nativelibs4java.opencl.JavaCL.CL;
import static com.nativelibs4java.opencl.JavaCL.log;
import java.util.logging.Level;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_PROGRAM_BINARIES;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_PROGRAM_BINARY_SIZES;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_PROGRAM_BUILD_LOG;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_PROGRAM_SOURCE;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_SUCCESS;
import static org.bridj.util.DefaultParameterizedType.paramType;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.net.URL;

import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Arrays;
import java.util.Map;

import com.nativelibs4java.opencl.library.OpenCLLibrary.cl_device_id;
import com.nativelibs4java.opencl.library.OpenCLLibrary.cl_kernel;
import com.nativelibs4java.opencl.library.OpenCLLibrary.cl_program;
import com.nativelibs4java.util.IOUtils;
import com.ochafik.util.string.StringUtils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.Process;
import java.net.URL;
import java.util.Collection;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;

import org.bridj.*;
import static org.bridj.Pointer.*;

/**
 * OpenCL program.<br/>
 * An OpenCL program consists of a set of kernels that are identified as functions declared with the __kernel qualifier in the program source. OpenCL programs may also contain auxiliary functions and constant data that can be used by __kernel functions. The program executable can be generated online or offline by the OpenCL compiler for the appropriate target device(s).<br/>
 * A program object encapsulates the following information:
 * <ul>
 * <li>An associated context.</li>
 * <li>A program source or binary.</li>
 * <li>The latest successfully built program executable</li>
 * <li>The list of devices for which the program executable is built</li>
 * <li>The build options used and a build log. </li>
 * <li>The number of kernel objects currently attached.</li>
 * </ul>
 *
 * A program can be compiled on the fly (costly) but its binaries can be stored and
 * loaded back in subsequent executions to avoid recompilation.<br>
 * By default, program binaries are automatically cached on stable platforms (which currently exclude ATI Stream), 
 * but the caching can be forced on/off with * see {@link CLContext#setCacheBinaries(boolean) }.<br>
 * To create a program from sources, please use see {@link CLContext#createProgram(java.lang.String[]) } 
 * @author Olivier Chafik
 */
public class CLProgram extends CLAbstractEntity {

    protected final CLContext context;
    protected boolean loadedFromBinary;

	#declareInfosGetter("infos", "CL.clGetProgramInfo")

    CLDevice[] devices;
    CLProgram(CLContext context, CLDevice... devices) {
        super(0, true);
        this.context = context;
        this.devices = devices == null || devices.length == 0 ? context.getDevices() : devices;
    }
    CLProgram(CLContext context, Map<CLDevice, byte[]> binaries, String source) {
        super(0, true);
        this.context = context;
        this.source = source;

        setBinaries(binaries);
    }

    protected void setBinaries(Map<CLDevice, byte[]> binaries) {
        if (this.devices == null) {
            this.devices = new CLDevice[binaries.size()];
            int iDevice = 0;
            for (CLDevice device : binaries.keySet())
                this.devices[iDevice++] = device;
        }
        int nDevices = this.devices.length;
        if (binaries.size() != nDevices)
        		throw new IllegalArgumentException("Not enough binaries in provided map : expected " + nDevices + " (devices = " + Arrays.asList(devices) + "), got " + binaries.size() + " (" + binaries.keySet() + ")");
        
        #declareReusablePtrsAndPErr()
        Pointer<SizeT> lengths = ptrs.sizeT3_1.allocatedSizeTs(nDevices);
        Pointer<SizeT> deviceIds = ptrs.sizeT3_2.allocatedSizeTs(nDevices);
        Pointer<SizeT> binariesArray = ptrs.sizeT3_3.allocatedSizeTs(nDevices);
        Pointer<Byte>[] binariesMems = new Pointer[nDevices];

        int iDevice = 0;
        for (CLDevice device : devices) {
            byte[] binary = binaries.get(device);
            if (binary == null)
                throw new IllegalArgumentException("No binary for device " + device + " in provided binaries");

            binariesArray.setPointerAtIndex(iDevice, binariesMems[iDevice] = pointerToBytes(binary));
            lengths.setSizeTAtIndex(iDevice, binary.length);
            deviceIds.setSizeTAtIndex(iDevice, device.getEntity());
            
            iDevice++;
        }
        nDevices = iDevice;
        if (nDevices == 0)
            return;
        	
        int previousAttempts = 0;
        Pointer<Integer> statuses = ptrs.int3_1.allocatedInts(nDevices);
        do {
            setEntity(CL.clCreateProgramWithBinary(context.getEntity(), nDevices, getPeer(deviceIds), getPeer(lengths), getPeer(binariesArray), getPeer(statuses), getPeer(pErr)));
        } while (failedForLackOfMemory(pErr.getInt(), previousAttempts++));
        if (getEntity() != 0)
            loadedFromBinary = true;
    }

    /**
     * Write the compiled binaries of this program (for all devices it was compiled for), so that it can be restored later using {@link CLContext#loadProgram(java.io.InputStream) }
     * @param out will be closed
     * @throws CLBuildException
     * @throws IOException
     */
    public void store(OutputStream out) throws CLBuildException, IOException {
        writeBinaries(getBinaries(), getSource(), null, out);
    }
    
    private static final void addStoredEntry(ZipOutputStream zout, String name, byte[] data) throws IOException {
        ZipEntry ze = new ZipEntry(name);
        ze.setMethod(ZipEntry.STORED);
        ze.setSize(data.length);
        CRC32 crc = new CRC32();
		crc.update(data,0,data.length);
		ze.setCrc(crc.getValue());
        zout.putNextEntry(ze);
        zout.write(data);
        zout.closeEntry();
    }
	
    private static final String BinariesSignatureZipEntryName = "SIGNATURE", SourceZipEntryName = "SOURCE", textEncoding = "utf-8";
    public static void writeBinaries(Map<CLDevice, byte[]> binaries, String source, String contentSignatureString, OutputStream out) throws IOException {
        Map<String, byte[]> binaryBySignature = new HashMap<String, byte[]>();
        for (Map.Entry<CLDevice, byte[]> e : binaries.entrySet())
            binaryBySignature.put(e.getKey().createSignature(), e.getValue()); // Maybe multiple devices will have the same signature : too bad, we don't care and just write one binary per signature.

        ZipOutputStream zout = new ZipOutputStream(new GZIPOutputStream(new BufferedOutputStream(out)));
        if (contentSignatureString != null)
            addStoredEntry(zout, BinariesSignatureZipEntryName, contentSignatureString.getBytes(textEncoding));
        
        if (source != null)
        		addStoredEntry(zout, SourceZipEntryName, source.getBytes(textEncoding));
        			
        for (Map.Entry<String, byte[]> e : binaryBySignature.entrySet())
            addStoredEntry(zout, e.getKey(), e.getValue());
        
        zout.close();
    }
    public static Pair<Map<CLDevice, byte[]>, String> readBinaries(List<CLDevice> allowedDevices, String expectedContentSignatureString, InputStream in) throws IOException {
        Map<CLDevice, byte[]> ret = new HashMap<CLDevice, byte[]>();
        Map<String, List<CLDevice>> devicesBySignature = CLDevice.getDevicesBySignature(allowedDevices);

        ZipInputStream zin = new ZipInputStream(new GZIPInputStream(new BufferedInputStream(in)));
        ZipEntry ze;
        String source = null;
        
        boolean first = true;
        byte[] b = new byte[65536];
        while ((ze = zin.getNextEntry()) != null) {
            String signature = ze.getName();
            boolean isSignature = signature.equals(BinariesSignatureZipEntryName);
            if (first && !isSignature && expectedContentSignatureString != null ||
                !first && isSignature)
                throw new IOException("Expected signature to be the first zip entry, got '" + signature + "' instead !");

            first = false;
            int len;
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            while ((len = zin.read(b)) > 0)
                bout.write(b, 0, len);

            byte[] data = bout.toByteArray();
            if (isSignature) {
                if (expectedContentSignatureString != null) {
                    String contentSignatureString = new String(data, textEncoding);
                    if (!expectedContentSignatureString.equals(contentSignatureString))
                        throw new IOException("Content signature does not match expected one :\nExpected '" + expectedContentSignatureString + "',\nGot '" + contentSignatureString + "'");
                }
            } else if (signature.equals(SourceZipEntryName)) {
                source = new String(data, textEncoding);
            } else {
                List<CLDevice> devices = devicesBySignature.get(signature);
                for (CLDevice device : devices)
                    ret.put(device, data);
            }
        }
        zin.close();
        return new Pair<Map<CLDevice, byte[]>, String>(ret, source);
    }
    
	List<String> sources = new ArrayList<String>();
    Map<CLDevice, cl_program> programByDevice = new HashMap<CLDevice, cl_program>();

    public CLDevice[] getDevices() {
        return devices.clone();
    }

    /// Workaround to avoid crash of ATI Stream 2.0.0 final (beta 3 & 4 worked fine)
    public static boolean passMacrosAsSources = true;

    public synchronized void allocate() {
        if (isAllocated())
            throw new IllegalThreadStateException("Program was already allocated !");

        if (passMacrosAsSources) {
            if (macros != null && !macros.isEmpty()) {
                StringBuilder b = new StringBuilder();
                for (Map.Entry<String, Object> m : macros.entrySet())
                    b.append("#define " + m.getKey() + " " + m.getValue() + "\n");
                this.sources.add(0, b.toString());
            }
        }
        
		if (!"false".equals(System.getProperty("javacl.adjustDoubleExtension")) && !"0".equals(System.getenv("JAVACL_ADJUST_DOUBLE_EXTENSION"))) {
			for (int i = 0, len = sources.size(); i < len; i++) {
				String source = sources.get(i);
				for (CLDevice device : getDevices())
					source = device.replaceDoubleExtensionByExtensionActuallyAvailable(source);
				sources.set(i, source);
				// TODO keep different sources for each device !!!
			}
		}
        
        String[] sources = this.sources.toArray(new String[this.sources.size()]);
        #declareReusablePtrsAndPErr()
        
        Pointer<SizeT> pLengths = ptrs.sizeT3_1.allocatedSizeTs(sources.length);
        long[] lengths = new long[sources.length];
        for (int i = 0; i < sources.length; i++) {
            pLengths.setSizeTAtIndex(i, sources[i].length());
        }
        
        long program;
        int previousAttempts = 0;
        Pointer<Pointer<Byte>> pSources = pointerToCStrings(sources);
        do {
                program = CL.clCreateProgramWithSource(
                        context.getEntity(), 
                        sources.length, 
                        getPeer(pSources),
                        getPeer(pLengths), 
                        getPeer(pErr)
                );
        } while (failedForLackOfMemory(pErr.getInt(), previousAttempts++));
        Pointer.release(pSources);
        setEntity(program);
    }
    
    private boolean isAllocated() {
    	return super.getEntity() != 0;
    }
    /*
    @Override
    protected synchronized cl_program getEntity() {
        if (!isAllocated())
            allocate();

        return super.getEntity();
    }*/
    
    @Override
    protected synchronized long getEntity() {
        if (!isAllocated())
            allocate();

        return super.getEntity();
    }
	
    List<String> includes;
    
    /**
     * Add a path (file or URL) to the list of paths searched for included files.<br>
     * OpenCL kernels may contain <code>\#include "subpath/file.cl"</code> statements.<br>
     * This automatically adds a "-Ipath" argument to the compilator's command line options.<br>
     * Note that it's not necessary to add include paths for files that are in the classpath.
     * @param path A file or URL that points to the root path from which includes can be resolved. 
     */
    public synchronized void addInclude(String path) {
        if (includes == null)
            includes = new ArrayList<String>();
        includes.add(path);
        resolvedInclusions = null;
    }
	public synchronized void addSource(String src) {
        if (isAllocated())
            throw new IllegalThreadStateException("Program was already allocated : cannot add sources anymore.");
        sources.add(src);
        resolvedInclusions = null;
	}
    
    Map<String, URL> resolvedInclusions;
        
    protected Runnable copyIncludesToTemporaryDirectory() throws IOException {
        Map<String, URL> inclusions = resolveInclusions();
        File includesDir = JavaCL.createTempDirectory("includes", "", "includes");
        final List<File> filesToDelete = new ArrayList<File>();
        for (Map.Entry<String, URL> e : inclusions.entrySet()) {
            assert log(Level.INFO, "Copying include '" + e.getKey() + "' from '" + e.getValue() + "' to '" + includesDir + "'");
            File f = new File(includesDir, e.getKey().replace('/', File.separatorChar));
            File p = f.getParentFile();
            filesToDelete.add(f);
            if (p != null) {
            		p.mkdirs();
            		filesToDelete.add(p);
            }
            InputStream in = e.getValue().openStream();
            OutputStream out = new FileOutputStream(f);
            IOUtils.readWrite(in, out);
            in.close();
            out.close();
            f.deleteOnExit();
        }
        filesToDelete.add(includesDir);
        addInclude(includesDir.toString());
        return new Runnable() { public void run() {
        		for (File f : filesToDelete) 
        			f.delete();
        }};
    }
    public Map<String, URL> resolveInclusions() throws IOException {
        if (resolvedInclusions == null) {
			resolvedInclusions = new HashMap<String, URL>();
			for (String source : sources)
				resolveInclusions(source, resolvedInclusions);
		}
        return resolvedInclusions;
    }
    
    static Pattern includePattern = Pattern.compile("#\\s*include\\s*\"([^\"]+)\"");
    private void resolveInclusions(String source, Map<String, URL> ret) throws IOException {
    		List<String> includedPaths = new ArrayList<String>();
    		Matcher m = includePattern.matcher(source);
    		while (m.find()) {
    			includedPaths.add(m.group(1));
    		}
        for (String includedPath : includedPaths) {
            if (ret.containsKey(includedPath))
                continue;
            URL url = getIncludedSourceURL(includedPath);
            if (url == null) {
                assert log(Level.SEVERE, "Failed to resolve include '" + includedPath + "'");
            } else {
                String s = IOUtils.readText(url);
                ret.put(includedPath, url);
                resolveInclusions(s, ret);
            }
        }
    }

    public String getIncludedSourceContent(String path) throws IOException {
        URL url = getIncludedSourceURL(path);
        if (url == null)
            return null;
        
        String src = IOUtils.readText(url);
        return src;
    }
    
    public URL getIncludedSourceURL(String path) throws MalformedURLException {
        File f = new File(path);
        if (f.exists())
            return f.toURI().toURL();
        URL url = Platform.getClassLoader(getClass()).getResource(path);        
        if (url != null)
        		return url;
        	
        	if (includes != null)
            for (String include : includes) {
                f = new File(new File(include), path);
                if (f.exists())
                    return f.toURI().toURL();
                
                url = Platform.getClassLoader(getClass()).getResource(f.toString());
                if (url != null)
                    return url;
                
                try {
                		url = new URL(include + (include.endsWith("/") ? "" : "/") + path);
                		url.openStream().close();
                		return url;
                } catch (IOException ex) {
                		// Bad URL or impossible to read from the URL
                }
                	
            }
        
        return null;
    }
	
    String source;
	/**
	 * Get the source code of this program
	 */
	public synchronized String getSource() {
		if (source == null && !loadedFromBinary)
			source = infos.getString(getEntity(), CL_PROGRAM_SOURCE);
		
		return source;
	}

	/**
	 * Get the binaries of the program (one for each device, in order)
	 * @return map from each device the program was compiled for to the corresponding binary data
	 */
    public Map<CLDevice, byte[]> getBinaries() throws CLBuildException {
        synchronized (this) {
            if (!built)
                build();
        }
        
        Pointer<?> s = infos.getMemory(getEntity(), CL_PROGRAM_BINARY_SIZES);
        int n = (int)s.getValidBytes() / Platform.SIZE_T_SIZE;
        long[] sizes = s.getSizeTs(n);
        //int[] sizes = new int[n];
        //for (int i = 0; i < n; i++) {
        //	sizes[i] = s.getNativeLong(i * Native.LONG_SIZE).intValue();
        //}

        Pointer<?>[] binMems = (Pointer<?>[])new Pointer[n];
        #declareReusablePtrs()
        Pointer<SizeT> binPtrs = ptrs.sizeT3_1.allocatedSizeTs(n);//allocatePointers(n);
        for (int i = 0; i < n; i++) {
            binPtrs.setPointerAtIndex(i, binMems[i] = allocateBytes(sizes[i]));
        }
        error(infos.getInfo(getEntity(), CL_PROGRAM_BINARIES, binPtrs.getValidBytes(), binPtrs, null));

        Map<CLDevice, byte[]> ret = new HashMap<CLDevice, byte[]>(devices.length);
        int iBin = n == devices.length + 1 ? 1 : 0;
        for (int i = 0; i < devices.length; i++) {
            CLDevice device = devices[i];
            Pointer<?> bytes = binMems[iBin + i];
            if (bytes != null) {
                ret.put(device, bytes.getBytes((int)sizes[iBin + i]));
                Pointer.release(bytes);
            }
        }
        return ret;
    }

	/**
	 * Returns the context of this program
	 */
    public CLContext getContext() {
        return context;
    }
    Map<String, Object> macros;
    public CLProgram defineMacro(String name, Object value) {
        createMacros();
        macros.put(name, value);
        return this;
    }
    public CLProgram undefineMacro(String name) {
        if (macros != null)
            macros.remove(name);
        return this;
    }

    private void createMacros() {
        if (macros == null)
            macros = new LinkedHashMap<String, Object>();
    }
    public void defineMacros(Map<String, Object> macros) {
        createMacros();
        this.macros.putAll(macros);
    }
    List<String> extraBuildOptions;
    
    /**
     * Add the -cl-fast-relaxed-math compile option.<br>
     * Sets the optimization options -cl-finite-math-only and -cl-unsafe-math-optimizations. 
     * This allows optimizations for floating-point arithmetic that may violate the IEEE 754 standard and the OpenCL numerical compliance requirements defined in the specification in section 7.4 for single-precision floating-point, section 9.3.9 for double-precision floating-point, and edge case behavior in section 7.5. 
     * This option causes the preprocessor macro __FAST_RELAXED_MATH__ to be defined in the OpenCL program. <br>
     * Also see : <a href="http://www.khronos.org/registry/cl/sdk/1.0/docs/man/xhtml/clBuildProgram.html">Khronos' documentation for clBuildProgram</a>.
     */
    public void setFastRelaxedMath() {
    		addBuildOption("-cl-fast-relaxed-math");
    }
    
    /**
     * Add the -cl-no-signed-zero compile option.<br>
     * Allow optimizations for floating-point arithmetic that ignore the signedness of zero. 
     * IEEE 754 arithmetic specifies the behavior of distinct +0.0 and -0.0 values, which then prohibits simplification of expressions such as x+0.0 or 0.0*x (even with -clfinite-math only). 
     * This option implies that the sign of a zero result isn't significant. <br>
     * Also see : <a href="http://www.khronos.org/registry/cl/sdk/1.0/docs/man/xhtml/clBuildProgram.html">Khronos' documentation for clBuildProgram</a>.
     */
    public void setNoSignedZero() {
    		addBuildOption("-cl-no-signed-zero");
    }
    
    /**
     * Add the -cl-mad-enable compile option.<br>
     * Allow a * b + c to be replaced by a mad. The mad computes a * b + c with reduced accuracy. For example, some OpenCL devices implement mad as truncate the result of a * b before adding it to c.<br>
     * Also see : <a href="http://www.khronos.org/registry/cl/sdk/1.0/docs/man/xhtml/clBuildProgram.html">Khronos' documentation for clBuildProgram</a>.
     */
    public void setMadEnable() {
    		addBuildOption("-cl-mad-enable");
    }
    /**
     * Add the -cl-finite-math-only compile option.<br>
     * Allow optimizations for floating-point arithmetic that assume that arguments and results are not NaNs or infinites. This option may violate the OpenCL numerical compliance requirements defined in in section 7.4 for single-precision floating-point, section 9.3.9 for double-precision floating-point, and edge case behavior in section 7.5.<br>
     * Also see : <a href="http://www.khronos.org/registry/cl/sdk/1.0/docs/man/xhtml/clBuildProgram.html">Khronos' documentation for clBuildProgram</a>.
     */
    public void setFiniteMathOnly() {
    		addBuildOption("-cl-finite-math-only");
    }
    /**
     * Add the -cl-unsafe-math-optimizations option.<br>
     * Allow optimizations for floating-point arithmetic that (a) assume that arguments and results are valid, (b) may violate IEEE 754 standard and (c) may violate the OpenCL numerical compliance requirements as defined in section 7.4 for single-precision floating-point, section 9.3.9 for double-precision floating-point, and edge case behavior in section 7.5. This option includes the -cl-no-signed-zeros and -cl-mad-enable options.<br>
     * Also see : <a href="http://www.khronos.org/registry/cl/sdk/1.0/docs/man/xhtml/clBuildProgram.html">Khronos' documentation for clBuildProgram</a>.
     */
    public void setUnsafeMathOptimizations() {
    		addBuildOption("-cl-unsafe-math-optimizations");
    }
    /**
     * Add the <a href="http://www.cs.cmu.edu/afs/cs/academic/class/15668-s11/www/cuda-doc/OpenCL_Extensions/cl_nv_compiler_options.txt">-cl-nv-verbose</a> compilation option (<b><i>NVIDIA GPUs only</i></b>)<br>
     * Enable verbose mode. Output will be reported in JavaCL's log at the INFO level
     */
    public void setNVVerbose() {
		addBuildOption("-cl-nv-verbose");
    }
    /**
     * Add the <a href="http://www.cs.cmu.edu/afs/cs/academic/class/15668-s11/www/cuda-doc/OpenCL_Extensions/cl_nv_compiler_options.txt">-cl-nv-maxrregcount=N</a> compilation option (<b><i>NVIDIA GPUs only</i></b>)<br>
     * Specify the maximum number of registers that GPU functions can use.
     * Until a function-specific limit, a higher value will generally increase
     * the performance of individual GPU threads that execute this function.
     * However, because thread registers are allocated from a global register
     * pool on each GPU, a higher value of this option will also reduce the
     * maximum thread block size, thereby reducing the amount of thread
     * parallelism. Hence, a good maxrregcount value is the result of a
     * trade-off.
     * If this option is not specified, then no maximum is assumed. Otherwise
     * the specified value will be rounded to the next multiple of 4 registers
     * until the GPU specific maximum of 128 registers.
     * @param N positive integer
     */
    public void setNVMaximumRegistryCount(int N) {
		addBuildOption("-cl-nv-maxrregcount=" + N);
    }
    /**
     * Add the <a href="http://www.cs.cmu.edu/afs/cs/academic/class/15668-s11/www/cuda-doc/OpenCL_Extensions/cl_nv_compiler_options.txt">-cl-nv-opt-level</a> compilation option (<b><i>NVIDIA GPUs only</i></b>)<br>
     * Specify optimization level (default value: 3)
     * @param N positive integer, or 0 (no optimization).
     */
    public void setNVOptimizationLevel(int N) {
		addBuildOption("-cl-nv-opt-level=" + N);
    }
    
    /**
     * Please see <a href="http://www.khronos.org/registry/cl/sdk/1.0/docs/man/xhtml/clBuildProgram.html">OpenCL's clBuildProgram documentation</a> for details on supported build options.
     */
    public synchronized void addBuildOption(String option) {
		if (option.startsWith("-I")) {
			addInclude(option.substring(2));
			return;
		}
    		if (extraBuildOptions == null)
    			extraBuildOptions = new ArrayList<String>();
    		
    		extraBuildOptions.add(option);
    }
    
    protected String getOptionsString() {
        StringBuilder b = new StringBuilder("-DJAVACL=1 ");
        
        if (extraBuildOptions != null) {
            if (JavaCL.debug) {
                boolean isCPUOnly = true;
                for (CLDevice device : getDevices()) {
                    if (!device.getType().contains(CLDevice.Type.CPU)) {
                        isCPUOnly = false;
                        break;
                    }
                }
                if (!isCPUOnly)
                    log(Level.WARNING, "Debug mode : cannot only compile with debug flags on a CPU-only context");
                else {
                    for (Iterator<String> it = extraBuildOptions.iterator(); it.hasNext();) {
                        String opt = it.next();
                        if ((opt.startsWith("-O") || opt.startsWith("-g:")) && !JavaCL.DEBUG_COMPILER_FLAGS.contains(opt)) {
                            log(Level.WARNING, "Debug mode : removed argument \"" + opt + "\" from OpenCL compiler command-line arguments");
                            it.remove();
                        }
                    }
                    extraBuildOptions.addAll(JavaCL.DEBUG_COMPILER_FLAGS);
                    log(Level.ALL, "Debug mode : added OpenCL compiler command-line arguments \"" + StringUtils.implode(JavaCL.DEBUG_COMPILER_FLAGS, " ") + "\"");
                }
            }
            for (String option : extraBuildOptions)
                b.append(option.contains(" ") || option.contains("\"") ? "\"" + option.replaceAll("\"", "\\\"") + "\"" : option).append(' ');
            
        }
    			
        // http://www.khronos.org/registry/cl/sdk/1.0/docs/man/xhtml/clBuildProgram.html
        //b.append("-O2 -cl-no-signed-zeros -cl-unsafe-math-optimizations -cl-finite-math-only -cl-fast-relaxed-math -cl-strict-aliasing ");
        
        if (!passMacrosAsSources && macros != null && !macros.isEmpty())
            for (Map.Entry<String, Object> m : macros.entrySet())
                b.append("-D" + m.getKey() + "=" + m.getValue() + " ");

        if (includes != null)
            for (String path : includes)
            		if (new File(path).exists()) // path can be an URL as well, in which case it's copied to a local file copyIncludesToTemporaryDirectory()
            			b.append("-I").append(path).append(' ');
        
        return b.toString();
    }
    
    private volatile Boolean cached;
    public synchronized void setCached(boolean cached) {
    		this.cached = cached;
    }
    public synchronized boolean isCached() {
		if (cached == null)
			cached = context.getCacheBinaries();
		return cached;
    }

    protected String computeCacheSignature() throws IOException {
    		StringBuilder b = new StringBuilder(16 * 1024);
    		getContext().getPlatform().toString(b);
    		b.append('\n');
    		for (CLDevice device : getDevices())
    			b.append(device).append('\n');
    		
    		b.append(getOptionsString()).append('\n');
    		if (macros != null && !macros.isEmpty())
    			for (Map.Entry<String, Object> m : macros.entrySet())
                b.append("-D").append(m.getKey()).append('=').append(m.getValue()).append('\n');
        
        if (includes != null && !includes.isEmpty())
            for (String path : includes)
                b.append("-I").append(path).append('\n');
        
        if (sources != null && !sources.isEmpty())
			for (String source : sources)
				b.append(source).append('\n');
    		
		Map<String, URL> inclusions = resolveInclusions();
		if (inclusions != null && !inclusions.isEmpty())
			for (Map.Entry<String, URL> e : inclusions.entrySet()) {
					URLConnection con = e.getValue().openConnection();
					InputStream in = con.getInputStream();
					b.append('#').append(e.getKey()).append(con.getLastModified()).append('\n');
					in.close();
			}
		
        for (String name : propsToIncludeInSignature)
        		b.append(name).append('=').append(System.getProperty(name)).append('\n');
        	
    		return b.toString();
    }
    private final String[] propsToIncludeInSignature = new String[] {
		//"java.vm.version", // probably superfluous... 
		"os.version",
		"os.name"
	};
  
	private String getProgramBuildInfo(long pgm, long deviceId) {
		Pointer<SizeT> pLen = allocateSizeT();
		error(CL.clGetProgramBuildInfo(pgm, deviceId, CL_PROGRAM_BUILD_LOG, 0, 0, getPeer(pLen)));
		long len = pLen.getSizeT();
		Pointer<?> buffer = allocateBytes(len);
		error(CL.clGetProgramBuildInfo(pgm, deviceId, CL_PROGRAM_BUILD_LOG, len, getPeer(buffer), 0));
		String s = buffer.getCString();
		Pointer.release(pLen);
		Pointer.release(buffer);
		return s;
	}  
	private Set<String> getProgramBuildInfo(long pgm, Pointer<SizeT> deviceIds) {
		Set<String> errs = new HashSet<String>();
		if (deviceIds == null) {
		  String s = getProgramBuildInfo(pgm, 0);
			if (s.length() > 0)
				errs.add(s);
		} else {
			for (SizeT device : deviceIds) {
			  String s = getProgramBuildInfo(pgm, device.longValue());
				if (s.length() > 0)
					errs.add(s);
			}
		}
		return errs;
	}
    boolean built;
	/**
	 * Returns the context of this program
	 */
    public synchronized CLProgram build() throws CLBuildException {
        if (built)
            throw new IllegalThreadStateException("Program was already built !");
        
        String contentSignature = null;
        File cacheFile = null;
        boolean readBinaries = false;
        if (isCached()) {
        		try {
        			contentSignature = computeCacheSignature();
        			byte[] sha = java.security.MessageDigest.getInstance("MD5").digest(contentSignature.getBytes(textEncoding));
        			StringBuilder shab = new StringBuilder();
        			for (byte b : sha)
        				shab.append(Integer.toHexString(b & 0xff));
        			String hash = shab.toString();
        			cacheFile = new File(JavaCL.userCacheDir, hash);
        			if (cacheFile.exists()) {
					Pair<Map<CLDevice, byte[]>, String> bins = readBinaries(Arrays.asList(getDevices()), contentSignature, new FileInputStream(cacheFile));
					setBinaries(bins.getFirst());
					this.source = bins.getSecond();
					assert log(Level.INFO, "Read binaries cache from '" + cacheFile + "'");
					readBinaries = true;
				}
        		} catch (Throwable ex) {
        			assert log(Level.WARNING, "Failed to load cached program : " + ex.getMessage());
        			setEntity(0);
        		}
        }
        
        if (!isAllocated())
            allocate();

        Runnable deleteTempFiles = null;
        if (!readBinaries)
			try {
					deleteTempFiles = copyIncludesToTemporaryDirectory();
			} catch (IOException ex) {
					throw new CLBuildException(this, ex.toString(), Collections.EMPTY_LIST);
			}
        
        int nDevices = devices.length;
        Pointer<SizeT> deviceIds = null;
        if (nDevices != 0) {
            deviceIds = allocateSizeTs(nDevices);
            for (int i = 0; i < nDevices; i++)
                deviceIds.setSizeTAtIndex(i, devices[i].getEntity());
        }
        Pointer<Byte> pOptions = pointerToCString(getOptionsString());
        int err = CL.clBuildProgram(
            getEntity(), 
            nDevices, 
            getPeer(deviceIds), 
            getPeer(pOptions), 
            0, 
            0
		);
        Pointer.release(pOptions);
        Set<String> errors = getProgramBuildInfo(getEntity(), deviceIds);
        
        if (err != CL_SUCCESS) {
            throw new CLBuildException(this, "Compilation failure : " + errorString(err) + " (devices: " + Arrays.asList(getDevices()) + ")", errors);
        } else {
            if (!errors.isEmpty())
                JavaCL.log(Level.INFO, "Build info :\n\t" + StringUtils.implode(errors, "\n\t"));	
        }
        built = true;
        if (deleteTempFiles != null)
        		deleteTempFiles.run();
        
        	if (isCached() && !readBinaries && !loadedFromBinary) {
        		JavaCL.userCacheDir.mkdirs();
        		try {
                Map<CLDevice, byte[]> binaries = getBinaries();
                if (!binaries.isEmpty()) {
                    writeBinaries(getBinaries(), getSource(), contentSignature, new FileOutputStream(cacheFile));
                    assert log(Level.INFO, "Wrote binaries cache to '" + cacheFile + "'"); 
                }
        		} catch (Exception ex) {
        			new IOException("[JavaCL] Failed to cache program", ex).printStackTrace();
        		}
        	}
        			
        return this;
    }

    @Override
    protected void clear() {
        error(CL.clReleaseProgram(getEntity()));
    }

    /**
#documentCallsFunction("clCreateKernelsInProgram")
     * Return all the kernels found in the program.
     */
    public CLKernel[] createKernels() throws CLBuildException {
        synchronized (this) {
           if (!built)
               build();
        }
        #declareReusablePtrs()
        Pointer<Integer> pCount = ptrs.int1;
        int previousAttempts = 0;
        while (failedForLackOfMemory(CL.clCreateKernelsInProgram(getEntity(), 0, 0, getPeer(pCount)), previousAttempts++)) {}

        int count = pCount.getInt();
        Pointer<SizeT> kerns = ptrs.sizeT3_1.allocatedSizeTs(count);
        previousAttempts = 0;
        while (failedForLackOfMemory(CL.clCreateKernelsInProgram(getEntity(), count, getPeer(kerns), getPeer(pCount)), previousAttempts++)) {}

        CLKernel[] kernels = new CLKernel[count];
        for (int i = 0; i < count; i++)
            kernels[i] = new CLKernel(this, null, kerns.getSizeTAtIndex(i));

        return kernels;
    }

    /**
#documentCallsFunction("clCreateKernel")
     * Find a kernel by its functionName, and optionally bind some arguments to it.
     */
    public CLKernel createKernel(String name, Object... args) throws CLBuildException {
        synchronized (this) {
            if (!built)
                build();
        }
        #declareReusablePtrsAndPErr()
        Pointer<Byte> pName = pointerToCString(name);
		long kernel;
		int previousAttempts = 0;
		do {
			kernel = CL.clCreateKernel(getEntity(), getPeer(pName), getPeer(pErr));
		} while (failedForLackOfMemory(pErr.getInt(), previousAttempts++));
        Pointer.release(pName);

        CLKernel kn = new CLKernel(this, name, kernel);
        if (args.length != 0)
        	kn.setArgs(args);
        return kn;
    }
}
