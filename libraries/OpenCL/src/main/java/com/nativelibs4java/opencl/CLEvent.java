package com.nativelibs4java.opencl;
import com.nativelibs4java.opencl.library.OpenCLLibrary;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;
import com.sun.jna.*;
import com.sun.jna.ptr.*;
import java.nio.*;
import static com.nativelibs4java.opencl.OpenCL4Java.*;

/**
 * OpenCL event object.<br/>
 * Event objects can be used to refer to a kernel execution command (clEnqueueNDRangeKernel, clEnqueueTask, clEnqueueNativeKernel), or read, write, map and copy commands on memory objects (clEnqueue{Read|Write|Map}{Buffer|Image}, clEnqueueCopy{Buffer|Image}, clEnqueueCopyBufferToImage, or clEnqueueCopyImageToBuffer).<br/>
 * An event object can be used to track the execution status of a command. The API calls that enqueue commands to a command-queue create a new event object that is returned in the event argument. In case of an error enqueuing the command in the command-queue the event argument does not return an event object.<br/>
 * The execution status of an enqueued command at any given point in time can be CL_QUEUED (command has been enqueued in the command-queue), CL_SUBMITTED (enqueued command has been submitted by the host to the device associated with the command-queue), CL_RUNNING (device is currently executing this command), CL_COMPLETE (command has successfully completed) or the appropriate error code if the command was abnormally terminated (this may be caused by a bad memory access etc.). The error code returned by a terminated command is a negative integer value. A command is considered to be complete if its execution status is CL_COMPLETE or is a negative integer value.<br/>
 * If the execution of a command is terminated, the command-queue associated with this terminated command, and the associated context (and all other command-queues in this context) may no longer be available. The behavior of OpenCL API calls that use this context (and command-queues associated with this context) are now considered to be implementation- defined. The user registered callback function specified when context is created can be used to report appropriate error information.<br/>
 * 
 * @author ochafik
 */
public class CLEvent extends CLEntity<cl_event> {

	static CLInfoGetter<cl_event> infos = new CLInfoGetter<cl_event>() {
		@Override
		protected int getInfo(cl_event entity, int infoTypeEnum, NativeLong size, Pointer out, NativeLongByReference sizeOut) {
			return CL.clGetEventInfo(entity, infoTypeEnum, size, out, sizeOut);
		}
	};
	
	CLEvent(cl_event evt) {
		super(evt);
	}
	
	@Override
	protected void clear() {
		CL.clReleaseEvent(get());
	}

	/**
	 * Return the execution status of the command identified by event.  <br/>
	 * Valid values are:
	 * <ul>
	 * <li>CL_QUEUED (command has been enqueued in the command-queue),</li>
	 * <li>CL_SUBMITTED (enqueued command has been submitted by the host to the device associated with the command-queue),</li>
	 * <li>CL_RUNNING (device is currently executing this command),</li>
	 * <li>CL_COMPLETE (the command has completed), or</li>
	 * <li>Error code given by a negative integer value. (command was abnormally terminated Ð this may be caused by a bad memory access etc.).</li>
	 * </ul>
	 */
	public int getCommandExecutionStatus() {
		return infos.getInt(get(), CL_EVENT_COMMAND_EXECUTION_STATUS);
	}

	/**
	 * Return the command associated with event. <br/>
	 * Can be one of the following values:
	 * <ul>
	 * <li>CL_COMMAND_NDRANGE_KERNEL</li>
	 * <li>CL_COMMAND_TASK</li>
	 * <li>CL_COMMAND_NATIVE_KERNEL</li>
	 * <li>CL_COMMAND_READ_BUFFER</li>
	 * <li>CL_COMMAND_WRITE_BUFFER</li>
	 * <li>CL_COMMAND_COPY_BUFFER</li>
	 * <li>CL_COMMAND_READ_IMAGE</li>
	 * <li>CL_COMMAND_WRITE_IMAGE</li>
	 * <li>CL_COMMAND_COPY_IMAGE</li>
	 * <li>CL_COMMAND_COPY_BUFFER_TO_IMAGE</li>
	 * <li>CL_COMMAND_COPY_IMAGE_TO_BUFFER           </li>
	 * <li>CL_COMMAND_MAP_BUFFER CL_COMMAND_MAP_IMAGE</li>
	 * <li>CL_COMMAND_UNMAP_MEM_OBJECT               </li>
	 * <li>CL_COMMAND_MARKER                         </li>
	 * <li>CL_COMMAND_ACQUIRE_GL_OBJECTS             </li>
	 * <li>CL_COMMAND_RELEASE_GL_OBJECTS             </li>
	 * </ul>
	 */
	public int getCommandType() {
		return infos.getInt(get(), CL_EVENT_COMMAND_TYPE);
	}
	
}
