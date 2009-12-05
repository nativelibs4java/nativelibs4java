package com.nativelibs4java.blas;

import com.nativelibs4java.util.NIOUtils;
import java.nio.Buffer;
import java.nio.DoubleBuffer;
import java.util.Arrays;
import java.util.EnumSet;

public interface Matrix<M extends Matrix<M, B>, B extends Buffer> extends Data<B> {
	M multiply(M m, M out);
	//M dot(M other, M out);
	
	SVD<M, B> svd();
	LU<M, B> lu();
	Cholesky<M, B> cholesky();
	Eigen<M, B> eigen();
	QR<M, B> qr();

	int getRows();
	int getColumns();
}
