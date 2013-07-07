#parse("main/Header.vm")
package com.nativelibs4java.opencl;
import static com.nativelibs4java.opencl.CLException.error;
import static com.nativelibs4java.opencl.JavaCL.CL;
import static com.nativelibs4java.opencl.library.IOpenCLImplementation.CL_FALSE;
import static com.nativelibs4java.opencl.library.IOpenCLImplementation.CL_IMAGE_ELEMENT_SIZE;
import static com.nativelibs4java.opencl.library.IOpenCLImplementation.CL_IMAGE_FORMAT;
import static com.nativelibs4java.opencl.library.IOpenCLImplementation.CL_TRUE;
import static com.nativelibs4java.util.NIOUtils.directCopy;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import com.nativelibs4java.opencl.library.cl_image_format;
import com.nativelibs4java.opencl.library.IOpenCLImplementation.cl_event;
import com.nativelibs4java.opencl.library.IOpenCLImplementation.cl_mem;
import com.nativelibs4java.util.NIOUtils;

import com.nativelibs4java.util.Pair;
import org.bridj.*;
import static org.bridj.Pointer.*;


/**
 * OpenCL Image Memory Object.<br/>
 * An image object is used to store a two- or three- dimensional texture, frame-buffer or image<br/>
 * An image object is used to represent a buffer that can be used as a texture or a frame-buffer. The elements of an image object are selected from a list of predefined image formats.
 * @author Oliveir Chafik
 */
public abstract class CLImage extends CLMem {

	CLImageFormat format;
	CLImage(CLContext context, long entityPeer, CLImageFormat format) {
        super(context, -1, entityPeer);
		this.format = format;
	}



	/**
	 * Return image format descriptor specified when image is created with CLContext.create{Input|Output|InputOutput}{2D|3D}.
	 */
	@InfoName("CL_IMAGE_FORMAT")
	public CLImageFormat getFormat() {
		if (format == null) {
			format = new CLImageFormat(new cl_image_format(infos.getMemory(getEntity(), CL_IMAGE_FORMAT)));
		}
		return format;
	}

	/**
	 * Return size of each element of the image memory object given by image. <br/>
	 * An element is made up of n channels. The value of n is given in cl_image_format descriptor.
	 */
	@InfoName("CL_IMAGE_ELEMENT_SIZE")
	public long getElementSize() {
		return infos.getIntOrLong(getEntity(), CL_IMAGE_ELEMENT_SIZE);
	}

	/**
#documentCallsFunction("clEnqueueReadImage")
     */
    protected CLEvent read(CLQueue queue, Pointer<SizeT> origin, Pointer<SizeT> region, long rowPitch, long slicePitch, Buffer out, boolean blocking, CLEvent... eventsToWaitFor) {
		return read(queue, origin, region, rowPitch, slicePitch, pointerToBuffer(out), blocking, eventsToWaitFor);
	}
	/**
#documentCallsFunction("clEnqueueReadImage")
     */
    protected CLEvent read(CLQueue queue, Pointer<SizeT> origin, Pointer<SizeT> region, long rowPitch, long slicePitch, Pointer<?> out, boolean blocking, CLEvent... eventsToWaitFor) {
		/*if (!out.isDirect()) {

		}*/
		#declareReusablePtrsAndEventsInOutBlockable()
        error(CL.clEnqueueReadImage(
        	queue.getEntity(), 
        	getEntity(),
			blocking ? CL_TRUE : CL_FALSE,
			getPeer(origin),
			getPeer(region),
			rowPitch,
			slicePitch,
			getPeer(out),
			#eventsInOutArgsRaw()
		));
		#returnEventOut("queue")
	}

	/**
#documentCallsFunction("clEnqueueWriteImage")
     */
    protected CLEvent write(CLQueue queue, Pointer<SizeT> origin, Pointer<SizeT> region, long rowPitch, long slicePitch, Buffer in, boolean blocking, CLEvent... eventsToWaitFor) {
		return write(queue, origin, region, rowPitch, slicePitch, pointerToBuffer(in), blocking, eventsToWaitFor);
	}
	/**
#documentCallsFunction("clEnqueueWriteImage")
     */
    protected CLEvent write(CLQueue queue, Pointer<SizeT> origin, Pointer<SizeT> region, long rowPitch, long slicePitch, Pointer<?> in, boolean blocking, CLEvent... eventsToWaitFor) {
		#declareReusablePtrsAndEventsInOutBlockable()
        error(CL.clEnqueueWriteImage(
        	queue.getEntity(),
        	getEntity(),
			blocking ? CL_TRUE : CL_FALSE,
			getPeer(origin),
			getPeer(region),
			rowPitch,
			slicePitch,
			getPeer(in),
			#eventsInOutArgsRaw()
		));
		CLEvent evt = #eventOutWrapper("queue");

		if (!blocking) {
			final Pointer<?> toHold = in;
			evt.invokeUponCompletion(new Runnable() {
				public void run() {
					// Make sure the GC held a reference to directData until the write was completed !
					toHold.order();
				}
			});
		}

		return evt;
	}

    protected Pair<ByteBuffer, CLEvent> map(CLQueue queue, MapFlags flags,
            Pointer<SizeT> offset3, Pointer<SizeT> length3,
            Long imageRowPitch,
            Long imageSlicePitch,
            boolean blocking, CLEvent... eventsToWaitFor)
    {
		#declareReusablePtrsAndEventsInOutBlockable()
		#declarePErr()
        long mappedPeer = CL.clEnqueueMapImage(
            queue.getEntity(), 
            getEntity(), 
            blocking ? CL_TRUE : CL_FALSE,
            flags.value(),
			getPeer(offset3),
            getPeer(length3),
            imageRowPitch == null ? 0 : getPeer(ptrs.sizeT3_1.pointerToSizeTs((long)imageRowPitch)),
            imageSlicePitch == null ? 0 : getPeer(ptrs.sizeT3_1.pointerToSizeTs((long)imageSlicePitch)),
			#eventsInOutArgsRaw(),
			getPeer(pErr)
		);
		#checkPErr()
        return new Pair<ByteBuffer, CLEvent>(
			pointerToAddress(mappedPeer).getByteBuffer(getByteCount()),
			#eventOutWrapper("queue")
		);
    }

    /**
#documentCallsFunction("clEnqueueUnmapMemObject")
     * see {@link CLImage2D\#map(com.nativelibs4java.opencl.CLQueue, com.nativelibs4java.opencl.CLMem.MapFlags, com.nativelibs4java.opencl.CLEvent[]) }
     * see {@link CLImage3D\#map(com.nativelibs4java.opencl.CLQueue, com.nativelibs4java.opencl.CLMem.MapFlags, com.nativelibs4java.opencl.CLEvent[]) }
     * @param queue
     * @param buffer
#documentEventsToWaitForAndReturn()
     */
    public CLEvent unmap(CLQueue queue, ByteBuffer buffer, CLEvent... eventsToWaitFor) {
        #declareReusablePtrsAndEventsInOut()
        Pointer<?> pBuffer = pointerToBuffer(buffer);
        error(CL.clEnqueueUnmapMemObject(queue.getEntity(), getEntity(), getPeer(pBuffer), #eventsInOutArgsRaw()));
		#returnEventOut("queue")
    }
}
