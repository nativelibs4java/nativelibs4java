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
            Logger.getLogger(UJMPOpenCLTest.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void testInstalledUJMPCL() {
        assertTrue(MatrixFactory.dense(1, 1) instanceof CLDenseDoubleMatrix2DFactory);
    }
    @Test
	public void testMult() {

        DenseDoubleMatrix2D m = (DenseDoubleMatrix2D)MatrixFactory.dense(2, 2);
        DenseDoubleMatrix2D v = (DenseDoubleMatrix2D)MatrixFactory.dense(2, 1);

        write(new double[] { 0, 1, 1, 0 }, m);
        DenseDoubleMatrix2D mout = (DenseDoubleMatrix2D) m.mtimes(m);

        //System.out.println(m);
		//System.out.println(mout);

		//if (la instanceof CLLinearAlgebra)
		//	((CLLinearAlgebra)la).queue.finish();
		//dmout.write((DoubleBuffer)mout.read());

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
