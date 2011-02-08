package com.nativelibs4java.opencl.util.fft;

import com.nativelibs4java.opencl.*;
import java.io.IOException;
import java.nio.DoubleBuffer;

public class DoubleDFT extends AbstractDFT<DoubleBuffer, double[]> {

    final DoubleDFTProgram program;

    public DoubleDFT(CLContext context) throws IOException, CLException {
        super(context, DoubleBuffer.class);
        this.program = new DoubleDFTProgram(context);
    }

    @Override
    protected CLEvent dft(CLQueue queue, CLBuffer<DoubleBuffer> inBuf, CLBuffer<DoubleBuffer> outBuf, int length, int sign, int[] dims, CLEvent... events) throws CLException {
        return program.dft(queue, (CLDoubleBuffer)inBuf, (CLDoubleBuffer)outBuf, length, sign, dims, null, events);
    }
 }
