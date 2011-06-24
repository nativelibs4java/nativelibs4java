package com.nativelibs4java.opencl.util.fft;

import com.nativelibs4java.opencl.*;
import com.nativelibs4java.opencl.CLPlatform.DeviceFeature;
import java.io.IOException;
import java.nio.DoubleBuffer;

/**
 * OpenCL Fast Fourier Transform for array sizes that are powers of two (double precision floating point numbers)
 */
public class DoubleFFTPow2 extends AbstractFFTPow2<Double, double[]> {

    final DoubleFFTProgram program;

    public DoubleFFTPow2(CLContext context) throws IOException, CLException {
        super(context, Double.class);
        this.program = new DoubleFFTProgram(context);
        program.getProgram().setFastRelaxedMath();
    }
    public DoubleFFTPow2() throws IOException {
        this(JavaCL.createBestContext(DeviceFeature.DoubleSupport));
    }

    protected CLEvent cooleyTukeyFFTTwiddleFactors(CLQueue queue, int N, CLBuffer<Double> buf, CLEvent... evts) throws CLException {
        return program.cooleyTukeyFFTTwiddleFactors(queue, N, buf, new int[] { N / 2 }, null, evts);
    }
    protected CLEvent cooleyTukeyFFTCopy(CLQueue queue, CLBuffer<Double> inBuf, CLBuffer<Double> outBuf, int length, CLBuffer<Integer> offsetsBuf, boolean inverse, CLEvent... evts) throws CLException {
        return program.cooleyTukeyFFTCopy(queue, inBuf, outBuf, length, offsetsBuf, inverse ? 1.0 / length : 1, new int[] { length }, null, evts);
    }
    protected CLEvent cooleyTukeyFFT(CLQueue queue, CLBuffer<Double> Y, int N, CLBuffer<Double> twiddleFactors, int inverse, int[] dims, CLEvent... evts) throws CLException {
        return program.cooleyTukeyFFT(queue, Y, N, twiddleFactors, inverse, dims, null, evts);
    }
 }

