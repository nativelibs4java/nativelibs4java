package com.nativelibs4java.opencl;


import static com.nativelibs4java.opencl.JavaCL.CL;
import static com.nativelibs4java.opencl.CLException.error;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;
import com.nativelibs4java.opencl.library.OpenCLLibrary.cl_event;

public class CLUserEvent extends CLEvent {
	CLUserEvent(cl_event evt) { 
		super(evt);
	}
	/**
	 * Sets the execution status of a this event object.
	 * NOTE: Enqueued commands that specify user events in the event_wait_list argument of clEnqueue*** commands must ensure that the status of these user events being waited on are set using clSetUserEventStatus before any OpenCL APIs that release OpenCL objects except for event objects are called; otherwise the behavior is undefined. More details in the OpenCL specifications at section 5.9.
	 * @param executionStatus specifies the new execution status to be set and can be CL_COMPLETE or a negative integer value to indicate an error. A negative integer value causes all enqueued commands that wait on this user event to be terminated.	setStatus can only be called once to change the execution status of event.
	 */
	public void setStatus(int executionStatus) {
		error(CL.clSetUserEventStatus(getEntity(), executionStatus));
	}
	
	/**
	 * Calls setStatus(CL_COMPLETE)
	 */
	public void setComplete() {
		setStatus(CL_COMPLETE);
	}
}
