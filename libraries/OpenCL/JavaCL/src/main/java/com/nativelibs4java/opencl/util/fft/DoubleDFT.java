package com.nativelibs4java.opencl.util.fft;

import com.nativelibs4java.opencl.*;
import com.nativelibs4java.opencl.CLPlatform.DeviceFeature;
import java.io.IOException;
import java.nio.DoubleBuffer;

public class DoubleDFT extends AbstractDFT<Double, DoubleBuffer, double[]> {

    final DoubleDFTProgram program;

    public DoubleDFT(CLContext context) throws IOException {
        super(context, Double.class);
        this.program = new DoubleDFTProgram(context);
    }
    public DoubleDFT() throws IOException {
        this(JavaCL.createBestContext(DeviceFeature.DoubleSupport));
    }

    @Override
    protected CLEvent dft(CLQueue queue, CLBuffer<Double> inBuf, CLBuffer<Double> outBuf, int length, int sign, int[] dims, CLEvent... events) throws CLException {
        return program.dft(queue, (CLBuffer<Double>)inBuf, (CLBuffer<Double>)outBuf, length, sign, dims, null, events);
    }

    @Override
    public double[] transform(CLQueue queue, double[] input, boolean inverse) {
        return super.transform(queue, input, inverse);
    }
 }
