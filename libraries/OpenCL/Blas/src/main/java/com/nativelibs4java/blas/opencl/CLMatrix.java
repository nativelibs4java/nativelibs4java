/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.blas.opencl;

import com.nativelibs4java.blas.Cholesky;
import com.nativelibs4java.blas.Eigen;
import com.nativelibs4java.blas.LU;
import com.nativelibs4java.blas.Matrix;
import com.nativelibs4java.blas.QR;
import com.nativelibs4java.blas.SVD;
import com.nativelibs4java.blas.Vector;
import com.nativelibs4java.blas.java.DefaultMatrix;
import com.nativelibs4java.opencl.CLDoubleBuffer;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLMem;
import com.nativelibs4java.opencl.CLMem.MapFlags;
import com.nativelibs4java.opencl.CLMem.Usage;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Olivier
 */
public class CLMatrix extends CLDoubleData implements Matrix<CLMatrix, CLVector, DoubleBuffer> {

	protected final int rows, columns;
    public CLMatrix(CLLinearAlgebra al, int rows, int columns) {
        super(al, rows * columns);
		this.rows = rows;
		this.columns = columns;
    }


	@Override
	public int getRows() {
		return rows;
	}

	@Override
	public int getColumns() {
		return columns;
	}

	@Override
	public CLMatrix multiply(CLMatrix other, CLMatrix out) {
		if (getColumns() != other.getRows())
			throw new IllegalArgumentException("These two matrices cannot be multiplied (incompatible dimensions)");

		List<CLEvent> evtsList = new ArrayList<CLEvent>();

		if (out == null)
			out = al.newMatrix(getRows(), other.getColumns());
		else if (out.getRows() != getRows() || out.getColumns() != other.getColumns())
			throw new IllegalArgumentException("The output matrix does not have the expected size");
		else
			out.eventsBeforeWriting(evtsList);

		eventsBeforeReading(evtsList);
		other.eventsBeforeReading(evtsList);
		al.multiply(this, other, out, evtsList.toArray(new CLEvent[evtsList.size()]));
		return out;
	}

	@Override
	public CLVector multiply(CLVector v, CLVector out) {
		if (getColumns() != v.size())
			throw new IllegalArgumentException("This vector cannot be multiplied by this matrix  (incompatible dimensions)");

		List<CLEvent> evtsList = new ArrayList<CLEvent>();

		if (out == null)
			out = al.newVector(getRows());
		else if (out.size() != getRows())
			throw new IllegalArgumentException("The output vector does not have the expected size");
		else
			out.eventsBeforeWriting(evtsList);

		eventsBeforeReading(evtsList);
		v.eventsBeforeReading(evtsList);
		al.multiply(this, v, out, evtsList.toArray(new CLEvent[evtsList.size()]));
		return out;
	}

	public DefaultMatrix toDefaultMatrix() {
		DefaultMatrix m = new DefaultMatrix(rows, columns);
		m.write(read());
		return m;
	}

	@Override
	public SVD<CLMatrix, CLVector, DoubleBuffer> svd() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public LU<CLMatrix, CLVector, DoubleBuffer> lu() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Cholesky<CLMatrix, CLVector, DoubleBuffer> cholesky() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Eigen<CLMatrix, CLVector, DoubleBuffer> eigen() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public QR<CLMatrix, CLVector, DoubleBuffer> qr() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String toString() {
		return toDefaultMatrix().toString();
	}


}
