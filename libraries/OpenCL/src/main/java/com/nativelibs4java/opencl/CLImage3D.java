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
 *
 * @author ochafik
 */
public class CLImage3D extends CLImage {
	public CLImage3D(CLContext context, cl_mem entity) {
        super(context, entity);
	}

	/**
	 * Return size in bytes of a 2D slice for the 3D image object given by image. <br/>
	 * For a 2D image object this value will be 0.
	 */
	@InfoName("CL_IMAGE_SLICE_PITCH")
	public long getImageSlicePitch() {
		return infos.getNativeLong(get(), CL_IMAGE_SLICE_PITCH);
	}

	/**
	 * Return depth of the image in pixels. For a 2D image, depth = 0.
	 */
	@InfoName("CL_IMAGE_DEPTH")
	public long getImageDepth() {
		return infos.getNativeLong(get(), CL_IMAGE_DEPTH);
	}
}
