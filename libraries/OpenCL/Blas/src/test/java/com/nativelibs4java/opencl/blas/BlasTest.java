package com.nativelibs4java.opencl.blas;
import com.nativelibs4java.blas.Data.Usage;
import com.nativelibs4java.blas.LinearAlgebra;
import com.nativelibs4java.blas.Matrix;
import com.nativelibs4java.blas.Vector;
import com.nativelibs4java.blas.java.JavaLinearAlgebra;
import com.nativelibs4java.blas.opencl.CLLinearAlgebra;
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
        testMult(new JavaLinearAlgebra());
    }
    @Test
	public void testMultCL() {
        try {
            testMult(new CLLinearAlgebra());
        } catch (Exception ex) {
            Logger.getLogger(BlasTest.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
            assertTrue(ex.toString(), false);
        }
    }
    public void testMult(LinearAlgebra la) {
		Matrix m = la.newMatrix(2, 2);
		Matrix mout = la.newMatrix(2, 2);
		Vector v = la.newVector(2);
        m.attach(Usage.Write);
		m.set(0, 0, 0);
		m.set(1, 1, 0);
		m.set(0, 1, 1);
		m.set(1, 0, 1);
		m.detach();

        v.attach(Usage.Write);
		v.set(0, 1);
		v.detach();

		Vector vout = la.newVector(2);
		la.multiplyNow(m, v, vout);

        vout.attach(Usage.Read);
		assertEquals(0, vout.get(0), 0);
		assertEquals(1, vout.get(1), 0);
		vout.detach();

		la.multiplyNow(m, m, mout);

        mout.attach(Usage.Read);
		assertEquals(0, mout.get(0, 1), 0);
		assertEquals(0, mout.get(1, 0), 0);
        assertEquals(1, mout.get(0, 0), 0);
		assertEquals(1, mout.get(1, 1), 0);
		mout.detach();
	}
}
