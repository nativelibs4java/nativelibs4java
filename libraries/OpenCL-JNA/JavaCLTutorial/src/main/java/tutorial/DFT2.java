package tutorial;

import com.nativelibs4java.opencl.*;
import java.io.IOException;
import java.nio.DoubleBuffer;

public class DFT2 {

    final CLQueue queue;
    final CLContext context;
    final DiscreteFourierTransformProgram program;

    public DFT2(CLQueue queue) throws IOException, CLBuildException {
        this.queue = queue;
        this.context = queue.getContext();
        this.program = new DiscreteFourierTransformProgram(context);
    }

    public synchronized DoubleBuffer dft(DoubleBuffer in, boolean forward) throws CLBuildException {
        assert in.capacity() % 2 == 0;
        int length = in.capacity() / 2;

        CLDoubleBuffer inBuf = context.createDoubleBuffer(CLMem.Usage.Input, in, true); // true = copy
        CLDoubleBuffer outBuf = context.createDoubleBuffer(CLMem.Usage.Output, length * 2);

        // The following call is type-safe, thanks to the JavaCL Maven generator :
        // (if the OpenCL function signature changes, the generated Java definition will be updated and compilation will fail) 
        CLEvent dftEvt = program.dft(queue, inBuf, outBuf, length, forward ? 1 : -1, new int[]{length}, null);
        return outBuf.read(queue, dftEvt);
    }
    
	public double[] dft(double[] complexValues, boolean forward) throws CLBuildException {
        DoubleBuffer outBuffer = dft(DoubleBuffer.wrap(complexValues), forward);
        double[] out = new double[complexValues.length];
        outBuffer.get(out);
        return out;
    }
 }
