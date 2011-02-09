package com.nativelibs4java.opencl.util.fft;

import com.nativelibs4java.opencl.*;
import com.nativelibs4java.opencl.CLPlatform.DeviceFeature;
import com.nativelibs4java.util.*;
import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;

public class FloatDFT extends AbstractDFT<Float, FloatBuffer, float[]> {

    final FloatDFTProgram program;

    public FloatDFT(CLContext context) throws IOException, CLException {
        super(context, Float.class, FloatBuffer.class);
        program = new FloatDFTProgram(context);
    }
    public FloatDFT() throws IOException {
        this(JavaCL.createBestContext());
    }

    @Override
    protected CLEvent dft(CLQueue queue, CLBuffer<FloatBuffer> inBuf, CLBuffer<FloatBuffer> outBuf, int length, int sign, int[] dims, CLEvent... events) throws CLException {
        return program.dft(queue, inBuf, outBuf, length, sign, dims, null, events);
    }
}
