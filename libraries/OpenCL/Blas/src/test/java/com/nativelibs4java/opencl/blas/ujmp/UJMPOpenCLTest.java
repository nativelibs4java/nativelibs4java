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
import static org.junit.Assert.*;

import static java.lang.Math.*;

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
    CLDenseDoubleMatrix2DFactory doubleFactory = new CLDenseDoubleMatrix2DFactory();
    CLDenseFloatMatrix2DFactory floatFactory = new CLDenseFloatMatrix2DFactory();
    
    @Before
    public void installUJMPCL() {
        try {
            MatrixMapper.getInstance().setDenseFloatMatrix2DClassName(CLDenseFloatMatrix2D.class.getName());
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
	public void testOp2() {
        
        DenseFloatMatrix2D a = floatFactory.dense(2, 2);
        DenseFloatMatrix2D b = floatFactory.dense(2, 2);
        
        float[] fa = new float[] { 10, 20, 30, 40 }, fb = new float[] { 1, 2, 3, 4 };
        write(fa, a.getColumnCount(), a);
        write(fb, b.getColumnCount(), b);
        
        assertArrayEquals("failed plus", new float[] { 11, 22, 33, 44 }, read((FloatMatrix2D)a.plus(b), a.getColumnCount()).getFloats(), 0);
        assertArrayEquals("failed minus", new float[] { 9, 18, 27, 36 }, read((FloatMatrix2D)a.minus(b), a.getColumnCount()).getFloats(), 0);
        assertArrayEquals("failed times", new float[] { 10, 40, 90, 160 }, read((FloatMatrix2D)a.times(b), b.getColumnCount()).getFloats(), 0);
        assertArrayEquals("failed divide", new float[] { 10, 10, 10, 10 }, read((FloatMatrix2D)a.divide(b), a.getColumnCount()).getFloats(), 0);
        
        assertArrayEquals("failed scalar plus", new float[] { 11, 21, 31, 41 }, read((FloatMatrix2D)a.plus(1), a.getColumnCount()).getFloats(), 0);
        assertArrayEquals("failed scalar minus", new float[] { 9, 19, 29, 39 }, read((FloatMatrix2D)a.minus(1), a.getColumnCount()).getFloats(), 0);
        assertArrayEquals("failed scalar times", new float[] { 20, 40, 60, 80 }, read((FloatMatrix2D)a.times(2), b.getColumnCount()).getFloats(), 0);
        assertArrayEquals("failed scalar divide", new float[] { 1, 2, 3, 4 }, read((FloatMatrix2D)a.divide(10), a.getColumnCount()).getFloats(), 0);
        
        assertArrayEquals("failed sin", new float[] { (float)sin(fa[0]), (float)sin(fa[1]), (float)sin(fa[2]), (float)sin(fa[3]) }, 
                read((FloatMatrix2D)a.sin(Ret.NEW), a.getColumnCount()).getFloats(), 0.0001f);
        assertArrayEquals("failed cos", new float[] { (float)cos(fa[0]), (float)cos(fa[1]), (float)cos(fa[2]), (float)cos(fa[3]) }, 
                read((FloatMatrix2D)a.cos(Ret.NEW), a.getColumnCount()).getFloats(), 0.0001f);
        assertArrayEquals("failed tan", new float[] { (float)tan(fa[0]), (float)tan(fa[1]), (float)tan(fa[2]), (float)tan(fa[3]) }, 
                read((FloatMatrix2D)a.tan(Ret.NEW), a.getColumnCount()).getFloats(), 0.0001f);
        
    }
    
    @Test
	public void testMultFloat() {
        
        DenseFloatMatrix2D m = floatFactory.dense(3, 3);
        DenseFloatMatrix2D v = floatFactory.dense(3, 1);
        //CLBuffer<Float> buffer = ((CLDenseFloatMatrix2D)m).getBuffer();
        CLQueue queue = ((CLDenseFloatMatrix2D)m).getImpl().getQueue();
        //System.out.println("Context = " + buffer.getContext());
        
        float[] min = new float[] { 0, 0, 1, 0, 1, 0, 1, 0, 0 };
        write(min, m.getColumnCount(), m);
        Pointer<Float> back = read(m, m.getColumnCount());
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

        back = read(mout, mout.getColumnCount());
        //for (int i = 0, cap = (int)back.getValidElements(); i < cap; i++)
        //    System.out.println(back.get(i));

		assertEquals(0, mout.getFloat(0, 1), 0);
		assertEquals(0, mout.getFloat(1, 0), 0);

        assertEquals(1, mout.getFloat(0, 0), 0);
		assertEquals(1, mout.getFloat(1, 1), 0);
        assertEquals(1, mout.getFloat(2, 2), 0);

		write(new float[] { 1, 0, 0}, v.getColumnCount(), v);
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
        CLDenseFloatMatrix2D m = floatFactory.dense(2, 2);
        int row = 1, column = 1;
        m.setFloat(1.1f, row, column);
        assertEquals(1.1f, m.getFloat(row, column), 0.0);
        assertTrue(m.containsFloat(1.1f));
        assertTrue(!m.containsFloat(2.0f));
    }
    
    @Test
    public void testClearFloat() {
        CLDenseFloatMatrix2D m = floatFactory.dense(2, 2);
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
