/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.blas.ujmp;

import com.nativelibs4java.util.NIOUtils;
import java.nio.DoubleBuffer;
import org.ujmp.core.doublematrix.DoubleMatrix2D;

/**
 *
 * @author ochafik
 */
public class MatrixUtils {
    public static void write(double[] b, DoubleMatrix2D out) {
        write(DoubleBuffer.wrap(b), out);
    }
    public static void write(DoubleBuffer b, DoubleMatrix2D out) {
        long rows = out.getRowCount(), columns = out.getColumnCount();
        if (b.remaining() < rows * columns)
            throw new IllegalArgumentException("Not enough data in input buffer to write into " + rows + "x" + columns + " matrix (only has " + b.remaining() + ")");
        b = b.duplicate();
        if (out instanceof CLDenseDoubleMatrix2D) {
            CLDenseDoubleMatrix2D mout = (CLDenseDoubleMatrix2D)out;
            mout.write(b);
        } else {
            for (long i = 0; i < rows; i++)
                for (long j = 0; j < columns; j++)
                    out.setDouble(b.get(), i, j);
        }
    }

    public static void read(DoubleMatrix2D m, double[] out) {
        read(m, DoubleBuffer.wrap(out));
    }
    public static DoubleBuffer read(DoubleMatrix2D m) {
        DoubleBuffer buffer = NIOUtils.directDoubles((int)(m.getColumnCount() * m.getRowCount()), CLDenseDoubleMatrix2DFactory.LINEAR_ALGEBRA_KERNELS.getContext().getKernelsDefaultByteOrder());
        read(m, buffer);
        return buffer;
    }
    public static void read(DoubleMatrix2D m, DoubleBuffer out) {
        long rows = m.getRowCount(), columns = m.getColumnCount();
        if (out.remaining() < rows * columns)
            throw new IllegalArgumentException("Not enough space in output buffer to read into " + rows + "x" + columns + " matrix (only has " + out.remaining() + ")");
        out = out.duplicate();
        if (m instanceof CLDenseDoubleMatrix2D) {
            CLDenseDoubleMatrix2D mm = (CLDenseDoubleMatrix2D)m;
            mm.read(out);
        } else {
            for (long i = 0; i < rows; i++)
                for (long j = 0; j < columns; j++)
                    out.put(m.getDouble(i, j));
        }
    }
}
