/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nativelibs4java.opencl.blas;

import com.nativelibs4java.opencl.CLBuildException;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.util.Fun1;
import com.nativelibs4java.opencl.util.Fun2;
import com.nativelibs4java.opencl.util.Primitive;
import com.nativelibs4java.opencl.util.ReductionUtils.Reductor;

/**
 *
 * @author ochafik
 */
public class CLMatrixUtils {
    
    static CLEvent[] join(CLEvent[]... evts) {
        int n = 0;
        for (CLEvent[] e : evts)
            n += e.length;
        CLEvent[] out = new CLEvent[n];
        n = 0;
        for (CLEvent[] e : evts)
            System.arraycopy(e, 0, out, n, e.length);
        
        return out;
    }
    
    
    public static <T> void matrixMultiply(
            final CLMatrix2D<T> a, 
            final CLMatrix2D<T> b, 
            final CLMatrix2D<T> out) 
            throws CLBuildException
    {
        final CLKernels kernels = a.getKernels();
        final Primitive primitive = a.getPrimitive();
        a.getEvents().performRead(new CLEvents.Action() {
            public CLEvent perform(final CLEvent[] aevents) {
                return b.getEvents().performRead(new CLEvents.Action() {
                    public CLEvent perform(final CLEvent[] bevents) {
                        return out.getEvents().performWrite(new CLEvents.Action() {
                            public CLEvent perform(final CLEvent[] cevents) {
                                CLEvent evt = kernels.matrixMultiply(
                                    primitive, 
                                    a.getBuffer(), (int)a.getRowCount(), (int)a.getColumnCount(), 
                                    b.getBuffer(), (int)b.getRowCount(), (int)b.getColumnCount(), 
                                    out.getBuffer(), 
                                    join(aevents, bevents, cevents)
                                );
                                return evt;
                            }
                        });
                    }
                });
            }
        });
    }
    
    static final int MAX_REDUCTION_SIZE = 32;
    
    public static <T> void reduce(
            final CLMatrix2D<T> in,
            final CLMatrix2D<T> out,
            final Reductor<T> reductor
    ) {
        in.getEvents().performRead(new CLEvents.Action() {
            public CLEvent perform(final CLEvent[] ievents) {
                return out.getEvents().performWrite(new CLEvents.Action() {
                    public CLEvent perform(CLEvent[] oevents) {
                        return reductor.reduce(in.getQueue(), in.getBuffer(), in.getBuffer().getElementCount(), out.getBuffer(), MAX_REDUCTION_SIZE, join(ievents, oevents));
                    }
                });
            }
        });
    }
    public static <T> void matrixTranspose(
            final CLMatrix2D<T> a,
            final CLMatrix2D<T> out) 
            throws CLBuildException
    {
        final Primitive primitive = a.getPrimitive();
        final CLKernels kernels = a.getKernels();
        a.getEvents().performRead(new CLEvents.Action() {
            public CLEvent perform(final CLEvent[] aevents) {
                return out.getEvents().performWrite(new CLEvents.Action() {
                    public CLEvent perform(final CLEvent[] cevents) {
                        CLEvent evt = kernels.matrixTranspose(
                            primitive,
                            a.getBuffer(), (int)a.getRowCount(), (int)a.getColumnCount(), 
                            out.getBuffer(), 
                            join(aevents, cevents)
                        );
                        return evt;
                    }
                });
            }
        });
    }
    
    public static <T> CLMatrix2D<T> clone(final CLMatrix2D<T> matrix) {
        final CLMatrix2D<T> out = matrix.blankClone();
        matrix.getEvents().performRead(new CLEvents.Action() {
            public CLEvent perform(final CLEvent[] aevents) {
                return out.getEvents().performWrite(new CLEvents.Action() {
                    public CLEvent perform(CLEvent[] bevents) {
                        return matrix.getBuffer().copyTo(matrix.getQueue(), out.getBuffer(), CLMatrixUtils.join(aevents, bevents));
                    }
                });
            }
        }); 
        return out;
    }
    
    
    public static <T> CLMatrix2D<T> createMatrix(long rows, long columns, Class<T> elementClass, CLKernels kernels) {
        if (elementClass == Double.class)
            return (CLMatrix2D<T>)new CLDefaultMatrix2D(Primitive.Double, null, rows, columns, kernels);
      
        throw new UnsupportedOperationException("Cannot build buffers of " + elementClass.getName() + " yet");
    }
    
    
    public static <V> CLMatrix2D<V> op1(final CLMatrix2D<V> in, final Fun1 fun, final CLMatrix2D<V> out) throws CLBuildException {
        in.getEvents().performRead(new CLEvents.Action() {

            public CLEvent perform(final CLEvent[] ievents) {
                return out.getEvents().performWrite(new CLEvents.Action() {

                    public CLEvent perform(CLEvent[] oevents) {
                        return in.getKernels().op1(in.getPrimitive(), fun, in.getBuffer(), in.getRowCount(), in.getColumnCount(), out.getBuffer(), join(ievents, oevents));
                    }
                });
            }
        });
        return out;
    }


    public static <V> CLMatrix2D<V> op2(final CLMatrix2D<V> in1, final Fun2 fun, final CLMatrix2D<V> in2, final CLMatrix2D<V> out) throws CLBuildException {
        in1.getEvents().performRead(new CLEvents.Action() {

            public CLEvent perform(final CLEvent[] i1events) {
                return in2.getEvents().performRead(new CLEvents.Action() {

                    public CLEvent perform(final CLEvent[] i2events) {
                        return out.getEvents().performWrite(new CLEvents.Action() {

                            public CLEvent perform(CLEvent[] oevents) {
                                return in1.getKernels().op2(in1.getPrimitive(), fun, in1.getBuffer(), in2.getBuffer(), in1.getRowCount(), in1.getColumnCount(), out.getBuffer(), join(i1events, i2events, oevents));
                            }
                        });
                    }
                });
            }
        });
        return out;
    }
    public static <V> CLMatrix2D<V> op2(final CLMatrix2D<V> in, final Fun2 fun, final V s2, final CLMatrix2D<V> out) throws CLBuildException {
        in.getEvents().performRead(new CLEvents.Action() {
            public CLEvent perform(final CLEvent[] ievents) {
                return out.getEvents().performWrite(new CLEvents.Action() {

                    public CLEvent perform(CLEvent[] oevents) {
                        return in.getKernels().op2(in.getPrimitive(), fun, in.getBuffer(), s2, in.getRowCount(), in.getColumnCount(), out.getBuffer(), join(ievents, oevents));
                    }
                });
            }
        });
        return out;
    }

}
