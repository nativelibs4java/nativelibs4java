/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.util;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLBuildException;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLException;
import com.nativelibs4java.opencl.CLMem;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.util.NIOUtils;
import java.nio.Buffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

import org.bridj.*;
import static org.bridj.Pointer.*;

/**
 * Generic homogen transformer class
 * @author ochafik
 * @param <B> NIO buffer class that represents the data consumed and produced by this transformer
 * @param <A> primitive array class that represents the data consumed and produced by this transformer
 */
public interface Transformer<T, B extends Buffer, A> {
	CLContext getContext();
    A transform(CLQueue queue, A input, boolean inverse);
    B transform(CLQueue queue, B input, boolean inverse);
    Pointer<T> transform(CLQueue queue, Pointer<T> input, boolean inverse);
    CLEvent transform(CLQueue queue, CLBuffer<T> input, CLBuffer<T> output, boolean inverse, CLEvent... eventsToWaitFor) throws CLException;
    long computeOutputSize(long inputSize);
    
    public abstract class AbstractTransformer<T, B extends Buffer, A> implements Transformer<T, B, A> {
        protected final Class<T> primitiveClass;
        protected final CLContext context;

        public AbstractTransformer(CLContext context, Class<T> primitiveClass) {
            this.primitiveClass = primitiveClass;
            this.context = context;
        }
        
        public CLContext getContext() { return context; }

        public long computeOutputSize(long inputSize) {
            return inputSize;
        }

        
        public B transform(CLQueue queue, B in, boolean inverse) {
        		throw new UnsupportedOperationException("use the Pointer<T> variant instead");
        }
        public A transform(CLQueue queue, A input, boolean inverse) {
        		return (A)transform(queue, (Pointer<T>)pointerToArray(input), inverse).getArray();
            //return (A)NIOUtils.getArray(transform(queue, (B)NIOUtils.wrapArray(input), inverse));
        }
        public Pointer<T> transform(CLQueue queue, Pointer<T> in, boolean inverse) {
            long inputSize = (int)in.getValidElements();
            long length = inputSize / 2;

            CLBuffer<T> inBuf = context.createBuffer(CLMem.Usage.Input, in, true); // true = copy
            CLBuffer<T> outBuf = context.createBuffer(CLMem.Usage.Output, primitiveClass, computeOutputSize(inputSize));

            CLEvent dftEvt = transform(queue, inBuf, outBuf, inverse);
            inBuf.release();
            
            Pointer<T> out = outBuf.read(queue, dftEvt);
            outBuf.release();
            return out;
        }

    }
}