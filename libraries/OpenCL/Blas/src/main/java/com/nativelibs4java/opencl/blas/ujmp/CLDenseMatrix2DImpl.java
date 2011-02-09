/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.blas.ujmp;

import com.nativelibs4java.util.Pair;
import org.bridj.Pointer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.ujmp.core.Matrix;

import org.ujmp.core.calculation.Calculation.Ret;
import org.ujmp.core.exceptions.MatrixException;

import com.nativelibs4java.opencl.CLBuildException;
import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLMem.MapFlags;
import com.nativelibs4java.opencl.CLMem.Usage;
import com.nativelibs4java.opencl.util.Fun1;
import com.nativelibs4java.opencl.util.Fun2;
import com.nativelibs4java.opencl.util.OpenCLType;
import com.nativelibs4java.opencl.util.Primitive;
import com.nativelibs4java.opencl.util.ReductionUtils;
import com.nativelibs4java.opencl.util.ReductionUtils.Reductor;

/**
 *
 * @author ochafik
 */
public class CLDenseMatrix2DImpl<V> {
    final Primitive primitive;
    final long rows, columns, length;
    
    protected final OpenCLUJMP clUJMP;
    protected CLBuffer<V> buffer;
    protected Pointer<V> data;

    public CLDenseMatrix2DImpl(Primitive primitive, CLBuffer<V> buffer, long rows, long columns, OpenCLUJMP clUJMP) {
        this.primitive = primitive;
        this.buffer = buffer == null ? (CLBuffer)clUJMP.getContext().createBuffer(Usage.InputOutput, primitive.primitiveType, rows * columns) : buffer;
        this.clUJMP = clUJMP;
        this.rows = rows;
        this.columns = columns;
        this.length = rows * columns;
    }
    private final static CLEvent[] EMPTY_EVENTS = new CLEvent[0];

	final List<CLEvent> writeEvents = new ArrayList<CLEvent>(), readEvents = new ArrayList<CLEvent>();

    CLEvent map(CLEvent... eventsToWaitFor) {
        return buffer.mapLater(clUJMP.getQueue(), MapFlags.ReadWrite, eventsToWaitFor).getSecond();
    }
    CLEvent unmap(CLEvent... eventsToWaitFor) {
        return buffer.unmap(clUJMP.getQueue(), data, eventsToWaitFor);
    }

    protected long getStorageIndex(long row, long column) {
        return row * columns + column;
    }

    public V get(long row, long column) {
        CLEvent[] evts = eventsBeforeReading();
        Pointer<V> ptr = (Pointer)Pointer.allocate(primitive.primitiveType);
        buffer.read(clUJMP.getQueue(), getStorageIndex(row, column), 1, ptr, true, evts);
        purgeEvents(evts);
        return ptr.get();
    }

    public void set(V value, long row, long column) {
        Pointer<V> ptr = (Pointer)Pointer.allocate(primitive.primitiveType);
        ptr.set(value);
        addWriteEvent(buffer.write(clUJMP.getQueue(), getStorageIndex(row, column), 1, ptr, true, eventsBeforeWriting()));
    }

    public synchronized CLDenseMatrix2DImpl<V> multiplyMatrix(CLDenseMatrix2DImpl<V> matrix) throws MatrixException {
        synchronized (matrix) {
            try {
                if (columns != matrix.rows)
                    throw new MatrixException("Incompatible dimensions");

                List<CLEvent> eventsToWaitFor = new ArrayList<CLEvent>();
                eventsBefore(false, eventsToWaitFor);

                CLDenseMatrix2DImpl<V> out = new CLDenseMatrix2DImpl<V>(primitive, null, rows, matrix.columns, clUJMP);
                CLEvent evt = clUJMP.matrixMultiply(Primitive.Double, buffer, rows, columns, matrix.buffer, matrix.rows, matrix.columns, out.buffer, events(eventsToWaitFor)).getSecond();
                out.addWriteEvent(evt);
                addReadEvent(evt);
                return out;
            } catch (CLBuildException ex) {
                throw new MatrixException("Failed to multiply matrices", ex);
            }
        }
    }

    public synchronized CLDenseMatrix2DImpl<V> copy() throws MatrixException {
        CLDenseMatrix2DImpl<V> out = new CLDenseMatrix2DImpl<V>(primitive, null, rows, columns, clUJMP);
        CLEvent evt = out.buffer.copyTo(clUJMP.getQueue(), 0, length, out.buffer, 0, eventsBeforeReading());
        out.addWriteEvent(evt);
        addReadEvent(evt);
        return out;
    }

    public synchronized CLDenseMatrix2DImpl<V> transpose(Ret returnType) throws MatrixException {
        try {
            CLDenseMatrix2DImpl<V> out = returnType == Ret.ORIG ? new CLDenseMatrix2DImpl<V>(primitive, null, columns, rows, clUJMP) : this;
            CLEvent evt = clUJMP.matrixTranspose(primitive, buffer, rows, columns, out.buffer, returnType == Ret.ORIG ? eventsBeforeWriting() : eventsBeforeReading()).getSecond();
            addReadEvent(evt);
            out.addWriteEvent(evt);
            return out;
        } catch (CLBuildException ex) {
            throw new RuntimeException("Failed to transpose matrix", ex);
        }
    }

    public CLEvent[] eventsBeforeReading() {
		return eventsBefore(false);
	}
	public CLEvent[] eventsBeforeWriting() {
		return eventsBefore(true);
	}
	public void eventsBeforeReading(List<CLEvent> out) {
		eventsBefore(false, out);
	}
	public void eventsBeforeWriting(List<CLEvent> out) {
		eventsBefore(true, out);
	}
    protected CLEvent[] events(List<CLEvent> events) {
        if (events.isEmpty())
            return EMPTY_EVENTS;
        return events.toArray(new CLEvent[events.size()]);
    }
	protected synchronized CLEvent[] eventsBefore(boolean includeReads) {
		int nr = !includeReads || readEvents == null ? 0 : readEvents.size();
		int nw = writeEvents == null ? 0 : writeEvents.size();
		int n = nr + nw;
		if (n == 0)
			return EMPTY_EVENTS;

		CLEvent[] ret = new CLEvent[n];
		for (int i = 0; i < nr; i++)
			ret[i] = readEvents.get(i);
		for (int i = 0; i < nw; i++)
			ret[nr + i] = writeEvents.get(i);

		return ret;
	}
	protected synchronized void eventsBefore(boolean includeReads, List<CLEvent> out) {
		if (includeReads)
			out.addAll(readEvents);
        out.addAll(writeEvents);
	}
	protected synchronized void addWriteEvent(CLEvent evt) {
		if (evt == null)
			return;
		readEvents.add(evt);
	}
	protected synchronized void addReadEvent(CLEvent evt) {
		if (evt == null)
			return;
		writeEvents.add(evt);
	}

	protected synchronized void purgeEvents(CLEvent[] evts) {
        if (evts.length == 0)
            return;
        List<CLEvent> evtList = Arrays.asList(evts);
        readEvents.removeAll(evtList);
        writeEvents.removeAll(evtList);
    }

    public void write(Pointer<V> b) {
        addWriteEvent(buffer.write(clUJMP.getQueue(), b, false, eventsBeforeWriting()));
    }

    public void read(Pointer<V> b) {
        buffer.read(clUJMP.getQueue(), b, true, eventsBeforeReading());
    }

    public Pointer<V> read() {
        Pointer<V> b = (Pointer)Pointer.allocateArray(primitive.primitiveType, rows * columns);
        read(b);
        return b;
    }
    
    public CLDenseMatrix2DImpl<V> sin(Ret returnType) throws MatrixException {
        return op1(primitive, Fun1.sin, returnType);
    }

    
    public CLDenseMatrix2DImpl<V> cos(Ret returnType) throws MatrixException {
        return op1(primitive, Fun1.cos, returnType);
    }

    
    public CLDenseMatrix2DImpl<V> sinh(Ret returnType) throws MatrixException {
        return op1(primitive, Fun1.sinh, returnType);
    }

    
    public CLDenseMatrix2DImpl<V> cosh(Ret returnType) throws MatrixException {
        return op1(primitive, Fun1.cosh, returnType);
    }

    
    public CLDenseMatrix2DImpl<V> tan(Ret returnType) throws MatrixException {
        return op1(primitive, Fun1.tan, returnType);
    }

    
    public CLDenseMatrix2DImpl<V> tanh(Ret returnType) throws MatrixException {
        return op1(primitive, Fun1.tanh, returnType);
    }


    public CLDenseMatrix2DImpl<V> asin(Ret returnType) throws MatrixException {
        return op1(primitive, Fun1.asin, returnType);
    }


    public CLDenseMatrix2DImpl<V> acos(Ret returnType) throws MatrixException {
        return op1(primitive, Fun1.acos, returnType);
    }


    public CLDenseMatrix2DImpl<V> asinh(Ret returnType) throws MatrixException {
        return op1(primitive, Fun1.asinh, returnType);
    }


    public CLDenseMatrix2DImpl<V> acosh(Ret returnType) throws MatrixException {
        return op1(primitive, Fun1.acosh, returnType);
    }


    public CLDenseMatrix2DImpl<V> atan(Ret returnType) throws MatrixException {
        return op1(primitive, Fun1.atan, returnType);
    }


    public CLDenseMatrix2DImpl<V> atanh(Ret returnType) throws MatrixException {
        return op1(primitive, Fun1.atanh, returnType);
    }


    public CLDenseMatrix2DImpl<V> minus(Ret returnType, boolean ignoreNaN, CLDenseMatrix2DImpl<V> m) throws MatrixException {
        return op2(primitive, Fun2.substract, m, returnType);
    }

    public CLDenseMatrix2DImpl<V> minus(Ret returnType, boolean ignoreNaN, V v) throws MatrixException {
        return op2(primitive, Fun2.substract, v, returnType);
    }

    public CLDenseMatrix2DImpl<V> plus(Ret returnType, boolean ignoreNaN, CLDenseMatrix2DImpl<V> m) throws MatrixException {
        return op2(primitive, Fun2.add, m, returnType);
    }

    public CLDenseMatrix2DImpl<V> plus(Ret returnType, boolean ignoreNaN, V v) throws MatrixException {
        return op2(primitive, Fun2.add, v, returnType);
    }

    public CLDenseMatrix2DImpl<V> times(Ret returnType, boolean ignoreNaN, CLDenseMatrix2DImpl<V> factor) throws MatrixException {
        return op2(primitive, Fun2.multiply, factor, returnType);
    }

    public CLDenseMatrix2DImpl<V> times(Ret returnType, boolean ignoreNaN, V factor) throws MatrixException {
        return op2(primitive, Fun2.multiply, factor, returnType);
    }

    public CLDenseMatrix2DImpl<V> divide(Ret returnType, boolean ignoreNaN, CLDenseMatrix2DImpl<V> factor) throws MatrixException {
        return op2(primitive, Fun2.divide, factor, returnType);
    }

    public CLDenseMatrix2DImpl<V> divide(Ret returnType, boolean ignoreNaN, V factor) throws MatrixException {
        return op2(primitive, Fun2.divide, factor, returnType);
    }
    //protected abstract Matrix createMatrix(CLMatrixImpl<V> mi);


    public CLDenseMatrix2DImpl<V> op1(Primitive prim, Fun1 fun, Ret returnType) throws MatrixException {
        try {

            Pair<CLBuffer<V>, CLEvent> bufEvt = clUJMP.op1(prim, fun, buffer, rows, columns, returnType == Ret.ORIG ? buffer : null, returnType == Ret.ORIG ? eventsBeforeWriting() : eventsBeforeReading());
            CLDenseMatrix2DImpl<V> m;
            switch (returnType) {
                case LINK:
                case NEW:
                    m = new CLDenseMatrix2DImpl<V>(primitive, bufEvt.getFirst(), rows, columns, clUJMP);
                    break;
                case ORIG:
                    m = this;
                    break;
                default:
                    throw new RuntimeException("Unknown return type");
            }
            m.addWriteEvent(bufEvt.getSecond());
            return m;
        } catch (CLBuildException ex) {
            throw new MatrixException("Failed to compute sinus", ex);
        }
    }


    public CLDenseMatrix2DImpl<V> op2(Primitive prim, Fun2 fun, CLDenseMatrix2DImpl<V> m2, Ret returnType) throws MatrixException {
        try {
            List<CLEvent> evts = new ArrayList<CLEvent>();
            eventsBefore(returnType == Ret.ORIG, evts);
            m2.eventsBefore(false, evts);
            Pair<CLBuffer<V>, CLEvent> bufEvt = clUJMP.op2(prim, fun, buffer, m2.buffer, rows, columns, returnType == Ret.ORIG ? buffer : null, evts.toArray(new CLEvent[evts.size()]));
            CLDenseMatrix2DImpl<V> m;
            switch (returnType) {
                case LINK:
                case NEW:
                    m = new CLDenseMatrix2DImpl<V>(primitive, bufEvt.getFirst(), rows, columns, clUJMP);
                    break;
                case ORIG:
                    m = this;
                    break;
                default:
                    throw new RuntimeException("Unknown return type");
            }
            m.addWriteEvent(bufEvt.getSecond());
            return m;
        } catch (CLBuildException ex) {
            throw new MatrixException("Failed to compute sinus", ex);
        }
    }
    public CLDenseMatrix2DImpl<V> op2(Primitive prim, Fun2 fun, V s2, Ret returnType) throws MatrixException {
        try {
            Pair<CLBuffer<V>, CLEvent> bufEvt = clUJMP.op2(prim, fun, buffer, s2, rows, columns, returnType == Ret.ORIG ? buffer : null, returnType == Ret.ORIG ? eventsBeforeWriting() : eventsBeforeReading());
            CLDenseMatrix2DImpl<V> m;
            switch (returnType) {
                case LINK:
                case NEW:
                    m = new CLDenseMatrix2DImpl<V>(primitive, bufEvt.getFirst(), rows, columns, clUJMP);
                    break;
                case ORIG:
                    m = this;
                    break;
                default:
                    throw new RuntimeException("Unknown return type");
            }
            m.addWriteEvent(bufEvt.getSecond());
            return m;
        } catch (CLBuildException ex) {
            throw new MatrixException("Failed to compute sinus", ex);
        }
    }

    static final int MAX_REDUCTION_SIZE = 32;
    Reductor<V> minReductor;
    public CLDenseMatrix2DImpl<V> min() throws CLBuildException {
        synchronized (this) {
            if (minReductor == null)
                minReductor = ReductionUtils.createReductor(clUJMP.getContext(), ReductionUtils.Operation.Min, OpenCLType.Double, 1);
        }
        Pointer<V> reduce = minReductor.reduce(clUJMP.getQueue(), buffer, length, MAX_REDUCTION_SIZE, eventsBeforeReading());
        return new CLDenseMatrix2DImpl<V>(primitive, clUJMP.getContext().createBuffer(Usage.InputOutput, reduce, true), 1, 1, clUJMP);
    }
    Reductor<V> maxReductor;
    public CLDenseMatrix2DImpl<V> max() throws CLBuildException {
        synchronized (this) {
            if (maxReductor == null)
                maxReductor = ReductionUtils.createReductor(clUJMP.getContext(), ReductionUtils.Operation.Max, OpenCLType.Double, 1);
        }
        Pointer<V> reduce = maxReductor.reduce(clUJMP.getQueue(), buffer, length, MAX_REDUCTION_SIZE, eventsBeforeReading());
        return new CLDenseMatrix2DImpl<V>(primitive, clUJMP.getContext().createBuffer(Usage.InputOutput, reduce, true), 1, 1, clUJMP);
    }

    boolean containsValue(V v) throws CLBuildException {
        CLEvent[] evts = eventsBeforeReading();
        boolean b = clUJMP.containsValue(primitive, buffer, length, v, evts);
        purgeEvents(evts);
        return b;
    }

    void clear() throws CLBuildException {
        addWriteEvent(clUJMP.clear(primitive, buffer, length, eventsBeforeWriting()));
    }

    public void waitFor() {
        CLEvent[] evts = eventsBeforeReading();
        CLEvent.waitFor(evts);
        purgeEvents(evts);

        evts = eventsBeforeWriting();
        CLEvent.waitFor(evts);
        purgeEvents(evts);
    }

}
