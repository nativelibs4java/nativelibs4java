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
 * OpenCL Memory Buffer Object with Double values.<br/>
 * @author Olivier Chafik
 */
public class CLDoubleBuffer extends CLBuffer {
	CLDoubleBuffer(CLContext context, long byteCount, cl_mem entity, Buffer buffer) {
        super(context, byteCount, entity, buffer);
	}
	protected static Pair<DoubleBuffer, CLEvent> as(Pair<ByteBuffer, CLEvent> p) {
		return new Pair<DoubleBuffer, CLEvent>(p.getFirst().asDoubleBuffer(), p.getSecond());
	}

	public DoubleBuffer map(CLQueue queue, MapFlags flags, CLEvent... eventsToWaitFor) {
		return map(queue, flags, 0, getByteCount(), eventsToWaitFor);
    }
	public DoubleBuffer map(CLQueue queue, MapFlags flags, long offset, long length, CLEvent... eventsToWaitFor) {
		return map(queue, flags, offset, length, true, eventsToWaitFor).getFirst().asDoubleBuffer();
    }
	public Pair<DoubleBuffer, CLEvent> mapLater(CLQueue queue, MapFlags flags, CLEvent... eventsToWaitFor) {
		return as(map(queue, flags, 0, getByteCount(), false, eventsToWaitFor));
    }
	public Pair<DoubleBuffer, CLEvent> mapLater(CLQueue queue, MapFlags flags, long offset, long length, CLEvent... eventsToWaitFor) {
		Pair<ByteBuffer, CLEvent> p = map(queue, flags, offset, length, false, eventsToWaitFor);
		return new Pair<DoubleBuffer, CLEvent>(p.getFirst().asDoubleBuffer(), p.getSecond());
    }
	public DoubleBuffer read(CLQueue queue, CLEvent... eventsToWaitFor) {
		return readBytes(queue, eventsToWaitFor).asDoubleBuffer();
	}
	public DoubleBuffer read(CLQueue queue, long offset, long length, CLEvent... eventsToWaitFor) {
		return readBytes(queue, offset, length, eventsToWaitFor).asDoubleBuffer();
	}
}
