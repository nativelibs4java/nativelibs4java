/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.util;
import com.nativelibs4java.opencl.*;
import com.nativelibs4java.opencl.CLPlatform.DeviceFeature;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

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
    @Test
    public void testDoubleDFT() throws IOException, CLBuildException {
        
        
        CLContext context = JavaCL.createBestContext(DeviceFeature.DoubleSupport);
        CLQueue queue = context.createDefaultOutOfOrderQueueIfPossible();

        DiscreteFourierTransform dft = new DiscreteFourierTransform(queue);
        
        for (double[] in : createTestDoubleInputs()) {

            double[] out = dft.transform(in);
            assertEquals(in.length, out.length);
            assertTrue(Math.abs(out[0] - in[0]) > 0.1);
            double[] back = dft.reverseTransform(out);
            assertEquals(back.length, out.length);
            
            double precision = 1e-5;
            for (int i = 0; i < in.length; i++) {
                assertEquals(in[i], back[i], precision);
            }
        }
    }
}
