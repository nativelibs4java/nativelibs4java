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

/**
 * OpenCL Memory Buffer Object with Short values.<br/>
 * @see CLContext#createShortBuffer(com.nativelibs4java.opencl.CLMem.Usage, long)
 * @see CLContext#createShortBuffer(com.nativelibs4java.opencl.CLMem.Usage, java.nio.ShortBuffer, boolean)
 * @author Olivier Chafik
 */
public class CLShortBuffer extends CLBuffer<ShortBuffer> {
	CLShortBuffer(CLContext context, long byteCount, cl_mem entity, Buffer buffer) {
        super(context, byteCount, entity, buffer, 2);
	}

    @Override
    protected ShortBuffer typedBuffer(ByteBuffer b) {
        return b.asShortBuffer();
    }

    @Override
    protected void put(ShortBuffer out, ShortBuffer in) {
        out.put(in);
    }

    @Override
    public Class<ShortBuffer> typedBufferClass() {
        return ShortBuffer.class;
    }


}
