package com.nativelibs4java.opencl.util.fft;

import com.nativelibs4java.opencl.*;
import java.io.IOException;
import java.nio.FloatBuffer;

public class FloatFFTPow2 extends AbstractFFTPow2<Float, FloatBuffer, float[]> {

    final FloatFFTProgram program;

    public FloatFFTPow2(CLContext context) throws IOException {
        super(context, Float.class);
        this.program = new FloatFFTProgram(context);
        program.getProgram().setFastRelaxedMath();
    }
    public FloatFFTPow2() throws IOException {
        this(JavaCL.createBestContext());
    }

    protected CLEvent cooleyTukeyFFTTwiddleFactors(CLQueue queue, int N, CLBuffer<Float> buf, CLEvent... evts) throws CLException {
        return program.cooleyTukeyFFTTwiddleFactors(queue, N, buf, new int[] { N / 2 }, null, evts);
    }
    protected CLEvent cooleyTukeyFFTCopy(CLQueue queue, CLBuffer<Float> inBuf, CLBuffer<Float> outBuf, int length, CLBuffer<Integer> offsetsBuf, boolean inverse, CLEvent... evts) throws CLException {
        return program.cooleyTukeyFFTCopy(queue, inBuf, outBuf, length, offsetsBuf, inverse ? 1.0f / length : 1, new int[] { length }, null, evts);
    }
    protected CLEvent cooleyTukeyFFT(CLQueue queue, CLBuffer<Float> Y, int N, CLBuffer<Float> twiddleFactors, int inverse, int[] dims, CLEvent... evts) throws CLException {
        return program.cooleyTukeyFFT(queue, Y, N, twiddleFactors, inverse, dims, null, evts);
    }
 }

