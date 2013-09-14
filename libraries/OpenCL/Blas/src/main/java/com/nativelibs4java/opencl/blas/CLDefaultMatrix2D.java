/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nativelibs4java.opencl.blas;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLMem.Usage;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.util.Primitive;

import org.bridj.Pointer;

/**
 *
 * @author ochafik
 */
public class CLDefaultMatrix2D<T> implements CLMatrix2D<T> {
    protected final Primitive primitive;
    protected final Class<T> primitiveClass;
    protected final long rows, columns, length;
    
    protected final CLKernels kernels;
    protected final CLBuffer<T> buffer;
    protected final CLQueue queue;
    protected final CLContext context;
    protected CLEvents _events = new CLEvents();

    public CLDefaultMatrix2D(Primitive primitive, CLBuffer<T> buffer, long rows, long columns, CLKernels kernels) {
        this.primitive = primitive;
        this.primitiveClass = (Class<T>)primitive.primitiveType;
        this.length = CLMatrixUtils.roundUp(rows) * CLMatrixUtils.roundUp(columns);
        if (buffer != null) {
            if (buffer.getElementCount() < this.length) {
                throw new IllegalArgumentException("Buffer size too small; buffer of size " + this.length + " expected, size " + buffer.getByteCount() + " was given");
            }
            this.buffer = buffer;
        } else {
            this.buffer = (CLBuffer)kernels.getContext().createBuffer(Usage.InputOutput, primitive.primitiveType, length);
        }
        this.kernels = kernels;
        this.rows = rows;
        this.columns = columns;
        this.queue = kernels.getQueue();
        this.context = kernels.getContext();
    }
    
    public CLMatrix2D<T> blankClone() {
        return blankMatrix(getRowCount(), getColumnCount());
    }
    public CLMatrix2D<T> blankMatrix(long rows, long columns) {
        return new CLDefaultMatrix2D<T>(primitive, null, rows, columns, kernels);
    }

    public long getRowCount() {
        return rows;
    }

    public long getColumnCount() {
        return columns;
    }

    public CLEvents getEvents() {
        return _events;
    }
    
    public void write(final Pointer<T> b) {
        getEvents().performWrite(new CLEvents.Action() {
            public CLEvent perform(CLEvent[] events) {
                return buffer.write(queue, b, false, events);
            }
        });
    }

    public void read(final Pointer<T> b) {
        getEvents().performRead(new CLEvents.Action() {
            public CLEvent perform(CLEvent[] events) {
                return buffer.read(queue, b, true, events);
            }
        });
    }
    public Pointer<T> read() {
        Pointer<T> out = Pointer.allocateArray(primitiveClass, length);
        read(out);
        return out;
    }
    
    
    public CLBuffer<T> getBuffer() {
        return buffer;
    }

    public CLContext getContext() {
        return context;
    }

    public synchronized CLQueue getQueue() {
        return queue;
    }

    /*
    public synchronized void setQueue(CLQueue queue) {
        if (this.queue != null && queue != null) {
            if (this.queue.equals(queue))
                return;
        }
        getEvents().waitFor();
        this.queue = queue;
    }
     * */

    public Primitive getPrimitive() {
        return primitive;
    }

    public Class<T> getPrimitiveClass() {
        return primitiveClass;
    }

    public CLKernels getKernels() {
        return kernels;
    }
    
}
