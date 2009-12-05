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
import com.nativelibs4java.blas.java.DefaultLinearAlgebra;
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
public class CLMatrix extends CLDoubleData implements Matrix<CLMatrix, DoubleBuffer> {

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
	public SVD<CLMatrix, DoubleBuffer> svd() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public LU<CLMatrix, DoubleBuffer> lu() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Cholesky<CLMatrix, DoubleBuffer> cholesky() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Eigen<CLMatrix, DoubleBuffer> eigen() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public QR<CLMatrix, DoubleBuffer> qr() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String toString() {
		return DefaultLinearAlgebra.getInstance().newMatrix(this).toString();
	}


}
