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
import com.nativelibs4java.opencl.CLDoubleBuffer;
import com.nativelibs4java.opencl.CLMem;
import com.nativelibs4java.opencl.CLMem.MapFlags;
import com.nativelibs4java.opencl.CLMem.Usage;
import java.nio.DoubleBuffer;

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
	public CLMatrix multiply(CLMatrix m, CLMatrix out) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public CLVector multiply(CLVector v, CLVector out) {
		throw new UnsupportedOperationException("Not supported yet.");
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

}
