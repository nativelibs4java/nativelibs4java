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

public class GeneratorTest {
    
    @Test
    public void testStructs() throws IOException {
        CLContext context = JavaCL.createBestContext();
        CLQueue queue = context.createDefaultQueue();
        
        Structs structs = new Structs(context);
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
}