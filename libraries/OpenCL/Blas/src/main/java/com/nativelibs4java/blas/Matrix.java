package com.nativelibs4java.blas;

import com.nativelibs4java.util.NIOUtils;
import java.nio.Buffer;
import java.nio.DoubleBuffer;
import java.util.Arrays;
import java.util.EnumSet;

public interface Matrix<M extends Matrix<M, V, B>, V extends Vector<M, V, B>, B extends Buffer> extends Data<B> {
	M multiply(M m, M out);
	V multiply(V v, V out);
	SVD<M, V, B> svd();
	LU<M, V, B> lu();
	Cholesky<M, V, B> cholesky();
	Eigen<M, V, B> eigen();
	QR<M, V, B> qr();

	int getRows();
	int getColumns();
}
