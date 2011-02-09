package tutorial;

import com.nativelibs4java.opencl.*;
import com.nativelibs4java.opencl.CLPlatform.DeviceFeature;
import com.nativelibs4java.util.*;
import java.io.IOException;
import java.nio.DoubleBuffer;

public class DFT {

    final CLQueue queue;
    final CLContext context;
    final CLProgram program;
    final CLKernel kernel;

    public DFT(CLQueue queue) throws IOException, CLBuildException {
        this.queue = queue;
        this.context = queue.getContext();

        String source = IOUtils.readText(DFT.class.getResource("DiscreteFourierTransformProgram.cl"));
        program = context.createProgram(source);
        kernel = program.createKernel("dft");
    }

    /**
     * Method that takes complex values in input (sequence of pairs of real and imaginary values) and 
     * returns the Discrete Fourier Transform of these values if forward == true or the inverse
     * transform if forward == false.
     */
    public synchronized DoubleBuffer dft(DoubleBuffer in, boolean forward) {
        assert in.capacity() % 2 == 0;
        int length = in.capacity() / 2;

        // Create an input CLBuffer that will be a copy of the NIO buffer :
        CLDoubleBuffer inBuf = context.createDoubleBuffer(CLMem.Usage.Input, in, true); // true = copy
        
        // Create an output CLBuffer :
        CLDoubleBuffer outBuf = context.createDoubleBuffer(CLMem.Usage.Output, length * 2);

        // Set the args of the kernel :
        kernel.setArgs(inBuf, outBuf, length, forward ? 1 : -1);
        
        // Ask for `length` parallel executions of the kernel in 1 dimension :
        CLEvent dftEvt = kernel.enqueueNDRange(queue, new int[]{ length });

        // Return an NIO buffer read from the output CLBuffer :
        return outBuf.read(queue, dftEvt);
    }

    /// Wrapper method that takes and returns double arrays
    public double[] dft(double[] complexValues, boolean forward) {
        DoubleBuffer outBuffer = dft(DoubleBuffer.wrap(complexValues), forward);
        double[] out = new double[complexValues.length];
        outBuffer.get(out);
        return out;
    }

    public static void main(String[] args) throws IOException, CLBuildException {
    		// Create a context with the best double numbers support possible :
    		// (try using DeviceFeature.GPU, DeviceFeature.CPU...)
        CLContext context = JavaCL.createBestContext(DeviceFeature.DoubleSupport);
        
        // Create a command queue, if possible able to execute multiple jobs in parallel
        // (out-of-order queues will still respect the CLEvent chaining)
        CLQueue queue = context.createDefaultOutOfOrderQueueIfPossible();

        DFT dft = new DFT(queue);
        //DFT2 dft = new DFT2(queue);

        // Create some fake test data :
        double[] in = createTestDoubleData();

        // Transform the data (spatial -> frequency transform) :
        double[] transformed = dft.dft(in, true);
        
        for (int i = 0; i < transformed.length / 2; i++) {
            // Print the transformed complex values (real + i * imaginary)
            System.out.println(transformed[i * 2] + "\t + \ti * " + transformed[i * 2 + 1]);
        }
        
        // Reverse-transform the transformed data (frequency -> spatial transform) :
        double[] backTransformed = dft.dft(transformed, false);

        // Check the transform + inverse transform give the original data back :
        double precision = 1e-5;
        for (int i = 0; i < in.length; i++) {
            if (Math.abs(in[i] - backTransformed[i]) > precision)
                throw new RuntimeException("Different values in back-transformed array than in original array !");
        }
    }

    static double[] createTestDoubleData() {
        int n = 32;
        double[] in = new double[2 * n];

        for (int i = 0; i < n; i++) {
            in[i * 2] = 1 / (double) (i + 1);
            in[i * 2 + 1] = 0;
        }
        return in;
    }
}
