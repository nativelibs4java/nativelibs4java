/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.util;

import org.bridj.Pointer;
import static org.bridj.Pointer.*;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.nativelibs4java.opencl.CLBuildException;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.JavaCL;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.CLMem.Usage;

/**
 * Parallel Random numbers generator (<a href="http://en.wikipedia.org/wiki/Xorshift">Xorshift</a> adapted to work in parallel).<br>
 * This class was designed as a drop-in replacement for java.util.Random (albeit with a more limited API) :
 * <pre>{@code
 * ParallelRandom r = new ParallelRandom();
 * r.setPreload(true); // faster
 * while (true) {
 *     System.out.println(r.nextDouble());
 * }
 * }</pre>
 * <br>
 * It is also possible to get entire batches of random integers with {@link ParallelRandom#next()} or {@link ParallelRandom#next(Pointer)}.<br>
 * The preload feature precomputes a new batch in background as soon as one starts to consume numbers from the current batch.
 * @author ochafik
 */
public class ParallelRandom {

    protected final XORShiftRandom randomProgram;
    //private final IntBuffer outputBuffer;
    //private IntBuffer mappedOutputBuffer;
    protected final CLQueue queue;
    protected final CLContext context;
    protected final int parallelSize;
    protected final int[] globalWorkSizes;
	
	protected int consumedInts = 0;

	boolean preload;
	CLEvent preloadEvent;
	protected CLBuffer<Integer> seeds, output;
	Pointer<Integer> lastData;
	boolean isDataFresh;
	
	public ParallelRandom() throws IOException {
		this(JavaCL.createBestContext().createDefaultQueue(), 32 * 1024, System.currentTimeMillis());
	}
    public ParallelRandom(CLQueue queue, int parallelSize, final long seed) throws IOException {
        try {
            this.queue = queue;
            this.context = queue.getContext();
            randomProgram = new XORShiftRandom(context);
            this.parallelSize = parallelSize;

            int seedsNeededByWorkItem = 4;
            //int generatedNumbersByWorkItemIteration = 1;
            int maxUnits = queue.getDevice().getMaxComputeUnits();
            int unitsFactor = maxUnits < 10 ? 1 : 16;
            int scheduledWorkItems = maxUnits * unitsFactor;
            //int countByWorkItem = parallelSize / scheduledWorkItems;
            if (scheduledWorkItems > parallelSize / seedsNeededByWorkItem) {
                scheduledWorkItems = parallelSize / seedsNeededByWorkItem;
                scheduledWorkItems += parallelSize % seedsNeededByWorkItem;
            }
            //int iterationsByWorkItem = parallelCount / (generatedNumbersByWorkItemIteration * scheduledWorkItems);
            globalWorkSizes = new int[] { scheduledWorkItems };

            //int lws = 1;//(int)queue.getDevice().getMaxWorkGroupSize();
            //if (lws > 32)
            //    lws = 32;
            //localWorkSizes = new int[] { lws };

            randomProgram.getProgram().defineMacro("NUMBERS_COUNT", parallelSize);
            randomProgram.getProgram().defineMacro("WORK_ITEMS_COUNT", scheduledWorkItems);

            final int nSeeds = seedsNeededByWorkItem * parallelSize;
            final Pointer<Integer> seedsBuf = allocateInts(nSeeds).order(context.getKernelsDefaultByteOrder());
            initSeeds(seedsBuf, seed);
            //println(seedsBuf);
            this.seeds = context.createBuffer(Usage.InputOutput, seedsBuf, true);
            //this.lastOutputData = NIOUtils.directInts(parallelSize, context.getKernelsDefaultByteOrder());
            this.output = context.createBuffer(Usage.Output, Integer.class, parallelSize);
        } catch (InterruptedException ex) {
            Logger.getLogger(ParallelRandom.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException("Failed to initialized parallel random", ex);
        }
    }
	
	static final int floatMask = 0x00ffffff;
	static final double floatDivid = (1 << 24);
	//static final int mask = (1 << 30) - 1;
	//static final double divid = (1 << 30);

	public int nextInt() {
		waitForData(1);
		return lastData.get(consumedInts++);		
	}
	
	/**
	 * If true, a new batch of parallel random numbers is automatically precomputed in background as soon as one starts to consume numbers from the current batch (this gives faster random numbers, at the risk of computing more values than needed)
	 */
	public synchronized boolean isPreload() {
		return preload;
	}
	/**
	 * If true, a new batch of parallel random numbers is automatically precomputed in background as soon as one starts to consume numbers from the current batch (this gives faster random numbers, at the risk of computing more values than needed)
	 */
	public synchronized void setPreload(boolean preload) throws CLBuildException {
		this.preload = preload;
		if (preload && preloadEvent == null) {
			if (lastData == null) {
				preloadEvent = randomProgram.gen_numbers(queue, seeds, output, globalWorkSizes, null);
			} else if (consumedInts > 0) {
				preload();
			}
		}
	}
	private synchronized CLEvent preload() throws CLBuildException {
		return preloadEvent = randomProgram.gen_numbers(queue, seeds, output, globalWorkSizes, null, preloadEvent);
	}
	private synchronized void waitForData(int n) {
		try {
			if (lastData == null) {
				//lastOutputData = NIOUtils.directInts(parallelSize, context.getKernelsDefaultByteOrder());
				if (preloadEvent == null)
					preloadEvent = randomProgram.gen_numbers(queue, seeds, output, globalWorkSizes, null);
					
				readLastOutputData();
			}
			if (consumedInts > parallelSize - n) {
				preload().waitFor();
				consumedInts = 0;
				readLastOutputData();
			}
			if (preload && preloadEvent == null)
				preload();
		} catch (CLBuildException ex) {
			throw new RuntimeException(ex);
		}
	}
	private synchronized void readLastOutputData() {
		if (lastData == null)
			lastData = output.read(queue, preloadEvent);
		else
			output.read(queue, lastData, true, preloadEvent);
		preloadEvent = null;
	}
	public long nextLong() {
        return (((long)nextInt()) << 32) | nextInt();
    }
	
	private static final int intSignMask = 1 << 31;
	public int nextInt(int n) {
        if (n <= 0)
            throw new IllegalArgumentException("n must be positive");

        if ((n & -n) == n)  // i.e., n is a power of 2
            return (int)((n * (long)(nextInt() & intSignMask)) >> 31);

        int bits, val;
        do {
            bits = nextInt() & intSignMask;
            val = bits % n;
        } while (bits - val + (n-1) < 0);
        return val;
    }

	public float nextFloat() {
		return (float)((nextInt() & floatMask) / floatDivid);
	}
	
	private static final int doubleMask = (1 << 27) - 1;
	private static final double doubleDivid = 1L << 53;
	
	public double nextDouble() {
		return (((long)(nextInt() & doubleMask) << 27) | (nextInt() & doubleMask)) / doubleDivid;
	}

	public CLBuffer<Integer> getSeeds() {
		return seeds;
	}
	public CLQueue getQueue() {
		return queue;
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
                    output, globalWorkSizes, null);
        } catch (CLBuildException ex) {
            Logger.getLogger(ParallelRandom.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException("Failed to compile the random number generation routine", ex);
        }
    }

    /**
     * Copies the next @see ParallelRandom#getParallelSize() random integers in the provided output buffer
     * @param output
     */
    public synchronized void next(Pointer<Integer> output) {
        CLEvent evt = doNext();
        this.output.read(queue, output, true, evt);
    }

    
    /**
     * Returns a direct NIO buffer containing the next @see ParallelRandom#getParallelSize() random integers.<br>
     * This buffer is read only and will only be valid until any of the "next" method is called again.
     * @param output buffer of capacity @see ParallelRandom#getParallelSize()
     */
    public synchronized Pointer<Integer> next() {
        CLEvent evt = doNext();
        //queue.finish(); evt = null;
        //return outputBuffer;
        //return (mappedOutputBuffer = output.map(queue, MapFlags.Read, evt)).asReadOnlyBuffer();
        return output.read(queue, evt);
    }

    private void initSeeds(final Pointer<Integer> seedsBuf, final long seed) throws InterruptedException {
        final long nSeeds = seedsBuf.getValidElements();

        long start = System.nanoTime();

        // TODO benchmark threshold :
        boolean parallelize = nSeeds > 10000;
        //parallelize = false;
        if (parallelize) {
            Random random = new Random(seed);
            for (long i = nSeeds; i-- != 0;)
                seedsBuf.set(i, random.nextInt());
        } else {
            // Parallelize seeds initialization
            final int nThreads = Runtime.getRuntime().availableProcessors();// * 2;
            ExecutorService service = Executors.newFixedThreadPool(nThreads);
            for (int i = 0; i < nThreads; i++) {
                final int iThread = i;
                service.execute(new Runnable() {

                    public void run() {
                        long n = nSeeds / nThreads;
                        long offset = n * iThread;
                        Random random = new Random(seed + iThread);// * System.currentTimeMillis());
                        if (iThread == nThreads - 1)
                            n += nSeeds - n * nThreads;
                        
                        for (long i = n; i-- != 0;)
                            seedsBuf.set(offset++, random.nextInt());
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
