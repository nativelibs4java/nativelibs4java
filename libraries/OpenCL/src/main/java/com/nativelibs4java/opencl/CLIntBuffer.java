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
 * OpenCL Memory Buffer Object with Int values.<br/>
 * @author Olivier Chafik
 */
public class CLIntBuffer extends CLBuffer {
	CLIntBuffer(CLContext context, long byteCount, cl_mem entity, Buffer buffer) {
        super(context, byteCount, entity, buffer);
	}
	protected static Pair<IntBuffer, CLEvent> as(Pair<ByteBuffer, CLEvent> p) {
		return new Pair<IntBuffer, CLEvent>(p.getFirst().asIntBuffer(), p.getSecond());
	}

	public IntBuffer map(CLQueue queue, MapFlags flags, CLEvent... eventsToWaitFor) {
		return map(queue, flags, 0, getByteCount(), eventsToWaitFor);
    }
	public IntBuffer map(CLQueue queue, MapFlags flags, long offset, long length, CLEvent... eventsToWaitFor) {
		return map(queue, flags, offset, length, true, eventsToWaitFor).getFirst().asIntBuffer();
    }
	public Pair<IntBuffer, CLEvent> mapLater(CLQueue queue, MapFlags flags, CLEvent... eventsToWaitFor) {
		return as(map(queue, flags, 0, getByteCount(), false, eventsToWaitFor));
    }
	public Pair<IntBuffer, CLEvent> mapLater(CLQueue queue, MapFlags flags, long offset, long length, CLEvent... eventsToWaitFor) {
		Pair<ByteBuffer, CLEvent> p = map(queue, flags, offset, length, false, eventsToWaitFor);
		return new Pair<IntBuffer, CLEvent>(p.getFirst().asIntBuffer(), p.getSecond());
    }
	public IntBuffer read(CLQueue queue, CLEvent... eventsToWaitFor) {
		return readBytes(queue, eventsToWaitFor).asIntBuffer();
	}
	public IntBuffer read(CLQueue queue, long offset, long length, CLEvent... eventsToWaitFor) {
		return readBytes(queue, offset, length, eventsToWaitFor).asIntBuffer();
	}
}
