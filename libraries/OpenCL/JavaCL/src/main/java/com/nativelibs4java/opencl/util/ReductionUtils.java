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
import com.nativelibs4java.util.NIOUtils;
import com.ochafik.util.listenable.Pair;

/**
 *
 * @author Olivier
 */
public class ReductionUtils {
    static String source;
    static final String sourcePath = ReductionUtils.class.getPackage().getName().replace('.', '/') + "/" + "Reduction.c";
    static synchronized String getSource() throws IOException {
        InputStream in = ReductionUtils.class.getClassLoader().getResourceAsStream(sourcePath);
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
    public interface Reductor<B extends Buffer> {
        public CLEvent reduce(CLQueue queue, CLBuffer<B> input, long inputLength, B output, int maxReductionSize, CLEvent... eventsToWaitFor);
        public B reduce(CLQueue queue, CLBuffer<B> input, long inputLength, int maxReductionSize, CLEvent... eventsToWaitFor);
        public CLEvent reduce(CLQueue queue, CLBuffer<B> input, long inputLength, CLBuffer<B> output, int maxReductionSize, CLEvent... eventsToWaitFor);
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

    public static <B extends Buffer> Reductor<B> createReductor(final CLContext context, Operation op, OpenCLType valueType, final int valueChannels) throws CLBuildException {
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
				@SuppressWarnings("unchecked")
				@Override
                public CLEvent reduce(CLQueue queue, CLBuffer<B> input, long inputLength, B output, int maxReductionSize, CLEvent... eventsToWaitFor) {
                    Pair<CLBuffer<B>, CLEvent[]> outAndEvts = reduceHelper(queue, input, (int)inputLength, (Class<B>)output.getClass(), maxReductionSize, eventsToWaitFor);
                    return outAndEvts.getFirst().read(queue, 0, valueChannels, output, false, outAndEvts.getSecond());
                }
                @Override
                public B reduce(CLQueue queue, CLBuffer<B> input, long inputLength, int maxReductionSize, CLEvent... eventsToWaitFor) {
                    B output = NIOUtils.directBuffer((int)inputLength, context.getByteOrder(), input.typedBufferClass());
                    CLEvent evt = reduce(queue, input, inputLength, output, maxReductionSize, eventsToWaitFor);
                    //queue.finish();
                    //TODO
                    evt.waitFor();
                    return output;
                }
                @Override
                public CLEvent reduce(CLQueue queue, CLBuffer<B> input, long inputLength, CLBuffer<B> output, int maxReductionSize, CLEvent... eventsToWaitFor) {
                    Pair<CLBuffer<B>, CLEvent[]> outAndEvts = reduceHelper(queue, input, (int)inputLength, output.typedBufferClass(), maxReductionSize, eventsToWaitFor);
                    return outAndEvts.getFirst().copyTo(queue, 0, valueChannels, output, 0, outAndEvts.getSecond());
                }
                @SuppressWarnings("unchecked")
				public Pair<CLBuffer<B>, CLEvent[]> reduceHelper(CLQueue queue, CLBuffer<B> input, int inputLength, Class<B> outputClass, int maxReductionSize, CLEvent... eventsToWaitFor) {
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
                        CLBuffer<? extends Buffer> currentInput = depth == 0 ? input : tempBuffers[iOutput ^ 1];
                        currentOutput = (CLBuffer<B>)tempBuffers[iOutput];
                        if (currentOutput == null)
                            currentOutput = (CLBuffer<B>)(tempBuffers[iOutput] = context.createBuffer(CLMem.Usage.InputOutput, blocksInCurrentDepth * valueChannels, outputClass));
						
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
