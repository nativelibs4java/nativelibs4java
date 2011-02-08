package com.nativelibs4java.opencl.util.fft;

import com.nativelibs4java.opencl.*;
import java.io.IOException;
import java.nio.DoubleBuffer;

public class DoubleFFTPow2 extends AbstractFFTPow2<DoubleBuffer> {

    final DoubleFFTProgram program;

    public DoubleFFTPow2(CLQueue queue) throws IOException, CLBuildException {
        super(queue, DoubleBuffer.class);
        this.program = new DoubleFFTProgram(context);
    }

    protected CLEvent cooleyTukeyFFTTwiddleFactors(int N, CLBuffer<DoubleBuffer> buf, CLEvent... evts) throws CLBuildException {
        return program.cooleyTukeyFFTTwiddleFactors(queue, N, (CLDoubleBuffer)buf, new int[] { N / 2 }, null, evts);
    }
    protected CLEvent cooleyTukeyFFTCopy(CLBuffer<DoubleBuffer> inBuf, CLBuffer<DoubleBuffer> outBuf, int length, CLIntBuffer offsetsBuf, boolean inverse, CLEvent... evts) throws CLBuildException {
        return program.cooleyTukeyFFTCopy(queue, (CLDoubleBuffer)inBuf, (CLDoubleBuffer)outBuf, length, offsetsBuf, inverse ? 1 : 1.0 / length, new int[] { length }, null, evts);
    }
    protected CLEvent cooleyTukeyFFT(CLBuffer<DoubleBuffer> Y, int N, CLBuffer<DoubleBuffer> twiddleFactors, int inverse, int[] dims, CLEvent... evts) throws CLBuildException {
        return program.cooleyTukeyFFT(queue, (CLDoubleBuffer)Y, N, (CLDoubleBuffer)twiddleFactors, inverse, dims, null, evts);
    }
	public double[] fft(double[] complexValues, boolean inverse) throws CLBuildException {
        return (double[])fftArray(complexValues, inverse);
    }
 }

