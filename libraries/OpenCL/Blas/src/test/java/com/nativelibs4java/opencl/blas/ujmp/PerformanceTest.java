/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nativelibs4java.opencl.blas.ujmp;

import com.nativelibs4java.opencl.CLDevice;
import org.ujmp.core.calculation.Calculation.Ret;
import org.ujmp.core.floatmatrix.impl.DefaultDenseFloatMatrix2D;
import org.ujmp.core.Matrix;
import com.nativelibs4java.util.Pair;
import com.nativelibs4java.opencl.blas.CLKernels;
import com.nativelibs4java.opencl.JavaCL;
import com.nativelibs4java.opencl.CLPlatform.DeviceFeature;
import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author ochafik
 */
public class PerformanceTest {
    
    public PerformanceTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Test
	public void testGPUPerfFloat() throws IOException {
        //CLKernels.setInstance(new CLKernels(JavaCL.createBestContext(DeviceFeature.GPU).createDefaultQueue()));
        
        //int size = 100;
        
        for (int size : new int[] { 10, 50, 100/*, 200, 400*/ }) {
        
            DefaultDenseFloatMatrix2D mJava = new DefaultDenseFloatMatrix2D(size, size);
            Matrix pJava = testPerf("Java(size = " + size +")", mJava).getValue();

            for (DeviceFeature feat : new DeviceFeature[] { DeviceFeature.CPU, DeviceFeature.GPU }) {
                CLKernels.setInstance(new CLKernels(JavaCL.createBestContext(feat).createDefaultQueue()));
                CLDevice device = CLKernels.getInstance().getQueue().getDevice();

                CLDenseFloatMatrix2D mCL = new CLDenseFloatMatrix2D(size, size);
                Matrix pCL = testPerf("OpenCL(size = " + size +", device = " + device + ")", mCL).getValue();

                assertEquals(pJava, pCL);
            }
        }
    }
    
    interface Action<V> {
        V perform();
    }
    
    private <V> Pair<Double, V> testMillis(String title, Action<V> action) {
        int times = 10;
        double total = 0;
        V v = null;
        for (int i = 0; i <= times; i++) {
            long start = System.nanoTime();
            v = action.perform();
            double time = (System.nanoTime() - start) / 1000000.0;
            if (i > 0) // skip first value (rough warmup)
                total += time;
        }
        double avg = total / times;
        System.out.println("Time[ " + title + " ] = " + avg + " ms");
        return new Pair<Double, V>(avg, v);
    }
    
    private Pair<Double, Matrix> testPerf(String title, final Matrix _m) {
        final int pow = 100;
        
        System.out.println();
        testMillis("svd(" + title + ")", new Action<Void>() {
            final Matrix m = _m.copy();
            public Void perform() {
                Matrix[] svd = m.svd();
                for (Matrix s : svd)
                    s.getAsDouble(0, 0);
                return null;
            }
        });
        
        testMillis("sq(" + title + ")", new Action<Matrix>() {
            final Matrix m = _m.copy();
            public Matrix perform() {
                Matrix sq = m.mtimes(Ret.NEW, true, m);
                sq.getAsDouble(0, 0);
                return sq;
            }
        });
        
        testMillis("add(" + title + ")", new Action<Matrix>() {
            final Matrix m = _m.copy();
            public Matrix perform() {
                Matrix mm = m.plus(Ret.ORIG, true, m).plus(Ret.ORIG, true, m).plus(Ret.ORIG, true, m).plus(Ret.ORIG, true, m);
                mm.getAsDouble(0, 0);
                return mm;
            }
        });
        
        testMillis("sin(" + title + ")", new Action<Matrix>() {
            final Matrix m = _m.copy();
            public Matrix perform() {
                Matrix mm = m.sin(Ret.ORIG);
                mm.getAsDouble(0, 0);
                return mm;
            }
        });
        
        testMillis("transpose(" + title + ")", new Action<Matrix>() {
            final Matrix m = _m.copy();
            public Matrix perform() {
                Matrix sq = m.transpose(Ret.ORIG).transpose(Ret.ORIG).transpose(Ret.ORIG).transpose(Ret.ORIG);
                sq.getAsDouble(0, 0);
                return sq;
            }
        });
        
        return testMillis("pow(" + title + ", " + pow + ")", new Action<Matrix>() {
            final Matrix m = _m.copy();
            public Matrix perform() {
                Matrix power = m;
                for (int i = 1; i < pow; i++)
                    power.mtimes(Ret.ORIG, true, m);
                //Matrix power = m.power(Ret.NEW, pow);
                power.getAsDouble(0, 0);
                return power;
            }
        });
    }
}
