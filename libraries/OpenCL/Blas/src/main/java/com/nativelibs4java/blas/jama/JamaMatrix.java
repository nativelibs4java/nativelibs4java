/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.blas.jama;

import com.nativelibs4java.blas.Cholesky;
import com.nativelibs4java.blas.Data;
import com.nativelibs4java.blas.Eigen;
import com.nativelibs4java.blas.LU;
import com.nativelibs4java.blas.Matrix;
import com.nativelibs4java.blas.QR;
import com.nativelibs4java.blas.SVD;
import com.nativelibs4java.blas.java.DoubleData;
import gov.nist.math.jama.CholeskyDecomposition;
import gov.nist.math.jama.EigenvalueDecomposition;
import gov.nist.math.jama.LUDecomposition;
import gov.nist.math.jama.QRDecomposition;
import gov.nist.math.jama.SingularValueDecomposition;
import java.nio.DoubleBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author ochafik
 */
public class JamaMatrix implements Matrix<JamaMatrix, DoubleBuffer> {
	gov.nist.math.jama.Matrix matrix;
	public JamaMatrix(int rows, int columns) {
		this(new gov.nist.math.jama.Matrix(rows, columns));
	}
	public JamaMatrix(gov.nist.math.jama.Matrix matrix) {
		this.matrix = matrix;
	}


	@Override
	public JamaMatrix multiply(JamaMatrix m, JamaMatrix out) {
		return mat(matrix.times(m.matrix));
	}


	@Override
	public SVD<JamaMatrix, DoubleBuffer> svd() {
		return new SVD<JamaMatrix, DoubleBuffer>() {
			SingularValueDecomposition svd = new SingularValueDecomposition(matrix);

			@Override
			public JamaMatrix getS() {
				return mat(svd.getS());
			}

			@Override
			public JamaMatrix getV() {
				return mat(svd.getV());
			}

			@Override
			public JamaMatrix getU() {
				return mat(svd.getU());
			}

			@Override
			public JamaMatrix getCond() {
				return mat(svd.cond());
			}

			@Override
			public JamaMatrix getSingularValues() {
				return column(svd.getSingularValues());
			}

			@Override
			public JamaMatrix getRank() {
				return mat(svd.rank());
			}

			@Override
			public JamaMatrix getNorm2() {
				return mat(svd.norm2());
			}
		};
	}

	static final JamaMatrix mat(gov.nist.math.jama.Matrix m) {
		return new JamaMatrix(m);
	}
	static final JamaMatrix mat(double v) {
		return mat(new gov.nist.math.jama.Matrix(new double[][] {{ v }}));
	}
	static final JamaMatrix column(double[] column) {
		gov.nist.math.jama.Matrix m = new gov.nist.math.jama.Matrix(column.length, 1);
		for (int i = column.length; i-- != 0;)
			m.set(i, 0, column[i]);
		return mat(m);
	}
	@Override
	public LU<JamaMatrix, DoubleBuffer> lu() {
		return new LU<JamaMatrix, DoubleBuffer>() {
			LUDecomposition lu = new LUDecomposition(matrix);

			@Override
			public JamaMatrix getPivot() {
				return column(lu.getDoublePivot());
			}

			@Override
			public JamaMatrix getL() {
				return mat(lu.getL());
			}

			@Override
			public JamaMatrix getU() {
				return mat(lu.getU());
			}

			@Override
			public JamaMatrix solve(JamaMatrix m) {
				return mat(lu.solve(m.matrix));
			}

			@Override
			public JamaMatrix det() {
				return mat(lu.det());
			}

			@Override
			public boolean isNonSingular() {
				return lu.isNonsingular();
			}
		};
	}

	@Override
	public Cholesky<JamaMatrix, DoubleBuffer> cholesky() {
		return new Cholesky<JamaMatrix, DoubleBuffer>() {
			CholeskyDecomposition cholesky = new CholeskyDecomposition(matrix);

			@Override
			public JamaMatrix getL() {
				return mat(cholesky.getL());
			}

			@Override
			public boolean isSPD() {
				return cholesky.isSPD();
			}

			@Override
			public JamaMatrix solve(JamaMatrix m) {
				return mat(cholesky.solve(m.matrix));
			}
		};
	}

	@Override
	public Eigen<JamaMatrix, DoubleBuffer> eigen() {
		return new Eigen<JamaMatrix, DoubleBuffer>() {
			EigenvalueDecomposition eigen = new EigenvalueDecomposition(matrix);

			@Override
			public JamaMatrix getD() {
				return mat(eigen.getD());
			}

			@Override
			public JamaMatrix getV() {
				return mat(eigen.getV());
			}

			@Override
			public Data<DoubleBuffer> getComplexEigenValues() {
				double[] reals = eigen.getRealEigenvalues(), imags = eigen.getImagEigenvalues();
				DoubleBuffer b = DoubleBuffer.allocate(reals.length * 2);
				for (int i = 0; i < reals.length; i++) {
					b.put(reals[i]);
					b.put(imags[i]);
				}
				return new DoubleData(b);
			}
		};
	}

	@Override
	public QR<JamaMatrix, DoubleBuffer> qr() {
		return new QR<JamaMatrix, DoubleBuffer>() {
			QRDecomposition qr = new QRDecomposition(matrix);

			@Override
			public JamaMatrix getH() {
				return mat(qr.getH());
			}

			@Override
			public JamaMatrix getQ() {
				return mat(qr.getQ());
			}

			@Override
			public JamaMatrix getR() {
				return mat(qr.getR());
			}

			@Override
			public boolean isFullRank() {
				return qr.isFullRank();
			}

			@Override
			public JamaMatrix solve(JamaMatrix m) {
				return mat(qr.solve(m.matrix));
			}
		};
	}

	@Override
	public int getRows() {
		return matrix.getRowDimension();
	}

	@Override
	public int getColumns() {
		return matrix.getColumnDimension();
	}

	@Override
	public int size() {
		return matrix.getRowDimension() * matrix.getColumnDimension();
	}

	@Override
	public void read(DoubleBuffer out) {
		int columns = matrix.getColumnDimension();
		for (int i = 0, rows = matrix.getRowDimension(); i < rows; i++) {
			int off = i * columns;
			for (int j = 0; j < columns; j++) {
				out.put(off + j, matrix.get(i, j));
			}
		}
	}

	@Override
	public void write(DoubleBuffer in) {
		int columns = matrix.getColumnDimension();
		for (int i = 0, rows = matrix.getRowDimension(); i < rows; i++) {
			int off = i * columns;
			for (int j = 0; j < columns; j++) {
				matrix.set(i, j, in.get(off + j));
			}
		}
	}

	@Override
	public DoubleBuffer read() {
		DoubleBuffer b = DoubleBuffer.allocate(size());
		read(b);
		return b;
	}

}
