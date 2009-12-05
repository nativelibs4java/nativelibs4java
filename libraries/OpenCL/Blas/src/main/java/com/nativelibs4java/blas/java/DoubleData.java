/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.blas.java;

import com.nativelibs4java.blas.Data;
import com.nativelibs4java.util.NIOUtils;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;

/**
 *
 * @author ochafik
 */
public class DoubleData implements Data<DoubleBuffer> {
	protected final DoubleBuffer data;
	protected final int size;
	public DoubleData(int size) {
		data = NIOUtils.directDoubles(size);
		this.size = size;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public void read(DoubleBuffer out) {
		int pos = out.position();
		out.put(data);
		data.rewind();
		out.position(pos);
	}

	@Override
	public void write(DoubleBuffer in) {
		int pos = in.position();
		//getDoubleBuffer(out)
		data.put(in);//getDoubleBuffer(in));
		data.rewind();
		in.position(pos);
	}

	private DoubleBuffer getDoubleBuffer(Buffer out) {
		if (out instanceof DoubleBuffer)
			return (DoubleBuffer)out;
		if (out instanceof ByteBuffer)
			return ((ByteBuffer)out).asDoubleBuffer();
		throw new IllegalArgumentException("Cannot convert a " + out.getClass().getName() + " to a DoubleBuffer");
	}

	@Override
	public DoubleBuffer read() {
		DoubleBuffer b = NIOUtils.directDoubles(size());
		read(b);
		return b;
	}
}
