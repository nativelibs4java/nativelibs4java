/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.blas.ujmp;

import com.nativelibs4java.opencl.blas.LinearAlgebraKernels;
import com.nativelibs4java.util.NIOUtils;
import java.nio.DoubleBuffer;
import org.ujmp.core.Matrix;
import org.ujmp.core.benchmark.AbstractMatrix2DBenchmark;
import org.ujmp.core.doublematrix.DoubleMatrix;
import org.ujmp.core.doublematrix.DoubleMatrix2D;
import org.ujmp.core.exceptions.MatrixException;

/**
 *
 * @author ochafik
 */
public class CLMatrixBenchmark extends AbstractMatrix2DBenchmark {

    @Override
    public DoubleMatrix2D createMatrix(long... size) throws MatrixException {
        return new CLDenseDoubleMatrix2D(size);
    }

    @Override
    public DoubleMatrix2D createMatrix(Matrix source) throws MatrixException {
        if (source instanceof CLDenseDoubleMatrix2D)
            return (DoubleMatrix2D)((CLDenseDoubleMatrix2D)source).copy();
        else {
            DoubleMatrix2D dsource = (DoubleMatrix2D)source;
            int rows = (int)dsource.getRowCount(), columns = (int)dsource.getColumnCount();
            DoubleBuffer b = NIOUtils.directDoubles(rows * columns, CLDenseDoubleMatrix2DFactory.LINEAR_ALGEBRA_KERNELS.getContext().getKernelsDefaultByteOrder());
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < rows; j++) {
                    b.put(dsource.getDouble(i, j));
                }
            }
            CLDenseDoubleMatrix2D copy = new CLDenseDoubleMatrix2D(rows, columns);
            copy.write(b);
            return copy;
        }
    }

}
