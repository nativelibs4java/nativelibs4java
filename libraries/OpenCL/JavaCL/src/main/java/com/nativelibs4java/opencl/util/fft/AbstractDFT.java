package com.nativelibs4java.opencl.util.fft;

import com.nativelibs4java.opencl.*;
import com.nativelibs4java.opencl.util.Transformer.AbstractTransformer;
import java.io.IOException;
import java.nio.Buffer;

public abstract class AbstractDFT<B extends Buffer, A> extends AbstractTransformer<B, A> {

    // package-private constructor 
    AbstractDFT(CLContext context, Class<B> bufferClass) throws IOException, CLException {
        super(context, bufferClass);
    }
    protected abstract CLEvent dft(CLQueue queue, CLBuffer<B> inBuf, CLBuffer<B> outBuf, int length, int sign, int[] dims, CLEvent... events) throws CLException;

    @Override
    public CLEvent transform(CLQueue queue, CLBuffer<B> inBuf, CLBuffer<B> outBuf, boolean inverse, CLEvent... eventsToWaitFor) throws CLException {
        int length = (int)inBuf.getElementCount() / 2;
        return dft(queue, inBuf, outBuf, length, inverse ? -1 : 1, new int[]{length}, eventsToWaitFor);
    }
 }