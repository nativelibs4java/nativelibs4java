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
import com.nativelibs4java.opencl.library.OpenCLLibrary;
import com.ochafik.lang.jnaerator.runtime.NativeSize;
import com.ochafik.lang.jnaerator.runtime.NativeSizeByReference;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;
import com.ochafik.util.listenable.Pair;
import com.sun.jna.*;
import com.sun.jna.ptr.*;
import java.util.*;
import java.nio.*;
import static com.nativelibs4java.opencl.JavaCL.*;
import static com.nativelibs4java.opencl.CLException.*;
import static com.nativelibs4java.util.JNAUtils.*;
import static com.nativelibs4java.util.NIOUtils.*;

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

    CLProgram(CLContext context, cl_program entity) {
        super(entity);
        this.context = context;
    }

	/**
	 * Get the source code of this program
	 */
	public String getSource() {
		return infos.getString(get(), CL_PROGRAM_SOURCE);
	}

	/**
	 * Get the binaries of the program (one for each device, in order)
	 * @return
	 */
    public byte[][] getBinaries() {
		Memory s = infos.getMemory(get(), CL_PROGRAM_BINARY_SIZES);
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
		error(infos.getInfo(get(), CL_PROGRAM_BINARIES, toNS(ptrs.getSize()), ptrs, null));

		byte[][] ret = new byte[n][];
		for (int i = 0; i < n; i++) {
			Memory bytes = binMems[i];
			ret[i] = bytes.getByteArray(0, sizes[i].intValue());
		}
		return ret;
	}

	/**
	 * Returns the context of this program
	 */
    public CLContext getContext() {
        return context;
    }
    Map<String, String> macros;
    public void defineMacro(String name, String value) {
        createMacros();
        macros.put(name, value);
    }

    private void createMacros() {
        if (macros == null)
            macros = new LinkedHashMap<String, String>();
    }
    public void defineMacros(Map<String, String> macros) {
        createMacros();
        this.macros.putAll(macros);
    }
    
    protected String getOptionsString() {
        if (macros == null)
            return null;

        StringBuilder b = new StringBuilder();
        if (macros != null)
            for (Map.Entry<String, String> m : macros.entrySet())
                b.append("-D" + m.getKey() + "=" + m.getValue() + " ");

        return b.toString();
    }
	/**
	 * Returns the context of this program
	 */
    public CLProgram build() throws CLBuildException {

        int err = CL.clBuildProgram(get(), 0, null/*context.getDeviceIds()*/, getOptionsString(), null, null);
        if (err != CL_SUCCESS) {//BUILD_PROGRAM_FAILURE) {
            NativeSizeByReference len = new NativeSizeByReference();
            int bufLen = 2048;
            Memory buffer = new Memory(bufLen);

            HashSet<String> errs = new HashSet<String>();
            for (cl_device_id device_id : context.deviceIds) {
                error(CL.clGetProgramBuildInfo(get(), device_id, CL_PROGRAM_BUILD_LOG, toNS(bufLen), buffer, len));
                String s = buffer.getString(0);
                errs.add(s);
            }
            throw new CLBuildException(this, errorString(err), errs);
        } //else
	//		error(err);
		
        return this;
    }

    @Override
    protected void clear() {
        error(CL.clReleaseProgram(get()));
    }

	/**
	 * Return all the kernels found in the program.
	 */
	public CLKernel[] createKernels() {
		IntByReference pCount = new IntByReference();
        error(CL.clCreateKernelsInProgram(get(), 0, (cl_kernel[])null, pCount));

		int count = pCount.getValue();
		cl_kernel[] kerns = new cl_kernel[count];
		error(CL.clCreateKernelsInProgram(get(), count, kerns, pCount));

		CLKernel[] kernels = new CLKernel[count];
		for (int i = 0; i < count; i++)
			kernels[i] = new CLKernel(this, null, kerns[i]);

		return kernels;
	}

    /**
     * Find a kernel by its functionName, and optionally bind some arguments to it.
     */
    public CLKernel createKernel(String name, Object... args) {
        IntBuffer errBuff = IntBuffer.wrap(new int[1]);
        cl_kernel kernel = CL.clCreateKernel(get(), name, errBuff);
        error(errBuff.get(0));

        CLKernel kn = new CLKernel(this, name, kernel);
        kn.setArgs(args);
        return kn;
    }


}