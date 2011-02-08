package com.nativelibs4java.opencl.util.fft;

import com.nativelibs4java.opencl.*;
import com.nativelibs4java.opencl.CLPlatform.DeviceFeature;
import com.nativelibs4java.opencl.util.Transformer.FloatTransformer;
import com.nativelibs4java.util.*;
import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;

public class FloatDFT extends AbstractDFT<FloatBuffer, float[]> implements FloatTransformer {

    final FloatDFTProgram program;

    public FloatDFT(CLQueue queue) throws IOException, CLBuildException {
        super(queue, FloatBuffer.class);
        program = new FloatDFTProgram(context);
    }

    @Override
    protected CLEvent dft(CLBuffer<FloatBuffer> inBuf, CLBuffer<FloatBuffer> outBuf, int length, int sign, int[] dims, CLEvent... events) throws CLBuildException {
        return program.dft(queue, (CLFloatBuffer)inBuf, (CLFloatBuffer)outBuf, length, sign, dims, null, events);
    }
}
