package com.nativelibs4java.opencl.util.fft;

import com.nativelibs4java.opencl.*;
import java.io.IOException;
import java.nio.FloatBuffer;

public class FloatFFTPow2 extends AbstractFFTPow2<FloatBuffer> {

    final FloatFFTProgram program;

    public FloatFFTPow2(CLQueue queue) throws IOException, CLBuildException {
        super(queue, FloatBuffer.class);
        this.program = new FloatFFTProgram(context);
    }

    protected CLEvent cooleyTukeyFFTTwiddleFactors(int N, CLBuffer<FloatBuffer> buf, CLEvent... evts) throws CLBuildException {
        return program.cooleyTukeyFFTTwiddleFactors(queue, N, (CLFloatBuffer)buf, new int[] { N / 2 }, null, evts);
    }
    protected CLEvent cooleyTukeyFFTCopy(CLBuffer<FloatBuffer> inBuf, CLBuffer<FloatBuffer> outBuf, int length, CLIntBuffer offsetsBuf, boolean inverse, CLEvent... evts) throws CLBuildException {
        return program.cooleyTukeyFFTCopy(queue, (CLFloatBuffer)inBuf, (CLFloatBuffer)outBuf, length, offsetsBuf, inverse ? 1 : 1.0f / length, new int[] { length }, null, evts);
    }
    protected CLEvent cooleyTukeyFFT(CLBuffer<FloatBuffer> Y, int N, CLBuffer<FloatBuffer> twiddleFactors, int inverse, int[] dims, CLEvent... evts) throws CLBuildException {
        return program.cooleyTukeyFFT(queue, (CLFloatBuffer)Y, N, (CLFloatBuffer)twiddleFactors, inverse, dims, null, evts);
    }
	public float[] fft(float[] complexValues, boolean inverse) throws CLBuildException {
        return (float[])fftArray(complexValues, inverse);
    }
 }

