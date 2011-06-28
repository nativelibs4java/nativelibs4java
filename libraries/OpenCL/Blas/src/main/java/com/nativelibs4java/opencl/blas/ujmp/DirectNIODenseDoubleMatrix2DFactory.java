/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.blas.ujmp;

import org.ujmp.core.doublematrix.DenseDoubleMatrix2D;
import org.ujmp.core.doublematrix.factory.AbstractDoubleMatrix2DFactory;
import org.ujmp.core.exceptions.MatrixException;

/**
 *
 * @author ochafik
 */
public class DirectNIODenseDoubleMatrix2DFactory extends
		AbstractDoubleMatrix2DFactory {
	private static final long serialVersionUID = 4390694808314618187L;


	public DenseDoubleMatrix2D dense(long rows, long columns)
			throws MatrixException {
		return new DirectNIODenseDoubleMatrix2D(rows, columns);
	}

}