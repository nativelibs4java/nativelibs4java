#parse("main/Header.vm")
package com.nativelibs4java.opencl;
import static com.nativelibs4java.opencl.CLException.error;
import static com.nativelibs4java.opencl.JavaCL.CL;
import static com.nativelibs4java.opencl.JavaCL.check;
import static com.nativelibs4java.opencl.library.IOpenCLLibrary.CL_FALSE;
import static com.nativelibs4java.opencl.library.IOpenCLLibrary.CL_IMAGE_ELEMENT_SIZE;
import static com.nativelibs4java.opencl.library.IOpenCLLibrary.CL_IMAGE_FORMAT;
import static com.nativelibs4java.opencl.library.IOpenCLLibrary.CL_TRUE;
import static com.nativelibs4java.util.NIOUtils.directCopy;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import com.nativelibs4java.opencl.library.cl_image_format;
import com.nativelibs4java.opencl.library.IOpenCLLibrary.cl_event;
import com.nativelibs4java.opencl.library.IOpenCLLibrary.cl_mem;
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

	protected abstract long[] getDimensions();

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

    /**
#documentCallsFunction("clEnqueueFillImage")
     * @param queue
     * @param queue Queue on which to enqueue this fill buffer command.
     * @param color Color components to fill the buffer with.
     * @param origin Origin point.
     * @param region Size of the region to fill.
#documentEventsToWaitForAndReturn()
     */
    public CLEvent fillImage(CLQueue queue, Object color, CLEvent... eventsToWaitFor) {
    	long[] region = getDimensions();
    	long[] origin = new long[region.length];
    	return fillImage(queue, color, origin, region, eventsToWaitFor);
    }

    /**
#documentCallsFunction("clEnqueueFillImage")
     * @param queue
     * @param queue Queue on which to enqueue this fill buffer command.
     * @param color Color components to fill the buffer with.
     * @param origin Origin point.
     * @param region Size of the region to fill.
#documentEventsToWaitForAndReturn()
     */
    public CLEvent fillImage(CLQueue queue, Object color, long[] origin, long[] region, CLEvent... eventsToWaitFor) {
    	context.getPlatform().requireMinVersionValue("clEnqueueFillImage", 1.2);
		Pointer<?> pColor;
		if (color instanceof int[]) {
			pColor = pointerToInts((int[]) color);
		} else if (color instanceof float[]) {
			pColor = pointerToFloats((float[]) color);
		} else {
			throw new IllegalArgumentException("Color should be an int[] or a float[] with 4 elements.");
		}
		check(pColor.getValidElements() == 4, "Color should have 4 elements.");

		#declareReusablePtrsAndEventsInOut()
		error(CL.clEnqueueFillImage(
			queue.getEntity(),
			getEntity(),
			getPeer(pColor),
			getPeer(writeOrigin(origin, ptrs.sizeT3_1)),
			getPeer(writeRegion(region, ptrs.sizeT3_2)),
			#eventsInOutArgsRaw()
		));
        #returnEventOut("queue")
    }

	// clEnqueueFillImage (	cl_command_queue command_queue,
 // 	cl_mem image,
 // 	const void *fill_color,
 // 	const size_t *origin,
 // 	const size_t *region,
 // 	cl_uint num_events_in_wait_list,
 // 	const cl_event *event_wait_list,
 // 	cl_event *event)

    protected Pair<ByteBuffer, CLEvent> map(CLQueue queue, MapFlags flags,
            Pointer<SizeT> offset3, Pointer<SizeT> length3,
            Long imageRowPitch,
            Long imageSlicePitch,
            boolean blocking, CLEvent... eventsToWaitFor)
    {
        if (flags == MapFlags.WriteInvalidateRegion) {
            context.getPlatform().requireMinVersionValue("CL_MAP_WRITE_INVALIDATE_REGION", 1.2);
        }
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
            imageSlicePitch == null ? 0 : getPeer(ptrs.sizeT3_2.pointerToSizeTs((long)imageSlicePitch)),
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
     * see {@link CLImage2D#map(com.nativelibs4java.opencl.CLQueue, com.nativelibs4java.opencl.CLMem.MapFlags, com.nativelibs4java.opencl.CLEvent[]) }
     * see {@link CLImage3D#map(com.nativelibs4java.opencl.CLQueue, com.nativelibs4java.opencl.CLMem.MapFlags, com.nativelibs4java.opencl.CLEvent[]) }
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

    protected abstract Pointer<SizeT> writeOrigin(long[] origin, ReusablePointer out);
    protected abstract Pointer<SizeT> writeRegion(long[] region, ReusablePointer out);

    /**
#documentCallsFunction("clEnqueueCopyImage")
     * see {@link CLImage2D#map(com.nativelibs4java.opencl.CLQueue, com.nativelibs4java.opencl.CLMem.MapFlags, com.nativelibs4java.opencl.CLEvent[]) }
     * see {@link CLImage3D#map(com.nativelibs4java.opencl.CLQueue, com.nativelibs4java.opencl.CLMem.MapFlags, com.nativelibs4java.opencl.CLEvent[]) }
     * @param queue
#documentEventsToWaitForAndReturn()
     */
    public CLEvent copyTo(CLQueue queue, CLImage destination, CLEvent... eventsToWaitFor) {
        long[] region = getDimensions();
        long[] origin = new long[region.length];
        return copyTo(queue, destination, origin, origin, region, eventsToWaitFor);
    }

    /**
#documentCallsFunction("clEnqueueCopyImage")
     * see {@link CLImage2D#map(com.nativelibs4java.opencl.CLQueue, com.nativelibs4java.opencl.CLMem.MapFlags, com.nativelibs4java.opencl.CLEvent[]) }
     * see {@link CLImage3D#map(com.nativelibs4java.opencl.CLQueue, com.nativelibs4java.opencl.CLMem.MapFlags, com.nativelibs4java.opencl.CLEvent[]) }
     * @param queue
#documentEventsToWaitForAndReturn()
     */
    public CLEvent copyTo(CLQueue queue, CLImage destination, long[] sourceOrigin, long[] destinationOrigin, long[] region, CLEvent... eventsToWaitFor) {
        #declareReusablePtrsAndEventsInOut()
        error(CL.clEnqueueCopyImage(
            queue.getEntity(),
            getEntity(),
            destination.getEntity(),
            getPeer(writeOrigin(sourceOrigin, ptrs.sizeT3_1)),
            getPeer(writeOrigin(destinationOrigin, ptrs.sizeT3_2)),
            getPeer(writeRegion(region, ptrs.sizeT3_3)),
            #eventsInOutArgsRaw()));
		#returnEventOut("queue")
    }
}
