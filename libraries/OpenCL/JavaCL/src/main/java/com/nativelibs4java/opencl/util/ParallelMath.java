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
        atanh;

        void expr(String a, StringBuilder out) {
            out.append(name()).append('(').append(a).append(")");
        }
    }
    public enum Fun2 {
        atan2,
        dist,
        modulo("%"),
        rshift(">>"),
        lshift("<<"),
        add("+"),
        substract("-"),
        multiply("*"),
        divide("/");

        String infixOp;
        Fun2() {}
        Fun2(String infixOp) {
            this.infixOp = infixOp;
        }
        void expr(String a, String b, StringBuilder out) {
            if (infixOp == null)
                out.append(name()).append('(').append(a).append(", ").append(b).append(")");
            else
                out.append(a).append(' ').append(infixOp).append(' ').append(b);
        }
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
        Byte16;

        String type() {
            return name().toLowerCase();
        }
    }

    protected String createVectFun1Source(Fun1 function, Primitive type, StringBuilder out, boolean inPlace) {
        String t = type.type();
        String kernelName = "vect_" + function.name() + "_" + t + (inPlace ? "_inplace" : "");
        out.append("__kernel void " + kernelName + "(\n");
        if (!inPlace)
            out.append("\t__global const " + t + "* in,\n");
        out.append("\t__global " + t + "* out\n");
        out.append(") {\n");
        out.append("\tint i = get_global_id(0);\n");
        out.append("\tout[i] = ");
        function.expr(inPlace ? "out" : "in", out);
        out.append("[i]);\n");
        out.append("}\n");
        return kernelName;
    }
    
    
    protected String createVectFun2Source(Fun2 function, Primitive type1, Primitive type2, Primitive typeOut, StringBuilder out) {
        String t1 = type1.type(), t2 = type2.type(), to = typeOut.type();
        String kernelName = "vect_" + function.name() + "_" + t1 + "_" + t2 + "_" + to;
        out.append("__kernel void " + kernelName + "(\n");
        out.append("\t__global const " + t1 + "* in1,\n");
        out.append("\t__global const " + t2 + "* in2,\n");
        out.append("\t__global " + to + "* out\n");
        out.append(") {\n");
        out.append("\tint i = get_global_id(0);\n");
        out.append("\tout[i] = (" + to + ")");
        function.expr("in1[i]", "in2[i]", out);
        out.append(";\n");
        out.append("}\n");
        return kernelName;
    }


    private static class Fun1Kernels {
        CLKernel inPlace, notInPlace;
    }
    private EnumMap<Fun1, EnumMap<Primitive, Fun1Kernels>> fun1Kernels = new EnumMap<Fun1, EnumMap<Primitive, Fun1Kernels>>(Fun1.class);

    
    public synchronized CLKernel getKernel(Fun1 op, Primitive prim, boolean inPlace) throws CLBuildException {
        EnumMap<Primitive, Fun1Kernels> m = fun1Kernels.get(op);
        if (m == null)
            fun1Kernels.put(op, m = new EnumMap<Primitive, Fun1Kernels>(Primitive.class));

        Fun1Kernels kers = m.get(prim);
        if (kers == null) {
            StringBuilder out = new StringBuilder(300);
            String inPlaceName = createVectFun1Source(op, prim, out, true);
            String notInPlaceName = createVectFun1Source(op, prim, out, false);
            CLProgram prog = getContext().createProgram(out.toString()).build();
            kers = new Fun1Kernels();
            kers.inPlace = prog.createKernel(inPlaceName);
            kers.notInPlace = prog.createKernel(notInPlaceName);
            m.put(prim, kers);
        }
        return inPlace ? kers.inPlace : kers.notInPlace;
    }

    static class PrimitiveTrio extends Pair<Primitive, Pair<Primitive, Primitive>> {
        public PrimitiveTrio(Primitive a, Primitive b, Primitive c) {
            super(a, new Pair<Primitive, Primitive>(b, c));
        }
    }
    private EnumMap<Fun2, Map<PrimitiveTrio, CLKernel>> fun2Kernels = new EnumMap<Fun2, Map<PrimitiveTrio, CLKernel>>(Fun2.class);
    public synchronized CLKernel getKernel(Fun2 op, Primitive prim) throws CLBuildException {
        return getKernel(op, prim, prim, prim);
    }

    public synchronized CLKernel getKernel(Fun2 op, Primitive prim1, Primitive prim2, Primitive primOut) throws CLBuildException {
        Map<PrimitiveTrio, CLKernel> m = fun2Kernels.get(op);
        if (m == null)
            fun2Kernels.put(op, m = new HashMap<PrimitiveTrio, CLKernel>());

        PrimitiveTrio key = new PrimitiveTrio(prim1, prim2, primOut);
        CLKernel ker = m.get(key);
        if (ker == null) {
            StringBuilder out = new StringBuilder(300);
            String name = createVectFun2Source(op, prim1, prim2, primOut, out);
            CLProgram prog = getContext().createProgram(out.toString()).build();
            ker = prog.createKernel(name);
            m.put(key, ker);
        }
        return ker;
    }
    
}
