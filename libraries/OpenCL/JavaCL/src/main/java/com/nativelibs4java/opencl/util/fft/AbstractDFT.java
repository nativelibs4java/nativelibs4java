package com.nativelibs4java.opencl.util.fft;

import com.nativelibs4java.opencl.*;
import com.nativelibs4java.opencl.util.Transformer.AbstractTransformer;
import java.io.IOException;
import java.nio.Buffer;

abstract class AbstractDFT<T, A> extends AbstractTransformer<T, A> {

    // package-private constructor 
    AbstractDFT(CLContext context, Class<T> primitiveClass) throws IOException, CLException {
        super(context, primitiveClass);
    }
    protected abstract CLEvent dft(CLQueue queue, CLBuffer<T> inBuf, CLBuffer<T> outBuf, int length, int sign, int[] dims, CLEvent... events) throws CLException;

    @Override
    public CLEvent transform(CLQueue queue, CLBuffer<T> inBuf, CLBuffer<T> outBuf, boolean inverse, CLEvent... eventsToWaitFor) throws CLException {
        int length = (int)inBuf.getElementCount() / 2;
        return dft(queue, inBuf, outBuf, length, inverse ? -1 : 1, new int[]{length}, eventsToWaitFor);
    }
 }