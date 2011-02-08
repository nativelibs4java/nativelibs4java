package com.nativelibs4java.opencl.util.fft;

import com.nativelibs4java.opencl.*;
import com.nativelibs4java.opencl.util.Transformer.DoubleTransformer;
import java.io.IOException;
import java.nio.DoubleBuffer;

public class DoubleDFT extends AbstractDFT<DoubleBuffer, double[]> implements DoubleTransformer {

    final DoubleDFTProgram program;

    public DoubleDFT(CLQueue queue) throws IOException, CLBuildException {
        super(queue, DoubleBuffer.class);
        this.program = new DoubleDFTProgram(context);
    }

    @Override
    protected CLEvent dft(CLBuffer<DoubleBuffer> inBuf, CLBuffer<DoubleBuffer> outBuf, int length, int sign, int[] dims, CLEvent... events) throws CLBuildException {
        return program.dft(queue, (CLDoubleBuffer)inBuf, (CLDoubleBuffer)outBuf, length, sign, dims, null, events);
    }
 }
