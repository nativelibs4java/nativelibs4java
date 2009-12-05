/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.blas.opencl;

import com.nativelibs4java.blas.Matrix;
import com.nativelibs4java.blas.Vector;
import com.nativelibs4java.blas.java.DefaultVector;
import com.nativelibs4java.opencl.CLDoubleBuffer;
import com.nativelibs4java.opencl.CLMem;
import com.nativelibs4java.opencl.CLMem.MapFlags;
import com.nativelibs4java.opencl.CLMem.Usage;
import java.nio.DoubleBuffer;

/**
 *
 * @author Olivier
 */
public class CLVector extends CLDoubleData implements Vector<CLMatrix, CLVector, DoubleBuffer> {

    public CLVector(CLLinearAlgebra al, int size) {
        super(al, size);
    }

	@Override
	public CLVector dot(CLVector other, CLVector out) {
		if (out == null)
			out = al.newVector(1);
		else if (out.size() != 1)
			throw new IllegalArgumentException("Size of output vector for dot operation must be 1");
		//else
		//	out.waitForWrite();

		al.dot(this, other, out);
		return out;
	}


	public DefaultVector toDefaultVector() {
		DefaultVector m = new DefaultVector(size);
		m.write(read());
		return m;
	}

	@Override
	public String toString() {
		return toDefaultVector().toString();
	}


}
