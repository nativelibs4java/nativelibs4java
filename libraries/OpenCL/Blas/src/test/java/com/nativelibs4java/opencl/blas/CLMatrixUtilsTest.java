package com.nativelibs4java.opencl.blas;

import static com.nativelibs4java.opencl.blas.CLMatrixUtils.roundUp;
import static org.junit.Assert.*;

import org.junit.Test;
/**
 *
 * @author ochafik
 */

public class CLMatrixUtilsTest {
    
    @Test
	public void testRoundUp() {
        assertEquals(0, roundUp(0, 16));
        assertEquals(16, roundUp(1, 16));
        assertEquals(16, roundUp(16, 16));
        assertEquals(32, roundUp(17, 16));
    }
}
