/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.blas.ujmp;

import com.nativelibs4java.opencl.blas.CLKernels;
import org.ujmp.core.floatmatrix.DenseFloatMatrix2D;
import org.ujmp.core.floatmatrix.factory.AbstractFloatMatrix2DFactory;
import org.ujmp.core.exceptions.MatrixException;

/**
 *
 * @author ochafik
 */
public class CLDenseFloatMatrix2DFactory extends AbstractFloatMatrix2DFactory {
	public CLDenseFloatMatrix2D dense(long rows, long columns)
			throws MatrixException {
		return new CLDenseFloatMatrix2D(rows, columns);
	}
	public CLDenseFloatMatrix2D densePacked(long rows, long columns)
			throws MatrixException {
		return new CLDenseFloatMatrix2D(rows, columns, CLKernels.getInstance(), 1);
	}
}