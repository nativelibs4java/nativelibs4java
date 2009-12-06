/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.blas.java;

import com.nativelibs4java.blas.Cholesky;
import com.nativelibs4java.blas.Eigen;
import com.nativelibs4java.blas.LU;
import com.nativelibs4java.blas.Matrix;
import com.nativelibs4java.blas.QR;
import com.nativelibs4java.blas.SVD;
import com.nativelibs4java.util.NIOUtils;
import java.nio.DoubleBuffer;

/**
 *
 * @author Olivier
 */
public class DefaultMatrix extends DoubleData implements Matrix<DefaultMatrix, DoubleBuffer> {

	protected final int rows, columns;

    public DefaultMatrix(int rows, int columns) {
		super(rows * columns);
        this.rows = rows;
		this.columns = columns;
    }

	@Override
	public int getColumns() {
		return columns;
	}

	@Override
	public int getRows() {
		return rows;
	}

    public double get(int row, int column) {
        return data.get(row * columns + column);
    }

    public void set(int row, int column, double value) {
        data.put(row * columns + column, value);
    }

	@Override
	public DefaultMatrix multiply(DefaultMatrix other, DefaultMatrix out) {

		if (getColumns() != other.getRows())
			throw new IllegalArgumentException("These two matrices cannot be multiplied (incompatible dimensions)");

		if (out == null)
			out = new DefaultMatrix(getRows(), other.getColumns());
		else if (out.getRows() != getRows() || out.getColumns() != other.getColumns())
			throw new IllegalArgumentException("The output matrix does not have the expected size");

		for (int i = 0; i < getRows(); i++) {
			for (int j = 0; j < other.getColumns(); j++) {
				double sum = 0;
				for (int k = 0; k < getColumns(); k++) {
					sum += get(i, k) * other.get(k, j);
				}
				out.set(i, j, sum);
			}
		}
		return out;
	}

	@Override
	public SVD<DefaultMatrix, DoubleBuffer> svd() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public LU<DefaultMatrix, DoubleBuffer> lu() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Cholesky<DefaultMatrix, DoubleBuffer> cholesky() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Eigen<DefaultMatrix, DoubleBuffer> eigen() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public QR<DefaultMatrix, DoubleBuffer> qr() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder("{\n");
		for (int i = 0; i < rows; i++) {
			b.append("\t{");
			for (int j = 0; j < columns; j++) {
				if (j != 0)
					b.append(", ");
				b.append(get(i, j));
			}
			b.append("}\n");
		}
		b.append("}");
		return b.toString();
	}

	public DefaultMatrix dot(DefaultMatrix other, DefaultMatrix out) {
		if (out == null)
			out = new DefaultMatrix(1, 1);
		else if (out.size() != 1)
			throw new IllegalArgumentException("Size of output for dot operation must be 1x1");

		double total = 0;
		for (int i = size; i-- != 0;)
			total += data.get(i) * other.data.get(i);
		out.data.put(0, total);
		//out.write(DoubleBuffer.wrap(new double[] { total }));
		return out;
	}

    @Override
    public DefaultMatrix transpose(DefaultMatrix out) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
