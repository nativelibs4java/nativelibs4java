/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
*/

package com.nativelibs4java.opencl;
import com.nativelibs4java.opencl.library.OpenCLLibrary;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;
import com.sun.jna.*;
import com.sun.jna.ptr.PointerByReference;
import java.nio.*;
import static com.nativelibs4java.opencl.OpenCL4Java.*;
import static com.nativelibs4java.opencl.CLException.*;

/**
 * OpenCL command queue.<br/>
 * OpenCL objects such as memory, program and kernel objects are created using a context. <br/>
 * Operations on these objects are performed using a command-queue. <br/>
 * The command-queue can be used to queue a set of operations (referred to as commands) in order. <br/>
 * Having multiple command-queues allows applications to queue multiple independent commands without requiring synchronization. <br/>
 * Note that this should work as long as these objects are not being shared. <br/>
 * Sharing of objects across multiple command-queues will require the application to perform appropriate synchronization.<br/>
 * <br/>
 * A queue is bound to a single device.
 * @see CLDevice#createQueue(com.nativelibs4java.opencl.CLContext) 
 * @author Olivier Chafik
 *
 */
public class CLQueue extends CLEntity<cl_command_queue> {

    final CLContext context;

    CLQueue(CLContext context, cl_command_queue entity) {
        super(entity);
        this.context = context;
    }

    @Override
    protected void clear() {
        error(CL.clReleaseCommandQueue(get()));
    }

    /**
	 * Blocks until all previously queued OpenCL commands in this queue are issued to the associated device and have completed. <br/>
	 * finish() does not return until all queued commands in this queue have been processed and completed. <br/>
	 * finish() is also a synchronization point.
	 */
    public void finish() {
        error(CL.clFinish(get()));
    }

    /**
	 * Issues all previously queued OpenCL commands in this queue to the device associated with this queue. <br/>
	 * flush() only guarantees that all queued commands in this queue get issued to the appropriate device. <br/>
	 * There is no guarantee that they will be complete after flush() returns.
	 */
    public void flush() {
        error(CL.clFlush(get()));
    }

	/**
	 * Enqueues a wait for a specific event or a list of events to complete before any future commands queued in the this queue are executed.
	 */
	public void enqueueWaitForEvents(CLEvent... events) {
        error(CL.clEnqueueWaitForEvents(get(), events.length, CLEvent.to_cl_event_array(events)));
	}

	/**
	 * Enqueue a barrier operation.<br/>
	 * The enqueueBarrier() command ensures that all queued commands in command_queue have finished execution before the next batch of commands can begin execution. <br/>
	 * enqueueBarrier() is a synchronization point.
	 */
	public void enqueueBarrier() {
		error(CL.clEnqueueBarrier(get()));
	}

	/**
	 * Enqueue a marker command to command_queue. <br/>
	 * The marker command returns an event which can be used by to queue a wait on this marker event i.e. wait for all commands queued before the marker command to complete.
	 */
	public CLEvent enqueueMarker() {
		cl_event[] eventOut = new cl_event[1];
		error(CL.clEnqueueMarker(get(), eventOut));
		return CLEvent.createEvent(eventOut[0]);
	}
}