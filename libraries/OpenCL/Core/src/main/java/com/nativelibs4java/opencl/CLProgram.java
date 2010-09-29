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
import java.util.Arrays;
import static com.nativelibs4java.opencl.CLException.error;
import static com.nativelibs4java.opencl.CLException.errorString;
import static com.nativelibs4java.opencl.CLException.failedForLackOfMemory;
import static com.nativelibs4java.opencl.JavaCL.CL;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_PROGRAM_BINARIES;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_PROGRAM_BINARY_SIZES;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_PROGRAM_BUILD_LOG;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_PROGRAM_SOURCE;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_SUCCESS;
import static org.bridj.util.DefaultParameterizedType.paramType;
import java.io.IOException;

import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.nativelibs4java.opencl.library.OpenCLLibrary.cl_device_id;
import com.nativelibs4java.opencl.library.OpenCLLibrary.cl_kernel;
import com.nativelibs4java.opencl.library.OpenCLLibrary.cl_program;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

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
 * loaded back in subsequent executions to avoid recompilation.
 * @see CLContext#createProgram(java.lang.String[]) 
 * @author Olivier Chafik
 */
public class CLProgram extends CLAbstractEntity<cl_program> {

    protected final CLContext context;

	private static CLInfoGetter<cl_program> infos = new CLInfoGetter<cl_program>() {
		@Override
		protected int getInfo(cl_program entity, int infoTypeEnum, long size, Pointer out, Pointer<SizeT> sizeOut) {
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

        int nDevices = binaries.size();
        devices = new CLDevice[nDevices];
        Pointer<SizeT> lengths = allocateSizeTs(nDevices);
		Pointer<cl_device_id> deviceIds = allocateTypedPointers(cl_device_id.class, nDevices);
		Pointer<Pointer<Byte>> binariesArray = allocatePointers(paramType(Pointer.class, Byte.class), nDevices);
		Pointer<Byte>[] binariesMems = new Pointer[nDevices];

        int iDevice = 0;
        for (Map.Entry<CLDevice, byte[]> e : binaries.entrySet())
        {
            CLDevice device = e.getKey();
            byte[] binary = e.getValue();

            binariesArray.set(iDevice, binariesMems[iDevice] = pointerToBytes(binary));

            lengths.set(iDevice, new SizeT(binary.length));
            deviceIds.set(iDevice, (devices[iDevice] = device).getEntity());

            iDevice++;
        }
		Pointer<Integer> errBuff = allocateInt();
        int previousAttempts = 0;
        Pointer<Integer> statuses = allocateInts(nDevices);
		do {
			entity = CL.clCreateProgramWithBinary(context.getEntity(), nDevices, deviceIds, lengths, binariesArray, statuses, errBuff);
		} while (failedForLackOfMemory(errBuff.get(), previousAttempts++));
        
	}

    /**
     * Write the compiled binaries of this program (for all devices it was compiled for), so that it can be restored later using {@link CLContext#loadProgram(java.io.InputStream) }
     * @param out will be closed
     * @throws CLBuildException
     * @throws IOException
     */
    public void store(OutputStream out) throws CLBuildException, IOException {
        writeBinaries(getBinaries(), out);
    }
    public static void writeBinaries(Map<CLDevice, byte[]> binaries, OutputStream out) throws IOException {
        Map<String, byte[]> binaryBySignature = new HashMap<String, byte[]>();
        for (Map.Entry<CLDevice, byte[]> e : binaries.entrySet())
            binaryBySignature.put(e.getKey().createSignature(), e.getValue()); // Maybe multiple devices will have the same signature : too bad, we don't care and just write one binary per signature.

        ZipOutputStream zout = new ZipOutputStream(out);
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
    public static Map<CLDevice, byte[]> readBinaries(List<CLDevice> allowedDevices, InputStream in) throws IOException {
        Map<CLDevice, byte[]> ret = new HashMap<CLDevice, byte[]>();
        Map<String, List<CLDevice>> devicesBySignature = CLDevice.getDevicesBySignature(allowedDevices);

        ZipInputStream zin = new ZipInputStream(in);
        ZipEntry ze;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();

        byte[] b = new byte[1024];
        while ((ze = zin.getNextEntry()) != null) {
            String signature = ze.getName();
            bout.reset();
            int len;
            while ((len = zin.read(b)) > 0)
                bout.write(b, 0, len);

            byte[] data = bout.toByteArray();
            List<CLDevice> devices = devicesBySignature.get(signature);
            for (CLDevice device : devices)
                ret.put(device, data);
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
                StringBuilder b = new StringBuilder();//"-DJAVACL=1 ");
                for (Map.Entry<String, Object> m : macros.entrySet())
                    b.append("#define " + m.getKey() + " " + m.getValue() + "\n");
                this.sources.add(0, b.toString());
            }
        }

        String[] sources = this.sources.toArray(new String[this.sources.size()]);
        long[] lengths = new long[sources.length];
        for (int i = 0; i < sources.length; i++) {
            lengths[i] = sources[i].length();
        }
        Pointer<Integer> errBuff = allocateInt();
        cl_program program;
		int previousAttempts = 0;
		do {
			program = CL.clCreateProgramWithSource(context.getEntity(), sources.length, pointerToCStrings(sources), pointerToSizeTs(lengths), errBuff);
		} while (failedForLackOfMemory(errBuff.get(0), previousAttempts++));
        entity = program;
    }
    
    @Override
    protected synchronized cl_program getEntity() {
        if (entity == null)
            allocate();

        return entity;
    }
	
	public synchronized void addSource(String src) {
        if (entity != null)
            throw new IllegalThreadStateException("Program was already allocated : cannot add sources anymore.");
        sources.add(src);
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
        
		Pointer<?> s = infos.getMemory(getEntity(), CL_PROGRAM_BINARY_SIZES);
		int n = (int)s.getValidBytes() / JNI.SIZE_T_SIZE;
		long[] sizes = s.getSizeTs(0, n);
		//int[] sizes = new int[n];
		//for (int i = 0; i < n; i++) {
		//	sizes[i] = s.getNativeLong(i * Native.LONG_SIZE).intValue();
		//}

		Pointer<?>[] binMems = (Pointer<?>[])new Pointer[n];
		Pointer<Pointer<?>> ptrs = allocatePointers(n);
		for (int i = 0; i < n; i++) {
			ptrs.set(i, binMems[i] = allocateBytes(sizes[i]));
		}
		error(infos.getInfo(getEntity(), CL_PROGRAM_BINARIES, ptrs.getValidBytes(), ptrs, null));

		Map<CLDevice, byte[]> ret = new HashMap<CLDevice, byte[]>(devices.length);
        for (int i = 0; i < n; i++) {
            CLDevice device = devices[i];
			Pointer<?> bytes = binMems[i];
            ret.put(device, bytes.getBytes(0, (int)sizes[i]));
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
    
    protected String getOptionsString() {
        if ((macros == null || macros.isEmpty()) && (args == null || args.isEmpty()))
            return null;

        StringBuilder b = new StringBuilder();//"-DJAVACL=1 ");
        if (macros != null && !passMacrosAsSources)
            for (Map.Entry<String, Object> m : macros.entrySet())
                b.append("-D" + m.getKey() + "=" + m.getValue() + " ");
        if (args != null)
            for (String arg : args)
                b.append(arg).append(" ");
        
        String s = b.toString().trim();
        return s.length() == 0 ? null : s;
    }

    boolean built;
	/**
	 * Returns the context of this program
	 */
    public synchronized CLProgram build() throws CLBuildException {
        if (built)
            throw new IllegalThreadStateException("Program was already built !");
        if (entity == null)
            allocate();

        int nDevices = devices.length;
        Pointer<cl_device_id> deviceIds = null;
        if (nDevices != 0) {
            deviceIds = allocateTypedPointers(cl_device_id.class, nDevices);
            for (int i = 0; i < nDevices; i++)
                deviceIds.set(i, devices[i].getEntity());
        }
        int err = CL.clBuildProgram(getEntity(), nDevices, deviceIds, pointerToCString(getOptionsString()), null, null);
        //int err = CL.clBuildProgram(getEntity(), 0, null, getOptionsString(), null, null);
        if (err != CL_SUCCESS) {//BUILD_PROGRAM_FAILURE) {
            Pointer<SizeT> len = allocateSizeT();
            int bufLen = 2048 * 32; //TODO find proper size
            Pointer<?> buffer = allocateBytes(bufLen);

            HashSet<String> errs = new HashSet<String>();
            if (deviceIds == null) {
                error(CL.clGetProgramBuildInfo(getEntity(), null, CL_PROGRAM_BUILD_LOG, bufLen, buffer, len));
                String s = buffer.getCString(0);
                errs.add(s);
            } else
                for (cl_device_id device : deviceIds) {
                    error(CL.clGetProgramBuildInfo(getEntity(), device, CL_PROGRAM_BUILD_LOG, bufLen, buffer, len));
                    String s = buffer.getCString(0);
                    errs.add(s);
                }
                
            throw new CLBuildException(this, "Compilation failure : " + errorString(err), errs);
        }
        built = true;
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
		Pointer<Integer> pCount = allocateInt();
		int previousAttempts = 0;
		while (failedForLackOfMemory(CL.clCreateKernelsInProgram(getEntity(), 0, null, pCount), previousAttempts++)) {}

		int count = pCount.get();
		Pointer<cl_kernel> kerns = allocateTypedPointers(cl_kernel.class, count);
		previousAttempts = 0;
		while (failedForLackOfMemory(CL.clCreateKernelsInProgram(getEntity(), count, kerns, pCount), previousAttempts++)) {}

		CLKernel[] kernels = new CLKernel[count];
		for (int i = 0; i < count; i++)
			kernels[i] = new CLKernel(this, null, kerns.get(i));

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
        Pointer<Integer> errBuff = allocateInt();
        cl_kernel kernel;
		int previousAttempts = 0;
		do {
			kernel = CL.clCreateKernel(getEntity(), pointerToCString(name), errBuff);
		} while (failedForLackOfMemory(errBuff.get(0), previousAttempts++));

        CLKernel kn = new CLKernel(this, name, kernel);
        kn.setArgs(args);
        return kn;
    }

    List<String> args;
    public void addArgs(String... as) {
        if (args == null)
            args = new ArrayList<String>();
        args.addAll(Arrays.asList(as));
    }
}
