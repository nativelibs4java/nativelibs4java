/*
 * JavaCL - Java API and utilities for OpenCL
 * http://javacl.googlecode.com/
 *
 * Copyright (c) 2009-2010, Olivier Chafik (http://ochafik.free.fr/)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Olivier Chafik nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY OLIVIER CHAFIK AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.nativelibs4java.opencl;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;
import java.nio.*;

import com.nativelibs4java.opencl.library.OpenCLLibrary.cl_mem;

/**
 * OpenCL Memory Buffer Object with Int values.<br/>
 * @see CLContext#createIntBuffer(com.nativelibs4java.opencl.CLMem.Usage, long)
 * @see CLContext#createIntBuffer(com.nativelibs4java.opencl.CLMem.Usage, java.nio.IntBuffer, boolean)
 * @author Olivier Chafik
 */
public class CLIntBuffer extends CLBuffer<IntBuffer> {
	CLIntBuffer(CLContext context, long byteCount, cl_mem entity, Buffer buffer) {
        super(context, byteCount, entity, buffer, 4);
	}
	
	@Override
	protected CLBuffer<IntBuffer> createBuffer(cl_mem mem) {
		return new CLIntBuffer(getContext(), -1, mem, null);
	}

    @Override
    protected IntBuffer typedBuffer(ByteBuffer b) {
        return b.asIntBuffer();
    }

    @Override
    protected void put(IntBuffer out, IntBuffer in) {
        out.put(in);
    }

    @Override
    public Class<IntBuffer> typedBufferClass() {
        return IntBuffer.class;
    }
}
