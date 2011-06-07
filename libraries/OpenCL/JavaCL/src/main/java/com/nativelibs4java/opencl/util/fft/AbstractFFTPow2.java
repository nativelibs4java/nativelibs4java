package com.nativelibs4java.opencl.util.fft;

import com.nativelibs4java.opencl.*;
import com.nativelibs4java.opencl.util.Transformer.AbstractTransformer;
import com.nativelibs4java.util.NIOUtils;
import java.nio.Buffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

// TODO implement something like http://locklessinc.com/articles/non_power_of_2_fft/
public abstract class AbstractFFTPow2<T, B extends Buffer, A> extends AbstractTransformer<T, B, A> {

    AbstractFFTPow2(CLContext context, Class<T> primitiveClass) {
        super(context, primitiveClass);
    }

    private Map<Integer, CLIntBuffer> cachedOffsetsBufs = new HashMap<Integer, CLIntBuffer>();
    protected synchronized CLIntBuffer getOffsetsBuf(int length) {
        CLIntBuffer offsetsBuf = cachedOffsetsBufs.get(length);
        if (offsetsBuf == null) {
            int[] offsets = new int[length];
            fft_compute_offsetsX(offsets, length, 1, 0, 0);

            offsetsBuf = context.createIntBuffer(CLMem.Usage.InputOutput, IntBuffer.wrap(offsets), true);
            cachedOffsetsBufs.put(length, offsetsBuf);
        }
        return offsetsBuf;
    }
    protected abstract CLEvent cooleyTukeyFFTTwiddleFactors(CLQueue queue, int N, CLBuffer<T> buf, CLEvent... evts) throws CLException ;
    protected abstract CLEvent cooleyTukeyFFTCopy(CLQueue queue, CLBuffer<T> inBuf, CLBuffer<T> outBuf, int length, CLIntBuffer offsetsBuf, boolean inverse, CLEvent... evts) throws CLException;
    protected abstract CLEvent cooleyTukeyFFT(CLQueue queue, CLBuffer<T> Y, int N, CLBuffer<T> twiddleFactors, int inverse, int[] dims, CLEvent... evts) throws CLException;

    Map<Integer, CLBuffer<T>> cachedTwiddleFactors = new HashMap<Integer, CLBuffer<T>>();
    protected synchronized CLBuffer<T> getTwiddleFactorsBuf(CLQueue queue, int N) throws CLException {
        CLBuffer<T> buf = cachedTwiddleFactors.get(N);
        if (buf == null) {
            int halfN = N / 2;
            buf = context.createBuffer(CLMem.Usage.InputOutput, primitiveClass, N);
            CLEvent.waitFor(cooleyTukeyFFTTwiddleFactors(queue, N, buf));
            cachedTwiddleFactors.put(N, buf);
        }
        return buf;
    }
    private void fft_compute_offsetsX(int[] offsetsX, int N, int s, int offsetX, int offsetY) {
		if (N == 1) {
			offsetsX[offsetY] = offsetX;
		} else {
            int halfN = N / 2;
			int twiceS = s * 2;
			fft_compute_offsetsX(offsetsX, halfN, twiceS, offsetX, offsetY);
			fft_compute_offsetsX(offsetsX, halfN, twiceS, offsetX + s, offsetY + halfN);
        }
    }

    @Override
    public CLEvent transform(CLQueue queue, CLBuffer<T> inBuf, CLBuffer<T> outBuf, boolean inverse, CLEvent... eventsToWaitFor) throws CLException {
        int length = (int)inBuf.getElementCount() / 2;
        if (Integer.bitCount(length) != 1)
            throw new UnsupportedOperationException("Only supports FFTs of power-of-two-sized arrays (was given array of length " + length + ")");
        
        CLIntBuffer offsetsBuf = getOffsetsBuf(length);
        CLEvent copyEvt = cooleyTukeyFFTCopy(queue, inBuf, outBuf, length, offsetsBuf, inverse, eventsToWaitFor);
        CLEvent dftEvt = fft(queue, inBuf, length, 1, inverse ? 1 : 0, 1, outBuf, copyEvt);
        return dftEvt;
    }
	private CLEvent fft(CLQueue queue, CLBuffer<T> X, int N, int s, int inverse, int blocks, CLBuffer<T> Y, CLEvent... eventsToWaitFor) throws CLException {
		if (N == 1) {
            return null;
		} else {
			int halfN = N / 2;
			int twiceS = s * 2;

			CLEvent[] evts;
            if (halfN > 1) {
                evts = new CLEvent[] { fft(queue, X, halfN, twiceS, inverse, blocks * 2, Y, eventsToWaitFor) };
            } else {
                evts = eventsToWaitFor;
            }

			return cooleyTukeyFFT(queue, Y, N, getTwiddleFactorsBuf(queue, N), inverse, new int[] { halfN, blocks }, evts);
		}
	}

 }

