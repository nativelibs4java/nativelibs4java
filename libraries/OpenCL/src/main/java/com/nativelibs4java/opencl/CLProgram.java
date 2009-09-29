/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
*/

package com.nativelibs4java.opencl;
import com.nativelibs4java.opencl.library.OpenCLLibrary;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;
import com.sun.jna.*;
import com.sun.jna.ptr.*;
import java.util.*;
import java.nio.*;
import static com.nativelibs4java.opencl.OpenCL4Java.*;

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
 * @author Olivier Chafik
 */
public class CLProgram extends CLEntity<cl_program> {

    protected final CLContext context;

	static CLInfoGetter<cl_program> progInfos = new CLInfoGetter<cl_program>() {
		@Override
		protected int getInfo(cl_program entity, int infoTypeEnum, NativeLong size, Pointer out, NativeLongByReference sizeOut) {
			return CL.clGetProgramInfo(entity, infoTypeEnum, size, out, sizeOut);
		}
	};

    CLProgram(CLContext context, cl_program entity) {
        super(entity);
        this.context = context;
    }

	public String getSource() {
		return progInfos.getString(get(), CL_PROGRAM_SOURCE);
	}
    public byte[][] getBinaries() {
		Memory s = progInfos.getBytes(get(), CL_PROGRAM_BINARY_SIZES);
		int n = (int)s.getSize() / Native.LONG_SIZE;
		int[] sizes = new int[n];
		for (int i = 0; i < n; i++) {
			sizes[i] = s.getNativeLong(i * Native.LONG_SIZE).intValue();
		}

		Memory[] binMems = new Memory[n];
		Memory ptrs = new Memory(n * Native.POINTER_SIZE);
		for (int i = 0; i < n; i++) {
			ptrs.setPointer(i * Native.POINTER_SIZE, binMems[i] = new Memory(sizes[i]));
		}
		error(progInfos.getInfo(get(), CL_PROGRAM_BINARIES, toNL(ptrs.getSize()), ptrs, null));

		byte[][] ret = new byte[n][];
		for (int i = 0; i < n; i++) {
			Memory bytes = binMems[i];
			ret[i] = bytes.getByteArray(0, sizes[i]);
		}
		return ret;
	}
    public CLContext getContext() {
        return context;
    }
    public CLProgram build() throws CLBuildException {

        int err = CL.clBuildProgram(get(), 0, null/*context.getDeviceIds()*/, (String) null, null, null);
        if (err != CL_SUCCESS) {
            NativeLongByReference len = new NativeLongByReference();
            int bufLen = 2048;
            Memory buffer = new Memory(bufLen);

            HashSet<String> errs = new HashSet<String>();
            for (cl_device_id device_id : context.deviceIds) {
                error(CL.clGetProgramBuildInfo(get(), device_id, CL_PROGRAM_BUILD_LOG, toNL(bufLen), buffer, len));
                String s = buffer.getString(0);
                errs.add(s);
            }
            throw new CLBuildException(errorString(err), errs);
        }
        return this;
    }

    @Override
    protected void clear() {
        CL.clReleaseProgram(get());
    }

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
    public CLKernel createKernel(String name, Object... args) {
        IntBuffer errBuff = IntBuffer.wrap(new int[1]);
        cl_kernel kernel = CL.clCreateKernel(get(), name, errBuff);
        error(errBuff.get(0));

        CLKernel kn = new CLKernel(this, name, kernel);
        kn.setArgs(args);
        return kn;
    }

}