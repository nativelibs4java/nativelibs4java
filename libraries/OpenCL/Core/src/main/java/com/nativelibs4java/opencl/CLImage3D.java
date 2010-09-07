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
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_IMAGE_DEPTH;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_IMAGE_SLICE_PITCH;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import com.nativelibs4java.opencl.library.OpenCLLibrary.cl_mem;
import com.nativelibs4java.util.Pair;

import static org.bridj.Pointer.*;
/**
 * OpenCL 3D Image Memory Object<br/>
 * @see CLContext#createImage3D(com.nativelibs4java.opencl.CLMem.Usage, com.nativelibs4java.opencl.CLImageFormat, long, long, long)
 * @see CLContext#createImage3D(com.nativelibs4java.opencl.CLMem.Usage, com.nativelibs4java.opencl.CLImageFormat, long, long, long, long, long)
 * @see CLContext#createImage3D(com.nativelibs4java.opencl.CLMem.Usage, com.nativelibs4java.opencl.CLImageFormat, long, long, long, long, long, java.nio.Buffer, boolean)
 * @see CLContext#createImage3DFromGLTexture3D(com.nativelibs4java.opencl.CLMem.Usage, int, int) 
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
