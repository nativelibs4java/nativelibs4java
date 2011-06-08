/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.util;

import com.nativelibs4java.opencl.CLBuildException;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLProgram;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.JavaCL;
import com.nativelibs4java.opencl.util.ReductionUtils;
import com.nativelibs4java.opencl.util.ReductionUtils.Reductor;
import com.nativelibs4java.util.IOUtils;
import com.nativelibs4java.util.Pair;
import static com.nativelibs4java.util.NIOUtils.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ochafik
 */
@SuppressWarnings("unused")
public class ParallelMath {

    protected CLContext context;
    protected CLQueue queue;

    public ParallelMath() {
        this(JavaCL.createBestContext().createDefaultQueue());
    }

    public ParallelMath(CLQueue queue) {
        this.queue = queue;
		CLContext context = queue.getContext();
    }

    public CLQueue getQueue() {
        return queue;
    }

	public CLContext getContext() {
		return getQueue().getContext();
	}

    protected String createVectFun1Source(Fun1 function, Primitive type, StringBuilder out) {
        String t = type.clTypeName();
        String kernelName = "vect_" + function.name() + "_" + t;// + (inPlace ? "_inplace" : "");
        out.append("__kernel void " + kernelName + "(\n");
        out.append("\t__global const " + t + "* in,\n");
        out.append("\t__global " + t + "* out,\n");
        out.append("\tlong length\n");
        out.append(") {\n");
        out.append("\tint i = get_global_id(0);\n");
        out.append("\tif (i >= length) return;\n");
        out.append("\tout[i] = ");
        function.expr("in", out);
        out.append("[i]);\n");
        out.append("}\n");
        return kernelName;
    }
    
    
    protected String createVectFun2Source(Fun2 function, Primitive type1, Primitive type2, Primitive typeOut, StringBuilder out, boolean secondOperandIsScalar) {
        String t1 = type1.clTypeName(), t2 = type2.clTypeName(), to = typeOut.clTypeName();
        String kernelName = "vect_" + function.name() + "_" + t1 + "_" + t2 + "_" + to;
        out.append("__kernel void " + kernelName + "(\n");
        out.append("\t__global const " + t1 + "* in1,\n");
        if (secondOperandIsScalar)
            out.append("\t" + t2 + "* in2,\n");
        else
            out.append("\t__global const " + t2 + "* in2,\n");
        out.append("\t__global " + to + "* out,\n");
        out.append("\tlong length\n");
        out.append(") {\n");
        out.append("\tint i = get_global_id(0);\n");
        out.append("\tif (i >= length) return;\n");
        out.append("\tout[i] = (" + to + ")");
        function.expr("in1[i]", (secondOperandIsScalar ? "in2" : "in2[i]"), out);
        out.append(";\n");
        out.append("}\n");
        return kernelName;
    }


    private EnumMap<Fun1, EnumMap<Primitive, CLKernel>> fun1Kernels = new EnumMap<Fun1, EnumMap<Primitive, CLKernel>>(Fun1.class);

    
    public synchronized CLKernel getKernel(Fun1 op, Primitive prim) throws CLBuildException {
        EnumMap<Primitive, CLKernel> m = fun1Kernels.get(op);
        if (m == null)
            fun1Kernels.put(op, m = new EnumMap<Primitive, CLKernel>(Primitive.class));

        CLKernel kers = m.get(prim);
        if (kers == null) {
            StringBuilder out = new StringBuilder(300);
            String name = createVectFun1Source(op, prim, out);
            CLProgram prog = getContext().createProgram(out.toString()).build();
            kers = prog.createKernel(name);
            m.put(prim, kers);
        }
        return kers;
    }

    static class PrimitiveTrio extends Pair<Pair<Primitive, Primitive>, Pair<Primitive, Boolean>> {
        public PrimitiveTrio(Primitive a, Primitive b, Primitive c, boolean secondOperandIsScalar) {
            super(new Pair<Primitive, Primitive>(a, b), new Pair<Primitive, Boolean>(c, secondOperandIsScalar));
        }
    }
    private EnumMap<Fun2, Map<PrimitiveTrio, CLKernel>> fun2Kernels = new EnumMap<Fun2, Map<PrimitiveTrio, CLKernel>>(Fun2.class);
    public synchronized CLKernel getKernel(Fun2 op, Primitive prim, boolean secondOperandIsScalar) throws CLBuildException {
        return getKernel(op, prim, prim, prim, secondOperandIsScalar);
    }

    public synchronized CLKernel getKernel(Fun2 op, Primitive prim1, Primitive prim2, Primitive primOut, boolean secondOperandIsScalar) throws CLBuildException {
        Map<PrimitiveTrio, CLKernel> m = fun2Kernels.get(op);
        if (m == null)
            fun2Kernels.put(op, m = new HashMap<PrimitiveTrio, CLKernel>());

        PrimitiveTrio key = new PrimitiveTrio(prim1, prim2, primOut, secondOperandIsScalar);
        CLKernel ker = m.get(key);
        if (ker == null) {
            StringBuilder out = new StringBuilder(300);
            String name = createVectFun2Source(op, prim1, prim2, primOut, out, secondOperandIsScalar);
            CLProgram prog = getContext().createProgram(out.toString()).build();
            ker = prog.createKernel(name);
            m.put(key, ker);
        }
        return ker;
    }
    
}
