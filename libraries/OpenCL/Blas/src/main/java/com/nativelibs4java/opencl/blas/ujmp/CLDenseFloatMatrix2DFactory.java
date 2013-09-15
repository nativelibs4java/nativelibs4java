/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.blas.ujmp;

import com.nativelibs4java.opencl.blas.CLDefaultMatrix2D;
import com.nativelibs4java.opencl.blas.CLKernels;
import org.ujmp.core.floatmatrix.DenseFloatMatrix2D;
import org.ujmp.core.floatmatrix.factory.AbstractFloatMatrix2DFactory;
import org.ujmp.core.exceptions.MatrixException;

/**
 *
 * @author ochafik
 */
public class CLDenseFloatMatrix2DFactory extends AbstractFloatMatrix2DFactory {
  final int blockSize;
  public CLDenseFloatMatrix2DFactory(int blockSize) {
    this.blockSize = blockSize;
  }
  public CLDenseFloatMatrix2DFactory() {
    this(CLDefaultMatrix2D.DEFAULT_BLOCK_SIZE);
  }
  public CLDenseFloatMatrix2D dense(long rows, long columns)
      throws MatrixException {
    return new CLDenseFloatMatrix2D(rows, columns, CLKernels.getInstance(), blockSize);
  }
}
