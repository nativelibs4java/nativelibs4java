/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.blas;

import java.nio.Buffer;

/**
 *
 * @author ochafik
 */
public abstract class AbstractLinearAlgebra<M extends Matrix> implements LinearAlgebra<M> {

	@Override
	public M newMatrix(Matrix m) {
		M n = newMatrix(m.getRows(), m.getColumns());
		n.write(m.read());
		return n;
	}

}
