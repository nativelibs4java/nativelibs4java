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
 * OpenCL Memory Buffer Object.<br/>
 * A buffer object stores a one-dimensional collection of elements.<br/>
 * Elements of a buffer object can be a scalar data type (such as an int, float), vector data type, or a user-defined structure.<br/>
 * @see CLContext#createInput(long)
 * @see CLContext#createOutput(long)
 * @author Olivier Chafik
 */
public class CLByteBuffer extends CLBuffer {
	CLByteBuffer(CLContext context, long byteCount, cl_mem entity, Buffer buffer) {
        super(context, byteCount, entity, buffer);
	}
	public ByteBuffer map(CLQueue queue, MapFlags flags, CLEvent... eventsToWaitFor) {
		return map(queue, flags, 0, getByteCount(), true, eventsToWaitFor).getFirst();
    }
	public ByteBuffer map(CLQueue queue, MapFlags flags, long offset, long length, CLEvent... eventsToWaitFor) {
		return map(queue, flags, offset, length, true, eventsToWaitFor).getFirst();
    }

	public Pair<ByteBuffer, CLEvent> mapLater(CLQueue queue, MapFlags flags, CLEvent... eventsToWaitFor) {
		return map(queue, flags, 0, getByteCount(), false, eventsToWaitFor);
    }
	public Pair<ByteBuffer, CLEvent> mapLater(CLQueue queue, MapFlags flags, long offset, long length, CLEvent... eventsToWaitFor) {
		return map(queue, flags, offset, length, false, eventsToWaitFor);
    }
	public ByteBuffer read(CLQueue queue, CLEvent... eventsToWaitFor) {
		return readBytes(queue, eventsToWaitFor);
	}
	public ByteBuffer read(CLQueue queue, long offset, long length, CLEvent... eventsToWaitFor) {
		return readBytes(queue, offset, length, eventsToWaitFor);
	}
}
