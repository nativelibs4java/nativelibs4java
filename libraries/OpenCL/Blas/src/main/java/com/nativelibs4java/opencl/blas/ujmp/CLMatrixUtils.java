/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nativelibs4java.opencl.blas.ujmp;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLBuildException;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLMem.Usage;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.util.LinearAlgebraUtils;
import java.nio.DoubleBuffer;
import org.ujmp.core.doublematrix.DoubleMatrix2D;
import com.nativelibs4java.util.NIOUtils;
import java.nio.Buffer;
import org.ujmp.core.exceptions.MatrixException;
import org.ujmp.core.matrix.Matrix2D;

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
    
    
    public static <T> void mtimes(
            final CLMatrix2D<T> a, 
            final CLMatrix2D<T> b, 
            final CLMatrix2D<T> out, 
            final LinearAlgebraUtils kernels) throws MatrixException 
    {
        try {
            a.getEvents().performRead(new CLEvents.Action() {
                public CLEvent perform(final CLEvent[] aevents) {
                    return b.getEvents().performRead(new CLEvents.Action() {
                        public CLEvent perform(final CLEvent[] bevents) {
                            return out.getEvents().performWrite(new CLEvents.Action() {
                                public CLEvent perform(final CLEvent[] cevents) {
                                    CLEvent evt = kernels.multiply(
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
        } catch (CLBuildException ex) {
            throw new RuntimeException("Failed to build OpenCL kernels", ex);
        }
    }
    
    public static <T> void transpose(
            final CLMatrix2D<T> a,
            final CLMatrix2D<T> out,
            final LinearAlgebraUtils kernels) throws MatrixException
    {
        try {
            a.getEvents().performRead(new CLEvents.Action() {
                public CLEvent perform(final CLEvent[] aevents) {
                    return out.getEvents().performWrite(new CLEvents.Action() {
                        public CLEvent perform(final CLEvent[] cevents) {
                            CLEvent evt = kernels.transpose(
                                a.getBuffer(), (int)a.getRowCount(), (int)a.getColumnCount(), 
                                out.getBuffer(), 
                                join(aevents, cevents)
                            );
                            return evt;
                        }
                    });
                }
            });
        } catch (CLBuildException ex) {
            throw new RuntimeException("Failed to build OpenCL kernels", ex);
        }
    }
    
    public static <T> CLMatrix2D<T> clone(final CLMatrix2D<T> matrix, final CLQueue queue) {
        final CLMatrix2D<T> out = matrix.blankClone();
        matrix.getEvents().performRead(new CLEvents.Action() {
            public CLEvent perform(final CLEvent[] aevents) {
                return out.getEvents().performWrite(new CLEvents.Action() {
                    public CLEvent perform(CLEvent[] bevents) {
                        return matrix.getBuffer().copyTo(queue, out.getBuffer(), CLMatrixUtils.join(aevents, bevents));
                    }
                });
            }
        }); 
        return out;
    }
    
    public static <T> CLMatrix2D<T> asInputMatrix(final Matrix2D matrix, final CLQueue queue, final CLContext context) {
        if (matrix instanceof CLMatrix2D)
            return (CLMatrix2D<T>)matrix;
        
        Class ec;
        if (matrix instanceof DoubleMatrix2D)
            ec = Double.class;
        else
            throw new UnsupportedOperationException();
        
        final Class elementClass = ec;
        
        return new CLMatrix2D<T>() {

            CLEvents events = new CLEvents();
            public CLEvents getEvents() {
                return events;
            }

            CLBuffer<T> buffer;
            Buffer data;
            public synchronized CLBuffer<T> getBuffer() {
                long length = matrix.getRowCount() * matrix.getColumnCount();
                
                // Read data
                if (data == null)
                    data = NIOUtils.directBuffer((int)length, context.getByteOrder(), NIOUtils.getBufferClass(elementClass));
                MatrixUtils.read(matrix, data);
                
                // Write data to CLBuffer
                if (buffer == null)
                    buffer = context.createBuffer(Usage.Input, length, elementClass);
                
                events.performWrite(new CLEvents.Action() {
                    public CLEvent perform(CLEvent[] events) {
                        return buffer.write(queue, data, false, events);
                    }
                });
                
                return buffer;
            }

            public long getRowCount() {
                return matrix.getRowCount();
            }

            public long getColumnCount() {
                return matrix.getColumnCount();
            }

            public CLContext getContext() {
                return context;
            }

            public CLMatrix2D<T> blankClone() {
                return createMatrix(getRowCount(), getColumnCount(), elementClass);
            }
            
        };
    }
    public static <T> CLMatrix2D<T> createMatrix(long rows, long columns, Class<T> elementClass) {
        if (elementClass == Double.class)
            return (CLMatrix2D<T>)new CLDenseDoubleMatrix2D(rows, columns);
      
        throw new UnsupportedOperationException("Cannot build buffers of " + elementClass.getName() + " yet");
    }
}
