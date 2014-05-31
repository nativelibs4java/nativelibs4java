#parse("main/Header.vm")
package com.nativelibs4java.opencl;
import static com.nativelibs4java.opencl.library.IOpenCLLibrary.CL_IMAGE_DEPTH;
import static com.nativelibs4java.opencl.library.IOpenCLLibrary.CL_IMAGE_SLICE_PITCH;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import com.nativelibs4java.opencl.library.IOpenCLLibrary.cl_mem;
import com.nativelibs4java.util.Pair;
import org.bridj.Pointer;

import static org.bridj.Pointer.*;
import org.bridj.SizeT;
/**
 * OpenCL 3D Image Memory Object<br/>
 * see {@link CLContext#createImage3D(com.nativelibs4java.opencl.CLMem.Usage, com.nativelibs4java.opencl.CLImageFormat, long, long, long) }
 * see {@link CLContext#createImage3D(com.nativelibs4java.opencl.CLMem.Usage, com.nativelibs4java.opencl.CLImageFormat, long, long, long, long, long) }
 * see {@link CLContext#createImage3D(com.nativelibs4java.opencl.CLMem.Usage, com.nativelibs4java.opencl.CLImageFormat, long, long, long, long, long, java.nio.Buffer, boolean) }
 * see {@link CLContext#createImage3DFromGLTexture3D(com.nativelibs4java.opencl.CLMem.Usage, int, int) } 
 * @author Olivier Chafik
 */
public class CLImage3D extends CLImage2D {
	CLImage3D(CLContext context, long entityPeer, CLImageFormat format) {
        super(context, entityPeer, format);
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

	@Override
	protected long[] getDimensions() {
		return new long[] { getWidth(), getHeight(), getDepth() };
	}

    @Override
    protected Pointer<SizeT> writeOrigin(long[] origin, ReusablePointer out) {
        assert(origin.length == 3);
        return out.pointerToSizeTs(origin);
    }
    
    @Override
    protected Pointer<SizeT> writeRegion(long[] region, ReusablePointer out) {
        assert(region.length == 3);
        return out.pointerToSizeTs(region);
    }

	/**
#documentEventsToWaitForAndReturn()
	 */
	public CLEvent read(CLQueue queue, long minX, long minY, long minZ, long width, long height, long depth, long rowPitch, long slicePitch, Buffer out, boolean blocking, CLEvent... eventsToWaitFor) {
		return read(queue, pointerToSizeTs(minX, minY, minZ), pointerToSizeTs(width, height, depth), rowPitch, slicePitch, out, blocking, eventsToWaitFor);
	}

	/**
#documentEventsToWaitForAndReturn()
	 */
	public CLEvent write(CLQueue queue, long minX, long minY, long minZ, long width, long height, long depth, long rowPitch, long slicePitch, Buffer in, boolean blocking, CLEvent... eventsToWaitFor) {
		return write(queue, pointerToSizeTs(minX, minY, minZ), pointerToSizeTs(width, height, depth), rowPitch, slicePitch, in, blocking, eventsToWaitFor);
	}

    public ByteBuffer map(CLQueue queue, MapFlags flags, CLEvent... eventsToWaitFor) {
		return map(queue, flags, 0, 0, 0, getWidth(), getHeight(), getDepth(), getWidth(), getHeight(), true, eventsToWaitFor);
    }
	/**
#documentEventsToWaitForAndPairReturn("mapped data")
	 */
	public Pair<ByteBuffer, CLEvent> mapLater(CLQueue queue, MapFlags flags, CLEvent... eventsToWaitFor) {
		return map(queue, flags, pointerToSizeTs(0, 0, 0), pointerToSizeTs(getWidth(), getHeight(), getDepth()), getWidth(), getHeight(), true, eventsToWaitFor);
    }

    public ByteBuffer map(CLQueue queue, MapFlags flags, long offsetX, long offsetY, long offsetZ, long lengthX, long lengthY, long lengthZ, long rowPitch, long slicePitch, boolean blocking, CLEvent... eventsToWaitFor) {
        return map(queue, flags, pointerToSizeTs(offsetX, offsetY, offsetZ), pointerToSizeTs(lengthX, lengthY, lengthZ), rowPitch, slicePitch, true, eventsToWaitFor).getFirst();
    }
    /**
#documentEventsToWaitForAndPairReturn("mapped data")
	 */
	public Pair<ByteBuffer, CLEvent> mapLater(CLQueue queue, MapFlags flags, long offsetX, long offsetY, long offsetZ, long lengthX, long lengthY, long lengthZ, long rowPitch, long slicePitch, CLEvent... eventsToWaitFor) {
        return map(queue, flags, pointerToSizeTs(offsetX, offsetY, offsetZ), pointerToSizeTs(lengthX, lengthY, lengthZ), rowPitch, slicePitch, true, eventsToWaitFor);
    }

}
