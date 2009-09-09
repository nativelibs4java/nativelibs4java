package com.nativelibs4java.opencl;

import java.nio.*;
import static com.nativelibs4java.opencl.OpenCL4Java.*;

/// @see http://ati.amd.com/technology/streamcomputing/intro_opencl.html#simple
public class OpenCL4JavaExample {
	public static void main(String[] args) {
		try {
			int dataSize = 128;
		    
			FloatBuffer data = floatBuffer(dataSize);
			FloatBuffer resultsf = floatBuffer(dataSize);
			IntBuffer resultsi = intBuffer(dataSize);
		 
		    for(int i = 0; i < dataSize; i++)
		        data.put(i, i);
		 
		    CLDevice[] devices = CLDevice.listAllDevices();
		    for (CLDevice device : devices) {
		    	System.out.println("[OpenCL] Found device \"" + device.getName() + "\"");
		    	System.out.println(device.getExecutionCapabilities());
		    }
		    CLContext context = CLContext.createContext(devices);
		    
		    String src = "\n"  +
		    "__kernel square(                                                       \n" +
		    "   __global const float* input,                                              \n"  +
		    "   __global float* outputf,                                             \n"  +
		    "   __global int* outputi,                                             \n"  +
		    "   const unsigned int count)                                           \n"  +
		    "{                                                                      \n"  +
		    "   int i = get_global_id(0);                                           \n"  +
		    "   if(i < count)                                                       \n"  +
		    "       outputf[i] = input[i] * input[i];                                \n"  +
		    "       outputi[i] = i;// + input[i] * input[i];                                \n"  +
		    "}                                                                      \n"  +
		    "\n"
		    ;

		    CLProgram program = context.createProgram(src).build();
		    CLKernel kernel = program.createKernel(
		    	"square", 
		    	context.createInput(data, false), 
		    	context.createOutput(resultsf), 
		    	context.createOutput(resultsi), 
		    	dataSize
		    );

		    CLQueue queue = context.createDefaultQueue();
		    kernel.enqueueNDRange(queue, new int[]{dataSize}, new int[]{1});
		    queue.finish();
		    
		    for (int iRes = 0; iRes < dataSize; iRes++) {
		    	float d = data.get(iRes), rf = resultsf.get(iRes);
		    	int ri = resultsi.get(iRes);
		    	System.out.println(d + "\t->\tfloat: " + rf + ", int: " + ri);
		    }
		} catch (CLBuildException e) {
			e.printStackTrace();
		}
	}
}
