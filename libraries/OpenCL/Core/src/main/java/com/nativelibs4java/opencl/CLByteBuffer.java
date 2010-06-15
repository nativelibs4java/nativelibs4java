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
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;
import java.nio.*;

import com.nativelibs4java.opencl.library.OpenCLLibrary.cl_mem;

/**
 * OpenCL Memory Buffer Object.<br/>
 * A buffer object stores a one-dimensional collection of elements.<br/>
 * Elements of a buffer object can be a scalar data type (such as an int, float), vector data type, or a user-defined structure.<br/>
 * @see CLContext#createByteBuffer(com.nativelibs4java.opencl.CLMem.Usage, long)
 * @see CLContext#createByteBuffer(com.nativelibs4java.opencl.CLMem.Usage, java.nio.ByteBuffer, boolean)
 * @author Olivier Chafik
 */
public class CLByteBuffer extends CLBuffer<ByteBuffer> {
	CLByteBuffer(CLContext context, long byteCount, cl_mem entity, Buffer buffer) {
        super(context, byteCount, entity, buffer, 1);
	}

	@Override
	protected CLBuffer<ByteBuffer> createBuffer(cl_mem mem) {
		return new CLByteBuffer(getContext(), -1, mem, null);
	}

    @Override 
    public long getElementCount() {
        return getByteCount();
    }

    @Override
    protected ByteBuffer typedBuffer(ByteBuffer b) {
        return b;
    }

    @Override
    protected void put(ByteBuffer out, ByteBuffer in) {
        out.put(in);
    }

    @Override
    public Class<ByteBuffer> typedBufferClass() {
        return ByteBuffer.class;
    }
}
