package com.nativelibs4java.opencl.blas.ujmp;

import org.bridj.Pointer;
import static org.bridj.Pointer.*;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLQueue;
import static com.nativelibs4java.opencl.blas.ujmp.MatrixUtils.read;
import static com.nativelibs4java.opencl.blas.ujmp.MatrixUtils.write;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.ujmp.core.Matrix;
import org.ujmp.core.MatrixFactory;
import org.ujmp.core.doublematrix.DenseDoubleMatrix2D;
import org.ujmp.core.mapper.MatrixMapper;
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

        DenseDoubleMatrix2D m = (DenseDoubleMatrix2D)MatrixFactory.dense(3, 3);
        DenseDoubleMatrix2D v = (DenseDoubleMatrix2D)MatrixFactory.dense(3, 1);
        //CLBuffer<Double> buffer = ((CLDenseDoubleMatrix2D)m).getBuffer();
        CLQueue queue = ((CLDenseDoubleMatrix2D)m).getImpl().getMatrix().getQueue();
        //System.out.println("Context = " + buffer.getContext());
        
        double[] min = new double[] { 0, 0, 1, 0, 1, 0, 1, 0, 0 };
        write(min, m);
        Pointer<Double> back = read(m);
        for (int i = 0, cap = (int)back.getValidElements(); i < cap; i++) {
            assertEquals(min[i], back.get(i), 0);
            //System.out.println(back.get(i));
        }
        
        queue.finish();
        DenseDoubleMatrix2D mout = (DenseDoubleMatrix2D) m.mtimes(m);
        queue.finish();
        
        //System.out.println("m = \n" + m);
        //System.out.println("mout = \n" + mout);
        
		//if (la instanceof CLLinearAlgebra)
		//	((CLLinearAlgebra)la).queue.finish();
		//dmout.write((DoubleBuffer)mout.read());

        back = read(mout);
        //for (int i = 0, cap = (int)back.getValidElements(); i < cap; i++)
        //    System.out.println(back.get(i));

		assertEquals(0, mout.getDouble(0, 1), 0);
		assertEquals(0, mout.getDouble(1, 0), 0);

        assertEquals(1, mout.getDouble(0, 0), 0);
		assertEquals(1, mout.getDouble(1, 1), 0);
        assertEquals(1, mout.getDouble(2, 2), 0);

		write(new double[] { 1, 0, 0}, v);
		DenseDoubleMatrix2D vout = (DenseDoubleMatrix2D)m.mtimes(v);
		//System.out.println(v);
		//System.out.println(vout);

		assertEquals(0, vout.getDouble(0, 0), 0);
        assertEquals(0, vout.getDouble(1, 0), 0);
		assertEquals(1, vout.getDouble(2, 0), 0);
	}

    @Test
    public void testContainsDouble() {
        CLDenseDoubleMatrix2D m = (CLDenseDoubleMatrix2D)MatrixFactory.dense(2, 2);
        int row = 1, column = 1;
        m.setDouble(1.1, row, column);
        assertEquals(1.1, m.getDouble(row, column), 0.0);
        assertTrue(m.containsDouble(1.1));
        assertTrue(!m.containsDouble(2.0));
    }


    @Test
    public void testContainsFloat() {
        CLDenseFloatMatrix2D m = new CLDenseFloatMatrix2D(2, 2);
        int row = 1, column = 1;
        m.setFloat(1.1f, row, column);
        assertEquals(1.1f, m.getFloat(row, column), 0.0);
        assertTrue(m.containsFloat(1.1f));
        assertTrue(!m.containsFloat(2.0f));
    }
    
    @Test
    public void testClearFloat() {
        CLDenseFloatMatrix2D m = new CLDenseFloatMatrix2D(2, 2);
        int row = 0, column = 1;
        m.setFloat(1.1f, row, column);
        assertEquals(1.1f, m.getFloat(row, column), 0.0);
        m.clear();
        assertEquals(0f, m.getFloat(row, column), 0.0);
    }

    static void sleep(long millis) {
        try { Thread.sleep(millis); } catch (Exception ex) { ex.printStackTrace(); }
    }

}
