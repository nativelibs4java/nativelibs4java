#parse("main/Header.vm")
package com.nativelibs4java.opencl;
import com.nativelibs4java.opencl.ImageIOUtils.ImageInfo;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_IMAGE_HEIGHT;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_IMAGE_ROW_PITCH;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_IMAGE_WIDTH;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.bridj.Pointer;
import static org.bridj.Pointer.*;

import com.nativelibs4java.opencl.library.OpenCLLibrary.cl_mem;
import com.nativelibs4java.util.Pair;

/**
 * OpenCL 2D Image Memory Object<br/>
 * see {@link CLContext#createImage2D(com.nativelibs4java.opencl.CLMem.Usage, java.awt.Image, boolean) }
 * see {@link CLContext#createImage2D(com.nativelibs4java.opencl.CLMem.Usage, com.nativelibs4java.opencl.CLImageFormat, long, long) }
 * see {@link CLContext#createImage2D(com.nativelibs4java.opencl.CLMem.Usage, com.nativelibs4java.opencl.CLImageFormat, long, long, long) }
 * see {@link CLContext#createImage2D(com.nativelibs4java.opencl.CLMem.Usage, com.nativelibs4java.opencl.CLImageFormat, long, long, long, java.nio.Buffer, boolean) }
 * see {@link CLContext#createImage2DFromGLRenderBuffer(com.nativelibs4java.opencl.CLMem.Usage, int) }
 * see {@link CLContext#createImage2DFromGLTexture2D(com.nativelibs4java.opencl.CLMem.Usage, com.nativelibs4java.opencl.CLContext.GLTextureTarget, int, int) } 
 * @author Olivier Chafik
 */
public class CLImage2D extends CLImage {
	CLImage2D(CLContext context, long entityPeer, CLImageFormat format) {
        super(context, entityPeer, format);
	}

	/**
	 * Return size in bytes of a row of elements of the image object given by image.
	 */
	@InfoName("CL_IMAGE_ROW_PITCH")
	public long getRowPitch() {
		return infos.getIntOrLong(getEntity(), CL_IMAGE_ROW_PITCH);
	}

	/**
	 * Return width of the image in pixels
	 */
	@InfoName("CL_IMAGE_WIDTH")
	public long getWidth() {
		return infos.getIntOrLong(getEntity(), CL_IMAGE_WIDTH);
	}

	/**
	 * Return height of the image in pixels
	 */
	@InfoName("CL_IMAGE_HEIGHT")
	public long getHeight() {
		return infos.getIntOrLong(getEntity(), CL_IMAGE_HEIGHT);
	}

	/**
#documentEventsToWaitForAndReturn()
	 */
	public CLEvent read(CLQueue queue, long minX, long minY, long width, long height, long rowPitch, Buffer out, boolean blocking, CLEvent... eventsToWaitFor) {
		Pointer<?> ptrOut = pointerToBuffer(out);
		CLEvent evt = read(queue, minX, minY, width, height, rowPitch, ptrOut, blocking, eventsToWaitFor);
		ptrOut.updateBuffer(out); // in case the buffer wasn't direct !
		return evt;
	}
	/**
#documentEventsToWaitForAndReturn()
	 */
	public CLEvent read(CLQueue queue, long minX, long minY, long width, long height, long rowPitch, Pointer<?> out, boolean blocking, CLEvent... eventsToWaitFor) {
		return read(queue, pointerToSizeTs(minX, minY, 0), pointerToSizeTs(width, height, 1), rowPitch, 0, out, blocking, eventsToWaitFor);
	}
	/**
#documentEventsToWaitForAndReturn()
	 */
	public CLEvent write(CLQueue queue, long minX, long minY, long width, long height, long rowPitch, Buffer in, boolean blocking, CLEvent... eventsToWaitFor) {
		return write(queue, minX, minY, width, height, rowPitch, pointerToBuffer(in), blocking, eventsToWaitFor);
	}
	/**
#documentEventsToWaitForAndReturn()
	 */
	public CLEvent write(CLQueue queue, long minX, long minY, long width, long height, long rowPitch, Pointer<?> in, boolean blocking, CLEvent... eventsToWaitFor) {
		return write(queue, pointerToSizeTs(minX, minY, 0), pointerToSizeTs(width, height, 1), rowPitch, 0, in, blocking, eventsToWaitFor);
	}

	/**
	 * @param eventsToWaitFor Events that need to complete before this particular command can be executed.
     */
	public BufferedImage read(CLQueue queue, CLEvent... eventsToWaitFor) {
        ImageInfo info = ImageIOUtils.getBufferedImageInfo(getFormat());
        int imageType = info == null ? 0 : info.bufferedImageType;
        if (imageType == 0)
            throw new UnsupportedOperationException("Cannot convert image of format " + getFormat() + " to a BufferedImage.");
            //imageType = BufferedImage.TYPE_INT_ARGB;
        
		BufferedImage im = new BufferedImage((int)getWidth(), (int)getHeight(), imageType);
		read(queue, im, false, eventsToWaitFor);
		return im;
	}
	/**
	 * @param eventsToWaitFor Events that need to complete before this particular command can be executed.  
     */
	public void read(CLQueue queue, BufferedImage imageOut, boolean allowDeoptimizingDirectWrite, CLEvent... eventsToWaitFor) {
		//if (!getFormat().isIntBased())
		//	throw new IllegalArgumentException("Image-read only supports int-based RGBA images");
        ImageInfo info = ImageIOUtils.getBufferedImageInfo(getFormat());
        int width = imageOut.getWidth(null), height = imageOut.getHeight(null);

        Pointer<?> dataOut = allocateArray(info.bufferElementsClass, width * height * info.channelCount).order(getContext().getByteOrder());
		//Buffer dataOut = info.createBuffer(width, height, true);
		//IntBuffer dataOut = directInts(width * height, getContext().getByteOrder());
		read(queue, 0, 0, width, height, 0, dataOut, true, eventsToWaitFor);
        info.dataSetter.setData(imageOut, dataOut.getBuffer(), allowDeoptimizingDirectWrite);
	}
	/**
#documentEventsToWaitForAndReturn()
	 */
	public CLEvent write(CLQueue queue, Image image, CLEvent... eventsToWaitFor) {
		return write(queue, image, 0, 0, image.getWidth(null), image.getHeight(null), false, false, eventsToWaitFor);
	}
	/**
#documentEventsToWaitForAndReturn()
	 */
	public CLEvent write(CLQueue queue, Image image, boolean allowDeoptimizingDirectRead, boolean blocking, CLEvent... eventsToWaitFor) {
		return write(queue, image, 0, 0, image.getWidth(null), image.getHeight(null), allowDeoptimizingDirectRead, blocking, eventsToWaitFor);
	}
	/**
#documentEventsToWaitForAndReturn()
	 */
	public CLEvent write(CLQueue queue, Image image, int destX, int destY, int width, int height, boolean allowDeoptimizingDirectRead, boolean blocking, CLEvent... eventsToWaitFor) {
		//int imWidth = image.getWidth(null), height = image.getHeight(null);
        ImageInfo info = ImageIOUtils.getBufferedImageInfo(getFormat());
		return write(queue, 0, 0, width, height, width * info.pixelByteSize, info.dataGetter.getData(image, null, false, allowDeoptimizingDirectRead, getContext().getByteOrder()), blocking, eventsToWaitFor);
	}
	public void write(CLQueue queue, BufferedImage imageIn, boolean allowDeoptimizingDirectRead, CLEvent... eventsToWaitFor) {
		//if (!getFormat().isIntBased())
		//	throw new IllegalArgumentException("Image read only supports int-based RGBA images");

		int width = imageIn.getWidth(null), height = imageIn.getHeight(null);
		ImageInfo<BufferedImage> info = ImageIOUtils.getBufferedImageInfo(getFormat());
		write(queue, 0, 0, width, height, 0, info.dataGetter.getData(imageIn, null, false, allowDeoptimizingDirectRead, getContext().getByteOrder()), true, eventsToWaitFor);
	}
	public void write(CLQueue queue, BufferedImage im) {
		write(queue, im, false);
	}

    public ByteBuffer map(CLQueue queue, MapFlags flags, CLEvent... eventsToWaitFor) {
        return map(queue, flags, pointerToSizeTs(0, 0), pointerToSizeTs(getWidth(), getHeight()), getWidth(), null, true, eventsToWaitFor).getFirst();
    }
	public ByteBuffer map(CLQueue queue, MapFlags flags, long offsetX, long offsetY, long lengthX, long lengthY, long rowPitch, CLEvent... eventsToWaitFor) {
		return map(queue, flags, pointerToSizeTs(offsetX, offsetY), pointerToSizeTs(lengthX, lengthY), rowPitch, null, true, eventsToWaitFor).getFirst();
    }
	/**
#documentEventsToWaitForAndPairReturn("byte buffer")
	 */
	public Pair<ByteBuffer, CLEvent> mapLater(CLQueue queue, MapFlags flags, boolean blocking, CLEvent... eventsToWaitFor) {
		return map(queue, flags, pointerToSizeTs(0, 0), pointerToSizeTs(getWidth(), getHeight()), getWidth(), null, blocking, eventsToWaitFor);
    }
    /**
#documentEventsToWaitForAndPairReturn("byte buffer")
	 */
	public Pair<ByteBuffer, CLEvent> mapLater(CLQueue queue, MapFlags flags, long offsetX, long offsetY, long lengthX, long lengthY, long rowPitch, boolean blocking, CLEvent... eventsToWaitFor) {
		return map(queue, flags, pointerToSizeTs(offsetX, offsetY), pointerToSizeTs(lengthX, lengthY), rowPitch, null, blocking, eventsToWaitFor);
    }
}
