/*
	Copyright (c) 2009 Olivier Chafik (http://ochafik.free.fr/)
	
	This file is part of OpenCL4Java (http://code.google.com/p/nativelibs4java/wiki/OpenCL).
	
	OpenCL4Java is free software: you can redistribute it and/or modify
	it under the terms of the GNU Lesser General Public License as published by
	the Free Software Foundation, either version 2.1 of the License, or
	(at your option) any later version.
	
	OpenCL4Java is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU Lesser General Public License for more details.
	
	You should have received a copy of the GNU Lesser General Public License
	along with OpenCL4Java.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.nativelibs4java.opencl;
import com.nativelibs4java.opencl.library.OpenCLLibrary;
import com.ochafik.util.listenable.Pair;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;
import com.sun.jna.*;
import com.sun.jna.ptr.*;
import java.nio.*;
import static com.nativelibs4java.opencl.JavaCL.*;
import static com.nativelibs4java.opencl.CLException.*;
import static com.nativelibs4java.util.JNAUtils.*;
import static com.nativelibs4java.util.NIOUtils.*;

/**
 * OpenCL Memory Buffer Object with Double values.<br/>
 * @see CLContext#createDoubleBuffer(com.nativelibs4java.opencl.CLMem.Usage, long)
 * @see CLContext#createDoubleBuffer(com.nativelibs4java.opencl.CLMem.Usage, java.nio.DoubleBuffer, boolean)
 * @author Olivier Chafik
 */
public class CLDoubleBuffer extends CLBuffer<DoubleBuffer> {
	CLDoubleBuffer(CLContext context, long byteCount, cl_mem entity, Buffer buffer) {
        super(context, byteCount, entity, buffer);
	}
	static final int ELEMENT_SIZE = 8;
    @Override
	public int getElementSize() {
        return ELEMENT_SIZE;
    }
	@Override
	public long getElementCount() {
        return getByteCount() / ELEMENT_SIZE;
    }
	protected static Pair<DoubleBuffer, CLEvent> as(Pair<ByteBuffer, CLEvent> p) {
		return new Pair<DoubleBuffer, CLEvent>(p.getFirst().asDoubleBuffer(), p.getSecond());
	}

	@Override
	public DoubleBuffer map(CLQueue queue, MapFlags flags, CLEvent... eventsToWaitFor) {
		return map(queue, flags, 0, getByteCount(), true, eventsToWaitFor).getFirst().asDoubleBuffer();
    }
	@Override
	public DoubleBuffer map(CLQueue queue, MapFlags flags, long offset, long length, CLEvent... eventsToWaitFor) {
		return map(queue, flags, offset * ELEMENT_SIZE, length * ELEMENT_SIZE, true, eventsToWaitFor).getFirst().asDoubleBuffer();
    }
	@Override
	public Pair<DoubleBuffer, CLEvent> mapLater(CLQueue queue, MapFlags flags, CLEvent... eventsToWaitFor) {
		return as(map(queue, flags, 0, getByteCount(), false, eventsToWaitFor));
    }
	@Override
	public Pair<DoubleBuffer, CLEvent> mapLater(CLQueue queue, MapFlags flags, long offset, long length, CLEvent... eventsToWaitFor) {
		Pair<ByteBuffer, CLEvent> p = map(queue, flags, offset * ELEMENT_SIZE, length * ELEMENT_SIZE, false, eventsToWaitFor);
		return new Pair<DoubleBuffer, CLEvent>(p.getFirst().asDoubleBuffer(), p.getSecond());
    }
	@Override
	public DoubleBuffer read(CLQueue queue, CLEvent... eventsToWaitFor) {
		return readBytes(queue, eventsToWaitFor).asDoubleBuffer();
	}
	@Override
	public DoubleBuffer read(CLQueue queue, long offset, long length, CLEvent... eventsToWaitFor) {
		return readBytes(queue, offset * ELEMENT_SIZE, length * ELEMENT_SIZE, eventsToWaitFor).asDoubleBuffer();
	}
}
