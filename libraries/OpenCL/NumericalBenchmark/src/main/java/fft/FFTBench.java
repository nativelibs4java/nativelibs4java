package fft;

import com.nativelibs4java.opencl.util.fft.DoubleFFTPow2;
import com.nativelibs4java.opencl.util.fft.FloatFFTPow2;
import com.nativelibs4java.opencl.*;
import com.nativelibs4java.opencl.CLPlatform.DeviceFeature;
import com.nativelibs4java.opencl.util.Transformer;
import com.nativelibs4java.opencl.util.Transformer.AbstractTransformer;
import com.nativelibs4java.opencl.util.fft.DoubleDFT;
import com.nativelibs4java.opencl.util.fft.FloatDFT;
import com.nativelibs4java.util.*;
import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.DoubleBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.math.complex.Complex;
import org.apache.commons.math.transform.FastFourierTransformer;

/**
 * MVN_OPTS=" -XX:+DoEscapeAnalysis -XX:+AggressiveOpts -XX:+UseCompressedOops -XX:+UseBiasedLocking -Xmx4g " mvn compile exec:java -Dexec.mainClass=fft.FFTBench
 * @author ochafik
 */
public class FFTBench {

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
    static class ApacheComplexFFT extends AbstractTransformer<Double, DoubleBuffer, double[]> {
        FastFourierTransformer fft = new FastFourierTransformer();
        public ApacheComplexFFT() {
            super(null, Double.class, DoubleBuffer.class);
        }
        
        @Override
        public double[] transform(CLQueue queue, double[] input, boolean inverse) {
            Complex[] complexInput = interleavedDoublesToComplex(input);
            Complex[] output = inverse ?
                fft.inversetransform(complexInput) :
                fft.transform(complexInput);
            return complexToInterleavedDoubles(output);
        }

        @Override
        public CLEvent transform(CLQueue queue, CLBuffer<DoubleBuffer> input, CLBuffer<DoubleBuffer> output, boolean inverse, CLEvent... eventsToWaitFor) throws CLException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

    }
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
    }
    static final float precisionFloat = 1e-3f;
    static final double precisionDouble = 1e-10;
    
    static void gc() {
        try {
            System.gc();
            Thread.sleep(100);
            System.gc();
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            Logger.getLogger(FFTBench.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    static <T, B extends Buffer, A> void test(String title, final Transformer<T, B, A> tr, A warmupData, final A data, final CLQueue queue, boolean hasReverse, boolean firstRun) {
        int warmups = 2500;
        int tests = 5;


        System.out.println("[" + title + "] Context = " + tr.getContext());
        if (firstRun) {
            System.out.print("[" + title + "] Warming up...");
            for (int i = 0; i < warmups; i++) {
                tr.transform(queue, warmupData, false);
                if (hasReverse)
                    tr.transform(queue, warmupData, true);
            }
            System.out.println();
        }

        A trans = null;
        if (hasReverse && !"1".equals(System.getenv("NO_REVERSE"))) {
            System.out.print("[" + title + "] Checking consistency of inverse...");
            trans = tr.transform(queue, data, false);
            A back = tr.transform(queue, trans, true);
            assertArrayEquals(title, data, back);
            System.out.println();
        }
        gc();

        for (int i = 0; i < tests; i++) {
            time(title/* + " (transform)"*/, new Runnable() { public void run() {
                tr.transform(queue, data, false);
            }});
            gc();
        }
        /*
        if (hasReverse)
            for (int i = 0; i < tests; i++) {
                final A invData = trans;
                time(title + " (inverse transform)", new Runnable() { public void run() {
                    tr.transform(queue, invData, true);
                }});
                gc();
            }
            */
    }
    public static void main(String[] args) throws IOException, CLBuildException {
    		// Create a context with the best double numbers support possible :
    		// (try using DeviceFeature.GPU, DeviceFeature.CPU...)

        //Native.setPreserveLastError(true);
        CLContext contextDoubles = JavaCL.createBestContext(DeviceFeature.DoubleSupport);
        CLContext contextFloats = JavaCL.createBestContext();

        // Create a command queue, if possible able to execute multiple jobs in parallel
        // (out-of-order queues will still respect the CLEvent chaining)
        CLQueue queueFloats = contextFloats.createDefaultOutOfOrderQueueIfPossible();
        CLQueue queueDoubles = contextDoubles.createDefaultOutOfOrderQueueIfPossible();

        int nWarm = 8;
        double[] warmDoubles = createTestDoubleData(nWarm);
        float[] warmFloats = toFloat(warmDoubles);
        boolean testDFT = false;

        boolean first = true;
        for (int nTest : new int[] { 1024, 4 * 1024, 128 * 1024, 1024 * 1024, 8 * 1024 * 1024 }) {
            double[] testDoubles = createTestDoubleData(nTest);
            float[] testFloats = toFloat(testDoubles);

            String sizeSuffix =  ", n = " + nTest;
            test("JavaCL Float FFT" + sizeSuffix, new FloatFFTPow2(contextFloats), warmFloats, testFloats, queueFloats, true, first);
            test("JavaCL Double FFT" + sizeSuffix, new DoubleFFTPow2(contextDoubles), warmDoubles, testDoubles, queueDoubles, true, first);
            
            test("Apache Complex FFT" + sizeSuffix, new ApacheComplexFFT(), warmDoubles, testDoubles, null, true, first);
            //test("Apache Real FFT" + sizeSuffix, new ApacheRealFFT(), warmDoubles, testDoubles, null, false);

            if (testDFT) {
                test("JavaCL Double DFT" + sizeSuffix, new DoubleDFT(contextDoubles), warmDoubles, testDoubles, queueDoubles, true, first);
                test("JavaCL Float DFT" + sizeSuffix, new FloatDFT(contextFloats), warmFloats, testFloats, queueFloats, true, first);
            }
            if (first)
                first = false;
        }
    }
    static void time(String title, Runnable r) {
        long start = System.nanoTime();
        r.run();
        long time = System.nanoTime() - start;
        System.out.println("[" + title + "] = " + (time / 1000000.0) + " milliseconds");
    }
    
    static double[] toReal(double[] in) {
        double[] inReal = new double[in.length / 2];
        for (int i = 0; i < inReal.length; i++)
            inReal[i] = in[i / 2];

        return inReal;
    }
    static float[] toFloat(double[] in) {
        float[] out = new float[in.length];
        for (int i = 0; i < in.length; i++)
            out[i] = (float)in[i];

        return out;
    }
    static double[] createTestDoubleData(int n) {
        //int n = 1024 * 1024;
        double[] in = new double[2 * n];

        for (int i = 0; i < n; i++) {
            in[i * 2] = 1 / (double) (i + 1);
            in[i * 2 + 1] = 0;
        }
        return in;
    }
}
