/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
*/

package com.nativelibs4java.opencl;
import com.nativelibs4java.opencl.library.OpenCLLibrary;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;
import com.sun.jna.*;
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
	 * Wait for the queue to be fully executed. Costly.
	 */
    public void finish() {
        error(CL.clFinish(get()));
    }

	public void enqueueWaitForEvents(CLEvent... events) {
        error(CL.clEnqueueWaitForEvents(get(), events.length, CLEvent.to_cl_event_array(events)));
	}

	
}