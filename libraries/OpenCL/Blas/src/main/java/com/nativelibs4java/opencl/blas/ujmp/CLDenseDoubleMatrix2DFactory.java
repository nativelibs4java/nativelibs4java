/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.blas.ujmp;

import com.nativelibs4java.opencl.CLPlatform;
import com.nativelibs4java.opencl.CLPlatform.DeviceFeature;
import com.nativelibs4java.opencl.JavaCL;
import com.nativelibs4java.opencl.util.LinearAlgebraUtils;
import com.nativelibs4java.opencl.util.Primitive;
import org.ujmp.core.doublematrix.DenseDoubleMatrix2D;
import org.ujmp.core.doublematrix.factory.AbstractDoubleMatrix2DFactory;
import org.ujmp.core.exceptions.MatrixException;

/**
 *
 * @author ochafik
 */
public class CLDenseDoubleMatrix2DFactory extends AbstractDoubleMatrix2DFactory {
	
	public DenseDoubleMatrix2D dense(long rows, long columns)
			throws MatrixException {
		return new CLDenseDoubleMatrix2D(rows, columns, OpenCLUJMP.getInstance());
	}
}