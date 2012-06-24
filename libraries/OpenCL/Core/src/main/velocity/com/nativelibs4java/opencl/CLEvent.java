/*
 * JavaCL - Java API and utilities for OpenCL
 * http://javacl.googlecode.com/
 *
 * Copyright (c) 2009-2011, Olivier Chafik (http://ochafik.com/)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Olivier Chafik nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY OLIVIER CHAFIK AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.nativelibs4java.opencl;
import static com.nativelibs4java.opencl.CLException.error;
import static com.nativelibs4java.opencl.JavaCL.CL;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;

import java.util.Arrays;

import com.nativelibs4java.opencl.library.OpenCLLibrary;
import com.nativelibs4java.opencl.library.OpenCLLibrary.cl_event;
import com.nativelibs4java.util.EnumValue;
import com.nativelibs4java.util.EnumValues;
import org.bridj.*;
import static org.bridj.Pointer.*;

/**
 * OpenCL event object.<br/>
 * Event objects can be used to refer to a kernel execution command (clEnqueueNDRangeKernel, clEnqueueTask, clEnqueueNativeKernel), or read, write, map and copy commands on memory objects (clEnqueue{Read|Write|Map}{Buffer|Image}, clEnqueueCopy{Buffer|Image}, clEnqueueCopyBufferToImage, or clEnqueueCopyImageToBuffer).<br/>
 * An event object can be used to track the execution status of a command. <br/>
 * The API calls that enqueue commands to a command-queue create a new event object that is returned in the event argument. <br/>
 * In case of an error enqueuing the command in the command-queue the event argument does not return an event object.<br/>
 * The execution status of an enqueued command at any given point in time can be CL_QUEUED (command has been enqueued in the command-queue), CL_SUBMITTED (enqueued command has been submitted by the host to the device associated with the command-queue), CL_RUNNING (device is currently executing this command), CL_COMPLETE (command has successfully completed) or the appropriate error code if the command was abnormally terminated (this may be caused by a bad memory access etc.). <br/>
 * The error code returned by a terminated command is a negative integer value. <br/>
 * A command is considered to be complete if its execution status is CL_COMPLETE or is a negative integer value.<br/>
 * If the execution of a command is terminated, the command-queue associated with this terminated command, and the associated context (and all other command-queues in this context) may no longer be available. <br/>
 * The behavior of OpenCL API calls that use this context (and command-queues associated with this context) are now considered to be implementation- defined. <br/>
 * The user registered callback function specified when context is created can be used to report appropriate error information.<br/>
 * 
 * @author ochafik
 */
public class CLEvent extends CLAbstractEntity<cl_event> {

	/**
	 * Pass this to special value to any method that expects a variable number of events to wait for and that returns an event, to completely avoid returning the completion event (will return null instead of the event). 
	 */
	public static final CLEvent FIRE_AND_FORGET = new CLEvent(-1);
	
	private static CLInfoGetter<cl_event> infos = new CLInfoGetter<cl_event>() {
		@Override
		protected int getInfo(cl_event entity, int infoTypeEnum, long size, Pointer out, Pointer<SizeT> sizeOut) {
			return CL.clGetEventInfo(entity, infoTypeEnum, size, out, sizeOut);
		}
	};

	private static CLInfoGetter<cl_event> profilingInfos = new CLInfoGetter<cl_event>() {
		@Override
		protected int getInfo(cl_event entity, int infoTypeEnum, long size, Pointer out, Pointer<SizeT> sizeOut) {
			return CL.clGetEventProfilingInfo(entity, infoTypeEnum, size, out, sizeOut);
		}
	};
	
	CLEvent(cl_event evt) {
		super(evt, false);
	}

    CLEvent(long evt) {
		super(evt, false);
	}
	
    @Override
    protected cl_event createEntityPointer(long peer) {
    	return new cl_event(peer);
    }

    public interface EventCallback {
    	public void callback(CLEvent event, int executionStatus);
    }
    
    /**
     * Registers a user callback function for the completion execution status (CL_COMPLETE). <br/>
     * @param callback
     * @throws UnsupportedOperationException in OpenCL 1.0
     * @since OpenCL 1.1
     */
    public void setCompletionCallback(final EventCallback callback) {
    	setCallback(CL_COMPLETE, callback);
    }
    /**
     * Registers a user callback function for a specific command execution status. <br/>
     * The registered callback function will be called when the execution status of command associated with event changes to the execution status specified by command_exec_status.
     * @param commandExecStatus specifies the command execution status for which the callback is registered. The command execution callback values for which a callback can be registered are: CL_COMPLETE. There is no guarantee that the callback functions registered for various execution status values for an event will be called in the exact order that the execution status of a command changes.
     * @param callback
     * @throws UnsupportedOperationException in OpenCL 1.0
     * @since OpenCL 1.1
     */
    public void setCallback(int commandExecStatus, final EventCallback callback) {
    	try {
    		clSetEventCallback_arg1_callback cb = new clSetEventCallback_arg1_callback() {
	    		public void apply(OpenCLLibrary.cl_event evt, int executionStatus, Pointer voidPtr1) {
	    			callback.callback(CLEvent.this, executionStatus);
	    		}
	    	};
	    	// TODO manage lifespan of cb
    		BridJ.protectFromGC(cb);
	    	error(CL.clSetEventCallback(getEntity(), commandExecStatus, pointerTo(cb), null));
    	} catch (Throwable th) {
    		// TODO check if supposed to handle OpenCL 1.1
    		throw new UnsupportedOperationException("Cannot set event callback (OpenCL 1.1 feature).", th);
    	}
    }
    
    static CLEvent createEvent(final CLQueue queue, long evt) {
    		return createEvent(queue, evt, false);
    }
	static CLEvent createEvent(final CLQueue queue, long evt, boolean isUserEvent) {
		if (evt == 0)
			return null;

        return isUserEvent ? 
        		new CLUserEvent(evt) : 
        		new CLEvent(evt);
	}

    static CLEvent createEventFromPointer(CLQueue queue, Pointer<cl_event> evt1) {
		if (evt1 == null)
			return null;
        
        long peer = evt1.getSizeT();
        if (peer == 0)
            return null;
        
        return new CLEvent(peer);
	}


	/**
	 * Wait for this event, blocking the caller thread independently of any queue until all of the command associated with this events completes.
	 */
	public void waitFor() {
		waitFor(this);
	}

	/**
	 * Wait for events, blocking the caller thread independently of any queue until all of the commands associated with the events completed.
	 * @param eventsToWaitFor List of events which completion is to be waited for
	 */
	public static void waitFor(CLEvent... eventsToWaitFor) {
		if (eventsToWaitFor.length == 0)
			return;
		
		try {
            ReusablePointers ptrs = ReusablePointers.get();
            int[] eventsCount = new int[1];
            Pointer<cl_event> events = CLAbstractEntity.copyNonNullEntities(eventsToWaitFor, eventsCount, ptrs.events_in);
            if (events == null)
                return;
            error(CL.clWaitForEvents(eventsCount[0], getPeer(events)));
		} catch (Exception ex) {
			throw new RuntimeException("Exception while waiting for events " + Arrays.asList(eventsToWaitFor), ex);
		}
	}

	/**
	 * Invoke an action in a separate thread only after completion of the command associated with this event.<br/>
	 * Returns immediately.
	 * @param action an action to be ran
	 * @throws IllegalArgumentException if action is null
	 */
	public void invokeUponCompletion(final Runnable action) {
		invokeUponCompletion(action, this);
	}

	/**
	 * Invoke an action in a separate thread only after completion of all of the commands associated with the specified events.<br/>
	 * Returns immediately.
	 * @param action an action to be ran
	 * @param eventsToWaitFor list of events which commands's completion should be waited for before the action is ran
	 * @throws IllegalArgumentException if action is null
	 */
	public static void invokeUponCompletion(final Runnable action, final CLEvent... eventsToWaitFor) {
		if (action == null)
			throw new IllegalArgumentException("Null action !");

		new Thread() {
			public void run() {
				waitFor(eventsToWaitFor);
				action.run();
			}
		}.start();
	}

	static Pointer<cl_event> new_event_out(CLEvent[] eventsToWaitFor) {
		return new_event_out(eventsToWaitFor, ReusablePointers.get().event_out);
    }
    static Pointer<cl_event> new_event_out(CLEvent[] eventsToWaitFor, Pointer<cl_event> event_out) {
        if (eventsToWaitFor == null)
        	return null;
        
		for (int i = 0, n = eventsToWaitFor.length; i < n; i++) {
			if (eventsToWaitFor[i] == FIRE_AND_FORGET)
				return null;
		}
		
        return event_out;
    }
    
    @Deprecated
	static Pointer<cl_event> to_cl_event_array(CLEvent... events) {
        int[] countOut = new int[1];
        Pointer<cl_event> p = CLAbstractEntity.copyNonNullEntities(events, countOut, ReusablePointers.get().events_in);
        int count = countOut[0];
        return count == 0 ? null : p.as(cl_event.class).validElements(count);//p.validBytes(count * Pointer.SIZE);
    }

	@Override
	protected void clear() {
		error(CL.clReleaseEvent(getEntityPeer()));
	}

	/** Values for CL_EVENT_COMMAND_EXECUTION_STATUS */
	public enum CommandExecutionStatus implements com.nativelibs4java.util.ValuedEnum {
		/** command has been enqueued in the command-queue                                             */ 
		Queued(CL_QUEUED),
		/** enqueued command has been submitted by the host to the device associated with the command-queue */ 
		Submitted(CL_SUBMITTED),
		/** device is currently executing this command */ 
		Running(CL_RUNNING),
		/** the command has completed */ 
		Complete(CL_COMPLETE);
		
		CommandExecutionStatus(long value) { this.value = value; }
		long value;
		@Override
		public long value() { return value; }
		public static CommandExecutionStatus getEnum(long v) { return EnumValues.getEnum(v, CommandExecutionStatus.class); }
	}
	
	/**
	 * Return the execution status of the command identified by event.  <br/>
	 * @throws CLException is the execution status denotes an error
	 */
	public CommandExecutionStatus getCommandExecutionStatus() {
		int v = infos.getInt(getEntity(), CL_EVENT_COMMAND_EXECUTION_STATUS);
		CommandExecutionStatus status =  CommandExecutionStatus.getEnum(v);
		if (status == null)
			error(v);
		return status;
	}
	/**
	 * Return the execution status of the command identified by event.  <br/>
	 * @throws CLException is the execution status denotes an error
	 */
	@InfoName("CL_EVENT_COMMAND_EXECUTION_STATUS")
	public int getCommandExecutionStatusValue() {
		return infos.getInt(getEntity(), CL_EVENT_COMMAND_EXECUTION_STATUS);
	}

	/** Values for CL_EVENT_COMMAND_TYPE */
	public enum CommandType implements com.nativelibs4java.util.ValuedEnum {
		NDRangeKernel(CL_COMMAND_NDRANGE_KERNEL),
		Task(CL_COMMAND_TASK),
		NativeKernel(CL_COMMAND_NATIVE_KERNEL),
		ReadBuffer(CL_COMMAND_READ_BUFFER),
		WriteBuffer(CL_COMMAND_WRITE_BUFFER),
		CopyBuffer(CL_COMMAND_COPY_BUFFER),
		ReadImage(CL_COMMAND_READ_IMAGE),
		WriteImage(CL_COMMAND_WRITE_IMAGE),
		CopyImage(CL_COMMAND_COPY_IMAGE),
		CopyBufferToImage(CL_COMMAND_COPY_BUFFER_TO_IMAGE),
		CopyImageToBuffer(CL_COMMAND_COPY_IMAGE_TO_BUFFER),
		MapBuffer(CL_COMMAND_MAP_BUFFER),
		CommandMapImage(CL_COMMAND_MAP_IMAGE),
		UnmapMemObject(CL_COMMAND_UNMAP_MEM_OBJECT),             
		Marker(CL_COMMAND_MARKER),             
		AcquireGLObjects(CL_COMMAND_ACQUIRE_GL_OBJECTS),             
		ReleaseGLObjects(CL_COMMAND_RELEASE_GL_OBJECTS);

		CommandType(long value) { this.value = value; }
		long value;
		@Override
		public long value() { return value; }
		public static CommandType getEnum(long v) { return EnumValues.getEnum(v, CommandType.class); }
	}

	/**
	 * Return the command associated with event.
	 */
	@InfoName("CL_EVENT_COMMAND_TYPE")
	public CommandType getCommandType() {
		return CommandType.getEnum(infos.getInt(getEntity(), CL_EVENT_COMMAND_TYPE));
	}


	/**
	 * A 64-bit value that describes the current device time counter in nanoseconds when the command identified by event is enqueued in a command-queue by the host.
	 */
	@InfoName("CL_CL_PROFILING_COMMAND_QUEUED")
	public long getProfilingCommandQueued() {
		return profilingInfos.getIntOrLong(getEntity(), CL_PROFILING_COMMAND_QUEUED);
	}

	/**
	 * A 64-bit value that describes the current device time counter in nanoseconds when the command identified by event that has been enqueued is submitted by the host to the device associated with the command- queue.
	 */
	@InfoName("CL_CL_PROFILING_COMMAND_SUBMIT")
	public long getProfilingCommandSubmit() {
		return profilingInfos.getIntOrLong(getEntity(), CL_PROFILING_COMMAND_SUBMIT);
	}

	/**
	 * A 64-bit value that describes the current device time counter in nanoseconds when the command identified by event starts execution on the device.
	 */
	@InfoName("CL_CL_PROFILING_COMMAND_START")
	public long getProfilingCommandStart() {
		return profilingInfos.getIntOrLong(getEntity(), CL_PROFILING_COMMAND_START);
	}

	/**
	 * A 64-bit value that describes the current device time counter in nanoseconds when the command identified by event has finished execution on the device.
	 */
	@InfoName("CL_CL_PROFILING_COMMAND_END")
	public long getProfilingCommandEnd() {
		return profilingInfos.getIntOrLong(getEntity(), CL_PROFILING_COMMAND_END);
	}


	@Override
	public String toString() {
		return "Event {commandType: " + getCommandType() + "}";
	}
}
