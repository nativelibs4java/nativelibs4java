/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.blas.jama;

import com.nativelibs4java.blas.AbstractLinearAlgebra;
import com.nativelibs4java.blas.LinearAlgebra;
import com.nativelibs4java.blas.Matrix;

/**
 *
 * @author ochafik
 */
public class JamaLinearAlgebra extends AbstractLinearAlgebra<JamaMatrix> {

	@Override
	public JamaMatrix newMatrix(int rows, int columns) {
		return new JamaMatrix(rows, columns);
	}

}
