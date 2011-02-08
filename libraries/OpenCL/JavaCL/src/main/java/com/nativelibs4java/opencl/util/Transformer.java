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

/**
 * Generic homogen transformer class
 * @author ochafik
 * @param <B> NIO buffer class that represents the data consumed and produced by this transformer
 * @param <A> primitive array class that represents the data consumed and produced by this transformer
 */
public interface Transformer<B extends Buffer, A> {
	CLContext getContext();
    A transform(CLQueue queue, A input, boolean inverse);
    B transform(CLQueue queue, B input, boolean inverse);
    CLEvent transform(CLQueue queue, CLBuffer<B> input, CLBuffer<B> output, boolean inverse, CLEvent... eventsToWaitFor) throws CLException;
    
    public abstract class AbstractTransformer<B extends Buffer, A> implements Transformer<B, A> {
        protected final Class<B> bufferClass;
        protected final CLContext context;

        public AbstractTransformer(CLContext context, Class<B> bufferClass) {
            this.bufferClass = bufferClass;
            this.context = context;
        }
        
        public CLContext getContext() { return context; }

        public A transform(CLQueue queue, A input, boolean inverse) {
            return (A)NIOUtils.getArray(transform(queue, (B)NIOUtils.wrapArray(input), inverse));
        }
        public B transform(CLQueue queue, B in, boolean inverse) {
            int length = in.capacity() / 2;

            CLBuffer<B> inBuf = context.createBuffer(CLMem.Usage.Input, in, true); // true = copy
            CLBuffer<B> outBuf = context.createBuffer(CLMem.Usage.Output, length * 2, bufferClass);

            CLEvent dftEvt = transform(queue, inBuf, outBuf, inverse);
            return outBuf.read(queue, dftEvt);
        }

    }
}