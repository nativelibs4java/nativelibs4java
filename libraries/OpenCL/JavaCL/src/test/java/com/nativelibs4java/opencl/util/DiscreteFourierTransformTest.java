/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.util;
import com.nativelibs4java.opencl.util.fft.*;
import com.nativelibs4java.opencl.*;
import com.nativelibs4java.opencl.CLPlatform.DeviceFeature;
import java.io.IOException;
import java.util.*;
import java.nio.*;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author ochafik
 */
public class DiscreteFourierTransformTest {

    static Collection<double[]> createTestDoubleInputs() {
        int n = 32;
        double[] in = new double[2 * n];

        for (int i = 0; i < n; i++) {
            in[i * 2] = 1 / (double)(i + 1);
            in[i * 2 + 1] = 0;
        }
        return Arrays.asList(in);
    }
    static Collection<float[]> createTestFloatInputs() {
    		Collection<float[]> ret = new ArrayList<float[]>();
    		for (double[] in : createTestDoubleInputs()) {
    			float[] ff = new float[in.length];
    			for (int i = 0; i < in.length; i++)
    				ff[i] = (float)in[i];
    			ret.add(ff);
    		}
    		return ret;
    }
    
    @Test
    public void testDoubleDFT() throws IOException, CLException {
        testDoubleTransformer(new DoubleDFT(JavaCL.createBestContext(DeviceFeature.DoubleSupport)));
    }
    @Test
    public void testFloatDFT() throws IOException, CLException {
        testFloatTransformer(new FloatDFT(JavaCL.createBestContext()));
    }
    @Test
    public void testDoubleFFT() throws IOException, CLException {
        testDoubleTransformer(new DoubleFFTPow2(JavaCL.createBestContext(DeviceFeature.DoubleSupport)));
    }
    @Test
    public void testFloatFFT() throws IOException, CLException {
        testFloatTransformer(new FloatFFTPow2(JavaCL.createBestContext()));
    }
    void testDoubleTransformer(Transformer<DoubleBuffer, double[]> t) throws IOException, CLException {
        CLQueue queue = t.getContext().createDefaultOutOfOrderQueueIfPossible();
        System.out.println("Context: " + t.getContext()); 
        
        for (double[] in : createTestDoubleInputs()) {

            double[] out = t.transform(queue, in, false);
            assertEquals(in.length, out.length);
            assertTrue(Math.abs(out[0] - in[0]) > 0.1);
            double[] back = t.transform(queue, out, true);
            assertEquals(back.length, out.length);
            
            double precision = 1e-5;
            for (int i = 0; i < in.length; i++) {
                assertEquals(in[i], back[i], precision);
            }
        }
    }
    
    void testFloatTransformer(Transformer<FloatBuffer, float[]> t) throws IOException, CLException {
        CLQueue queue = t.getContext().createDefaultOutOfOrderQueueIfPossible();
        System.out.println("Context: " + t.getContext()); 
        for (float[] in : createTestFloatInputs()) {

            float[] out = t.transform(queue, in, false);
            assertEquals(in.length, out.length);
            assertTrue(Math.abs(out[0] - in[0]) > 0.1);
            float[] back = t.transform(queue, out, true);
            assertEquals(back.length, out.length);
            
            float precision = 1e-5f;
            for (int i = 0; i < in.length; i++) {
                assertEquals(in[i], back[i], precision);
            }
        }
    }
    
}
