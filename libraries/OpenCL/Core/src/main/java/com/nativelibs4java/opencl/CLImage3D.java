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
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_IMAGE_DEPTH;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_IMAGE_SLICE_PITCH;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import com.nativelibs4java.opencl.library.OpenCLLibrary.cl_mem;
import com.nativelibs4java.util.Pair;

import static org.bridj.Pointer.*;
/**
 * OpenCL 3D Image Memory Object<br/>
 * see {@link CLContext#createImage3D(com.nativelibs4java.opencl.CLMem.Usage, com.nativelibs4java.opencl.CLImageFormat, long, long, long) }
 * see {@link CLContext#createImage3D(com.nativelibs4java.opencl.CLMem.Usage, com.nativelibs4java.opencl.CLImageFormat, long, long, long, long, long) }
 * see {@link CLContext#createImage3D(com.nativelibs4java.opencl.CLMem.Usage, com.nativelibs4java.opencl.CLImageFormat, long, long, long, long, long, java.nio.Buffer, boolean) }
 * see {@link CLContext#createImage3DFromGLTexture3D(com.nativelibs4java.opencl.CLMem.Usage, int, int) } 
 * @author Olivier Chafik
 */
public class CLImage3D extends CLImage2D {
	CLImage3D(CLContext context, cl_mem entity, CLImageFormat format) {
        super(context, entity, format);
	}

	/**
	 * Return size in bytes of a 2D slice for this 3D image object. <br/>
	 */
	@InfoName("CL_IMAGE_SLICE_PITCH")
	public long getSlicePitch() {
		return infos.getIntOrLong(getEntity(), CL_IMAGE_SLICE_PITCH);
	}

	/**
	 * Return depth of the image in pixels.
	 */
	@InfoName("CL_IMAGE_DEPTH")
	public long getDepth() {
		return infos.getIntOrLong(getEntity(), CL_IMAGE_DEPTH);
	}

	public CLEvent read(CLQueue queue, long minX, long minY, long minZ, long width, long height, long depth, long rowPitch, long slicePitch, Buffer out, boolean blocking, CLEvent... eventsToWaitFor) {
		return read(queue, pointerToSizeTs(minX, minY, minZ), pointerToSizeTs(width, height, depth), rowPitch, slicePitch, out, blocking, eventsToWaitFor);
	}

	public CLEvent write(CLQueue queue, long minX, long minY, long minZ, long width, long height, long depth, long rowPitch, long slicePitch, Buffer in, boolean blocking, CLEvent... eventsToWaitFor) {
		return write(queue, pointerToSizeTs(minX, minY, minZ), pointerToSizeTs(width, height, depth), rowPitch, slicePitch, in, blocking, eventsToWaitFor);
	}

    public ByteBuffer map(CLQueue queue, MapFlags flags, CLEvent... eventsToWaitFor) {
		return map(queue, flags, 0, 0, 0, getWidth(), getHeight(), getDepth(), getWidth(), getHeight(), true, eventsToWaitFor);
    }
	public Pair<ByteBuffer, CLEvent> mapLater(CLQueue queue, MapFlags flags, CLEvent... eventsToWaitFor) {
		return map(queue, flags, pointerToSizeTs(0, 0, 0), pointerToSizeTs(getWidth(), getHeight(), getDepth()), getWidth(), getHeight(), true, eventsToWaitFor);
    }

    public ByteBuffer map(CLQueue queue, MapFlags flags, long offsetX, long offsetY, long offsetZ, long lengthX, long lengthY, long lengthZ, long rowPitch, long slicePitch, boolean blocking, CLEvent... eventsToWaitFor) {
        return map(queue, flags, pointerToSizeTs(offsetX, offsetY, offsetZ), pointerToSizeTs(lengthX, lengthY, lengthZ), rowPitch, slicePitch, true, eventsToWaitFor).getFirst();
    }
    public Pair<ByteBuffer, CLEvent> mapLater(CLQueue queue, MapFlags flags, long offsetX, long offsetY, long offsetZ, long lengthX, long lengthY, long lengthZ, long rowPitch, long slicePitch, CLEvent... eventsToWaitFor) {
        return map(queue, flags, pointerToSizeTs(offsetX, offsetY, offsetZ), pointerToSizeTs(lengthX, lengthY, lengthZ), rowPitch, slicePitch, true, eventsToWaitFor);
    }

}
