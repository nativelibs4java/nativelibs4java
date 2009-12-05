/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.blas.java;

import com.nativelibs4java.blas.Vector;
import com.nativelibs4java.util.NIOUtils;
import com.ochafik.util.string.StringUtils;
import java.nio.DoubleBuffer;

/**
 *
 * @author Olivier
 */
public class DefaultVector extends DoubleData implements Vector<DefaultMatrix, DefaultVector, DoubleBuffer> {

    public DefaultVector(int size) {
        super(size);
    }

    public double get(int i) {
        return data.get(i);
    }

    public void set(int i, double value) {
        data.put(i, value);
    }

	@Override
	public DefaultVector dot(DefaultVector other, DefaultVector out) {
		if (out == null)
			out = new DefaultVector(1);
		else if (out.size() != 1)
			throw new IllegalArgumentException("Size of output vector for dot operation must be 1");

		double total = 0;
		for (int i = size; i-- != 0;)
			total += get(i) * other.get(i);
		out.data.put(0, total);
		//out.write(DoubleBuffer.wrap(new double[] { total }));
		return out;
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder("{");
		for (int i = 0; i < size; i++) {
			if (i != 0)
				b.append(", ");
			b.append(get(i));
		}
		b.append("}");
		return b.toString();
	}



}
