/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.blas;

import com.nativelibs4java.opencl.CLBuildException;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLDoubleBuffer;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLProgram;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.JavaCL;
import com.nativelibs4java.opencl.ReductionUtils;
import com.nativelibs4java.opencl.ReductionUtils.Reductor;
import com.nativelibs4java.util.IOUtils;
import static com.nativelibs4java.util.NIOUtils.*;
import static com.nativelibs4java.util.JNAUtils.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.DoubleBuffer;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ochafik
 */
public class LinearAlgebraUtils {

    protected CLContext context;
    protected CLQueue queue;

    public LinearAlgebraUtils() throws IOException, CLBuildException {
        this(JavaCL.createBestContext());
    }

    public LinearAlgebraUtils(CLContext context) throws IOException, CLBuildException {
        this(context, context.createDefaultQueue());
    }

    LinearAlgebraKernels kernels;
    public LinearAlgebraUtils(CLContext context, CLQueue queue) throws IOException, CLBuildException {
        this.context = context;
        this.queue = queue;
        kernels = new LinearAlgebraKernels(context);
    }

    public CLContext getContext() {
        return context;
    }

    public CLQueue getQueue() {
        return queue;
    }

    public enum Fun1 {
        log,
        exp,
        sqrt,
        sin,
        cos,
        tan,
        atan,
        asin,
        acos,
        sinh,
        cosh,
        tanh,
        asinh,
        acosh,
        atanh
    }
    public enum Fun2 {
        atan2,
        dist
    }
    public enum Primitive {
        Float,
        Double,
        Long,
        Int,
        Short,
        Byte,

        Float2,
        Double2,
        Long2,
        Int2,
        Short2,
        Byte2,

        Float3,
        Double3,
        Long3,
        Int3,
        Short3,
        Byte3,

        Float4,
        Double4,
        Long4,
        Int4,
        Short4,
        Byte4,

        Float8,
        Double8,
        Long8,
        Int8,
        Short8,
        Byte8,

        Float16,
        Double16,
        Long16,
        Int16,
        Short16,
        Byte16,
    }

    protected String createVectFun1Source(String functionName, String type, PrintWriter out, boolean inPlace) {
        String kernelName = "vect_" + functionName + "_" + type + (inPlace ? "_inplace" : "");
        out.println("__kernel void " + kernelName + "(\n");
        if (!inPlace)
            out.println("\t__global const " + type + "* in,");
        out.println("\t__global " + type + "* out");
        out.println(") {");
        out.println("\tint i = get_global_id(0);");
        out.println("\tout[i] = " + functionName + "(" + (inPlace ? "out" : "in") + "[i]);");
        out.println("}");
        return kernelName;
    }
    
    
    protected String createVectFun2Source(String functionName, String type, PrintWriter out) {
        String kernelName = "vect_" + functionName + "_" + type;
        out.println("__kernel void " + kernelName + "(\n");
        out.println("\t__global const " + type + "* in1,");
        out.println("\t__global const " + type + "* in2,");
        out.println("\t__global " + type + "* out");
        out.println(") {");
        out.println("\tint i = get_global_id(0);");
        out.println("\tout[i] = " + functionName + "(in1[i], in2[i]);");
        out.println("}");
        return kernelName;
    }


    private static class Fun1Kernels {
        CLKernel inPlace, notInPlace;
    }
    private EnumMap<Fun1, EnumMap<Primitive, Fun1Kernels>> fun1Kernels = new EnumMap<Fun1, EnumMap<Primitive, Fun1Kernels>>(Fun1.class);
    public synchronized CLKernel getFun1Kernel(Fun1 op, Primitive prim, boolean inPlace) throws CLBuildException {
        EnumMap<Primitive, Fun1Kernels> m = fun1Kernels.get(op);
        if (m == null)
            fun1Kernels.put(op, m = new EnumMap<Primitive, Fun1Kernels>(Primitive.class));

        Fun1Kernels kers = m.get(prim);
        if (kers == null) {
            StringWriter s = new StringWriter(300);
            PrintWriter out = new PrintWriter(s);
            String inPlaceName = createVectFun1Source(op.toString().toLowerCase(), prim.toString().toLowerCase(), out, true);
            String notInPlaceName = createVectFun1Source(op.toString().toLowerCase(), prim.toString().toLowerCase(), out, false);
            CLProgram prog = getContext().createProgram(s.toString()).build();
            kers = new Fun1Kernels();
            kers.inPlace = prog.createKernel(inPlaceName);
            kers.notInPlace = prog.createKernel(notInPlaceName);
            m.put(prim, kers);
        }
        return inPlace ? kers.inPlace : kers.notInPlace;
    }

    private EnumMap<Fun2, EnumMap<Primitive, CLKernel>> fun2Kernels = new EnumMap<Fun2, EnumMap<Primitive, CLKernel>>(Fun2.class);
    public synchronized CLKernel getFun2Kernel(Fun2 op, Primitive prim) throws CLBuildException {
        EnumMap<Primitive, CLKernel> m = fun2Kernels.get(op);
        if (m == null)
            fun2Kernels.put(op, m = new EnumMap<Primitive, CLKernel>(Primitive.class));

        CLKernel ker = m.get(prim);
        if (ker == null) {
            StringWriter s = new StringWriter(300);
            PrintWriter out = new PrintWriter(s);
            String name = createVectFun2Source(op.toString().toLowerCase(), prim.toString().toLowerCase(), out);
            CLProgram prog = getContext().createProgram(s.toString()).build();
            ker = prog.createKernel(name);
            m.put(prim, ker);
        }
        return ker;
    }
    

	private static final int[] unitIntArr = new int[] { 1 };
    private static final int[] unitInt2Arr = new int[] { 1, 1 };

	public synchronized CLEvent multiply(
            CLDoubleBuffer a, long aRows, long aColumns, 
            CLDoubleBuffer b, long bRows, long bColumns, 
            CLDoubleBuffer out, //long outRows, long outColumns,
            CLEvent... eventsToWaitFor) throws CLBuildException
    {
        long outRows = aRows;
        /*
            if (bColumns == 1) {
            mulVecKernel.setArgs(
                a, (int)aColumns,
                b, (int)bRows,
                out
            );
            return mulMatKernel.enqueueNDRange(queue, new int[] { (int)outRows }, unitIntArr, eventsToWaitFor);
        }
        */
        long outColumns = bColumns;
        return kernels.mulMat(queue,
            a, (int)aColumns,
            b, (int)bColumns,
            out,
            new int[] { (int)outRows, (int)outColumns },
            unitInt2Arr,
            eventsToWaitFor
        );
    }

    /*synchronized CLEvent dot(CLVector a b out, CLEvent... eventsToWaitFor) {
		CLEvent.waitFor(eventsToWaitFor);
		a.waitForRead();
		b.waitForRead();
		out.waitForWrite();
		FV aa  = newVector(fallBackLibrary, a);
		FV bb  = newVector(fallBackLibrary, b);
		out.write(aa.dot(bb, null).read());
		return null;
    }*/

	Reductor<DoubleBuffer> addReductor;
	synchronized Reductor<DoubleBuffer> getAddReductor() {
		if (addReductor == null) {
			try {
				addReductor = ReductionUtils.createReductor(context, ReductionUtils.Operation.Add, ReductionUtils.Type.Double, 1);
			} catch (CLBuildException ex) {
				Logger.getLogger(LinearAlgebraUtils.class.getName()).log(Level.SEVERE, null, ex);
				throw new RuntimeException("Failed to create an addition reductor !", ex);
			}
		}
		return addReductor;
	}

    public synchronized CLEvent transpose(CLDoubleBuffer a, long aRows, long aColumns, CLDoubleBuffer out, CLEvent... eventsToWaitFor) throws CLBuildException {
        return kernels.transpose(queue,
            a, aRows, aColumns,
            out,
            new int[] { (int)aColumns, (int)aRows },
            unitInt2Arr,
            eventsToWaitFor
        );
    }

}
