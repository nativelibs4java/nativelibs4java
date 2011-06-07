package com.nativelibs4java.opencl.blas.ujmp;

import java.io.IOException;
import org.bridj.Pointer;
import static org.bridj.Pointer.*;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLPlatform.DeviceFeature;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.JavaCL;
import com.nativelibs4java.opencl.blas.CLKernels;
import static com.nativelibs4java.opencl.blas.ujmp.MatrixUtils.read;
import static com.nativelibs4java.opencl.blas.ujmp.MatrixUtils.write;
import com.nativelibs4java.util.Pair;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.ujmp.core.Matrix;
import org.ujmp.core.MatrixFactory;
import org.ujmp.core.calculation.Calculation.Ret;
import org.ujmp.core.doublematrix.DenseDoubleMatrix2D;
import org.ujmp.core.doublematrix.impl.DefaultDenseDoubleMatrix2D;
import org.ujmp.core.floatmatrix.DenseFloatMatrix2D;
import org.ujmp.core.floatmatrix.FloatMatrix2D;
import org.ujmp.core.floatmatrix.impl.DefaultDenseFloatMatrix2D;
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
	public void testMultFloat() {
        
        DenseFloatMatrix2D m = new CLDenseFloatMatrix2D(3, 3);
        DenseFloatMatrix2D v = new CLDenseFloatMatrix2D(3, 1);
        //CLBuffer<Float> buffer = ((CLDenseFloatMatrix2D)m).getBuffer();
        CLQueue queue = ((CLDenseFloatMatrix2D)m).getImpl().getQueue();
        //System.out.println("Context = " + buffer.getContext());
        
        float[] min = new float[] { 0, 0, 1, 0, 1, 0, 1, 0, 0 };
        write(min, m);
        Pointer<Float> back = read(m);
        for (int i = 0, cap = (int)back.getValidElements(); i < cap; i++) {
            assertEquals(min[i], back.get(i), 0);
            //System.out.println(back.get(i));
        }
        
        queue.finish();
        DenseFloatMatrix2D mout = (DenseFloatMatrix2D) m.mtimes(m);
        queue.finish();
        
        //System.out.println("m = \n" + m);
        //System.out.println("mout = \n" + mout);
        
		//if (la instanceof CLLinearAlgebra)
		//	((CLLinearAlgebra)la).queue.finish();
		//dmout.write((FloatBuffer)mout.read());

        back = read(mout);
        //for (int i = 0, cap = (int)back.getValidElements(); i < cap; i++)
        //    System.out.println(back.get(i));

		assertEquals(0, mout.getFloat(0, 1), 0);
		assertEquals(0, mout.getFloat(1, 0), 0);

        assertEquals(1, mout.getFloat(0, 0), 0);
		assertEquals(1, mout.getFloat(1, 1), 0);
        assertEquals(1, mout.getFloat(2, 2), 0);

		write(new float[] { 1, 0, 0}, v);
		DenseFloatMatrix2D vout = (DenseFloatMatrix2D)m.mtimes(v);
		//System.out.println(v);
		//System.out.println(vout);

		assertEquals(0, vout.getFloat(0, 0), 0);
        assertEquals(0, vout.getFloat(1, 0), 0);
		assertEquals(1, vout.getFloat(2, 0), 0);
	}

    @Test
    public void testContainsDouble() throws IOException {
        
        CLKernels.setInstance(new CLKernels());
        
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
