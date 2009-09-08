
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import com.ochafik.util.string.StringUtils;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.NativeLongByReference;
import com.sun.jna.ptr.PointerByReference;

import opencl.OpenCLLibrary;
import opencl.OpenCLLibrary.cl_command_queue;
import opencl.OpenCLLibrary.cl_kernel;
import opencl.OpenCLLibrary.cl_mem;
import opencl.OpenCLLibrary.cl_program;
import static opencl.OpenCLLibrary.*;

/// @see http://ati.amd.com/technology/streamcomputing/intro_opencl.html#simple
public class KernelHelper {
	public static void main(String[] args) {
		test2();
	}
	static void test2() {
		int DATA_SIZE = 128;
		
		FloatBuffer data = ByteBuffer.allocateDirect(DATA_SIZE * 4).asFloatBuffer();
		FloatBuffer results = ByteBuffer.allocateDirect(DATA_SIZE * 4).asFloatBuffer();
		int correct;             // number of correct results returned
	 
	    //int global;                    // global domain size for our calculation
	    //int local;                     // local domain size for our calculation
	 
	    // Get data on which to operate
	    //
	    
	   int i = 0;
	    int count = DATA_SIZE;
	    for(i = 0; i < count; i++)
	        data.put(i, i);
	 
	    // Get an ID for the device                                    [2]
	    boolean gpu = false;
	    PointerByReference pdevice_id = new PointerByReference();
	    OpenCLLibrary CL = INSTANCE;
	    int err = CL.clGetDeviceIDs(null, gpu ? CL_DEVICE_TYPE_GPU : CL_DEVICE_TYPE_CPU, 1,
	                                                         pdevice_id, (IntBuffer)null);
	    if (err != CL_SUCCESS)
	    { error(err); }                                                        //      [3]
	 
	    // Create a context                                            [4]
	    //
	    IntByReference errRef = new IntByReference();
	    IntBuffer errBuff = IntBuffer.wrap(new int[1]);
		cl_context context = CL.clCreateContext(null, 1, pdevice_id, null, null, errRef);
	    err = errRef.getValue();
	    if (context == null)
	    	error(err);
	 
	    cl_device_id device_id = new cl_device_id(pdevice_id.getValue());           // device ID
	    
	    // Create a command queue                                              [5]
	    //
	    cl_command_queue queue = CL.clCreateCommandQueue(context, device_id, 0, errRef);
	    err = errRef.getValue();
	    if (queue == null)
	    { error(err); }
	 
	    // Create the compute program from the source buffer                   [6]
	    //
	    
	    String src = "\n"  +
	    "__kernel square(                                                       \n" +
	    "   __global float* input,                                              \n"  +
	    "   __global float* output,                                             \n"  +
	    "   const unsigned int count)                                           \n"  +
	    "{                                                                      \n"  +
	    "   int i = get_global_id(0);                                           \n"  +
	    "   if(i < count)                                                       \n"  +
	    "       output[i] = input[i] * input[i];                                \n"  +
	    "}                                                                      \n"  +
	    "\n"
	    ;
	    String[] source = new String[] { src };
	    NativeLong[] lengths = new NativeLong[] { toNL(src.length()) };
	    cl_program program = CL.clCreateProgramWithSource(context, 1, source, lengths, errBuff);
	    err = errBuff.get(0);
	    if ( program == null)
	    { error(err); }
	 
	    // Build the program executable                                        [7]
	    //
	    err = CL.clBuildProgram(program, 0, null, (String)null, null, null);
	    if (err != CL_SUCCESS)
	    {
	    	NativeLongByReference len = new NativeLongByReference();
	    	int bufLen = 2048;
	    	Memory buffer = new Memory(bufLen);
	 
	        //printf("Error: Failed to build program executable\n");       //      [8]
            CL.clGetProgramBuildInfo(program, device_id, CL_PROGRAM_BUILD_LOG,
	                                          toNL(bufLen), buffer, len);
	        //printf("%s\n", buffer);
            System.out.println(buffer.getString(0));
            error(err);
//            System.exit(1);
	    }
	 
	    // Create the compute kernel in the program we wish to run            [9]
	    //
	    cl_kernel kernel = CL.clCreateKernel(program, "square", errBuff);
	    err = errBuff.get(0);
	    if (kernel == null || err != CL_SUCCESS)
	    { error(err); }
	 
	    // Create the input and output arrays in device memory for our calculation
	    //                                                                   [10]
	    cl_mem input = CL.clCreateBuffer(context,  CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,  toNL(4 * count),
	    		Native.getDirectBufferPointer(data),
	                                                                  //null, 
	                                                                  errBuff);
	    error(err = errBuff.get(0));
	 
	    cl_mem output = CL.clCreateBuffer(context, CL_MEM_WRITE_ONLY, toNL(4 *count),
	                                                                  null, errBuff);
	    error(err = errBuff.get(0));
	    if (input == null || output == null)
	    	error(err);
	 
	    // Write our data set into the input array in device memory          [11]
	    //
//	    error(CL.clEnqueueWriteBuffer(queue, input, CL_TRUE, toNL(0),
//	                                   toNL(4 *count), Native.getDirectBufferPointer(data), 0, null, (PointerByReference)null));
	    
	    // Set the arguments to our compute kernel                           [12]
	    //
	    err = 0;
	    NativeLong sizeof_cl_mem = toNL(Pointer.SIZE);
	    error(CL.clSetKernelArg(kernel, 0, sizeof_cl_mem, new PointerByReference(input.getPointer()).getPointer()));
	    error(CL.clSetKernelArg(kernel, 1, sizeof_cl_mem, new PointerByReference(output.getPointer()).getPointer()));
	    
	    IntByReference countByRef = new IntByReference(count);
	    error(CL.clSetKernelArg(kernel, 2, toNL(4), countByRef.getPointer()));
	 
	    count = countByRef.getValue();
	    
	    // Get the maximum work-group size for executing the kernel on the device
	    //                                                                   [13]
	    IntByReference local = new IntByReference(1);
	    IntByReference global = new IntByReference(0);
//	 TODO   error(CL.clGetKernelWorkGroupInfo(kernel, device_id, CL_KERNEL_WORK_GROUP_SIZE,
//	                                                      toNL(4), local.getPointer(), null));
	 
	    // Execute the kernel over the entire range of the data set          [14]
	    //
	    global.setValue(count);
	    error(CL.clEnqueueNDRangeKernel(queue, kernel, 1, null, toNL(global), toNL(local),
	                                                              0, null, (PointerByReference)null));
	    
	    // Wait for the command queue to get serviced before reading back results
	    //                                                                   [15]
	    error(CL.clFinish(queue));
		 
	    
	    // Read the results from the device                                  [16]
	    //
//	    cl_event evt = new cl_event();
//	    PointerByReference pevt = new PointerByReference(evt.getPointer());
	    Pointer pres = Native.getDirectBufferPointer(results);
	    error(CL.clEnqueueReadBuffer(queue, output, CL_TRUE, toNL(0),
	                                toNL(4 *count), pres, 0, null, 
	                                (PointerByReference)null//pevt
	                                ));
	    
	    error(CL.clFinish(queue));
	    
//	    float f = pres.getFloat(4 * 10);
//	    error(CL.clWaitForEvents(1, pevt));
	    
	    
	    for (int iRes = 0; iRes < DATA_SIZE; iRes++) {
	    	float d = data.get(iRes), r = results.get(iRes);
	    	System.out.println(d + "\t->\t" + r);
	    }
	    // Shut down and clean up
	    //
	    CL.clReleaseMemObject(input);
	    CL.clReleaseMemObject(output);
	    CL.clReleaseProgram(program);
	    CL.clReleaseKernel(kernel);
	    CL.clReleaseCommandQueue(queue);
	    CL.clReleaseContext(context);
	 
	    
	}

	public static NativeLongByReference toNL(IntByReference local) {
		NativeLongByReference nl = new NativeLongByReference();
		nl.setPointer(local.getPointer());
		return nl;
	}
	public static NativeLong toNL(int i) {
		return new NativeLong(i);
	}
	private static void error(int err) {
		if (err == CL_SUCCESS)
			return;
		List<String> candidates = new ArrayList<String>();
		for (Field f : OpenCLLibrary.class.getDeclaredFields()) {
			if (!Modifier.isStatic(f.getModifiers()))
				continue;
			if (f.getType().equals(Integer.TYPE)) {
				try {
					int i = (Integer)f.get(null);
					if (i == err)
						candidates.add(f.getName());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		throw new RuntimeException("OpenCL Error : " + err + " (" + StringUtils.implode(candidates, " or ") + ")");
	}
	static void test() {
		OpenCLLibrary cl = INSTANCE;
		cl_context ctx = cl.clCreateContext(null, CL_DEVICE_TYPE_ALL, (cl_device_id[])null, null, null, (IntBuffer)null);
		
		NativeLongByReference deviceCount = new NativeLongByReference();
		cl.clGetContextInfo(ctx, CL_CONTEXT_DEVICES, new NativeLong(0), null, deviceCount);
//		NativeLong[] devices = new NativeLong[deviceCount.getValue().intValue()];
		int nDevices = deviceCount.getValue().intValue();
		Memory devicesMem = new Memory(nDevices * Native.POINTER_SIZE);
		cl.clGetContextInfo(ctx, CL_CONTEXT_DEVICES, deviceCount.getValue(), devicesMem, null);
		cl_device_id deviceId = new cl_device_id(devicesMem.getPointer(0)); // or is it the pointer to that value ?
		
		cl_command_queue cmdQueue = cl.clCreateCommandQueue(ctx, deviceId, 0, (IntBuffer)null);

		int n = 100;
		NativeLong nl = new NativeLong(n * 4);
		Memory ma = new Memory(100 * 4);
		Memory mb = new Memory(100 * 4);
//		Memory mc = new Memory(100 * 4);
		cl_mem bufA = cl.clCreateBuffer(ctx, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, nl, ma, (IntBuffer)null);
		cl_mem bufB = cl.clCreateBuffer(ctx, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, nl, mb, (IntBuffer)null);
		cl_mem bufC = cl.clCreateBuffer(ctx, CL_MEM_WRITE_ONLY, nl, null, (IntBuffer)null);
		
		String src = null;
		cl_program program = cl.clCreateProgramWithSource(ctx, 1, new String[] {src}, null, null);
		int err = cl.clBuildProgram(program, 0, null, (String)null, null, null);
		cl_kernel kernel = cl.clCreateKernel(program, "vec_add", null);
		cl.clSetKernelArg(kernel, 0, null, bufA.getPointer());
		cl.clSetKernelArg(kernel, 1, null, bufB.getPointer());
		cl.clSetKernelArg(kernel, 2, null, bufC.getPointer());
		
	}
}
