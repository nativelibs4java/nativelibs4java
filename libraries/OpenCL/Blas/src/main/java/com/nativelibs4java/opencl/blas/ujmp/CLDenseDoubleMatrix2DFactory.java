/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.blas.ujmp;

import com.nativelibs4java.opencl.blas.LinearAlgebraKernels;
import org.ujmp.core.doublematrix.DenseDoubleMatrix2D;
import org.ujmp.core.doublematrix.factory.AbstractDoubleMatrix2DFactory;
import org.ujmp.core.exceptions.MatrixException;

/**
 *
 * @author ochafik
 */
public class CLDenseDoubleMatrix2DFactory extends
		AbstractDoubleMatrix2DFactory {

    public static volatile LinearAlgebraKernels LINEAR_ALGEBRA_KERNELS;

    static synchronized LinearAlgebraKernels getLinearAlgebraKernels() {
        if (LINEAR_ALGEBRA_KERNELS == null) {
            try {
                LINEAR_ALGEBRA_KERNELS = new LinearAlgebraKernels();
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            }
        }
        return LINEAR_ALGEBRA_KERNELS;
    }


	public DenseDoubleMatrix2D dense(long rows, long columns)
			throws MatrixException {
		return new CLDenseDoubleMatrix2D(rows, columns);
	}

}