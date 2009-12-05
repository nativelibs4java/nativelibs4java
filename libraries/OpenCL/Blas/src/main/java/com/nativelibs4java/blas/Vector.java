package com.nativelibs4java.blas;

import com.nativelibs4java.util.NIOUtils;
import java.nio.Buffer;
import java.nio.DoubleBuffer;


public interface Vector<M extends Matrix<M, V, B>, V extends Vector<M, V, B>, B extends Buffer> extends Data<B>  {
	//V cross(V other, V out);

	/**
	 * @param other Another vector of the same size as this one.
	 * @return Vector of size 1 with the dot product of this vector and other.
	 */
	V dot(V other, V out);
}
