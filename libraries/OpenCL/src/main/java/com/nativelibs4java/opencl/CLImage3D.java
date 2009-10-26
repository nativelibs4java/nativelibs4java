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
import static com.nativelibs4java.opencl.OpenCL4Java.*;
import static com.nativelibs4java.opencl.CLException.*;
import static com.nativelibs4java.util.JNAUtils.*;
import static com.nativelibs4java.util.NIOUtils.*;

/**
 * OpenCL 3D Image Memory Object<br/>
 * @see CLContext#createInput3D(com.nativelibs4java.opencl.CLImageFormat, long, long, long)
 * @see CLContext#createOutput3D(com.nativelibs4java.opencl.CLImageFormat, long, long, long) 
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
		return infos.getIntOrLong(get(), CL_IMAGE_SLICE_PITCH);
	}

	/**
	 * Return depth of the image in pixels.
	 */
	@InfoName("CL_IMAGE_DEPTH")
	public long getDepth() {
		return infos.getIntOrLong(get(), CL_IMAGE_DEPTH);
	}

	public CLEvent read(CLQueue queue, long minX, long minY, long minZ, long width, long height, long depth, long rowPitch, long slicePitch, Buffer out, boolean blocking, CLEvent... eventsToWaitFor) {
		return read(queue, new long[] {minX, minY, minZ}, new long[] {width, height, depth}, rowPitch, slicePitch, out, blocking, eventsToWaitFor);
	}

	public CLEvent write(CLQueue queue, long minX, long minY, long minZ, long width, long height, long depth, long rowPitch, long slicePitch, Buffer in, boolean blocking, CLEvent... eventsToWaitFor) {
		return write(queue, new long[] {minX, minY, minZ}, new long[] {width, height, depth}, rowPitch, slicePitch, in, blocking, eventsToWaitFor);
	}

}
