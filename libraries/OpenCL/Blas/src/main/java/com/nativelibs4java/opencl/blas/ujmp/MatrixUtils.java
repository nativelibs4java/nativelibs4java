/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.blas.ujmp;

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
        if (out instanceof CLDenseDoubleMatrix2D) {
            CLDenseDoubleMatrix2D m = (CLDenseDoubleMatrix2D)out;
            m.write(b);
        }
        b = b.duplicate();
        for (long i = 0, rows = out.getRowCount(), columns = out.getColumnCount(); i < rows; i++)
            for (long j = 0; j < columns; j++)
                out.setDouble(b.get(), i, j);
    }

    public static void read(DoubleMatrix2D m, double[] out) {
        read(m, DoubleBuffer.wrap(out));
    }
    public static void read(DoubleMatrix2D m, DoubleBuffer out) {
        if (m instanceof CLDenseDoubleMatrix2D) {
            CLDenseDoubleMatrix2D mm = (CLDenseDoubleMatrix2D)m;
            mm.read(out);
        }
        out = out.duplicate();
        for (long i = 0, rows = m.getRowCount(), columns = m.getColumnCount(); i < rows; i++)
            for (long j = 0; j < columns; j++)
                out.put(m.getDouble(i, j));
    }
}
