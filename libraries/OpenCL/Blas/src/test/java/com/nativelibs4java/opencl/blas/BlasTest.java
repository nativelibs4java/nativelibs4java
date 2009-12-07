package com.nativelibs4java.opencl.blas;
import com.nativelibs4java.blas.LinearAlgebra;
import com.nativelibs4java.blas.Matrix;
import com.nativelibs4java.blas.java.DefaultLinearAlgebra;
import com.nativelibs4java.blas.java.DefaultMatrix;
import com.nativelibs4java.opencl.*;
import static com.nativelibs4java.opencl.JavaCL.*;
//import java.util.*;
import java.io.IOException;
import java.nio.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.junit.Assert.*;
import org.junit.*;
public class BlasTest {
	@Test
	public void testMultJava() {
        testMult(DefaultLinearAlgebra.getInstance());
    }
    
    public void testMult(LinearAlgebra la) {
		Matrix m = la.newMatrix(2, 2);
		Matrix mout = la.newMatrix(2, 2);
		Matrix v = la.newMatrix(2, 1);
        m.write(DoubleBuffer.wrap(new double[] { 0, 1, 1, 0 }));
		m.multiply(m, mout);

        //System.out.println(m);
		//System.out.println(mout);

		DefaultMatrix dmout = new DefaultMatrix(mout.getRows(), mout.getColumns());
		dmout.write((DoubleBuffer)mout.read());
		//if (la instanceof CLLinearAlgebra)
		//	((CLLinearAlgebra)la).queue.finish();
		//dmout.write((DoubleBuffer)mout.read());

		assertEquals(0, dmout.get(0, 1), 0);
		assertEquals(0, dmout.get(1, 0), 0);
        assertEquals(1, dmout.get(0, 0), 0);
		assertEquals(1, dmout.get(1, 1), 0);

		v.write(DoubleBuffer.wrap(new double[] { 1, 0 }));
		Matrix vout = la.newMatrix(2, 1);
		m.multiply(v, vout);
		//System.out.println(v);
		//System.out.println(vout);

        DefaultMatrix dvout = new DefaultMatrix(vout.getRows(), vout.getColumns());
		dvout.write((DoubleBuffer)vout.read());

		assertEquals(0, dvout.get(0, 0), 0);
		assertEquals(1, dvout.get(1, 0), 0);
	}
}
