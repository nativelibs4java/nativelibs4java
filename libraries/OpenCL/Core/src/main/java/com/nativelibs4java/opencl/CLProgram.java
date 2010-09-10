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
import static com.nativelibs4java.opencl.CLException.error;
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
import com.nativelibs4java.util.NIOUtils;
import com.ochafik.lang.jnaerator.runtime.NativeSize;
import com.ochafik.lang.jnaerator.runtime.NativeSizeByReference;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
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
		this.devices = binaries.keySet().toArray(new CLDevice[binaries.size()]);

		int nDevices = devices.length;
        NativeSize[] lengths = new NativeSize[nDevices];
		cl_device_id[] deviceIds = new cl_device_id[nDevices];
		Memory binariesArray = new Memory(Pointer.SIZE * nDevices);
		Memory[] binariesMems = new Memory[nDevices]; 
        for (int i = 0; i < nDevices; i++) {
			final byte[] binary = binaries.get(devices[i]);
			Memory binaryMem = new Memory(binary.length);
			binaryMem.write(0, binary, 0, binary.length);
			binariesArray.setPointer(i * Pointer.SIZE, binaryMem);
			binariesMems[i] = binaryMem;
            lengths[i] = toNS(binary.length);
			deviceIds[i] = devices[i].getEntity();
        }
		PointerByReference binariesPtr = new PointerByReference();
        binariesPtr.setPointer(binariesArray);
        
        IntBuffer errBuff = NIOUtils.directInts(1, ByteOrder.nativeOrder());
        cl_program program;
		int previousAttempts = 0;
		do {
			program = CL.clCreateProgramWithBinary(context.getEntity(), nDevices, deviceIds, lengths, binariesPtr, null, errBuff);
		} while (failedForLackOfMemory(errBuff.get(0), previousAttempts++));
        entity = program;
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
    
    protected String getOptionsString() {
        if (passMacrosAsSources)
            return null;

        if (macros == null || macros.isEmpty())
            return null;

        StringBuilder b = new StringBuilder();//"-DJAVACL=1 ");
        for (Map.Entry<String, Object> m : macros.entrySet())
            b.append("-D" + m.getKey() + "=" + m.getValue() + " ");

        return b.toString();
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
                
            throw new CLBuildException(this, "Compilation failure ! (code = " + err + ")"/*errorString(err)*/, errs);
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