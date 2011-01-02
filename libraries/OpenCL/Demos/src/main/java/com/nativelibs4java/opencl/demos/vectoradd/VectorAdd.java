package com.nativelibs4java.opencl.demos.vectoradd;
import com.nativelibs4java.opencl.*;
import java.nio.*;

/**
 * This is about the simplest possible JavaCL program.<br>
 * It adds two vectors of floats in parallel.<br>
 * This program can be written more easily with the JavaCL BLAS Library :
 * <code>
 * LinearAlgebraUtils la = new LinearAlgebraUtils();
 * CLKernel kernel = new LinearAlgebraUtils().getKernel(LinearAlgebraUtils.Fun2.add, LinearAlgebraUtils.Primitive.Float);
 * </code>
 * @author ochafik
 */
public class VectorAdd {

	public static void main(String[] args) {
        try {
            FloatBuffer a = FloatBuffer.wrap(new float[] {  1,  2,  3,  4 });
            FloatBuffer b = FloatBuffer.wrap(new float[] { 10, 20, 30, 40 });

            FloatBuffer sum = add(a, b);
            for (int i = 0, n = sum.capacity(); i < n; i++)
                System.out.println(sum.get(i));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
	}

	public static FloatBuffer add(FloatBuffer a, FloatBuffer b) throws CLBuildException {
		int n = a.capacity();
		
		CLContext context = JavaCL.createBestContext();
		CLQueue queue = context.createDefaultQueue();
		
		String source = 
			"__kernel void addFloats(__global const float* a, __global const float* b, __global float* output)     " +
			"{                                                                                                     " +
			"   int i = get_global_id(0);                                                                          " +
			"   output[i] = a[i] + b[i];                                                                           " +
			"}                                                                                                     ";
		
		CLKernel kernel = context.createProgram(source).createKernel("addFloats");
		CLFloatBuffer aBuf = context.createFloatBuffer(CLMem.Usage.Input, a, true);
		CLFloatBuffer bBuf = context.createFloatBuffer(CLMem.Usage.Input, b, true);
		CLFloatBuffer outBuf = context.createFloatBuffer(CLMem.Usage.Output, n);
		kernel.setArgs(aBuf, bBuf, outBuf);
		
		kernel.enqueueNDRange(queue, new int[]{n});
		queue.finish();
	
		return outBuf.read(queue);
	}
}

