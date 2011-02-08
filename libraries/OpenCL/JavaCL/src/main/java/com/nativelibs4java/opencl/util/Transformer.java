/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.util;

import com.nativelibs4java.util.NIOUtils;
import java.nio.Buffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

/**
 *
 * @author ochafik
 */
public interface Transformer<B extends Buffer, A> {
    A transform(A input);
    A inversetransform(A input);

    B transform(B input);
    B inversetransform(B input);
    
    public interface DoubleTransformer extends Transformer<DoubleBuffer, double[]> {}
    public interface FloatTransformer extends Transformer<FloatBuffer, float[]> {}

    public abstract class AbstractTransformer<B extends Buffer, A> implements Transformer<B, A> {
        public A transform(A input) {
            return (A)NIOUtils.getArray(transform((B)NIOUtils.wrapArray(input)));
        }
        public A inversetransform(A input) {
            return (A)NIOUtils.getArray(inversetransform((B)NIOUtils.wrapArray(input)));
        }
    }
}