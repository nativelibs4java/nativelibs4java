/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.util;
import org.apache.commons.math.complex.Complex;
import com.nativelibs4java.opencl.util.fft.*;
import com.nativelibs4java.opencl.*;
import com.nativelibs4java.opencl.CLPlatform.DeviceFeature;
import java.io.IOException;
import java.util.*;
import java.nio.*;
import org.apache.commons.math.transform.FastFourierTransformer;
import org.junit.*;
import static org.junit.Assert.*;

/**
 *
 * @author ochafik
 */
public class DiscreteFourierTransformTest {

    static double[] complexToInterleavedDoubles(Complex[] complexInput) {
        int length = complexInput.length;
        double[] output = new double[length * 2];
        for (int i = 0; i < length; i++) {
            int o = i * 2;
            Complex c = complexInput[i];
            output[o] = c.getReal();
            output[o + 1] = c.getImaginary();
        }
        return output;
    }
    static Complex[] interleavedDoublesToComplex(double[] input) {
        int length = input.length / 2;
        Complex[] complexOutput = new Complex[length];
        for (int i = 0; i < length; i++) {
            int o = i * 2;
            complexOutput[i] = new Complex(input[o], input[o + 1]);
        }
        return complexOutput;
    }
    static Complex[] realDoublesToComplex(double[] input) {
        int length = input.length;
        Complex[] complexOutput = new Complex[length];
        for (int i = 0; i < length; i++) {
            complexOutput[i] = new Complex(input[i], 0);
        }
        return complexOutput;
    }
    /*
    static void assertArrayEquals(String title, Object a, Object b) {
        if (a instanceof double[]) {
            double[] aa = (double[])a, bb = (double[])b;
            for (int i = 0; i < aa.length; i++) {
                if (Math.abs(aa[i] - bb[i]) > precisionDouble)
                    throw new RuntimeException("[" + title + "] Values different at index " + i + " : " + aa[i] + " vs. " + bb[i] + " !");
            }
        } else if (a instanceof float[]) {
            float[] aa = (float[])a, bb = (float[])b;
            for (int i = 0; i < aa.length; i++) {
                if (Math.abs(aa[i] - bb[i]) > precisionFloat)
                    throw new RuntimeException("[" + title + "] Values different at index " + i + " : " + aa[i] + " vs. " + bb[i] + " !");
            }
        }
    }*/
    static final float precisionFloat = 1e-3f, precisionInverseFloat = 1e-2f;
    static final double precisionDouble = 1e-10, precisionInverseDouble = 1e-5;
    
    static Collection<double[]> createTestDoubleInputs() {
        Collection<double[]> ret = new ArrayList<double[]>();
        for (int n : new int[] { 1, 2, 4, 8, 16, 1024 }) {
            double[] in = new double[2 * n];

            for (int i = 0; i < n; i++) {
                in[i * 2] = 1 / (double)(i + 1);
                in[i * 2 + 1] = 0;
            }
            ret.add(in);
        }
        return ret;
    }
    static Collection<float[]> createTestFloatInputs() {
    		Collection<float[]> ret = new ArrayList<float[]>();
    		for (double[] in : createTestDoubleInputs())
    			ret.add(toFloat(in));
    		return ret;
    }

    static float[] toFloat(double[] in) {
        float[] out = new float[in.length];
        for (int i = 0; i < in.length; i++)
            out[i] = (float)in[i];

        return out;
    }
    static double[] scale(double factor, double[] in) {
        double[] out = new double[in.length];
        for (int i = 0; i < in.length; i++)
            out[i] = factor * in[i];

        return out;
    }

    public void testDoubleValues(String title, Transformer<Double, double[]> tr) {
        FastFourierTransformer apache = new FastFourierTransformer();
        CLQueue queue = tr.getContext() == null ? null : tr.getContext().createDefaultOutOfOrderQueueIfPossible();
        for (double[] data : createTestDoubleInputs()) {
            double[] expected = complexToInterleavedDoubles(apache.transform(interleavedDoublesToComplex(data)));
            assertArrayEquals(title + " (n = " + (data.length / 2) + ")", expected, tr.transform(queue, data, false), precisionDouble);
        }
    }
    public void testFloatValues(String title, Transformer<Float, float[]> tr) {
        FastFourierTransformer apache = new FastFourierTransformer();
        CLQueue queue = tr.getContext() == null ? null : tr.getContext().createDefaultOutOfOrderQueueIfPossible();
        for (double[] data : createTestDoubleInputs()) {
            float[] dataf = toFloat(data);
            double[] expected = complexToInterleavedDoubles(apache.transform(interleavedDoublesToComplex(data)));
            assertArrayEquals(title + " (n = " + (data.length / 2) + ")", toFloat(expected), tr.transform(queue, dataf, false), precisionFloat);
        }
    }

    @Ignore
    @Test
    public void testDoubleFFTValues() throws IOException {
        testDoubleValues("Double FFT", new DoubleFFTPow2());
    }
    @Test
    public void testFloatFFTValues() throws IOException {
        testFloatValues("Float FFT", new FloatFFTPow2());
    }
    @Test
    public void testDoubleDFTValues() throws IOException {
        testDoubleValues("Double DFT", new DoubleDFT());
    }
    @Test
    public void testFloatDFTValues() throws IOException {
        testFloatValues("Float DFT", new FloatDFT());
    }
    @Test
    public void testDoubleDFTInverse() throws IOException, CLException {
        testDoubleTransformer("Double FFT Inverse", new DoubleDFT());
    }
    @Test
    public void testFloatDFTInverse() throws IOException, CLException {
        testFloatTransformer("Float DFT Inverse", new FloatDFT());
    }
    @Ignore
    @Test
    public void testDoubleFFTInverse() throws IOException, CLException {
        testDoubleTransformer("Double FFT Inverse", new DoubleFFTPow2());
    }
    @Test
    public void testFloatFFTInverse() throws IOException, CLException {
        testFloatTransformer("Float FFT Inverse", new FloatFFTPow2());
    }
    void testDoubleTransformer(String title, Transformer<Double, double[]> t) throws IOException, CLException {
        CLQueue queue = t.getContext().createDefaultOutOfOrderQueueIfPossible();
        //System.out.println("Context: " + t.getContext());
        for (double[] in : createTestDoubleInputs()) {
            double[] out = t.transform(queue, in, false);
            double[] back = t.transform(queue, out, true);
            assertArrayEquals(title + " (n = " + (in.length / 2) + ")", in, back, precisionInverseDouble);
        }
    }
    void testFloatTransformer(String title, Transformer<Float, float[]> t) throws IOException, CLException {
        CLQueue queue = t.getContext().createDefaultOutOfOrderQueueIfPossible();
        //System.out.println("Context: " + t.getContext());
        for (float[] in : createTestFloatInputs()) {
            float[] out = t.transform(queue, in, false);
            float[] back = t.transform(queue, out, true);
            assertArrayEquals(title + " (n = " + (in.length / 2) + ")", in, back, precisionInverseFloat);
        }
    }
}
