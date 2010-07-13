/*
	Copyright (c) 2009 Olivier Chafik (http://ochafik.free.fr/)
	
	This file is part of OpenCL4Java (http://code.google.com/p/nativelibs4java/wiki/OpenCL).
	
	OpenCL4Java is free software: you can redistribute it and/or modify
	it under the terms of the GNU Lesser General Public License as published by
	the Free Software Foundation, either version 2.1 of the License, or
	(at your option) any later version.
	
	OpenCL4Java is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU Lesser General Public License for more details.
	
	You should have received a copy of the GNU Lesser General Public License
	along with OpenCL4Java.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.nativelibs4java.opencl;
import static com.nativelibs4java.opencl.CLException.error;
import static com.nativelibs4java.opencl.JavaCL.CL;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_COMMAND_ACQUIRE_GL_OBJECTS;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_COMMAND_COPY_BUFFER;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_COMMAND_COPY_BUFFER_TO_IMAGE;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_COMMAND_COPY_IMAGE;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_COMMAND_COPY_IMAGE_TO_BUFFER;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_COMMAND_MAP_BUFFER;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_COMMAND_MAP_IMAGE;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_COMMAND_MARKER;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_COMMAND_NATIVE_KERNEL;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_COMMAND_NDRANGE_KERNEL;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_COMMAND_READ_BUFFER;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_COMMAND_READ_IMAGE;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_COMMAND_RELEASE_GL_OBJECTS;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_COMMAND_TASK;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_COMMAND_UNMAP_MEM_OBJECT;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_COMMAND_WRITE_BUFFER;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_COMMAND_WRITE_IMAGE;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_COMPLETE;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_EVENT_COMMAND_EXECUTION_STATUS;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_EVENT_COMMAND_TYPE;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_PROFILING_COMMAND_END;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_PROFILING_COMMAND_QUEUED;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_PROFILING_COMMAND_START;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_PROFILING_COMMAND_SUBMIT;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_QUEUED;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_RUNNING;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_SUBMITTED;

import java.util.Arrays;

import com.nativelibs4java.opencl.library.OpenCLLibrary.cl_event;
import com.nativelibs4java.util.EnumValue;
import com.nativelibs4java.util.EnumValues;

import com.bridj.Pointer;
import com.bridj.SizeT;
import static com.bridj.Pointer.*;

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
	
	private CLEvent(cl_event evt) {
		super(evt, false);
	}

    private CLEvent() {
		super(null, true);
	}

    static boolean noEvents = false;
    public static void setNoEvents(boolean noEvents) {
        CLEvent.noEvents = noEvents;
    }
	static CLEvent createEvent(final CLQueue queue, cl_event evt) {
		if (noEvents) {
            if (evt != null)
                CL.clReleaseEvent(evt);
            evt = null;

            return new CLEvent() {
                volatile boolean waited = false;
                @Override
                public synchronized void waitFor() {
                    if (!waited) {
                        queue.finish();
                        waited = true;
                    }
                }
            };
        }
        if (evt == null)
			return null;

        return new CLEvent(evt);
	}

    static CLEvent createEventFromPointer(CLQueue queue, Pointer<cl_event> evt1) {
		if (evt1 == null)
			return null;
		return createEvent(queue, evt1.get());
	}


	/**
	 * Wait for this event, blocking the caller thread independently of any queue until all of the command associated with this events completes.
	 */
	public void waitFor() {
		if (entity == null)
			return;
		waitFor(this);
        release();
	}

	/**
	 * Wait for events, blocking the caller thread independently of any queue until all of the commands associated with the events completed.
	 * @param eventsToWaitFor List of events which completion is to be waited for
	 */
	public static void waitFor(CLEvent... eventsToWaitFor) {
		if (eventsToWaitFor.length == 0)
			return;
		
		try {
			Pointer<cl_event> evts = CLEvent.to_cl_event_array(eventsToWaitFor);
            if (evts == null)
                return;
            error(CL.clWaitForEvents((int)evts.getRemainingElements(), evts));
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
        return noEvents || eventsToWaitFor == null ? null : allocateTypedPointer(cl_event.class);
    }
    
	static Pointer<cl_event> to_cl_event_array(CLEvent... events) {
        if (noEvents) {
            for (CLEvent evt : events)
                if (evt != null)
                    evt.waitFor();
            return null;
        }
        int n = events.length;
		if (n == 0)
			return null;
        int nonNulls = 0;
        for (int i = 0; i < n; i++)
            if (events[i] != null && events[i].getEntity() != null)
                nonNulls++;

        if (nonNulls == 0)
            return null;
        
        Pointer<cl_event> event_wait_list = allocateArray(cl_event.class, nonNulls);
        int iDest = 0;
		for (int i = 0; i < n; i++) {
            CLEvent event = events[i];
            if (event == null || event.getEntity() == null)
                continue;
            event_wait_list.set(iDest, event.getEntity());
        }
		return event_wait_list;	
	}

	@Override
	protected void clear() {
		error(CL.clReleaseEvent(getEntity()));
	}

	/** Values for CL_EVENT_COMMAND_EXECUTION_STATUS */
	public enum CommandExecutionStatus {
		/** command has been enqueued in the command-queue                                             */ 
		@EnumValue(CL_QUEUED	) Queued	,
		/** enqueued command has been submitted by the host to the device associated with the command-queue */ 
		@EnumValue(CL_SUBMITTED ) Submitted ,
		/** device is currently executing this command */ 
		@EnumValue(CL_RUNNING	) Running	,
		/** the command has completed */ 
		@EnumValue(CL_COMPLETE	) Complete	;
		
		public long getValue() { return EnumValues.getValue(this); }
		public static CommandExecutionStatus getEnum(long v) { return EnumValues.getEnum(v, CommandExecutionStatus.class); }
	}
	
	/**
	 * Return the execution status of the command identified by event.  <br/>
	 * @throws CLException is the execution status denotes an error
	 */
	@InfoName("CL_EVENT_COMMAND_EXECUTION_STATUS")
	public CommandExecutionStatus getCommandExecutionStatus() {
		int v = infos.getInt(getEntity(), CL_EVENT_COMMAND_EXECUTION_STATUS);
		CommandExecutionStatus status =  CommandExecutionStatus.getEnum(v);
		if (status == null)
			error(v);
		return status;
	}

	/** Values for CL_EVENT_COMMAND_TYPE */
	public enum CommandType {
		@EnumValue(CL_COMMAND_NDRANGE_KERNEL		) NDRangeKernel,
		@EnumValue(CL_COMMAND_TASK					) Task,
		@EnumValue(CL_COMMAND_NATIVE_KERNEL			) NativeKernel,
		@EnumValue(CL_COMMAND_READ_BUFFER			) ReadBuffer,
		@EnumValue(CL_COMMAND_WRITE_BUFFER			) WriteBuffer,
		@EnumValue(CL_COMMAND_COPY_BUFFER			) CopyBuffer,
		@EnumValue(CL_COMMAND_READ_IMAGE 			) ReadImage,
		@EnumValue(CL_COMMAND_WRITE_IMAGE			) WriteImage,
		@EnumValue(CL_COMMAND_COPY_IMAGE			) CopyImage,
		@EnumValue(CL_COMMAND_COPY_BUFFER_TO_IMAGE	) CopyBufferToImage,
		@EnumValue(CL_COMMAND_COPY_IMAGE_TO_BUFFER  ) CopyImageToBuffer,
		@EnumValue(CL_COMMAND_MAP_BUFFER 			) MapBuffer,
		@EnumValue(CL_COMMAND_MAP_IMAGE             ) CommandMapImage,
		@EnumValue(CL_COMMAND_UNMAP_MEM_OBJECT		) UnmapMemObject,             
		@EnumValue(CL_COMMAND_MARKER             	) Marker,             
		@EnumValue(CL_COMMAND_ACQUIRE_GL_OBJECTS    ) AcquireGLObjects,             
		@EnumValue(CL_COMMAND_RELEASE_GL_OBJECTS	) ReleaseGLObjects;

		public long getValue() { return EnumValues.getValue(this); }
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
