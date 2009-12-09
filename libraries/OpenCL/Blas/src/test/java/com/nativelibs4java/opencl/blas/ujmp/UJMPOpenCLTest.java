/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.blas.ujmp;


import static com.nativelibs4java.opencl.blas.ujmp.MatrixUtils.*;

import java.io.IOException;
import java.nio.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.junit.Assert.*;
import org.junit.*;
import org.ujmp.core.Matrix;
import org.ujmp.core.MatrixFactory;
import org.ujmp.core.calculation.Calculation.Ret;
import org.ujmp.core.doublematrix.DenseDoubleMatrix2D;
import org.ujmp.core.mapper.MatrixMapper;
import org.ujmp.core.matrix.DenseMatrix2D;
import org.ujmp.core.matrix.Matrix2D;
/**
 *
 * @author ochafik
 */

public class UJMPOpenCLTest {

    @Before
    public void installUJMPCL() {
        try {
            MatrixMapper.getInstance().setDenseDoubleMatrix2DClass(CLDenseDoubleMatrix2D.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void testInstalledUJMPCL() {
        Matrix m = MatrixFactory.dense(1, 1);
        assertTrue(m instanceof CLDenseDoubleMatrix2D);
    }
    @Test
	public void testMult() {

        DenseDoubleMatrix2D m = (DenseDoubleMatrix2D)MatrixFactory.dense(2, 2);
        DenseDoubleMatrix2D v = (DenseDoubleMatrix2D)MatrixFactory.dense(2, 1);

        double[] min = new double[] { 0, 1, 1, 0 };
        write(min, m);
        DoubleBuffer back = read(m);
        for (int i = 0, cap = back.capacity(); i < cap; i++) {
            assertEquals(min[i], back.get(i), 0);
            System.out.println(back.get(i));
        }
        DenseDoubleMatrix2D mout = (DenseDoubleMatrix2D) m.mtimes(m);

        //System.out.println(m);
		//System.out.println(mout);

		//if (la instanceof CLLinearAlgebra)
		//	((CLLinearAlgebra)la).queue.finish();
		//dmout.write((DoubleBuffer)mout.read());

        back = read(mout);
        for (int i = 0, cap = back.capacity(); i < cap; i++)
            System.out.println(back.get(i));

		assertEquals(0, mout.getDouble(0, 1), 0);
		assertEquals(0, mout.getDouble(1, 0), 0);
        assertEquals(1, mout.getDouble(0, 0), 0);
		assertEquals(1, mout.getDouble(1, 1), 0);

		write(new double[] { 1, 0 }, v);
		DenseDoubleMatrix2D vout = (DenseDoubleMatrix2D)m.mtimes(v);
		//System.out.println(v);
		//System.out.println(vout);

		assertEquals(0, vout.getDouble(0, 0), 0);
		assertEquals(1, vout.getDouble(1, 0), 0);
	}
}
