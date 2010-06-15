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
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import com.nativelibs4java.opencl.library.OpenCLLibrary.cl_mem;

/**
 * OpenCL Memory Buffer Object with Float values.<br/>
 * @see CLContext#createFloatBuffer(com.nativelibs4java.opencl.CLMem.Usage, long)
 * @see CLContext#createFloatBuffer(com.nativelibs4java.opencl.CLMem.Usage, java.nio.FloatBuffer, boolean)
 * @author Olivier Chafik
 */
public class CLFloatBuffer extends CLBuffer<FloatBuffer> {
	CLFloatBuffer(CLContext context, long byteCount, cl_mem entity, Buffer buffer) {
        super(context, byteCount, entity, buffer, 4);
	}

	@Override
	protected CLBuffer<FloatBuffer> createBuffer(cl_mem mem) {
		return new CLFloatBuffer(getContext(), -1, mem, null);
	}

    @Override
    protected FloatBuffer typedBuffer(ByteBuffer b) {
        return b.asFloatBuffer();
    }

    @Override
    protected void put(FloatBuffer out, FloatBuffer in) {
        out.put(in);
    }

    @Override
    public Class<FloatBuffer> typedBufferClass() {
        return FloatBuffer.class;
    }

}
