/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.demos.random;

import com.nativelibs4java.opencl.CLBuildException;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLIntBuffer;
import com.nativelibs4java.opencl.CLMem.MapFlags;
import com.nativelibs4java.opencl.CLMem.Usage;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.JavaCL;
import com.nativelibs4java.util.NIOUtils;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ochafik
 */
public class ParallelRandom {

    protected final XORShiftRandom randomProgram;
    protected final CLIntBuffer seeds, output;
    private final IntBuffer outputBuffer;
    private IntBuffer mappedOutputBuffer;
    protected final CLQueue queue;
    protected final int parallelSize;
    protected final int[] globalWorkSizes, localWorkSizes;

    public ParallelRandom(CLQueue queue, int parallelSize, final long seed) throws IOException {
        try {
            this.queue = queue;
            CLContext context = queue.getContext();
            randomProgram = new XORShiftRandom(context);
            this.parallelSize = parallelSize;

            int seedsNeededByWorkItem = 4;
            int generatedNumbersByWorkItemIteration = 1;
            int scheduledWorkItems = queue.getDevice().getMaxComputeUnits();// * 4;
            int countByWorkItem = parallelSize / scheduledWorkItems;
            if (scheduledWorkItems > parallelSize / seedsNeededByWorkItem) {
                scheduledWorkItems = parallelSize / seedsNeededByWorkItem;
                scheduledWorkItems += parallelSize % seedsNeededByWorkItem;
            }
            //int iterationsByWorkItem = parallelCount / (generatedNumbersByWorkItemIteration * scheduledWorkItems);
            globalWorkSizes = new int[] { scheduledWorkItems };

            int lws = 1;//(int)queue.getDevice().getMaxWorkGroupSize();
            if (lws > 32)
                lws = 32;
            localWorkSizes = new int[] { lws };

            randomProgram.getProgram().defineMacro("NUMBERS_COUNT", parallelSize);
            randomProgram.getProgram().defineMacro("WORK_ITEMS_COUNT", scheduledWorkItems);

            final int nSeeds = seedsNeededByWorkItem * parallelSize;
            final IntBuffer seedsBuf = NIOUtils.directInts(nSeeds, context.getKernelsDefaultByteOrder());
            initSeeds(seedsBuf, seed);
            //println(seedsBuf);
            this.seeds = context.createIntBuffer(Usage.InputOutput, seedsBuf, true);
            this.outputBuffer = NIOUtils.directInts(parallelSize, context.getKernelsDefaultByteOrder());
            this.output = context.createIntBuffer(Usage.Output, outputBuffer, false); // no copy of outputBuffer
        } catch (InterruptedException ex) {
            Logger.getLogger(ParallelRandom.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException("Failed to initialized parallel random", ex);
        }
    }

    /**
     * Number of random numbers generated at each call of @see ParallelRandom#next() or @see ParallelRandom#next(IntBuffer)<br>
     * The numbers might not all be generated exactly in parallel, the level of parallelism is implementation-dependent.
     * @return size of each buffer returned by @see ParallelRandom#next()
     */
    public int getParallelSize() {
        return parallelSize;
    }
    
    public synchronized CLEvent doNext() {
        try {
            //if (mappedOutputBuffer != null) {
            //    //output.unmap(queue, mappedOutputBuffer);
            //    mappedOutputBuffer = null;
            //}
            return randomProgram.gen_numbers(queue, seeds, //parallelSize,
                    output, globalWorkSizes, localWorkSizes);
        } catch (CLBuildException ex) {
            Logger.getLogger(ParallelRandom.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException("Failed to compile the random number generation routine", ex);
        }
    }

    /**
     * Copies the next @see ParallelRandom#getParallelSize() random integers in the provided output buffer
     * @param output
     */
    public synchronized void next(IntBuffer output) {
        CLEvent evt = doNext();
        this.output.read(queue, output, true, evt);
    }

    
    /**
     * Returns a direct NIO buffer containing the next @see ParallelRandom#getParallelSize() random integers.<br>
     * This buffer is read only and will only be valid until any of the "next" method is called again.
     * @param output buffer of capacity @see ParallelRandom#getParallelSize()
     */
    public synchronized IntBuffer next() {
        CLEvent evt = doNext();
        //queue.finish(); evt = null;
        //return outputBuffer;
        //return (mappedOutputBuffer = output.map(queue, MapFlags.Read, evt)).asReadOnlyBuffer();
        return output.read(queue, evt);
    }

    private void initSeeds(final IntBuffer seedsBuf, final long seed) throws InterruptedException {
        final int nSeeds = seedsBuf.capacity();

        long start = System.nanoTime();

        // TODO benchmark threshold :
        boolean parallelize = nSeeds < 10000;
        parallelize = false;
        if (parallelize) {
            Random random = new Random(seed);
            for (int i = nSeeds; i-- != 0;)
                seedsBuf.put(i, random.nextInt());
        } else {
            // Parallelize seeds initialization
            final int nThreads = Runtime.getRuntime().availableProcessors() * 2;
            ExecutorService service = Executors.newFixedThreadPool(nThreads);
            for (int i = 0; i < nThreads; i++) {
                final int iThread = i;
                service.execute(new Runnable() {

                    public void run() {
                        int n = nSeeds / nThreads;
                        int offset = n * iThread;
                        Random random = new Random(seed + iThread * System.currentTimeMillis());
                        if (iThread == nThreads - 1)
                            n += nSeeds - n * nThreads;
                        
                        for (int i = n; i-- != 0;)
                            seedsBuf.put(offset + i, random.nextInt());
                    }
                });
            }
            service.shutdown();
            service.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        }

        long time = System.nanoTime() - start;
        Logger.getLogger(ParallelRandom.class.getName()).log(Level.INFO, "Initialization of " + nSeeds + " seeds took " + (time/1000000) + " ms (" + (time / (double)nSeeds) + " ns per seed)");
    }
}
