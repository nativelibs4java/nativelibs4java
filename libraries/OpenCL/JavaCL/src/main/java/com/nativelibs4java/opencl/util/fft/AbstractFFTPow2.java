package com.nativelibs4java.opencl.util.fft;

import com.nativelibs4java.opencl.*;
import com.nativelibs4java.util.NIOUtils;
import java.nio.Buffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

// TODO implement something like http://locklessinc.com/articles/non_power_of_2_fft/
public abstract class AbstractFFTPow2<B extends Buffer> {

    protected final CLQueue queue;
    protected final CLContext context;
    protected final Class<B> bufferClass;

    public AbstractFFTPow2(CLQueue queue, Class<B> bufferClass) {
        this.queue = queue;
        this.context = queue.getContext();
        this.bufferClass = bufferClass;
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
    protected abstract CLEvent cooleyTukeyFFTTwiddleFactors(int N, CLBuffer<B> buf, CLEvent... evts) throws CLBuildException ;
    protected abstract CLEvent cooleyTukeyFFTCopy(CLBuffer<B> inBuf, CLBuffer<B> outBuf, int length, CLIntBuffer offsetsBuf, boolean inverse, CLEvent... evts) throws CLBuildException;
    protected abstract CLEvent cooleyTukeyFFT(CLBuffer<B> Y, int N, CLBuffer<B> twiddleFactors, int inverse, int[] dims, CLEvent... evts) throws CLBuildException;

    Map<Integer, CLBuffer<B>> cachedTwiddleFactors = new HashMap<Integer, CLBuffer<B>>();
    protected synchronized CLBuffer<B> getTwiddleFactorsBuf(int N) throws CLBuildException {
        CLBuffer<B> buf = cachedTwiddleFactors.get(N);
        if (buf == null) {
            int halfN = N / 2;
            buf = context.createBuffer(CLMem.Usage.InputOutput, N, bufferClass);
            CLEvent.waitFor(cooleyTukeyFFTTwiddleFactors(N, buf));
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
    
    public B fft(B in, boolean inverse) throws CLBuildException {
        int length = in.capacity() / 2;

        CLBuffer<B> inBuf = context.createBuffer(CLMem.Usage.Input, in, true); // true = copy
        CLBuffer<B> outBuf = context.createBuffer(CLMem.Usage.InputOutput, length * 2, bufferClass);
        CLEvent dftEvt = fft(inBuf, outBuf, inverse);
        return outBuf.read(queue, dftEvt);
    }
	
    public CLEvent fft(CLBuffer<B> inBuf, CLBuffer<B> outBuf, boolean inverse, CLEvent... eventsToWaitFor) throws CLBuildException {
        int length = (int)inBuf.getElementCount() / 2;
        if (Integer.bitCount(length) != 1)
            throw new UnsupportedOperationException("Only supports FFTs of power-of-two arrays (was given array of length " + length + ")");
        
        CLIntBuffer offsetsBuf = getOffsetsBuf(length);
        CLEvent copyEvt = cooleyTukeyFFTCopy(inBuf, outBuf, length, offsetsBuf, inverse, eventsToWaitFor);
        CLEvent dftEvt = fft(inBuf, length, 1, inverse ? 1 : 0, 1, outBuf, copyEvt);
        return dftEvt;
    }
	private CLEvent fft(CLBuffer<B> X, int N, int s, int inverse, int blocks, CLBuffer<B> Y, CLEvent... eventsToWaitFor) throws CLBuildException {
		if (N == 1) {
            return null;
		} else {
			int halfN = N / 2;
			int twiceS = s * 2;

			CLEvent[] evts;
            if (halfN > 1) {
                evts = new CLEvent[] { fft(X, halfN, twiceS, inverse, blocks * 2, Y, eventsToWaitFor) };
            } else {
                evts = eventsToWaitFor;
            }

			// The following call is type-safe, thanks to the JavaCL Maven generator :
			// (if the OpenCL function signature changes, the generated Java definition will be updated and compilation will fail)
            //int n = totalN / N;
			return cooleyTukeyFFT(Y, N, getTwiddleFactorsBuf(N), inverse, new int[] { halfN, blocks }, evts);
		}
	}
    protected Object fftArray(Object complexValues, boolean inverse) throws CLBuildException {
        return NIOUtils.getArray(fft((B)NIOUtils.wrapArray(complexValues), inverse));
    }

 }

