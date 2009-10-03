/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
		return infos.getNativeLong(get(), CL_IMAGE_SLICE_PITCH);
	}

	/**
	 * Return depth of the image in pixels.
	 */
	@InfoName("CL_IMAGE_DEPTH")
	public long getDepth() {
		return infos.getNativeLong(get(), CL_IMAGE_DEPTH);
	}

	public CLEvent read(CLQueue queue, long minX, long minY, long minZ, long width, long height, long depth, long rowPitch, long slicePitch, Buffer out, boolean blocking, CLEvent... eventsToWaitFor) {
		return read(queue, new long[] {minX, minY, minZ}, new long[] {width, height, depth}, rowPitch, slicePitch, out, blocking, eventsToWaitFor);
	}

	public CLEvent write(CLQueue queue, long minX, long minY, long minZ, long width, long height, long depth, long rowPitch, long slicePitch, Buffer in, boolean blocking, CLEvent... eventsToWaitFor) {
		return write(queue, new long[] {minX, minY, minZ}, new long[] {width, height, depth}, rowPitch, slicePitch, in, blocking, eventsToWaitFor);
	}

}
