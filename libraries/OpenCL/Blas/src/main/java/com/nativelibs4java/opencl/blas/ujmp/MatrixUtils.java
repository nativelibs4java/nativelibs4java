/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.blas.ujmp;

import com.nativelibs4java.opencl.blas.CLKernels;
import com.nativelibs4java.opencl.blas.CLMatrix2D;
import org.ujmp.core.floatmatrix.FloatMatrix2D;
import org.ujmp.core.doublematrix.DoubleMatrix2D;
import org.ujmp.core.matrix.Matrix2D;

import org.bridj.Pointer;
import static org.bridj.Pointer.*;

/**
 *
 * @author ochafik
 */
public class MatrixUtils {
    public static void write(double[] b, long stride, DoubleMatrix2D out) {
        write(pointerToDoubles(b), stride, out);
    }
    public static void write(float[] b, long stride, FloatMatrix2D out) {
        write(pointerToFloats(b), stride, out);
    }
    public static void write(Pointer<Double> b, long stride, DoubleMatrix2D out) {
        long rows = out.getRowCount(), columns = out.getColumnCount();
        if (b.getValidElements() < rows * columns)
            throw new IllegalArgumentException("Not enough data in input buffer to write into " + rows + "x" + columns + " matrix (only has " + b.getValidElements() + ")");
        if (out instanceof CLDenseDoubleMatrix2D && stride == ((CLDenseDoubleMatrix2D)out).getStride()) {
            CLDenseDoubleMatrix2D mout = (CLDenseDoubleMatrix2D)out;
            mout.write(b);
        } else {
            for (long i = 0; i < rows; i++) {
                long offset = i * stride;
                for (long j = 0; j < columns; j++)
                    out.setDouble(b.get(offset + j), i, j);
            }
        }
    }
    public static void write(Pointer<Float> b, long stride, FloatMatrix2D out) {
        long rows = out.getRowCount(), columns = out.getColumnCount();
        if (b.getValidElements() < rows * columns)
            throw new IllegalArgumentException("Not enough data in input buffer to write into " + rows + "x" + columns + " matrix (only has " + b.getValidElements() + ")");
        if (out instanceof CLDenseFloatMatrix2D && stride == ((CLDenseFloatMatrix2D)out).getStride()) {
            CLDenseFloatMatrix2D mout = (CLDenseFloatMatrix2D)out;
            mout.write(b);
        } else {
            for (long i = 0; i < rows; i++) {
                long offset = i * stride;
                for (long j = 0; j < columns; j++)
                    out.setFloat(b.get(offset + j), i, j);
            }
        }
    }

    public static Pointer<Double> read(DoubleMatrix2D m) {
        return read(m, m.getColumnCount());
    }
    public static Pointer<Double> read(DoubleMatrix2D m, long stride) {
        Pointer<Double> buffer = allocateDoubles(stride * m.getRowCount()).order(CLKernels.getInstance().getContext().getKernelsDefaultByteOrder());
        read(m, buffer, stride);
        return buffer;
    }
    public static <T> void read(Matrix2D m, Pointer<T> out, long stride) {
        if (m instanceof DoubleMatrix2D)
            read((DoubleMatrix2D)m, (Pointer<Double>)out, stride);
        else if (m instanceof FloatMatrix2D)
            read((FloatMatrix2D)m, (Pointer<Float>)out, stride);
        else
            throw new UnsupportedOperationException("Can only read DoubleMatrix2D into DoubleBuffer for now");
    }
    public static void read(DoubleMatrix2D m, Pointer<Double> out, long stride) {
        long rows = m.getRowCount(), columns = m.getColumnCount();
        if (out.getValidElements() < rows * columns)
            throw new IllegalArgumentException("Not enough space in output buffer to read into " + rows + "x" + columns + " matrix (only has " + out.getValidElements() + ")");
        
        if (m instanceof CLDenseDoubleMatrix2D && stride == ((CLDenseDoubleMatrix2D)m).getStride()) {
            CLDenseDoubleMatrix2D mm = (CLDenseDoubleMatrix2D)m;
            mm.read(out);
        } else {
            for (long i = 0; i < rows; i++) {
            		long offset = i * stride;
            		for (long j = 0; j < columns; j++) {
                    out.set(offset + j, m.getDouble(i, j));
				}
			}
        }
    }

    public static Pointer<Float> read(FloatMatrix2D m, long stride) {
        Pointer<Float> buffer = allocateFloats(stride * m.getRowCount()).order(CLKernels.getInstance().getContext().getKernelsDefaultByteOrder());
        read(m, buffer, stride);
        return buffer;
    }
    public static void read(FloatMatrix2D m, Pointer<Float> out, long stride) {
        long rows = m.getRowCount(), columns = m.getColumnCount();
        if (out.getValidElements() < rows * columns)
            throw new IllegalArgumentException("Not enough space in output buffer to read into " + rows + "x" + columns + " matrix (only has " + out.getValidElements() + ")");

        if (m instanceof CLDenseFloatMatrix2D && stride == ((CLDenseFloatMatrix2D)m).getStride()) {
            CLDenseFloatMatrix2D mm = (CLDenseFloatMatrix2D)m;
            mm.read(out);
        } else {
            for (long i = 0; i < rows; i++) {
            		long offset = i * stride;
            		for (long j = 0; j < columns; j++) {
                    out.set(offset + j, m.getFloat(i, j));
				}
			}
        }
    }
}
