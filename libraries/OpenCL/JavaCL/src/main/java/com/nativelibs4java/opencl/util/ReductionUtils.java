/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.util;

import com.nativelibs4java.opencl.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.nativelibs4java.util.IOUtils;
import com.nativelibs4java.util.Pair;

import org.bridj.Pointer;
import org.bridj.Platform;
import static org.bridj.Pointer.*;

/**
 * Parallel reduction utils (max, min, sum and product computations on OpenCL buffers of any type)
 * @author Olivier
 */
public class ReductionUtils {
	static final int DEFAULT_MAX_REDUCTION_SIZE = 4;
	
    static String source;
    static final String sourcePath = ReductionUtils.class.getPackage().getName().replace('.', '/') + "/" + "Reduction.c";
    static synchronized String getSource() throws IOException {
        InputStream in = Platform.getClassLoader(ReductionUtils.class).getResourceAsStream(sourcePath);
        if (in == null)
            throw new FileNotFoundException(sourcePath);
        return source = IOUtils.readText(in);
    }

    public enum Operation {
        Add,
        Multiply,
        Min,
        Max;
    }
    
    public static Pair<String, Map<String, Object>> getReductionCodeAndMacros(Operation op, OpenCLType valueType, int channels) throws IOException {
        Map<String, Object> macros = new LinkedHashMap<String, Object>();
        String cType = valueType.toCType() + (channels == 1 ? "" : channels);
        macros.put("OPERAND_TYPE", cType);
        String operation, seed;
        switch (op) {
            case Add:
                operation = "_add_";
                seed = "0";
                break;
            case Multiply:
                operation = "_mul_";
                seed = "1";
                break;
            case Min:
                operation = "_min_";
                switch (valueType) {
                    case Int:
                        seed = Integer.MAX_VALUE + "";
                        break;
                    case Long:
                        seed = Long.MAX_VALUE + "LL";
                        break;
                    case Short:
                        seed = Short.MAX_VALUE + "";
                        break;
                    case Float:
                        seed = "MAXFLOAT";
                        break;
                    case Double:
                        seed = "MAXDOUBLE";
                        break;
                    default:
                        throw new IllegalArgumentException("Unhandled seed type: " + valueType);
                }
                
                break;
            case Max:
                operation = "_max_";
                switch (valueType) {
                    case Int:
                        seed = Integer.MIN_VALUE + "";
                        break;
                    case Long:
                        seed = Long.MIN_VALUE + "LL";
                        break;
                    case Short:
                        seed = Short.MIN_VALUE + "";
                        break;
                    case Float:
                        seed = "-MAXFLOAT";
                        break;
                    case Double:
                        seed = "-MAXDOUBLE";
                        break;
                    default:
                        throw new IllegalArgumentException("Unhandled seed type: " + valueType);
                }
                break;
            default:
                throw new IllegalArgumentException("Unhandled operation: " + op);
        }
        macros.put("OPERATION", operation);
        macros.put("SEED", seed);
        return new Pair<String, Map<String, Object>>(getSource(), macros);
    }
    public interface Reductor<B> {
        /** Number of independent channels of the reductor */
        public int getChannels();
        public CLEvent reduce(CLQueue queue, CLBuffer<B> input, long inputLength, CLBuffer<B> output, int maxReductionSize, CLEvent... eventsToWaitFor);
        public Pointer<B> reduce(CLQueue queue, CLBuffer<B> input, long inputLength, int maxReductionSize, CLEvent... eventsToWaitFor);
        public CLEvent reduce(CLQueue queue, CLBuffer<B> input, long inputLength, Pointer<B> output, int maxReductionSize, CLEvent... eventsToWaitFor);
        /** 
         * Return the result of the reduction operation (with one value per channel).
         */
        public Pointer<B> reduce(CLQueue queue, CLBuffer<B> input, CLEvent... eventsToWaitFor);
        
    }
    /*public interface WeightedReductor<B extends Buffer, W extends Buffer> {
        public CLEvent reduce(CLQueue queue, CLBuffer<B> input, CLBuffer<W> weights, long inputLength, B output, int maxReductionSize, CLEvent... eventsToWaitFor);
        public CLEvent reduce(CLQueue queue, CLBuffer<B> input, CLBuffer<W> weights, long inputLength, CLBuffer<B> output, int maxReductionSize, CLEvent... eventsToWaitFor);
    }*/
    static int getNextPowerOfTwo(int i) {
        int shifted = 0;
        boolean lost = false;
        for (;;) {
            int next = i >> 1;
            if (next == 0) {
                if (lost)
                    return 1 << (shifted + 1);
                else
                    return 1 << shifted;
            }
            lost = lost || (next << 1 != i);
            shifted++;
            i = next;
        }
    }

    /**
     * Create a reductor for the provided operation and primitive type (on the provided number of independent channels).<br>
     * Channels are reduced independently, so that with 2 channels the max of elements { (1, 30), (2, 20), (3, 10) } would be (3, 30).
     */
    public static <B> Reductor<B> createReductor(final CLContext context, Operation op, final OpenCLType valueType, final int valueChannels) throws CLBuildException {
        try {


            Pair<String, Map<String, Object>> codeAndMacros = getReductionCodeAndMacros(op, valueType, valueChannels);
            CLProgram program = context.createProgram(codeAndMacros.getFirst());
            program.defineMacros(codeAndMacros.getValue());
            program.build();
            CLKernel[] kernels = program.createKernels();
            if (kernels.length != 1)
                throw new RuntimeException("Expected 1 kernel, found : " + kernels.length);
            final CLKernel kernel = kernels[0];
            return new Reductor<B>() {
                @Override
                public int getChannels() {
                		return valueChannels;
                }
				@SuppressWarnings("unchecked")
                public CLEvent reduce(CLQueue queue, CLBuffer<B> input, long inputLength, Pointer<B> output, int maxReductionSize, CLEvent... eventsToWaitFor) {
                    Pair<CLBuffer<B>, CLEvent[]> outAndEvts = reduceHelper(queue, input, (int)inputLength, maxReductionSize, eventsToWaitFor);
                    return outAndEvts.getFirst().read(queue, 0, valueChannels, output, false, outAndEvts.getSecond());
                }
                @Override
                public Pointer<B> reduce(CLQueue queue, CLBuffer<B> input, long inputLength, int maxReductionSize, CLEvent... eventsToWaitFor) {
                    Pointer<B> output = Pointer.allocateArray((Class)valueType.type, valueChannels).order(context.getByteOrder());
                    CLEvent evt = reduce(queue, input, inputLength, output, maxReductionSize, eventsToWaitFor);
                    //queue.finish();
                    //TODO
                    evt.waitFor();
                    return output;
                }
                @Override
                public Pointer<B> reduce(CLQueue queue, CLBuffer<B> input, CLEvent... eventsToWaitFor) {
                		return reduce(queue, input, input.getElementCount(), DEFAULT_MAX_REDUCTION_SIZE, eventsToWaitFor);
                }
                @Override
                public CLEvent reduce(CLQueue queue, CLBuffer<B> input, long inputLength, CLBuffer<B> output, int maxReductionSize, CLEvent... eventsToWaitFor) {
                    Pair<CLBuffer<B>, CLEvent[]> outAndEvts = reduceHelper(queue, input, (int)inputLength, maxReductionSize, eventsToWaitFor);
                    return outAndEvts.getFirst().copyTo(queue, 0, valueChannels, output, 0, outAndEvts.getSecond());
                }
                @SuppressWarnings("unchecked")
				public Pair<CLBuffer<B>, CLEvent[]> reduceHelper(CLQueue queue, CLBuffer<B> input, int inputLength, int maxReductionSize, CLEvent... eventsToWaitFor) {
					if (inputLength == 1) {
						return new Pair<CLBuffer<B>, CLEvent[]>(input, new CLEvent[0]);
					}
                    if (inputLength == 1) {
						return new Pair<CLBuffer<B>, CLEvent[]>(input, new CLEvent[0]);
					}
                    CLBuffer<?>[] tempBuffers = new CLBuffer<?>[2];
                    int depth = 0;
					CLBuffer<B> currentOutput = null;
					CLEvent[] eventsArr = new CLEvent[1];
					int[] blockCountArr = new int[1];

                    int maxWIS = (int)queue.getDevice().getMaxWorkItemSizes()[0];

                    while (inputLength > 1) {
                        int blocksInCurrentDepth = inputLength / maxReductionSize;
						if (inputLength > blocksInCurrentDepth * maxReductionSize)
							blocksInCurrentDepth++;
                        
						int iOutput = depth & 1;
                        CLBuffer<?> currentInput = depth == 0 ? input : tempBuffers[iOutput ^ 1];
                        currentOutput = (CLBuffer<B>)tempBuffers[iOutput];
                        if (currentOutput == null)
                            currentOutput = (CLBuffer<B>)(tempBuffers[iOutput] = context.createBuffer(CLMem.Usage.InputOutput, valueType.type, blocksInCurrentDepth * valueChannels));
						
                        synchronized (kernel) {
                            kernel.setArgs(currentInput, (long)blocksInCurrentDepth, (long)inputLength, (long)maxReductionSize, currentOutput);
                            int workgroupSize = blocksInCurrentDepth;
                            if (workgroupSize == 1)
                            		workgroupSize = 2;
                            blockCountArr[0] = workgroupSize;
                            eventsArr[0] = kernel.enqueueNDRange(queue, blockCountArr, null, eventsToWaitFor);
                        }
						eventsToWaitFor = eventsArr;
						inputLength = blocksInCurrentDepth;
                        depth++;
                    }
                    return new Pair<CLBuffer<B>, CLEvent[]>(currentOutput, eventsToWaitFor);
                }

            };
        } catch (IOException ex) {
            Logger.getLogger(ReductionUtils.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException("Failed to create a " + op + " reductor for type " + valueType + valueChannels, ex);
        }
    }
}
