package com.nativelibs4java.opencl.generator;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLMem;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.JavaCL;
import java.io.IOException;
import org.bridj.Pointer;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

public class GeneratorTest {
    CLContext context;
    CLQueue queue;
    Structs structs;
        
    @Before
    public void setup() throws IOException {
        context = JavaCL.createBestContext();
        queue = context.createDefaultQueue();
        structs = new Structs(context);
    }

    @Test
    public void testStructs() throws IOException {
        Structs.S s = new Structs.S();
        Pointer<Structs.S> pS = Pointer.getPointer(s);
        CLBuffer<Structs.S> b = context.createBuffer(CLMem.Usage.InputOutput, pS);
        
        CLEvent e = structs.f(queue, b, new int[] { 1 }, null);
        b.read(queue, pS, true, e);
        assertEquals(10, s.a());
        assertEquals(100, s.b());
        
        s.a(1).b(2);
        b.write(queue, pS, true);
        e = structs.f(queue, b, new int[] { 1 }, null);
        b.read(queue, pS, true, e);
        assertEquals(12, s.a());
        assertEquals(120, s.b());
        
    }
    
    @Test
    public void testFloat3() {
        float[] input = new float[] { 1, 2, 3 };
        CLBuffer<Float> outputBuffer = context.createFloatBuffer(CLMem.Usage.Output, 3);
        CLEvent e = structs.g(queue, input, outputBuffer, new int[] { 1 }, null);
        float[] output = outputBuffer.read(queue, e).getFloats();
        assertArrayEquals(input, output, 0.0f);
    }
}