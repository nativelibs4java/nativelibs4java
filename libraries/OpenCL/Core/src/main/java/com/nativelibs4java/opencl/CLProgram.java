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
import static com.nativelibs4java.opencl.CLException.error;
import static com.nativelibs4java.opencl.CLException.errorString;
import static com.nativelibs4java.opencl.CLException.failedForLackOfMemory;
import static com.nativelibs4java.opencl.JavaCL.CL;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_PROGRAM_BINARIES;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_PROGRAM_BINARY_SIZES;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_PROGRAM_BUILD_LOG;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_PROGRAM_SOURCE;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_SUCCESS;
import static com.nativelibs4java.util.JNAUtils.readNSArray;
import static com.nativelibs4java.util.JNAUtils.toNS;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.MalformedURLException;

import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Arrays;
import java.util.Map;

import com.nativelibs4java.opencl.library.OpenCLLibrary.cl_device_id;
import com.nativelibs4java.opencl.library.OpenCLLibrary.cl_kernel;
import com.nativelibs4java.opencl.library.OpenCLLibrary.cl_program;
import com.nativelibs4java.util.NIOUtils;
import com.ochafik.io.IOUtils;
import com.ochafik.io.ReadText;
import com.ochafik.lang.jnaerator.runtime.NativeSize;
import com.ochafik.lang.jnaerator.runtime.NativeSizeByReference;
import com.ochafik.util.string.RegexUtils;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.Process;
import java.net.URL;
import java.util.Collection;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

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
 * loaded back in subsequent executions to avoid recompilation.
 * @see CLContext#createProgram(java.lang.String[]) 
 * @author Olivier Chafik
 */
public class CLProgram extends CLAbstractEntity<cl_program> {

    protected final CLContext context;

	private static CLInfoGetter<cl_program> infos = new CLInfoGetter<cl_program>() {
		@Override
		protected int getInfo(cl_program entity, int infoTypeEnum, NativeSize size, Pointer out, NativeSizeByReference sizeOut) {
			return CL.clGetProgramInfo(entity, infoTypeEnum, size, out, sizeOut);
		}
	};

    CLDevice[] devices;
    CLProgram(CLContext context, CLDevice... devices) {
        super(null, true);
        this.context = context;
        this.devices = devices == null || devices.length == 0 ? context.getDevices() : devices;
    }
	CLProgram(CLContext context, Map<CLDevice, byte[]> binaries) {
		super(null, true);
		this.context = context;

		setBinaries(binaries);
	}
	protected void setBinaries(Map<CLDevice, byte[]> binaries) {
        int nDevices = binaries.size();
        this.devices = new CLDevice[nDevices];
        NativeSize[] lengths = new NativeSize[nDevices];
		cl_device_id[] deviceIds = new cl_device_id[nDevices];
		Memory binariesArray = new Memory(Pointer.SIZE * nDevices);
		Memory[] binariesMems = new Memory[nDevices];

        int iDevice = 0;
        for (Map.Entry<CLDevice, byte[]> e : binaries.entrySet())
        {
            CLDevice device = e.getKey();
            byte[] binary = e.getValue();

            Memory binaryMem = binariesMems[iDevice] = new Memory(binary.length);
			binaryMem.write(0, binary, 0, binary.length);
			binariesArray.setPointer(iDevice * Pointer.SIZE, binaryMem);

            lengths[iDevice] = toNS(binary.length);
			deviceIds[iDevice] = (devices[iDevice] = device).getEntity();

            iDevice++;
        }
		PointerByReference binariesPtr = new PointerByReference();
        binariesPtr.setPointer(binariesArray);
        
        IntBuffer errBuff = NIOUtils.directInts(1, ByteOrder.nativeOrder());
        int previousAttempts = 0;
        IntBuffer statuses = NIOUtils.directInts(nDevices, ByteOrder.nativeOrder());
		do {
			entity = CL.clCreateProgramWithBinary(context.getEntity(), nDevices, deviceIds, lengths, binariesPtr, statuses, errBuff);
		} while (failedForLackOfMemory(errBuff.get(0), previousAttempts++));
	}

    /**
     * Write the compiled binaries of this program (for all devices it was compiled for), so that it can be restored later using {@link CLContext#loadProgram(java.io.InputStream) }
     * @param out will be closed
     * @throws CLBuildException
     * @throws IOException
     */
    public void store(OutputStream out) throws CLBuildException, IOException {
        writeBinaries(getBinaries(), null, out);
    }
    
    private static final String BinariesSignatureZipEntryName = "SIGNATURE";
    public static void writeBinaries(Map<CLDevice, byte[]> binaries, String contentSignatureString, OutputStream out) throws IOException {
        Map<String, byte[]> binaryBySignature = new HashMap<String, byte[]>();
        for (Map.Entry<CLDevice, byte[]> e : binaries.entrySet())
            binaryBySignature.put(e.getKey().createSignature(), e.getValue()); // Maybe multiple devices will have the same signature : too bad, we don't care and just write one binary per signature.

        ZipOutputStream zout = new ZipOutputStream(out);
        if (contentSignatureString != null) {
			ZipEntry ze = new ZipEntry(BinariesSignatureZipEntryName);
			byte[] contentSignatureBytes = contentSignatureString.getBytes("utf-8");
			ze.setSize(contentSignatureBytes.length);
			zout.putNextEntry(ze);
			zout.write(contentSignatureBytes);
			zout.closeEntry();
		}
        
        for (Map.Entry<String, byte[]> e : binaryBySignature.entrySet()) {
            String name = e.getKey();
            byte[] data = e.getValue();
            ZipEntry ze = new ZipEntry(name);
            ze.setSize(data.length);
            zout.putNextEntry(ze);
            zout.write(data);
            zout.closeEntry();
        }
        zout.close();
    }
    public static Map<CLDevice, byte[]> readBinaries(List<CLDevice> allowedDevices, String expectedContentSignatureString, InputStream in) throws IOException {
        Map<CLDevice, byte[]> ret = new HashMap<CLDevice, byte[]>();
        Map<String, List<CLDevice>> devicesBySignature = CLDevice.getDevicesBySignature(allowedDevices);

        ZipInputStream zin = new ZipInputStream(in);
        ZipEntry ze;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();

        boolean first = true;
        byte[] b = new byte[1024];
        while ((ze = zin.getNextEntry()) != null) {
            String signature = ze.getName();
            boolean isSignature = signature.equals(BinariesSignatureZipEntryName);
            if (first && !isSignature && expectedContentSignatureString != null ||
            		!first && isSignature)
            		throw new IOException("Expected signature to be the first zip entry, got '" + signature + "' instead !");
            	
            	first = false;
            bout.reset();
            int len;
            while ((len = zin.read(b)) > 0)
                bout.write(b, 0, len);

            byte[] data = bout.toByteArray();
            if (isSignature) {
            		if (expectedContentSignatureString != null) {
					String contentSignatureString = new String(data, "utf-8");
					if (!expectedContentSignatureString.equals(contentSignatureString))
						throw new IOException("Content signature does not match expected one :\nExpected '" + expectedContentSignatureString + "',\nGot '" + contentSignatureString + "'");
				}
			} else {
				List<CLDevice> devices = devicesBySignature.get(signature);
				for (CLDevice device : devices)
					ret.put(device, data);
			}
        }
        zin.close();
        return ret;
    }
    
	List<String> sources = new ArrayList<String>();
    Map<CLDevice, cl_program> programByDevice = new HashMap<CLDevice, cl_program>();

    public CLDevice[] getDevices() {
        return devices.clone();
    }

    /// Workaround to avoid crash of ATI Stream 2.0.0 final (beta 3 & 4 worked fine)
    public static boolean passMacrosAsSources = true;

    public synchronized void allocate() {
        if (entity != null)
            throw new IllegalThreadStateException("Program was already allocated !");

        if (passMacrosAsSources) {
            if (macros != null && !macros.isEmpty()) {
                StringBuilder b = new StringBuilder();
                for (Map.Entry<String, Object> m : macros.entrySet())
                    b.append("#define " + m.getKey() + " " + m.getValue() + "\n");
                this.sources.add(0, b.toString());
            }
        }

        String[] sources = this.sources.toArray(new String[this.sources.size()]);
        NativeSize[] lengths = new NativeSize[sources.length];
        for (int i = 0; i < sources.length; i++) {
            lengths[i] = toNS(sources[i].length());
        }
        IntBuffer errBuff = NIOUtils.directInts(1, ByteOrder.nativeOrder());
        cl_program program;
		int previousAttempts = 0;
		do {
			program = CL.clCreateProgramWithSource(context.getEntity(), sources.length, sources, lengths, errBuff);
		} while (failedForLackOfMemory(errBuff.get(0), previousAttempts++));
        entity = program;
    }
    
    @Override
    protected synchronized cl_program getEntity() {
        if (entity == null)
            allocate();

        return entity;
    }
	
    List<String> includes;
    
    /**
     * Add a path (file or URL) to the list of paths searched for included files.<br>
     * OpenCL kernels may contain <code>#include "subpath/file.cl"</code> statements.<br>
     * This automatically adds a "-Ipath" argument to the compilator's command line options.<br>
     * Note that it's not necessary to add include paths for files that are in the classpath.
     * @param path A file or URL that points to the root path from which includes can be resolved. 
     */
    public synchronized void addInclude(String path) {
        if (includes == null)
            includes = new ArrayList<String>();
        includes.add(path);
    }
	public synchronized void addSource(String src) {
        if (entity != null)
            throw new IllegalThreadStateException("Program was already allocated : cannot add sources anymore.");
        sources.add(src);
	}
    
    static File tempIncludes = new File(new File(System.getProperty("java.io.tmpdir")), "JavaCL");
    public Runnable copyIncludesToTemporaryDirectory() throws IOException {
        Map<String, URL> inclusions = resolveInclusions();
        tempIncludes.mkdirs();
        File includesDir = File.createTempFile("includes", "", tempIncludes);
        includesDir.delete();
        includesDir.mkdirs();
        final List<File> filesToDelete = new ArrayList<File>();
        for (Map.Entry<String, URL> e : inclusions.entrySet()) {
            System.out.println("[JavaCL] Copying include '" + e.getKey() + "' from '" + e.getValue() + "' to '" + includesDir + "'");
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
        Map<String, URL> ret = new HashMap<String, URL>();
        for (String source : sources)
            resolveInclusions(source, ret);
        return ret;
    }
    
    static Pattern includePattern = Pattern.compile("#\\s*include\\s*\"([^\"]+)\"");
    private void resolveInclusions(String source, Map<String, URL> ret) throws IOException {
        Collection<String> includedPaths = RegexUtils.find(source, includePattern, 1);
        //System.out.println("Included paths = " + includedPaths);
        for (String includedPath : includedPaths) {
            if (ret.containsKey(includedPath))
                continue;
            URL url = getIncludedSourceURL(includedPath);
            if (url == null) {
                System.err.println("[JavaCL] Failed to resolve include '" + includedPath + "'");
            } else {
                String s = ReadText.readText(url);
                ret.put(includedPath, url);
                resolveInclusions(s, ret);
            }
        }
    }

    public String getIncludedSourceContent(String path) throws IOException {
        URL url = getIncludedSourceURL(path);
        if (url == null)
            return null;
        
        String src = ReadText.readText(url);
        return src;
    }
    public URL getIncludedSourceURL(String path) throws MalformedURLException {
        File f = new File(path);
        if (f.exists())
            return f.toURI().toURL();
        URL url = getClass().getClassLoader().getResource(path);        
        if (url != null)
        		return url;
        	
        	if (includes != null)
            for (String include : includes) {
                f = new File(new File(include), path);
                if (f.exists())
                    return f.toURI().toURL();
                
                url = getClass().getClassLoader().getResource(f.toString());
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
	
	/**
	 * Get the source code of this program
	 */
	public String getSource() {
		return infos.getString(getEntity(), CL_PROGRAM_SOURCE);
	}

	/**
	 * Get the binaries of the program (one for each device, in order)
	 * @return
	 */
    public Map<CLDevice, byte[]> getBinaries() throws CLBuildException {
        synchronized (this) {
            if (!built)
                build();
        }
        
		Memory s = infos.getMemory(getEntity(), CL_PROGRAM_BINARY_SIZES);
		int n = (int)s.getSize() / Native.SIZE_T_SIZE;
		NativeSize[] sizes = readNSArray(s, n);
		//int[] sizes = new int[n];
		//for (int i = 0; i < n; i++) {
		//	sizes[i] = s.getNativeLong(i * Native.LONG_SIZE).intValue();
		//}

		Memory[] binMems = new Memory[n];
		Memory ptrs = new Memory(n * Native.POINTER_SIZE);
		for (int i = 0; i < n; i++) {
			ptrs.setPointer(i * Native.POINTER_SIZE, binMems[i] = new Memory(sizes[i].intValue()));
		}
		error(infos.getInfo(getEntity(), CL_PROGRAM_BINARIES, toNS(ptrs.getSize() * Native.POINTER_SIZE), ptrs, null));

		Map<CLDevice, byte[]> ret = new HashMap<CLDevice, byte[]>(devices.length);
        for (int i = 0; i < n; i++) {
            CLDevice device = devices[i];
			Memory bytes = binMems[i];
            ret.put(device, bytes.getByteArray(0, sizes[i].intValue()));
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
     * Please see <a href="http://www.khronos.org/registry/cl/sdk/1.0/docs/man/xhtml/clBuildProgram.html">OpenCL's clBuildProgram documentation</a> for details on supported build options.
     */
    public synchronized void addBuildOption(String option) {
    		if (extraBuildOptions == null)
    			extraBuildOptions = new ArrayList<String>();
    		
    		extraBuildOptions.add(option);
    }
    	
    protected String getOptionsString() {
        StringBuilder b = new StringBuilder("-DJAVACL=1 ");
        
        if (extraBuildOptions != null)
        		for (String option : extraBuildOptions)
    				b.append(option).append(' ');
    			
        // http://www.khronos.org/registry/cl/sdk/1.0/docs/man/xhtml/clBuildProgram.html
        //b.append("-O2 -cl-no-signed-zeros -cl-unsafe-math-optimizations -cl-finite-math-only -cl-fast-relaxed-math -cl-strict-aliasing ");
        
        if (!passMacrosAsSources && macros != null && !macros.isEmpty())
            for (Map.Entry<String, Object> m : macros.entrySet())
                b.append("-D" + m.getKey() + "=" + m.getValue() + " ");

        if (includes != null)
            for (String path : includes)
                b.append("-I").append(path).append(' ');
        
            System.out.println("OpenCL build options = " + b);
        return b.toString();
    }
    
    boolean cached = JavaCL.cacheBinaries;
    public void setCached(boolean cached) {
    		this.cached = cached;
    }
    public boolean isCached() {
    		return cached;
    }

    protected String computeCacheSignature() {
    		StringBuilder b = new StringBuilder(1024);
    		for (CLDevice device : getDevices())
    			b.append(device).append("\n");
    		
    		b.append(getOptionsString()).append('\n');
    		if (macros != null)
    			for (Map.Entry<String, Object> m : macros.entrySet())
                b.append("-D").append(m.getKey()).append("=").append(m.getValue()).append('\n');
        
        if (includes != null)
            for (String path : includes)
                b.append("-I").append(path).append('\n');
        
        if (sources != null)
			for (String source : sources)
				b.append(source).append("\n");
    		
    		return b.toString();
    }
    
    //static File cacheDirectory = new File(new File(System.getProperty("user.home"), ".javacl"), "cachedProgramBinaries");
    static File cacheDirectory = new File(new File(System.getProperty("java.io.tmpdir"), "JavaCL"), "cachedProgramBinaries");
    
    boolean built;
	/**
	 * Returns the context of this program
	 */
    public synchronized CLProgram build() throws CLBuildException {
        if (built)
            throw new IllegalThreadStateException("Program was already built !");
        
        String contentSignature = null;
        File cacheFile = null;
        
        if (isCached()) {
        		try {
        			contentSignature = computeCacheSignature();
        			byte[] sha = java.security.MessageDigest.getInstance("SHA-1").digest(contentSignature.getBytes("utf-8"));
        			StringBuilder shab = new StringBuilder();
        			for (byte b : sha)
        				shab.append(Integer.toHexString(b & 0xff));
        			String hash = shab.toString();
        			cacheFile = new File(cacheDirectory, hash);
        			if (cacheFile.exists()) {
					Map<CLDevice, byte[]> bins = readBinaries(Arrays.asList(getDevices()), contentSignature, new FileInputStream(cacheFile));
					setBinaries(bins);
					//createKernels();
					System.out.println("[JavaCL] Read binaries cache from '" + cacheFile + "'");
				}
        		} catch (Exception ex) {
        			System.err.println("[JavaCL] Failed to load cached program :"); 
        			ex.printStackTrace();
        		}
        }
        
        if (entity == null)
            allocate();

        Runnable deleteTempFiles = null;
        try {
        		deleteTempFiles = copyIncludesToTemporaryDirectory();
        } catch (IOException ex) {
        		throw new CLBuildException(this, ex.toString(), Collections.EMPTY_LIST);
        }
        
        int nDevices = devices.length;
        cl_device_id[] deviceIds = null;
        if (nDevices != 0) {
            deviceIds = new cl_device_id[nDevices];
            for (int i = 0; i < nDevices; i++)
                deviceIds[i] = devices[i].getEntity();
        }
        int err = CL.clBuildProgram(getEntity(), nDevices, deviceIds, getOptionsString(), null, null);
        //int err = CL.clBuildProgram(getEntity(), 0, null, getOptionsString(), null, null);
        if (err != CL_SUCCESS) {//BUILD_PROGRAM_FAILURE) {
            NativeSizeByReference len = new NativeSizeByReference();
            int bufLen = 2048 * 32; //TODO find proper size
            Memory buffer = new Memory(bufLen);

            HashSet<String> errs = new HashSet<String>();
            if (deviceIds == null) {
                error(CL.clGetProgramBuildInfo(getEntity(), null, CL_PROGRAM_BUILD_LOG, toNS(bufLen), buffer, len));
                String s = buffer.getString(0);
                errs.add(s);
            } else
                for (cl_device_id device : deviceIds) {
                    error(CL.clGetProgramBuildInfo(getEntity(), device, CL_PROGRAM_BUILD_LOG, toNS(bufLen), buffer, len));
                    String s = buffer.getString(0);
                    errs.add(s);
                }
                
            throw new CLBuildException(this, "Compilation failure : " + errorString(err), errs);
        }
        built = true;
        if (deleteTempFiles != null)
        		deleteTempFiles.run();
        
        	if (isCached()) {
        		cacheDirectory.mkdirs();
        		try {
        			writeBinaries(getBinaries(), contentSignature, new FileOutputStream(cacheFile));
        			System.out.println("[JavaCL] Wrote binaries cache to '" + cacheFile + "'"); 
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
	 * Return all the kernels found in the program.
	 */
	public CLKernel[] createKernels() throws CLBuildException {
        synchronized (this) {
            if (!built)
                build();
        }
		IntByReference pCount = new IntByReference();
		int previousAttempts = 0;
		while (failedForLackOfMemory(CL.clCreateKernelsInProgram(getEntity(), 0, (cl_kernel[])null, pCount), previousAttempts++)) {}

		int count = pCount.getValue();
		cl_kernel[] kerns = new cl_kernel[count];
		previousAttempts = 0;
		while (failedForLackOfMemory(CL.clCreateKernelsInProgram(getEntity(), count, kerns, pCount), previousAttempts++)) {}

		CLKernel[] kernels = new CLKernel[count];
		for (int i = 0; i < count; i++)
			kernels[i] = new CLKernel(this, null, kerns[i]);

		return kernels;
	}

    /**
     * Find a kernel by its functionName, and optionally bind some arguments to it.
     */
    public CLKernel createKernel(String name, Object... args) throws CLBuildException {
        synchronized (this) {
            if (!built)
                build();
        }
        IntBuffer errBuff = NIOUtils.directInts(1, ByteOrder.nativeOrder());
        cl_kernel kernel;
		int previousAttempts = 0;
		do {
			kernel = CL.clCreateKernel(getEntity(), name, errBuff);
		} while (failedForLackOfMemory(errBuff.get(0), previousAttempts++));

        CLKernel kn = new CLKernel(this, name, kernel);
        kn.setArgs(args);
        return kn;
    }


}
