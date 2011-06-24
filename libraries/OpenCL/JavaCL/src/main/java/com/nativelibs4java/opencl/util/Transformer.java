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
 * Generic homogeneous transformer class
 * @author ochafik
 * @param <A> primitive array class that represents the data consumed and produced by this transformer
 */
public interface Transformer<T, A> {
	CLContext getContext();
    A transform(CLQueue queue, A input, boolean inverse);
    //B transform(CLQueue queue, B input, boolean inverse);
    Pointer<T> transform(CLQueue queue, Pointer<T> input, boolean inverse);
    CLEvent transform(CLQueue queue, CLBuffer<T> input, CLBuffer<T> output, boolean inverse, CLEvent... eventsToWaitFor) throws CLException;
    long computeOutputSize(long inputSize);
    
    public abstract class AbstractTransformer<T, A> implements Transformer<T, A> {
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

        
        public A transform(CLQueue queue, A input, boolean inverse) {
        		return (A)transform(queue, (Pointer<T>)pointerToArray(input), inverse).getArray();
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