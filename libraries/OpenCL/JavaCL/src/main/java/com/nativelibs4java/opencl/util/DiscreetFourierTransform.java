package com.nativelibs4java.opencl.util;

import com.nativelibs4java.opencl.*;
import com.nativelibs4java.opencl.*;
import com.nativelibs4java.util.*;
import java.io.IOException;
import java.nio.DoubleBuffer;

public class DiscreetFourierTransform {
    final DiscreetFourierTransformKernel dft;
    final CLQueue queue;
    final CLContext context;
	public DiscreetFourierTransform(CLQueue queue) throws IOException, CLBuildException {
        this.queue = queue;
        this.context = queue.getContext();
        dft = new DiscreetFourierTransformKernel(context);
	}
    public double[] transform(double[] complexValues) throws CLBuildException {
        return dft(complexValues, true);
    }
    public double[] reverseTransform(double[] complexValues) throws CLBuildException {
        return dft(complexValues, false);
    }

	protected double[] dft(double[] complexValues, boolean forward) throws CLBuildException {
        DoubleBuffer outBuffer = dft(DoubleBuffer.wrap(complexValues), forward);

		double[] out = new double[complexValues.length];
        outBuffer.get(out);
        return out;
	}

    public DoubleBuffer transform(DoubleBuffer complexValues) throws CLBuildException {
        return dft(complexValues, true);
    }
    public DoubleBuffer reverseTransform(DoubleBuffer complexValues) throws CLBuildException {
        return dft(complexValues, false);
    }

	protected DoubleBuffer dft(DoubleBuffer in, boolean forward) throws CLBuildException {
        assert in.capacity() % 2 == 0;
		int length = in.capacity() / 2;

        CLDoubleBuffer
			inBuf = context.createDoubleBuffer(CLMem.Usage.Input, length * 2),
			outBuf = context.createDoubleBuffer(CLMem.Usage.Output, length * 2);

        // Write the input data to the OpenCL input buffers (false = non-blocking write).
        CLEvent inEvt = inBuf.write(queue, in, false);
        CLEvent dftEvt = dft.dft(queue, inBuf, outBuf, length, forward ? 1 : -1, new int[] { length }, null, inEvt);

        return outBuf.read(queue, dftEvt);
	}
}
