/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nativelibs4java.opencl;

import com.nativelibs4java.opencl.CLDevice.*;
import com.nativelibs4java.opencl.library.OpenCLLibrary.*;
import java.lang.reflect.*;
import java.util.EnumSet;
import java.util.logging.*;
import org.junit.*;
import static org.junit.Assert.*;
import static com.nativelibs4java.test.MiscTestUtils.*;

/**
 *
 * @author ochafik
 */
public class InfoGettersTest {

    CLProgram createProgram() {
        CLProgram pg = createContext().createProgram("__kernel void f(__global int* a) {}");
        try {
            pg.build();
        } catch (CLBuildException ex) {
            assertFalse(ex.toString(), true);
        }
        return pg;
    }

    CLPlatform createPlatform() {
        return OpenCL4Java.listPlatforms()[0];
    }

    CLDevice createDevice() {
        return createPlatform().listAllDevices(true)[0];
    }

    CLContext createContext() {
        return createPlatform().createContext(createDevice());
    }

    CLKernel createKernel() {
        return createProgram().createKernels()[0];
    }

    CLEvent createEvent() {
        CLContext c = createContext();
        return c.createIntBuffer(CLMem.Usage.Input, 10).mapLater(c.createDefaultQueue(), CLMem.MapFlags.Read).getSecond();
    }

    CLSampler createSampler() {
        return createContext().createSampler(true, CLSampler.AddressingMode.ClampToEdge, CLSampler.FilterMode.Linear);
    }

    CLQueue createQueue() {
        CLContext c = createContext();
        CLDevice d = c.getDevices()[0];
        return d.createQueue(c);
    }

    @org.junit.Test
    public void CLProgramGetters() {
        testGetters(createProgram());
    }

    @org.junit.Test
    public void CLKernelGetters() {
        testGetters(createKernel());
    }

    @org.junit.Test
    public void CLMemGetters() {
        testGetters(createContext().createByteBuffer(CLMem.Usage.Input, 10));
        testGetters(createContext().createByteBuffer(CLMem.Usage.Output, 10));
    }

    @org.junit.Test
    public void CLQueueGetters() {
        testGetters(createQueue());
    }

    @org.junit.Test
    public void CLDeviceGetters() {
        testGetters(createDevice());
    }

    @org.junit.Test
    public void CLPlatformGetters() {
        testGetters(createPlatform());
    }

    @org.junit.Test
    public void CLContextGetters() {
        testGetters(createContext());
    }

    @org.junit.Test
    public void CLEventGetters() {
        testGetters(createEvent());
    }

    @org.junit.Test
    public void CLSamplerGetters() {
        testGetters(createSampler());
    }
}
