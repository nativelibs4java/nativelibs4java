package com.nativelibs4java.opencl.util.fft;

import com.nativelibs4java.opencl.*;
import com.nativelibs4java.opencl.CLPlatform.DeviceFeature;
import com.nativelibs4java.util.*;
import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;

public class FloatDFT extends AbstractDFT<FloatBuffer> {

    final FloatDFTProgram program;

    public FloatDFT(CLQueue queue) throws IOException, CLBuildException {
        super(queue, FloatBuffer.class);
        program = new FloatDFTProgram(context);
    }

    /// Wrapper method that takes and returns float arrays
    public float[] dft(float[] complexValues, boolean inverse) throws CLBuildException {
        return (float[])dftArray(complexValues, inverse);
    }
    @Override
    protected CLEvent dft(CLBuffer<FloatBuffer> inBuf, CLBuffer<FloatBuffer> outBuf, int length, int sign, int[] dims, CLEvent... events) throws CLBuildException {
        return program.dft(queue, (CLFloatBuffer)inBuf, (CLFloatBuffer)outBuf, length, sign, dims, null, events);
    }
}
