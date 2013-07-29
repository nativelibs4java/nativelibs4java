package com.nativelibs4java.opencl;

import org.junit.Test;

public class SweatTest {
    static {
        System.setProperty("bridj.debug.pointer.releases", "true");
    }
    @Test
    public void sweatTest() {
        long tot = 0;
        for (boolean cached : new boolean[] { false, true }) {
            for (int time  = 0; time < 100; time++) {
                CLContext context = JavaCL.createBestContext(CLPlatform.DeviceFeature.GPU);
                CLQueue queue = context.createDefaultQueue();
                CLProgram program = context.createProgram("kernel void f(global int* a) { a[0] = 1; }");
                program.setCached(cached);
                program.build();
                CLKernel kernel = program.createKernel("f");
                kernel.release();
                program.release();
                queue.release();
                context.release();
                System.gc();
            }
        }
        System.out.println(tot);
    }
}