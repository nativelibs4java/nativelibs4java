package com.nativelibs4java.opencl.blas;
import static com.nativelibs4java.opencl.blas.JavaCLBLAS.*;
import com.nativelibs4java.opencl.*;
import static com.nativelibs4java.opencl.JavaCL.*;
//import java.util.*;
import java.nio.*;
import static org.junit.Assert.*;
import org.junit.*;
public class BlasTest {
	@Test
	public void testMult() {
		Matrix m = new Matrix(2, 2);
		Matrix mout = new Matrix(2, 2);
		Vector v = new Vector(2);
		m.set(0, 0, 0);
		m.set(1, 1, 0);
		m.set(0, 1, 1);
		m.set(1, 0, 1);
		v.set(0, 1);
		
		Vector vout = new Vector(2);
		multiply(m, v, vout);
		assertEquals(0, vout.get(0), 0);
		assertEquals(1, vout.get(1), 0);
		
		multiply(m, m, mout);
		
		assertEquals(1, mout.get(0, 0), 0);
		assertEquals(1, mout.get(1, 1), 0);
		assertEquals(0, mout.get(0, 1), 0);
		assertEquals(0, mout.get(1, 0), 0);
		
	}
}
