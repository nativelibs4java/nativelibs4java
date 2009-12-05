/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.blas.opencl;

import com.nativelibs4java.opencl.CLMem;
import com.nativelibs4java.util.NIOUtils;
import java.nio.DoubleBuffer;

/**
 *
 * @author ochafik
 */
public class CLDoubleData extends AbstractCLData<DoubleBuffer> {

	public CLDoubleData(CLLinearAlgebra al, int size) {
		super(al, al.context.createDoubleBuffer(CLMem.Usage.InputOutput, size), size);
    }

	@Override
	public DoubleBuffer read() {
		DoubleBuffer b = NIOUtils.directDoubles(size());
		read(b);
		return b;
	}
    
}
