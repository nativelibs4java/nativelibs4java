/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl;


import com.nativelibs4java.util.IOUtils;
import com.ochafik.util.listenable.Pair;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        //MinMax,
        //Average,
        //AverageMinMax
    }
    public enum Type {
        Int, Long, Short, Byte, Double, Float, Half;

        public String toCType() {
            if (this == Byte)
                return "char";
            return name().toLowerCase();
        }
        public static Type fromClass(Class<? extends Number> valueType) {
            if (!valueType.isPrimitive())
                throw new IllegalArgumentException("Reduction value type is not a primitive: '" + valueType.getName() + "' !");

            if (valueType == Integer.TYPE)
                return Int;
            if (valueType == java.lang.Long.TYPE)
                return Long;
            if (valueType == java.lang.Short.TYPE)
                return Short;
            if (valueType == java.lang.Double.TYPE)
                return Double;
            if (valueType == java.lang.Float.TYPE)
                return Float;
            if (valueType == java.lang.Byte.TYPE)
                return Byte;
            throw new IllegalArgumentException("Primitive type not handled: '" + valueType.getName() + "' !");
        }
    }
    public static Pair<String, Map<String, String>> getReductionCodeAndMacros(Operation op, Type valueType, int channels) throws IOException {
        Map<String, String> macros = new LinkedHashMap<String, String>();
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
                        seed = Short.MAX_VALUE + "LL";
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
                        seed = Short.MIN_VALUE + "LL";
                        break;
                    case Float:
                        seed = "MINFLOAT";
                        break;
                    case Double:
                        seed = "MINDOUBLE";
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
        return new Pair<String, Map<String, String>>(getSource(), macros);
    }
    public interface Reductor<B extends Buffer> {
        public CLEvent reduce(CLQueue queue, CLBuffer<B> input, int inputStart, int inputLength, B output, int maxReductionSize, CLEvent... eventsToWaitFor);
    }
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
    public static <B extends Buffer> Reductor<B> createReductor(final CLContext context, Operation op, Type valueType, final int valueChannels) throws CLBuildException {
        try {
            Pair<String, Map<String, String>> codeAndMacros = getReductionCodeAndMacros(op, valueType, valueChannels);
            CLProgram program = context.createProgram(codeAndMacros.getFirst());
            program.defineMacros(codeAndMacros.getValue());
            program.build();
            CLKernel[] kernels = program.createKernels();
            if (kernels.length != 1)
                throw new RuntimeException("Expected 1 kernel, found : " + kernels.length);
            final CLKernel kernel = kernels[0];
            return new Reductor<B>() {
                @Override
                public CLEvent reduce(CLQueue queue, CLBuffer<B> input, int inputStart, int inputLength, B output, int maxReductionSize, CLEvent... eventsToWaitFor) {
                    /*if (inputLength <= maxReductionSize) {
                        CLBuffer out = context.createByteBuffer(CLMem.Usage.InputOutput, NIOUtils.getSizeInBytes(output));
                        kernel.setArgs(input, inputStart, inputLength, out);
                        CLEvent evt = kernel.enqueueNDRange(queue, new int[] { inputLength }, new int[] { 1 }, eventsToWaitFor);
                        return out.read(queue, output, false);
                    }*/

                    CLByteBuffer[] tempBuffers = new CLByteBuffer[2];
                    int depth = 0;
                    CLBuffer currentOutput = null;
                    while (inputLength > 1) {
                        int mod = inputLength % maxReductionSize;
                        int nInCurrentDepth = inputLength / maxReductionSize + (mod == 0 ? 0 : 1);
                        //CLEvent[] events = new CLEvent[nInCurrentDepth];
                        int iOutput = depth % tempBuffers.length;
                        CLBuffer currentInput = depth == 0 ? input : tempBuffers[((depth - 1 + tempBuffers.length) % tempBuffers.length)];
                        currentOutput = tempBuffers[iOutput];
                        if (currentOutput == null) {
                            int tempBufferSize = maxReductionSize * valueChannels * input.getElementSize();
                            currentOutput = tempBuffers[iOutput] = context.createByteBuffer(CLMem.Usage.InputOutput, tempBufferSize);
                        }
                        kernel.setArgs(currentInput, inputLength, inputLength, currentOutput);
                        int globalSize = inputLength;//getNextPowerOfTwo(inputLength);
                        eventsToWaitFor = new CLEvent[] {
                            kernel.enqueueNDRange(queue, new int[] { globalSize }, new int[] { 1 }, eventsToWaitFor)
                        };
                        /*
                        for (int iRed = 0; iRed < nInCurrentDepth; iRed++) {
                            int start = iRed * maxReductionSize, len = (iRed == nInCurrentDepth - 1 && mod != 0) ? mod : maxReductionSize;

                            kernel.setArgs(currentInput, start, start + len, currentOutput);
                            events[iRed] = kernel.enqueueTask(queue, eventsToWaitFor);//NDRange(queue, new int[] { inputLength }, new int[] { 1 }, eventsToWaitFor);
                        }
                        eventsToWaitFor = events;*/
                        inputLength = nInCurrentDepth;
                        depth++;
                    }
                    return currentOutput.read(queue, output, false, eventsToWaitFor);
                }

            };
        } catch (IOException ex) {
            Logger.getLogger(ReductionUtils.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException("Failed to create a " + op + " reductor for type " + valueType + valueChannels, ex);
        }
    }
}
